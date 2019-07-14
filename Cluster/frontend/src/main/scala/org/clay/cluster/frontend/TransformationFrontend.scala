package org.clay.cluster.frontend

import akka.actor.{Actor, ActorRef, Terminated}
import akka.util.Timeout
import org.clay.cluster.message.{BackendRegistration, JobFailed, TransformationJob, TransformationResult}

import scala.concurrent.duration._
import akka.pattern.{ask, pipe}

class TransformationFrontend extends Actor{

  var backends = IndexedSeq.empty[ActorRef]  //任务后台节点列表
  var jobCounter = 0

  override def receive: Receive = {
    case job:TransformationJob if backends.isEmpty => //目前暂无执行任务节点可用
      sender() ! JobFailed("Service unavailable, try again later",job)

    case job:TransformationJob => //执行相应任务
      jobCounter += 1
      implicit val timeout = Timeout(5 seconds)
      implicit val executionContext = context.dispatcher
      val backend = backends(jobCounter % backends.size)  //根据相应算法选择执行任务的节点
      println(s"the backend is ${backend} and the job is ${job}")
      val result = (backend ? job).map(x => x.asInstanceOf[TransformationResult])   // 后台节点处理得到结果
      result pipeTo sender   //向外部系统发送执行结果

    case BackendRegistration if !backends.contains(sender()) =>   // 添加新的后台任务节点
      println(s"====>new backend add to cluster client: ${sender()}")
      context.watch(sender())    //监控相应的任务节点
      backends = backends :+ sender()
      println(s"=====> backend size: ${backends.size}")

    case Terminated(a) =>
      backends = backends.filterNot(_ == a)   // 移除已经终止运行的节点
  }
}