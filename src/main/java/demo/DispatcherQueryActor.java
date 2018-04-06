package demo;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.pattern.BackoffSupervisor;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.JavaFlowSupport;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.Source$;
import akka.util.Timeout;
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
    private Cancellable postEntityTimeoutSchedule;
    private List<Entity> entities = new ArrayList<>();
    private final Materializer materializer = ActorMaterializer.create(context().system());

    {
        idle = receiveBuilder()
                .matchEquals("query", q -> queryIdle())
                .build();

        processing = receiveBuilder()
                .matchEquals("query", q -> queryProcessing())
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
        getContext().become(processing);
    }

    private void query() {
        // todo make this real
        entities = new ArrayList<>();
        for (int e = 1; e <= 10; e++) {
            entities.add(new Entity(Entity.tag(eventTag.id.value), Entity.id(e + "")));
        }

        Source.from(entities)
                .ask(1, dispatcherHttpPost, Entity.class, Timeout.apply(5, TimeUnit.SECONDS))
                .map(r -> {
                    log().debug("Entity post response {}", r);
                    return r;
                })
                .runWith(Sink.ignore(), materializer);

        log().debug("Query found {} rows for {}", entities.size(), eventTag);
    }

    private void queryProcessing() {
        log().debug("Still processing query results ({}) {}", entities.size(), eventTag);
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
}
