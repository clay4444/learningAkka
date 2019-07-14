package org.clay.DBClient

import akka.actor.{Actor, Stash}
import akka.event.Logging
import org.clay.DBServer.messages.{Connected, Disconnected, Request}

class HotswapClientActor(address:String) extends Actor with Stash{
  val log = Logging(context.system,this)
  private val remoteDb = context.system.actorSelection(address)

  //离线状态
  override def receive: Receive = {
    case x:Request =>
      log.info(s"get request,but still not have db connection")
      remoteDb ! new Connected  //请求创建连接
      stash()
    case _:Connected =>
      log.info(s"get db connection,begin handle with request")
      unstashAll()
      context.become(online)
  }

  //在线状态
  def online: Receive = {
    case x:Disconnected =>
      log.info("disconnect db connection")
      context.unbecome()   //转换为离线状态
    case x:Request =>
      log.info("begin handle with request，send request to real db")
      remoteDb forward x
  }
}