package org.clay.DBServer

import akka.actor.{ActorSystem, Props}

object ServerMain extends App {

  val system = ActorSystem("dbServerSystem")
  val remoteActor = system.actorOf(Props[AkkademyDb],name = "dbServerActor")
}
