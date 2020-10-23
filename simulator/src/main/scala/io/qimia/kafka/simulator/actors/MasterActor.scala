package io.qimia.kafka.simulator.actors

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.util.Random

object MasterActor {
  def props(store: List[CarEvent], loop: Int, delay: Int, worker: ActorRef): Props =
    Props(new MasterActor(store, loop, delay, worker))

  def name = "master"

  case object Start

  case object Clock

  case object Done

}

class MasterActor(var store: List[CarEvent], loop: Int, delay: Int, worker: ActorRef) extends Actor with ActorLogging {
  implicit def executionContext: ExecutionContextExecutor = context.dispatcher

  import MasterActor._

  private var clock: Int        = 0
  private var parent: ActorRef  = _
  val random: Random.type       = Random
  implicit val timeout: Timeout = Timeout(2 minute)

  def receive: Receive = {
    case Start => {
      log.info("Master start looping")
      parent = sender
      self ! Clock
    }
    case Clock => {
      clock = clock + 1
      log.info("start looping, clock:" + clock)
      val bookeds: List[Future[WorkerActor.Response]] = store
        .filter(_.state == "booked")
        .map(x => {
          val blk = random.nextInt(11);
          if (x.block > 0)
            worker.ask(WorkerActor.Notify(x)).mapTo[WorkerActor.Response]
          else if (blk == 10)
            worker.ask(WorkerActor.Block(x)).mapTo[WorkerActor.Response]
          else
            worker.ask(WorkerActor.Move(x)).mapTo[WorkerActor.Response]
        })
      log.debug("$$$ all bookeds sent:" + clock)
      val availables: List[Future[WorkerActor.Response]] = store
        .filter(_.state == "available")
        .map(x => {
          val blk = random.nextInt(11)
          val bk  = random.nextInt(5)
          if (x.block > 1)
            worker.ask(WorkerActor.Notify(x)).mapTo[WorkerActor.Response]
          else if (blk == 10)
            worker.ask(WorkerActor.Block(x)).mapTo[WorkerActor.Response]
          else if (bk == 4)
            worker.ask(WorkerActor.Book(x)).mapTo[WorkerActor.Response]
          else
            worker.ask(WorkerActor.Notify(x)).mapTo[WorkerActor.Response]
        })
      log.debug("$$$ all availabes sent:" + clock)
      val bookedList    = Future.sequence(bookeds)
      val availableList = Future.sequence(availables)
      log.debug("$$$ blocked on bookeds sent:" + clock)
      val res1 = Await.result(availableList, Timeout(120 seconds).duration)
      val res2 = Await.result(bookedList, Timeout(120 seconds).duration)
      log.info("$$$ FINISHED BLOCKING:" + clock)
      store = (res1 ::: res2).map(_.event)
      log.info("$$$$$$$$$$ size of store at clock end: " + store.size)
      if (clock < loop) {
        Thread.sleep(delay);
        self ! Clock
      } else {
        worker ! PoisonPill
        self ! PoisonPill
        parent ! Done
      }
    }
  }
}
