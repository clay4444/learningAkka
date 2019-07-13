package org.clay.AkkaTest

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, AllForOneStrategy, OneForOneStrategy, Props, ReceiveTimeout, SupervisorStrategy, Terminated}
import akka.event.LoggingReceive
import akka.util.Timeout
import akka.pattern.{ask, pipe}
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

/**
  *          system
  *  listener  <-  worker
  *                  |
  *             counterService
  *                  |
  *           Counter、Storage
  *
  * 整个流程中 counter 是不会出错的，只有Storage会抛出异常然后重启
  * 整个流程中，之所以能够在Storage重启后，继续按照之前的increment继续加，是因为counter没有挂过，它的count值是不会清除的，
  */
object FaultHandlingDocSample extends App {
  import Worker._

  val config = ConfigFactory.parseString(
    """
      |akka.loglevel = "DEBUG"
      |    akka.actor.debug {
      |      receive = on
      |      lifecycle = on
      |    }
    """.stripMargin)

  val system = ActorSystem("FaultHandlingDocSample",config)
  val worker = system.actorOf(Props[Worker],name = "worker")
  val listener = system.actorOf(Props[Listener],name="listener")

  //告诉worker启动任务，并在listener监听进度，注意要把sender设置为 listener，
  //worker会回复给sender，
  //worker ! (Start,listener)  //这种写法是错误的，
  worker.tell(Start,sender=listener)  //这种是正确的，why ?
}


/**
  * 监听worker的进展，并在进度为 100% 时停止系统
  */
class Listener extends Actor with ActorLogging {
  import Worker._

  context.setReceiveTimeout(15 seconds) //15秒没收到消息，就给自己发送一条timeout的消息

  override def receive: Receive = {
    case Progress(percent) =>
      log.info("current progress: {} %", percent)
      if (percent >= 100.0) {
        log.info("shutting down, that's all")
        context.system.terminate()
      }

    case ReceiveTimeout =>
      //15秒没接收到消息，说明服务不可用
      log.error("shutdown down due to unavailable service")
      context.system.terminate()
  }
}


object Worker {
  case object Start
  case object Do
  case class Progress(percent: Double)
}

//worker
class Worker extends Actor with ActorLogging {
  import Worker._
  import CounterService._

  implicit val askTimeout = Timeout(5 seconds)

  //服务不可用之后，直接停止 child    backlog 缓存的消息太多了，会导致服务不可用；
  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case _: ServiceUnavailable => Stop
  }

  var progressListener: Option[ActorRef] = None
  val counterService = context.actorOf(Props[CounterService], name = "counter")
  val totalcount = 51

  import context.dispatcher // 主线程池

  override def receive: Receive = LoggingReceive {
    case Start if progressListener.isEmpty =>
      progressListener = Some(sender()) //把消息的发送方设置为 listener
      context.system.scheduler.schedule(Duration.Zero, 1 second, self, Do) //初始化延迟0秒，一秒发送一次，发送给自己，发送Do 消息

    case Do =>
      counterService ! Increment(1)    //每秒长3；
      counterService ! Increment(1)
      counterService ! Increment(1)

      counterService ? GetCurrentCount map {   //每秒计算一下进度， 并报告给progressListener
        case CurrentCount(_, count) => Progress(100.0 * count / totalcount)
      } pipeTo progressListener.get
  }
}


object CounterService {
  case class Increment(n: Int)
  case object GetCurrentCount
  case class CurrentCount(key: String, count: Long)
  class ServiceUnavailable(msg: String) extends RuntimeException(msg)
  private case object Reconnect
}

/**
  * 接收到 Increment(count) 消息时，把count值新增到持久化counter上，
  * 接收到 GetCurrentCount 消息时，返回持久化counter
  * 它负责监督 Storage 和 Counter
  */
class CounterService extends Actor {
  import CounterService._
  import Counter._
  import Storage._

