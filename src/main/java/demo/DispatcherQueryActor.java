package demo;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;

public class DispatcherQueryActor extends AbstractLoggingActor {
    private final EventTag eventTag;

    public DispatcherQueryActor(EventTag eventTag) {
        this.eventTag = eventTag;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchEquals("query", q -> query())
                .build();
    }

    private void query() {
        log().debug("TODO query {}", eventTag);
    }

    @Override
    public void preStart() {
        log().debug("Start {}", eventTag);
    }

    @Override
    public void postStop() {
        log().debug("Stop {}", eventTag);
    }

    static Props props(EventTag eventTag) {
        return Props.create(DispatcherQueryActor.class, eventTag);
    }
}
