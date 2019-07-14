package org.clay.test1

import akka.actor.{ActorSystem, Status}
import akka.testkit.{TestActorRef, TestProbe}
import com.typesafe.config.ConfigFactory
import org.clay.DBServer.AkkademyDb
import org.clay.DBServer.messages.SetRequest
import org.scalatest.{FunSpecLike, Matchers}

class AkkademyDbSpec extends FunSpecLike with Matchers{

  implicit val system = ActorSystem("system",ConfigFactory.empty())

  describe("akkademyDb"){
    describe("given SetRequest"){
      val testProbe = TestProbe()  //模拟一个作为消息发送者的Actor

      it("should place key/value into map"){
        val actorRef = TestActorRef(new AkkademyDb)   //要测试的Actor
        actorRef ! SetRequest("key","value",testProbe.ref)

        val akkademyDb = actorRef.underlyingActor
        akkademyDb.map.get("key") should equal(Some("value"))
      }
    }

    describe("given List[SetRequest]"){
      val testProbe = TestProbe()

      it("should place key/value into map"){
        val actorRef = TestActorRef(new AkkademyDb)   //要测试的Actor

        actorRef ! List(
          SetRequest("key","value",testProbe.ref),
          SetRequest("key2","value2",testProbe.ref)
        )

        val akkademyDb = actorRef.underlyingActor
        akkademyDb.map.get("key") should equal(Some("value"))
        akkademyDb.map.get("key2") should equal(Some("value2"))

        testProbe.expectMsg(Status.Success)
        testProbe.expectMsg(Status.Success)
      }
    }
  }
}
