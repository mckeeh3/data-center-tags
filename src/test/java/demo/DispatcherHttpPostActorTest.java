package demo;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.http.javadsl.model.HttpResponse;
import akka.testkit.javadsl.TestKit;
import demo.DispatcherProtocol.Entity;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class DispatcherHttpPostActorTest {
    private static ActorSystem actorSystem;

    @Test
    public void t() {
        new TestKit(actorSystem) {{
            final ActorRef dispatcherHttpPost = actorSystem.actorOf(DispatcherHttpPostActor.props(), "dispatcherHttpPost");

            final TestKit probe = new TestKit(actorSystem);
            Entity entity = new Entity(Entity.tag("tag-x"), Entity.id("id-x"));

            dispatcherHttpPost.tell(entity, getRef());

            DispatcherHttpPostActor.Response response = (DispatcherHttpPostActor.Response) receiveOne(duration("5s"));
            System.out.println(response);
        }};
    }

    @BeforeClass
    public static void setup() {
        actorSystem = ActorSystem.create();
    }

    @AfterClass
    public static void tearDown() {
        TestKit.shutdownActorSystem(actorSystem);
        actorSystem = null;
    }
}
