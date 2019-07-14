package org.clay.cluster.backend

import akka.actor.{Actor, RootActorPath}
import akka.cluster.ClusterEvent.{CurrentClusterState, MemberEvent, MemberUp}
import akka.cluster.{Cluster, Member, MemberStatus}
import akka.util.Timeout

import scala.concurrent.duration._
import org.clay.cluster.message.{BackendRegistration, TransformationJob, TransformationResult}

import scala.util.{Failure, Success}

class TransformationBackend extends Actor{

  val cluster = Cluster(context.system)

  override def preStart(): Unit = cluster.subscribe(self,classOf[MemberEvent]) //在启动Actor时将该节点订阅到集群中
  override def postStop(): Unit = cluster.unsubscribe(self)

  override def receive: Receive = {
    case TransformationJob(text) =>
      val result = text.toUpperCase  // 任务执行得到结果（将字符串转换为大写）
      sender() ! TransformationResult(result)  // 向发送者返回结果

    case state:CurrentClusterState => {
      //println(s"=====> CurrentClusterState() size: ${state.members.size}")
      //state.members.foreach(x => println(s"====>${x}'s state is ${x.status}"))
      state.members.filter(_.status == MemberStatus.Up) foreach register
    }
    case MemberUp(m) => register(m)   // 将刚处于Up状态的节点向集群客户端注册
  }

  def register(member: Member) = {   //将节点注册到集群客户端
    println(s"=====>member: ${member} change state to up, begin register, address: ${member.address}")
    val actorSelection = context.actorSelection(RootActorPath(member.address) /  "user" / "frontend")
    implicit val timeout = Timeout(3 seconds)
    implicit val executionContext = context.dispatcher

    //println(s"=====> actorSelection's ref is ${actorSelection.resolveOne()}")
    actorSelection.resolveOne().onComplete{
      case Success(value) =>
        println(s"xxxxxxx success: $value")
      case Failure(exception) =>
        println(s"xxxxxxx failure: $exception")
    }
    actorSelection ! BackendRegistration
  }
}
