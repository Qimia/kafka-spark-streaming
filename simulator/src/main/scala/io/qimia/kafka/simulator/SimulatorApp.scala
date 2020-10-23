package io.qimia.kafka.simulator

import java.util.{Date, UUID}

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import io.qimia.kafka.simulator.actors.{CarEvent, MasterActor, WorkerActor}
import io.qimia.kafka.simulator.utils.{GeoStore, KafkaClient}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.Random

object SimulatorApp {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem               = ActorSystem("Simulator")
    implicit val context: ExecutionContextExecutor = system.dispatcher
    implicit val random: Random.type               = Random
    implicit val geoStore: Seq[(Double, Double)]   = GeoStore.init("random_geo_muenchen.txt")
    val store: List[CarEvent] =
      List.fill(20)(CarEvent(UUID.randomUUID.toString, getRandomLocation, "available", 0, (new Date).getTime, 0))

    val SCHEMA_REGISTRY_URL = "http://schema-registry:8081"
    val BOOTSTRAP_SERVERS   = "kafka-1:29090,kafka-2:29090,kafka-3:29090"
    val TOPIC_NAME          = "vehicle-data-topic"
    KafkaClient.createTopic(BOOTSTRAP_SERVERS, TOPIC_NAME)

    val worker: ActorRef = system.actorOf(
      WorkerActor.props(geoStore, BOOTSTRAP_SERVERS, TOPIC_NAME, SCHEMA_REGISTRY_URL),
      WorkerActor.name
    )
    val master: ActorRef = system.actorOf(MasterActor.props(store, 500, 4500, worker), MasterActor.name)

    implicit val timeout: Timeout = Timeout(10 minutes)
    val res: Future[Any]          = master ? MasterActor.Start
    res.onComplete { _ => system.terminate() }
  }

  def getRandomLocation(implicit geoStore: Seq[(Double, Double)], random: Random): (Double, Double) =
    geoStore(random.nextInt(geoStore.size))
}
