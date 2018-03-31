package demo;

import akka.actor.*;
import akka.cluster.sharding.ShardRegion;
import scala.concurrent.duration.FiniteDuration;

import java.io.Serializable;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

class DispatcherActor extends AbstractLoggingActor {
    private Cancellable queryCycle;
    private Cancellable heartbeatTimeout;

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
        scheduleQueryCycle(eventTagGo.eventTag.id);
    }

    private void query(QueryCycleTick queryCycleTick) {
        log().debug("TODO {}", queryCycleTick);
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

    private void scheduleQueryCycle(EventTag.Id id) {
        if (queryCycle == null) {
            FiniteDuration queryCycleTime = queryCycleTime();

            queryCycle = getContext().getSystem().scheduler().schedule(
                    queryCycleTime,
                    queryCycleTime,
                    getSelf(),
                    new QueryCycleTick(id),
                    getContext().dispatcher(),
                    ActorRef.noSender()
            );
        }
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
        final EventTag.Id id;

        private QueryCycleTick(EventTag.Id id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return String.format("%s[%s]", getClass().getSimpleName(), id);
        }
    }
}
