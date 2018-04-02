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

public class DispatcherDcMonitorActor extends AbstractLoggingActor {
    private final ActorRef shardRegion;
    private Cancellable heartbeat;
    private DataCenter.Name dataCenterName;

    public DispatcherDcMonitorActor(ActorRef shardRegion) {
        this.shardRegion = shardRegion;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchEquals("heartbeat", h -> heartbeat())
                .build();
    }

    private void heartbeat() {
        log().info("heartbeat");
        DataCenters dataCenters = readDataCenters();
        dataCenters.tagsFor(dataCenterName).forEach(this::dispatcherHeartbeat);
    }

    private void dispatcherHeartbeat(EventTag eventTag) {
        shardRegion.tell(new DispatcherProtocol.EventTagGo(eventTag), self());
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
        return Props.create(DispatcherDcMonitorActor.class, shardRegion);
    }

    @Override
    public void preStart() {
        log().debug("Start, shard region {}", shardRegion);

        retrieveDataCenterName();
        scheduleHeartbeat();
    }

    @Override
    public void postStop() {
        log().debug("Stop");
        cancelHeartbeat();
    }

    private void scheduleHeartbeat() {
        FiniteDuration tickInterval = heartbeatInterval();

        heartbeat = context().system().scheduler().schedule(
                tickInterval,
                tickInterval,
                self(),
                "heartbeat",
                context().dispatcher(),
                ActorRef.noSender()
        );
    }

    private void cancelHeartbeat() {
        if (heartbeat != null) {
            heartbeat.cancel();
            heartbeat = null;
        }
    }

    private FiniteDuration heartbeatInterval() {
        Duration tickInterval = context().system().settings().config().getDuration("dispatcher.heartbeat-interval");
        return FiniteDuration.create(tickInterval.toNanos(), TimeUnit.NANOSECONDS);
    }

    private void retrieveDataCenterName() {
        String dataCenterConfig = "akka.cluster.multi-data-center.self-data-center";
        dataCenterName = DataCenter.name(context().system().settings().config().getString(dataCenterConfig));
    }
}
