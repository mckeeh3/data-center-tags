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
    private ActorRef dispatcherQuereyActor;

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
        startQueryActorIfNotStarted(queryCycleTick.eventTag);
        dispatcherQuereyActor.tell("query", getSelf());
    }

    private void heartbeatTimedOut() {
        getContext().getParent().tell(new ShardRegion.Passivate(PoisonPill.getInstance()), getSelf());
    }

    static Props props() {
        return Props.create(DispatcherActor.class);
    }

    @Override
    public void preStart() {
        log().debug("Start");
        scheduleHeartbeatTimeout();
    }

    @Override
    public void postStop() {
        log().debug("Stop");
        queryCycle.cancel();
        heartbeatTimeout.cancel();
    }

    private void scheduleQueryCycle(EventTag eventTag) {
        if (queryCycle == null) {
            FiniteDuration queryCycleTime = queryCycleTime();

            queryCycle = getContext().getSystem().scheduler().schedule(
                    queryCycleTime,
                    queryCycleTime,
                    getSelf(),
                    new QueryCycleTick(eventTag),
                    getContext().dispatcher(),
                    ActorRef.noSender()
            );
        }
    }

    private void startQueryActorIfNotStarted(EventTag eventTag) {
        if (dispatcherQuereyActor == null) {
            dispatcherQuereyActor = startQueryActor(eventTag);
        }
    }

    private ActorRef startQueryActor(EventTag eventTag) {
        FiniteDuration minBackOff = FiniteDuration.create(5, TimeUnit.SECONDS);
        FiniteDuration maxBackOff = FiniteDuration.create(30, TimeUnit.SECONDS);

        return getContext().actorOf(
                BackoffSupervisor.props(
                        DispatcherQueryActor.props(eventTag),
                        String.format("dispatcherQuery-%s", eventTag.id.value),
                        minBackOff,
                        maxBackOff,
                        0.2
                ));
    }

    private void scheduleHeartbeatTimeout() {
        if (heartbeatTimeout != null) {
            heartbeatTimeout.cancel();
        }

        heartbeatTimeout = getContext().getSystem().scheduler().scheduleOnce(
                heartbeatTimeout(),
                getSelf(),
                "heartbeatTimeout",
                getContext().dispatcher(),
                ActorRef.noSender()
        );
    }

    private FiniteDuration queryCycleTime() {
        Duration queryCycleTime = retrieveTimeSetting("demo.dispatcher.query-cycle-time");

        return FiniteDuration.create(queryCycleTime.toNanos(), TimeUnit.NANOSECONDS);
    }

    private FiniteDuration heartbeatTimeout() {
        Duration tickInterval = retrieveTimeSetting("demo.dispatcher.heartbeat-timeout");

        return FiniteDuration.create(tickInterval.toNanos(), TimeUnit.NANOSECONDS);
    }

    private Duration retrieveTimeSetting(String timeSettingPath) {
        try {
            return getContext().getSystem().settings().config().getDuration(timeSettingPath);
        } catch (Exception e) {
            int defaultSeconds = 60;
            log().error(e, "Configuration setting '{}' not found, using {}s default", timeSettingPath, defaultSeconds);
            return Duration.ofSeconds(defaultSeconds);
        }
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
