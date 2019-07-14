package org.clay.AkkaTest

import akka.actor.{Actor, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object RemoteActorMain extends App {

  val system = ActorSystem("RemoteSystem")
  val remoteActor = system.actorOf(Props[RemoteActor],name = "RemoteActor")
}

case class JoinEvt(id:Long,name:String)

class RemoteActor extends Actor{

  override def receive: Receive = {
    case evt:JoinEvt => sender() ! s"the ${evt.name} has join"
    case msg:String =>
      println(s"RemoteActor received message: '$msg'")
      sender() ! "Hello from the RemoteActor"
  }
}