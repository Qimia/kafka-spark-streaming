package io.qimia.kafka.sparkstreaming

import org.apache.log4j.Logger
import org.apache.spark.sql.execution.streaming.FileStreamSource.Timestamp
import org.apache.spark.sql.functions.{approx_count_distinct, col, window}
import org.apache.spark.sql.streaming.{DataStreamWriter, OutputMode, StreamingQuery, Trigger}
import org.apache.spark.sql.types.StringType
import org.apache.spark.sql.{DataFrame, Dataset, Row, SparkSession}

object LostConnections extends App {
  private val logger: Logger = Logger.getLogger(this.getClass)

  logger.info("Starting Spark Streaming.")

  val spark = SparkSession
    .builder()
    .getOrCreate()

  import spark.implicits._

  logger.info("Connecting to topic.")

  val BOOTSTRAP_SERVERS = "kafka-1:29090,kafka-2:29090,kafka-3:29090"
  val TOPIC_NAME        = "vehicle-data-topic"

  val df: DataFrame = spark.readStream
    .format("kafka")
    .option("kafka.bootstrap.servers", BOOTSTRAP_SERVERS)
    .option("subscribe", TOPIC_NAME)
    .option("startingOffsets", "latest")
    .load()

  val selected: Dataset[(String, Timestamp)] = df
    .withColumn("vin", col("key").cast(StringType))
    .select("vin", "timestamp")
    .as[(String, Timestamp)]

  val grouped = selected
    .withWatermark("timestamp", "2 seconds")
    .groupBy(
      window(col("timestamp"), "10 seconds", "10 seconds")
    )

  val aggregated = grouped
    .agg(approx_count_distinct("vin").as("Number of alive cars"))
    .sort("window")

  val writer: DataStreamWriter[Row] = aggregated.writeStream
    .outputMode(OutputMode.Complete())
    .format("console")
    .option("truncate", value = false)
    .option("numRows", 1000)

  val writerWithTrigger: DataStreamWriter[Row] = writer
    .trigger(Trigger.ProcessingTime("10 seconds"))

  val query: StreamingQuery = writerWithTrigger
    .start()

  logger.info("Listening to topic.")
  query.awaitTermination(600000)
  logger.info("Done, quitting.")
}
