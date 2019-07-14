package org.clay.AkkaTest

import akka.actor.{Actor, ActorSystem, Props}

object RemoteActorMain extends App {

  val system = ActorSystem("RemoteSystem")
  val remoteActor = system.actorOf(Props[RemoteActor],name = "RemoteActor")
}

class RemoteActor extends Actor{

  override def receive: Receive = {
    case msg:String =>
      println(s"RemoteActor received message: '$msg'")
      sender() ! "Hello from the RemoteActor"
  }
}