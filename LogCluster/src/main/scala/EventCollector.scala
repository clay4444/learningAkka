import akka.cluster.ClusterEvent.MemberUp

class EventCollector extends ClusterRoledWorker{

  @volatile var recordCounter:Int = 0

  override def receive: Receive = {
    case MemberUp(member) =>
      log.info("")
  }

}
