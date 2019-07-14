package org.clay.DBClient

import akka.actor.{Actor, ActorLogging, FSM}
import org.clay.DBClient.StateContainerTypes.RequestQueue
import org.clay.DBServer.messages.Request

sealed trait State

/**
  * 定义状态
  */
//离线，队列中没有任何消息
case object Disconnected extends State

//在线，队列中没有任何消息
case object Connected extends State

//在线，队列中包含消息
case object ConnectedAndPending extends State

case object Flush

case object ConnectedMsg

object StateContainerTypes {
  //容器：保存一个请求列表作为状态容器，接收到Flush消息的时候处理这个列表中的请求；
  type RequestQueue = List[Request]
}

class FSMClientActor(address: String) extends FSM[State,RequestQueue]{


}
