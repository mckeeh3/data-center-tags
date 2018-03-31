package demo;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.cluster.sharding.ClusterSharding;
import akka.cluster.sharding.ClusterShardingSettings;
import akka.cluster.singleton.ClusterSingletonManager;
import akka.cluster.singleton.ClusterSingletonManagerSettings;

class DispatcherBootstrap {
    private DispatcherBootstrap(ActorSystem actorSystem) {
        createClusterSingletonManagerActor(actorSystem);
    }

    static void run(ActorSystem actorSystem) {
        new DispatcherBootstrap(actorSystem);
    }

    private static void createClusterSingletonManagerActor(ActorSystem actorSystem) {
        Props clusterSingletonManagerProps = ClusterSingletonManager.props(
                DispatcherDcMonitorActor.props(setupClusterSharding(actorSystem)),
                PoisonPill.getInstance(),
                ClusterSingletonManagerSettings.create(actorSystem)
        );

        actorSystem.actorOf(clusterSingletonManagerProps, "dispatcherManager");
    }

    private static ActorRef setupClusterSharding(ActorSystem actorSystem) {
        ClusterShardingSettings settings = ClusterShardingSettings.create(actorSystem);
        return ClusterSharding.get(actorSystem).start(
                "dispatcher",
                DispatcherActor.props(),
                settings,
                DispatcherProtocol.messageExtractor()
        );
    }
}
