package org.clay.AkkaTest

import akka.actor.{Actor, ActorSystem, Props}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import akka.pattern.ask

import scala.concurrent.duration._

object LocalActorTest extends App {

  val system = ActorSystem("LocalSystem")
  val localActor = system.actorOf(Props[LocalActor],name = "LocalActor")

  localActor ! Init
  localActor ! SendNoReturn
  //localActor ! SendHasReturn
  //localActor ! SendSerialization

}

case object Init
case object SendNoReturn
case object SendHasReturn
case object SendSerialization

case class JoinEvt(id:Long,name:String)

class LocalActor extends Actor{

  val path = ConfigFactory.defaultApplication().getString("remote.actor.name.test")
  implicit val timeout = Timeout(4 seconds)

  //获取ActorSelection对象，要获取ActorRef，只需要调用resolveOne() 方法
  val remoteActor = context.actorSelection(path)

  implicit val executionContext = context.dispatcher

  override def aroundReceive(receive: Receive, msg: Any): Unit = {
    println(s"调用aroundReceive方法，msg is: ${msg}")

    val new_message = SendHasReturn //全部转换成SendHasReturn消息，生效，也就是说这个方法在receive方法之前，可以进行拦截

    super.aroundReceive(receive, new_message)
  }

  override def receive: Receive = {
    case Init => "init local actor"
    case SendNoReturn => remoteActor ! "hello remote actor"
    case SendHasReturn =>
      for(
        r <- remoteActor.ask("hello remote actor")
      ) yield println(r)

    case SendSerialization =>
      for(
        r <- remoteActor.ask(JoinEvt(1L,"clay"))
      ) yield println(r)
  }
}