package org.clay.test1

import akka.actor.{ActorSystem, Props, Status}
import akka.testkit.{TestActorRef, TestProbe}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.clay.DBClient.HotswapClientActor
import org.clay.DBServer.AkkademyDb
import org.clay.DBServer.messages.{GetRequest, SetRequest}
import org.scalatest.{FunSpec, Matchers}

import scala.concurrent.duration._

class HotswapClientActorSpec extends FunSpec with Matchers{

  implicit val system = ActorSystem("test-system",ConfigFactory.defaultReference())
  implicit val timeout = Timeout(5 seconds)

  describe("HotswapClientActor"){
    it("should set value"){
      val dbRef = TestActorRef[AkkademyDb]   //服务端
      val db = dbRef.underlyingActor
      val probe = TestProbe()   //使用客户端的actor
      val clientRef = TestActorRef(Props.create(classOf[HotswapClientActor],dbRef.path.toString))  //客户端

      clientRef ! new SetRequest("testKey","testvalue",probe.ref)
      probe.expectMsg(Status.Success)
    }

    it("should get value"){
      val dbRef = TestActorRef[AkkademyDb]
      val db = dbRef.underlyingActor
      db.map.put("testkey", "testvalue")

      val probe = TestProbe()
      val clientRef = TestActorRef(Props.create(classOf[HotswapClientActor], dbRef.path.toString))

      clientRef ! new GetRequest("testkey", probe.ref)
      probe.expectMsg("testvalue")
    }
  }
}
