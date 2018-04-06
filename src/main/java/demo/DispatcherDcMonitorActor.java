package demo;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import scala.concurrent.duration.FiniteDuration;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static demo.DataCenter.*;

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
        log().debug("heartbeat");
        queryDataCenters();
    }

    private void heartbeat(EventTag eventTag) {
        shardRegion.tell(new DispatcherProtocol.EventTagGo(eventTag), self());
    }

    private void queryDataCenters() {
        queryDataCentersDb().thenAccept(this::queryDataCenters);
    }

    private void queryDataCenters(List<DataCenter> dataCenterList) {
        final DataCenters dataCenters = DataCenters.create().addAll(dataCenterList);

        dataCenters.tagsFor(dataCenterName).forEach(this::heartbeat);
    }

    private CompletionStage<List<DataCenter>> queryDataCentersDb() {
        // todo read data center status from database
        List<DataCenter> dataCenters = new ArrayList<>();
        if (LocalTime.now().getMinute() % 3 == 0) {
            dataCenters.add(DataCenter.create(name("dc1"), status(Status.Is.up), runner(Runner.Is.on), tagCount(10)));
            dataCenters.add(DataCenter.create(name("dc2"), status(Status.Is.up), runner(Runner.Is.on), tagCount(10)));
            dataCenters.add(DataCenter.create(name("dc3"), status(Status.Is.up), runner(Runner.Is.off), tagCount(10)));
        } else if (LocalTime.now().getMinute() % 2 == 0) {
            dataCenters.add(DataCenter.create(name("dc1"), status(Status.Is.up), runner(Runner.Is.on), tagCount(10)));
            dataCenters.add(DataCenter.create(name("dc2"), status(Status.Is.down), runner(Runner.Is.on), tagCount(10)));
            dataCenters.add(DataCenter.create(name("dc3"), status(Status.Is.up), runner(Runner.Is.on), tagCount(10)));
        } else {
            dataCenters.add(DataCenter.create(name("dc1"), status(Status.Is.up), runner(Runner.Is.on), tagCount(10)));
            dataCenters.add(DataCenter.create(name("dc2"), status(Status.Is.up), runner(Runner.Is.on), tagCount(10)));
            dataCenters.add(DataCenter.create(name("dc3"), status(Status.Is.up), runner(Runner.Is.on), tagCount(10)));
        }

        return CompletableFuture.completedFuture(dataCenters);
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
