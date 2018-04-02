package demo;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import scala.concurrent.duration.FiniteDuration;

import java.io.Serializable;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static akka.pattern.PatternsCS.pipe;

class DispatcherHttpPostActor extends AbstractLoggingActor {
    private final Receive idle;
    private final Receive processing;
    private String postUrl;
    private FiniteDuration postTimeoutDuration;
    private Cancellable postTimeoutSchedule;
    private ActorRef postSender;

    {
        idle = receiveBuilder()
                .match(DispatcherProtocol.Entity.class, this::postRequestIdle)
                .build();

        processing = receiveBuilder()
                .match(HttpResponse.class, this::response)
                .matchEquals("postTimeout", t -> postTimeout())
                .match(DispatcherProtocol.Entity.class, this::postRequestProcessing)
                .build();
    }

    @Override
    public Receive createReceive() {
        return idle;
    }

    private void postRequestIdle(DispatcherProtocol.Entity entity) {
        log().debug("Post request {}", entity);
        postSender = sender();
        getContext().become(processing);
        try {
            pipe(fetch(entity), context().dispatcher()).to(self());
            scheduleHttpPostTimeout();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Entity to Json failed", e);
        }
    }

    private CompletionStage<HttpResponse> fetch(DispatcherProtocol.Entity entity) throws JsonProcessingException {
        Http http = Http.get(context().system());

        return http.singleRequest(HttpRequest.POST(postUrl).withEntity(entityToJson(entity)));
    }

    private void response(HttpResponse httpResponse) {
        log().debug("Post response {}", httpResponse);
        postSender.tell(new Response(httpResponse.status().intValue(), httpResponse.status().reason()), self());
        getContext().become(idle);
        cancelHttpPostTimeout();
    }

    private void postTimeout() {
        log().debug("HTTP Post request timed out after {}", postTimeoutDuration);
        getContext().become(idle);
    }

    private void postRequestProcessing(DispatcherProtocol.Entity entity) {
        log().debug("Already processing HTTP post request for {}", entity);
    }

    private void scheduleHttpPostTimeout() {
        cancelHttpPostTimeout();

        postTimeoutSchedule = context().system().scheduler().scheduleOnce(
                postTimeoutDuration,
                self(),
                "postTimeout",
                context().dispatcher(),
                ActorRef.noSender()
        );
    }

    private void cancelHttpPostTimeout() {
        if (postTimeoutSchedule != null) {
            postTimeoutSchedule.cancel();
            postTimeoutSchedule = null;
        }
    }

    private String entityToJson(DispatcherProtocol.Entity entity) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        return objectMapper.writeValueAsString(entity);
    }

    private String retrievePostUrlSetting() {
        return context().system().settings().config().getString("dispatcher.http-post-entity-url");
    }

    private FiniteDuration retrievePostTimeoutSetting() {
        Duration postTimeout = context().system().settings().config().getDuration("dispatcher.http-post-entity-timeout");
        return FiniteDuration.create(postTimeout.toNanos(), TimeUnit.NANOSECONDS);
    }

    @Override
    public void preStart() {
        log().debug("Start");
        postUrl = retrievePostUrlSetting();
        postTimeoutDuration = retrievePostTimeoutSetting();
    }

    @Override
    public void postStop() {
        log().debug("Stop");
        cancelHttpPostTimeout();
    }

    static Props props() {
        return Props.create(DispatcherHttpPostActor.class);
    }

    static class Response implements Serializable {
        final int httpStatusCode;
        final String httpStatusMessage;

        Response(int httpStatusCode, String httpStatusMessage) {
            this.httpStatusCode = httpStatusCode;
            this.httpStatusMessage = httpStatusMessage;
        }

        @Override
        public String toString() {
            return String.format("%s[%d, %s]", getClass().getSimpleName(), httpStatusCode, httpStatusMessage);
        }
    }
}
