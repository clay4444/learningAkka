import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.{Cluster, Member}
import akka.cluster.ClusterEvent.{InitialStateAsEvents, MemberEvent, MemberUp, UnreachableMember}

abstract class ClusterRoledWorker extends Actor with ActorLogging{

  // 创建一个Cluster实例
  val cluster = Cluster(context.system)

  // 用来缓存下游注册过来的子系统ActorRef
  var workers = IndexedSeq.empty[ActorRef]

  override def preStart(): Unit = {
    // 订阅集群事件
    cluster.subscribe(self,initialStateMode = InitialStateAsEvents,
      classOf[MemberUp],classOf[UnreachableMember],classOf[MemberEvent])
  }

  override def postStop(): Unit = {
    cluster.subscribe(self)
  }

  def register(member:Member,): Unit ={

  }
}
