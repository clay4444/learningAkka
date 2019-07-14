package org.clay.AkkaTest

import akka.actor.{Actor, ActorSystem, Props}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

object LocalActorTest extends App {

  val system = ActorSystem("LocalSystem")
  val localActor = system.actorOf(Props[LocalActor],name = "LocalActor")

  localActor ! Init
  localActor ! SendNoReturn

}

case object Init
case object SendNoReturn

class LocalActor extends Actor{

  val path = ConfigFactory.defaultApplication().getString("remote.actor.name.test")
  implicit val timeout = Timeout(4 seconds)

  //获取ActorSelection对象，要获取ActorRef，只需要调用resolveOne() 方法
  val remoteActor = context.actorSelection(path)

  override def receive: Receive = {
    case Init => "init local actor"
    case SendNoReturn => remoteActor ! "hello remote actor"
  }
}