package demo;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;

class DispatcherHttpPostActor extends AbstractLoggingActor {
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .build();
    }

    @Override
    public void preStart() {
        log().debug("Start");
    }

    @Override
    public void postStop() {
        log().debug("Stop");
    }

    static Props props() {
        return Props.create(DispatcherHttpPostActor.class);
    }
}
