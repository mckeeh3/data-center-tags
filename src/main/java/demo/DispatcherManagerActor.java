package demo;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import scala.concurrent.duration.FiniteDuration;

import java.time.Duration;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

import static demo.DataCenter.name;
import static demo.DataCenter.status;
import static demo.DataCenter.tagCount;

public class DispatcherManagerActor extends AbstractLoggingActor {
    private final ActorRef shardRegion;
    private Cancellable heartbeat;
    private DataCenter.Name dataCenterName;

    public DispatcherManagerActor(ActorRef shardRegion) {
        this.shardRegion = shardRegion;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchEquals("tick", t -> tick())
                .build();
    }

    private void tick() {
        log().info("tick");
        DataCenters dataCenters = readDataCenters();
        dataCenters.tagsFor(dataCenterName).forEach(this::dispatcherHeartbeat);
    }

    private void dispatcherHeartbeat(EventTag eventTag) {
        shardRegion.tell(new DispatcherProtocol.EventTagGo(eventTag), getSelf());
    }

    private DataCenters readDataCenters() {
        // todo read data center status from database
        if (LocalTime.now().getMinute() % 2 == 0) {
            DataCenter dataCenter1 = DataCenter.create(name("dc1"), status(DataCenter.Status.Is.up), tagCount(10));
            DataCenter dataCenter2 = DataCenter.create(name("dc2"), status(DataCenter.Status.Is.up), tagCount(10));
            DataCenter dataCenter3 = DataCenter.create(name("dc3"), status(DataCenter.Status.Is.up), tagCount(10));

            return DataCenters.create().add(dataCenter1).add(dataCenter2).add(dataCenter3);
        } else {
            DataCenter dataCenter1 = DataCenter.create(name("dc1"), status(DataCenter.Status.Is.up), tagCount(10));
            DataCenter dataCenter2 = DataCenter.create(name("dc2"), status(DataCenter.Status.Is.down), tagCount(10));
            DataCenter dataCenter3 = DataCenter.create(name("dc3"), status(DataCenter.Status.Is.up), tagCount(10));

            return DataCenters.create().add(dataCenter1).add(dataCenter2).add(dataCenter3);
        }
    }

    static Props props(ActorRef shardRegion) {
        return Props.create(DispatcherManagerActor.class, shardRegion);
    }

    @Override
    public void preStart() {
        log().debug("Start, shard region {}", shardRegion);

        retrieveDataCenterName();
        scheduleHeartbeats();
    }

    @Override
    public void postStop() {
        log().debug("Stop");
        heartbeat.cancel();
    }

    private void scheduleHeartbeats() {
        FiniteDuration tickInterval = tickInterval();

        heartbeat = getContext().getSystem().scheduler().schedule(
                tickInterval,
                tickInterval,
                getSelf(),
                "tick",
                getContext().dispatcher(),
                ActorRef.noSender()
        );
    }

    private FiniteDuration tickInterval() {
        Duration tickInterval = retrieveHeartbeatInterval();

        return FiniteDuration.create(tickInterval.toNanos(), TimeUnit.NANOSECONDS);
    }

    private Duration retrieveHeartbeatInterval() {
        String heartbeatIntervalConfig = "demo.dispatcher.heartbeat-interval";
        try {
            return getContext().getSystem().settings().config().getDuration(heartbeatIntervalConfig);
        } catch (Exception e) {
            int defaultSeconds = 60;
            log().error(e, "Configuration setting '{}' not found, using {}s default", heartbeatIntervalConfig, defaultSeconds);
            return Duration.ofSeconds(defaultSeconds);
        }
    }

    private void retrieveDataCenterName() {
        String dataCenterConfig = "akka.cluster.multi-data-center.self-data-center";
        dataCenterName = DataCenter.name(getContext().getSystem().settings().config().getString(dataCenterConfig));
    }
}
