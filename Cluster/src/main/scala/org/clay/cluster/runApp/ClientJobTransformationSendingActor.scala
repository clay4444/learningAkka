package org.clay.cluster.runApp

import akka.actor.{Actor, ActorPath}
import akka.cluster.client.{ClusterClient, ClusterClientSettings}
import akka.cluster.pubsub.protobuf.msg.DistributedPubSubMessages.Send
import akka.util.Timeout
import org.clay.cluster.message.{TransformationJob, TransformationResult}

import scala.concurrent.duration._
import akka.pattern.{Patterns, ask, pipe}

import scala.util.{Failure, Success}

/**
  * 与集群客户端进行交互
  */
class ClientJobTransformationSendingActor extends Actor{

  val initialContacts = Set(
    ActorPath.fromString("akka.tcp://ClusterSystem@127.0.0.1:2551/system/receptionist")
  )
  val settings = ClusterClientSettings(context.system).withInitialContacts(initialContacts)

  val c = context.system.actorOf(ClusterClient.props(settings),"demo-client")



  override def receive: Receive = {

    case TransformationResult(result) =>
      println(s"=======>Client response and the result is ${result}")

    case Send(counter) =>
      //println(s"====>receive hello counter message: hello-${counter}")
      val job = TransformationJob(s"hello-${counter}")
      implicit val timeout = Timeout(5 second)
      implicit val executionContext = context.dispatcher
      val result = Patterns.ask(c,ClusterClient.Send("/user/frontend",job,localAffinity = true), timeout)

      result onComplete{
        case Success(transformationResult) => {
          self ! transformationResult
        }
        case Failure(t) => {
          println("====>An error has occured: " + t.getMessage)
        }
      }
  }
}