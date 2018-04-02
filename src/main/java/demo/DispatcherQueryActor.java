package demo;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.pattern.BackoffSupervisor;
import demo.DispatcherProtocol.Entity;
import scala.concurrent.duration.FiniteDuration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

class DispatcherQueryActor extends AbstractLoggingActor {
    private final EventTag eventTag;
    private final Receive idle;
    private final Receive processing;
    private ActorRef dispatcherHttpPost;
    private FiniteDuration postEntityTimeoutDuration;
    private Cancellable postEntityTimeoutSchedule;
    private List<Entity> entities = new ArrayList<>();

    {
        idle = receiveBuilder()
                .matchEquals("query", q -> queryIdle())
                .build();

        processing = receiveBuilder()
                .matchEquals("query", q -> queryProcessing())
                .match(DispatcherHttpPostActor.Response.class, this::httpPostResponse)
                .matchEquals("sendEntityTimeout", q -> sendEntityTimeout())
                .build();
    }

    public DispatcherQueryActor(EventTag eventTag) {
        this.eventTag = eventTag;
    }

    @Override
    public Receive createReceive() {
        return idle;
    }

    private void queryIdle() {
        query();
        sendNextEntity();
        getContext().become(processing);
    }

    private void query() {
        // todo make this real
        entities = new ArrayList<>();
        for (int e = 1; e <= 10; e++) {
            entities.add(new Entity(Entity.tag(eventTag.id.value), Entity.id(e + "")));
        }

        log().debug("Query found {} rows for {}", entities.size(), eventTag);
    }

    private void queryProcessing() {
        log().debug("Still processing query results ({}) {}", entities.size(), eventTag);
    }

    private void sendNextEntity() {
        if (entities.isEmpty()) {
            getContext().become(idle);
        } else {
            dispatcherHttpPost.tell(entities.remove(0), self());
            schedulePostEntityTimeout();
        }
    }

    private void httpPostResponse(DispatcherHttpPostActor.Response response) {
        log().debug("Entity post response {}", response);
        cancelPostEntityTimeout();
        sendNextEntity();
    }

    private void sendEntityTimeout() {
        log().warning("No response from last send entity request");
        sendNextEntity();
    }

    private void schedulePostEntityTimeout() {
        cancelPostEntityTimeout();

        postEntityTimeoutSchedule = context().system().scheduler().scheduleOnce(
                postEntityTimeoutDuration,
                self(),
                "sendEntityTimeout",
                context().dispatcher(),
                ActorRef.noSender()
        );
    }

    private void cancelPostEntityTimeout() {
        if (postEntityTimeoutSchedule != null) {
            postEntityTimeoutSchedule.cancel();
            postEntityTimeoutSchedule = null;
        }
    }

    private ActorRef startHttpActor() {
        FiniteDuration minBackOff = FiniteDuration.create(5, TimeUnit.SECONDS);
        FiniteDuration maxBackOff = FiniteDuration.create(30, TimeUnit.SECONDS);

        return context().actorOf(
                BackoffSupervisor.props(
                        DispatcherHttpPostActor.props(),
                        "entityHttpPost",
                        minBackOff,
                        maxBackOff,
                        0.2
                ), "supervisor");
    }

    @Override
    public void preStart() {
        log().debug("Start {}", eventTag);
        postEntityTimeoutDuration = retrievePostEntityTimeout();
        dispatcherHttpPost = startHttpActor();
    }

    @Override
    public void postStop() {
        log().debug("Stop {}", eventTag);
        cancelPostEntityTimeout();
    }

    static Props props(EventTag eventTag) {
        return Props.create(DispatcherQueryActor.class, eventTag);
    }

    private FiniteDuration retrievePostEntityTimeout() {
        Duration tickInterval = context().system().settings().config().getDuration("dispatcher.query-post-entity-timeout");
        return FiniteDuration.create(tickInterval.toNanos(), TimeUnit.NANOSECONDS);
    }
}