  //抛出StorageException异常时，重启storage actor
  //5秒内重试三次， Stop
  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 3,withinTimeRange = 5 seconds){
    case _:StorageException => Restart
  }

  val key = self.path.name   //用 CounterService 的 path 做 key，所以一直只有一个key
  var storage:Option[ActorRef] = None
  var counter:Option[ActorRef] = None
  var backlog = IndexedSeq.empty[(ActorRef,Any)]
  val MaxBackLog = 10000

  import context.dispatcher // Use this Actors' Dispatcher as ExecutionContext

  override def preStart(){
    initStorage()
  }

  //初始化
  def initStorage(){
    storage = Some(context.watch(context.actorOf(Props[Storage],name = "storge")))   //监控 Storage
    //这里为啥要用foreach呢？因为刚开始的时候，counter是None，此时直接调用tell消息，会报空指针，但是foreach当counter为None的时候根本不会执行，厉害!
    //而重启的时候，counter不为None，此时调用tell没有问题；会告诉 counter 已经启动了新的 storage 服务
    counter foreach ( _ ! UseStorage(storage))  //此时counter不是还为 None吗，直接在 preStart 生命周期中调用？因为监控到Storage死了之后，会重新调用这个方法，此时counter不为空；
    storage.get ! Get(key)  //这里为啥还要重新get一次呢？因为要让 storage返回一个Entry，下面的receive收到这个Entry，会做一个初始化，否则counter做不了初始化，消息就全部进入backlog了
                            //重启的时候返回的Entry没用，因为此时的counter不为None，
    println("这里的方法应该调用两次")
  }

  override def receive: Receive = LoggingReceive {
    case Entry(k,v) if k == key && counter == None =>
      val c = context.actorOf(Props(classOf[Counter],key,v))  //Props(class,args) args是Counter构造器的参数
      counter = Some(c)
      // Tell the counter to use current storage
      c ! UseStorage(storage)
      for((repltTo,msg) <- backlog) c.tell(msg,sender=repltTo)
      backlog = IndexedSeq.empty

    case msg @ Increment(n) => forwardOrPlaceInBacklog(msg)

    case msg @ GetCurrentCount => forwardOrPlaceInBacklog(msg)

    case Terminated(actorRef) if Some(actorRef) == storage =>
      //3次重试之后，storge 会stop，因为我们已经在initStorage中watch了storage，所以会受到Terminated消息；
      storage = None
      // 告诉counter此时没有可用的 storage，counter就不会调用storage去存储了，否则counter会向已经终止的 storage发送消息；
      counter foreach {_ ! UseStorage(None)}
      //
      context.system.scheduler.scheduleOnce(10 seconds,self,Reconnect)  //10秒后发给自己一个 Reconnect 消息

    case Reconnect =>
      initStorage()
  }

  def forwardOrPlaceInBacklog(msg: Any): Unit ={

    counter match {
      case Some(c) =>
        c forward msg
      case None =>
        println("收到消息，但是counter是None")  //不会发生，
        if (backlog.size >= MaxBackLog)
          throw new ServiceUnavailable("CounterService not available, lack of initial value")
        backlog :+= (sender() -> msg)
    }
  }
}


object Counter {
  case class UseStorage(storage: Option[ActorRef])
}

/**
  * 收到UseStorage消息时，初始化 storage 服务；
  * 接收到Increment消息时，增加count 并持久化数据
  * 接收到getCurrentCount消息时，返回count，
  */
class Counter(key: String, initialValue: Long) extends Actor {
  import Counter._
  import CounterService._
  import Storage._

  var count = initialValue
  var storage:Option[ActorRef] = None

  override def receive: Receive = LoggingReceive {
    case UseStorage(s) =>
      storage = s
      storeCount()

    case Increment(n) =>
      count += n     //而且这个count值也不会丢，
      storeCount()

    case getCurrentCount =>
      sender() ! CurrentCount(key, count)
  }

  def storeCount(){
    storage foreach { _ ! Store(Entry(key,count))}  //发个消息就完了，所以storage的状态不会影响到Counter
  }
}


object Storage{
  case class Store(entry: Entry)
  case class Get(key: String)
  case class Entry(key:String, value:Long)
  class StorageException(msg:String) extends RuntimeException(msg)
}

/**
  * 收到Store时，存储数据；
  * 收到Get时，返回key对应的value，如果没有对应的value，返回0
  * 存储时可能会抛出StorageException
  */
class Storage extends Actor{
  import Storage._

  val db = DummyDB

  override def receive: Receive = LoggingReceive {
    case Store(Entry(key, count)) =>
      db.save(key,count)
    case Get(key) => sender() ! Entry(key,db.load(key).getOrElse(0L))
  }
}

/**
  * 最终存储数据的地方
  * value 在 11和14 之间会抛出异常；StorageException
  */
object DummyDB{
  import Storage.StorageException
  private var db = Map[String,Long]()

  @throws(classOf[StorageException])
  def save(key: String, value:Long): Unit = synchronized {
    if(value >= 11 && value <= 14)
      throw new StorageException("Simulated store failure " + value)
    db += (key -> value)
  }

  @throws(classOf[StorageException])
  def load(key: String):Option[Long] = {
    db.get(key)
  }
}