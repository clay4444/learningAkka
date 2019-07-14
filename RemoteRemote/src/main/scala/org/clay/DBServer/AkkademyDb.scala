package org.clay.DBServer

import akka.actor.{Actor, ActorRef, Status}
import akka.event.Logging
import org.clay.DBServer.messages.SetRequest

import scala.collection.mutable.HashMap
import messages._

/**
  * 监督策略：
  * 1.继续 resume
  * 2.停止 stop
  * 3.重启 restart
  * 4.向上反映 escalate
  *
  * 默认的监督策略：
  * 运行过程中抛出异常：restart
  * 运行过程中发生错误：escalate
  * 初始化时发生异常：stop
  *
  * 生命周期
  * prestart() 开始之前，构造函数之后，也就是重启完之后，已经创建完了新的Actor
  * postStop() stop之后，也就是重启之前，还没创建新的Actor，
  * preRestart() 重启之前，默认调用postStop()
  * postRestart() 重启之后，默认调用prestart()
  */
class AkkademyDb extends Actor {

  val map = new HashMap[String,Object]
  val log = Logging(context.system,this)

  override def receive: Receive = {
    case x:Connected =>
      sender() ! x   //请求创建连接，返回连接，说明创建成功
    case x:List[_] =>
      x.foreach{
        case SetRequest(key,value,sender) =>
          handleSetRequest(key,value,sender)
        case GetRequest(key,sender) =>
          handleGetRequest(key,sender)
      }
    case SetRequest(key,value,sender) =>
      handleSetRequest(key,value,sender)
    case GetRequest(key,sender) =>
      handleGetRequest(key,sender)
    case o =>
      log.info("unknow message")
      sender() ! KeyNotFoundException
  }

  def handleSetRequest(key: String,value: Object,sender: ActorRef) = {
    log.info("received SetRequest - key: {} value: {}",key,value)
    map.put(key,value)
    sender ! Status.Success
  }

  def handleGetRequest(key: String, sender: ActorRef) = {
    log.info("received GetRequest - key: {}",key)
    val response:Option[Object] = map.get(key)
    response match {
      case Some(x) => sender ! x
      case None => sender ! Status.Failure(new KeyNotFoundException(key))
    }
  }
}
