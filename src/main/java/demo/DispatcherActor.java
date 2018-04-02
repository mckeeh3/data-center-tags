package demo;

import akka.actor.*;
import akka.cluster.sharding.ShardRegion;
import akka.pattern.BackoffSupervisor;
import scala.concurrent.duration.FiniteDuration;

import java.io.Serializable;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

class DispatcherActor extends AbstractLoggingActor {
    private Cancellable queryCycle;
    private Cancellable heartbeatTimeout;
    private ActorRef dispatcherQuery;

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(DispatcherProtocol.EventTagGo.class, this::eventTagGo)
                .match(QueryCycleTick.class, this::query)
                .matchEquals("heartbeatTimeout", t -> heartbeatTimedOut())
                .build();
    }

    private void eventTagGo(DispatcherProtocol.EventTagGo eventTagGo) {
        log().debug("{}", eventTagGo);
        scheduleHeartbeatTimeout();
        scheduleQueryCycle(eventTagGo.eventTag);
    }

    private void query(QueryCycleTick queryCycleTick) {
        log().debug("{}", queryCycleTick);
        dispatcherQuery(queryCycleTick.eventTag).tell("query", self());
    }

    private void heartbeatTimedOut() {
        context().parent().tell(new ShardRegion.Passivate(PoisonPill.getInstance()), self());
    }

    static Props props() {
        return Props.create(DispatcherActor.class);
    }

    @Override
    public void preStart() {
        log().debug("Start");
    }

    @Override
    public void postStop() {
        log().debug("Stop");
        cancelHeartbeatTimeout();
        cancelQueryCycle();
    }

    private void scheduleQueryCycle(EventTag eventTag) {
        if (queryCycle == null) {
            FiniteDuration queryCycleTime = queryCycleTime();

            queryCycle = context().system().scheduler().schedule(
                    queryCycleTime,
                    queryCycleTime,
                    self(),
                    new QueryCycleTick(eventTag),
                    context().dispatcher(),
                    ActorRef.noSender()
            );
        }
    }

    private void cancelQueryCycle() {
        if (queryCycle != null) {
            queryCycle.cancel();
            queryCycle = null;
        }
    }

    private ActorRef dispatcherQuery(EventTag eventTag) {
        if (dispatcherQuery == null) {
            dispatcherQuery = startQueryActor(eventTag);
        }
        return dispatcherQuery;
    }

    private ActorRef startQueryActor(EventTag eventTag) {
        FiniteDuration minBackOff = FiniteDuration.create(5, TimeUnit.SECONDS);
        FiniteDuration maxBackOff = FiniteDuration.create(30, TimeUnit.SECONDS);

        return context().actorOf(
                BackoffSupervisor.props(
                        DispatcherQueryActor.props(eventTag),
                        "query",
                        minBackOff,
                        maxBackOff,
                        0.2
                ), "supervisor");
    }

    private void scheduleHeartbeatTimeout() {
        cancelHeartbeatTimeout();

        heartbeatTimeout = context().system().scheduler().scheduleOnce(
                heartbeatTimeout(),
                self(),
                "heartbeatTimeout",
                context().dispatcher(),
                ActorRef.noSender()
        );
    }

    private void cancelHeartbeatTimeout() {
        if (heartbeatTimeout != null) {
            heartbeatTimeout.cancel();
            heartbeatTimeout = null;
        }
    }

    private FiniteDuration queryCycleTime() {
        Duration queryCycleTime = context().system().settings().config().getDuration("dispatcher.query-cycle-time");
        return FiniteDuration.create(queryCycleTime.toNanos(), TimeUnit.NANOSECONDS);
    }

    private FiniteDuration heartbeatTimeout() {
        Duration tickInterval = context().system().settings().config().getDuration("dispatcher.heartbeat-timeout");
        return FiniteDuration.create(tickInterval.toNanos(), TimeUnit.NANOSECONDS);
    }

    private static class QueryCycleTick implements Serializable {
        final EventTag eventTag;

        private QueryCycleTick(EventTag eventTag) {
            this.eventTag = eventTag;
        }

        @Override
        public String toString() {
            return String.format("%s[%s]", getClass().getSimpleName(), eventTag);
        }
    }
}
