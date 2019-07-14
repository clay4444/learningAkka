package org.clay.cluster.runApp

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{ActorSystem, Props}
import org.agrona.concurrent.status.AtomicCounter
import org.clay.cluster.backend.TransformationBackendApp
import org.clay.cluster.frontend.TransformationFrontendApp

import scala.concurrent.duration._
import scala.io.StdIn

object DemoClient extends App {

  TransformationFrontendApp.main(Seq("2551").toArray)   //启动集群客户端
  TransformationBackendApp.main(Seq("8001").toArray)    //启动三个后台节点
  TransformationBackendApp.main(Seq("8002").toArray)
  TransformationBackendApp.main(Seq("8003").toArray)

  val system = ActorSystem("OTHERSYSTEM")
  val clientJobTransformationSendingActor =
    system.actorOf(Props[ClientJobTransformationSendingActor],name="clientJobTransformationSendingActor")

  val counter = new AtomicInteger
  import system.dispatcher
  system.scheduler.schedule(2.seconds,2.seconds){   //定时发送任务
    //println(s"=====>sent message: hello-${counter}")
    clientJobTransformationSendingActor ! Send(counter.incrementAndGet())
  }
  StdIn.readLine()
  system.terminate()
}
