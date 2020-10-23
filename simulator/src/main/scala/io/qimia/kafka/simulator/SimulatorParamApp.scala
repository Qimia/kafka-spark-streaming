package io.qimia.kafka.simulator

import java.util.{Date, UUID}

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import io.qimia.kafka.simulator.actors.{CarEvent, MasterActor, WorkerActor}
import io.qimia.kafka.simulator.utils.GeoStore

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.util.Random

object SimulatorParamApp {
  def main(args: Array[String]): Unit = {
    val geoLocs                                    = args(0)
    val brokers                                    = args(1)
    val topic                                      = args(2)
    val schemaUrl                                  = args(3)
    implicit val system: ActorSystem               = ActorSystem("Simulator")
    implicit val context: ExecutionContextExecutor = system.dispatcher
    implicit val random: Random.type               = Random
    implicit val geoStore: Seq[(Double, Double)]   = GeoStore.init(geoLocs);
    val store =
      List.fill(100)(CarEvent(UUID.randomUUID.toString, getRandomLocation, "available", 0, (new Date).getTime, 0))
    val worker = system.actorOf(WorkerActor.props(geoStore, brokers, topic, schemaUrl), WorkerActor.name)
    val master = system.actorOf(MasterActor.props(store, 86400, 0, worker), MasterActor.name)

    implicit val timeout: Timeout = Timeout(10 minutes)
    val res                       = master ? MasterActor.Start
    res.onComplete { _ => system.terminate() }
  }

  def getRandomLocation(implicit geoStore: Seq[(Double, Double)], random: Random): (Double, Double) =
    geoStore(random.nextInt(geoStore.size))
}
