package io.qimia.kafka.simulator.utils

import java.util.Properties
import java.util.concurrent.Future

import io.confluent.kafka.serializers.KafkaJsonSerializer
import io.qimia.kafka.models.VehicleData
import io.qimia.kafka.simulator.actors.CarEvent
import org.apache.kafka.clients.admin.{AdminClient, AdminClientConfig, CreateTopicsResult, NewTopic}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization.StringSerializer

import scala.collection.JavaConverters._

object KafkaClient {

  /**
    * Creates a topic using the Kafka AdminClient.
    *
    * @param brokers The Kafka brokers.
    * @param topicName The topic name.
    * @return The CreateTopicsResult object.
    */
  def createTopic(brokers: String, topicName: String): CreateTopicsResult = {
    val config = new Properties
    config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, brokers)

    val admin = AdminClient.create(config)

    val partitions  = 3
    val replication = 2.toShort

    val topic = new NewTopic(topicName, partitions, replication)
    admin.createTopics(asJavaCollection(List(topic)))
  }

  /**
    * Creates a Kafka producer.
    *
    * @param brokers The Kafka brokers.
    * @param schemaUrl The Schema Registry URL.
    * @return The created Kafka producer.
    */
  def producer(brokers: String, schemaUrl: String): KafkaProducer[String, VehicleData] = {
    val kprops = new Properties
    kprops.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
    kprops.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[KafkaJsonSerializer[VehicleData]].getName)
    kprops.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers)
    kprops.put("schema.registry.url", schemaUrl)
    println(classOf[KafkaJsonSerializer[VehicleData]].getName)

    new KafkaProducer[String, VehicleData](kprops)
  }

  /**
    * Send a message using the provided Kafka producer.
    *
    * @param event The CarEvent object that will be converted to VehicleData and sent over Kafka.
    * @param topic The topic name.
    * @param producer The Kafka producer.
    * @return A future with the Record metadata.
    */
  def send(event: CarEvent, topic: String, producer: KafkaProducer[String, VehicleData]): Future[RecordMetadata] = {
    val msg: VehicleData = CarEventConvertor.convert(event)
    val data = new ProducerRecord[String, VehicleData](
      topic,
      msg.vin,
      msg
    )
    producer.send(data)

  }
}
