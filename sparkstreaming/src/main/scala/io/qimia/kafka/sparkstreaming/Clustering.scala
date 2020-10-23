package io.qimia.kafka.sparkstreaming

import org.apache.log4j.Logger
import org.apache.spark.ml.clustering.{KMeans, KMeansModel}
import org.apache.spark.ml.evaluation.ClusteringEvaluator
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.ml.{Pipeline, PipelineModel, linalg}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.{OutputMode, StreamingQuery, Trigger}
import org.apache.spark.sql.types.{DoubleType, StringType, StructField, StructType}
import org.apache.spark.sql.{DataFrame, SparkSession}

object Clustering extends App {
  private val logger: Logger = Logger.getLogger(this.getClass)
  logger.info("Starting Spark Streaming.")

  val spark = SparkSession
    .builder()
    .getOrCreate()

  val BOOTSTRAP_SERVERS   = "kafka-1:29090,kafka-2:29090,kafka-3:29090"
  val TOPIC_NAME          = "vehicle-data-topic"

  val sparkSchema = StructType(
    Seq(
      StructField("timeStamp", DoubleType),
      StructField("vin", StringType),
      StructField("fleet", StringType),
      StructField(
        "geoData",
        StructType(Seq(StructField("latitude", DoubleType), StructField("longitude", DoubleType)))
      ),
      StructField("carStateData", StructType(Seq(StructField("state", StringType))))
    )
  )

  val df: DataFrame = spark.readStream
    .format("kafka")
    .option("kafka.bootstrap.servers", BOOTSTRAP_SERVERS)
    .option("subscribe", TOPIC_NAME)
    .option("startingOffsets", "latest")
    .load()
    .withColumn("value_string", col("value").cast(StringType))
    .withColumn("vehicle", from_json(col("value_string"), sparkSchema))
    .select(col("vehicle.geoData.latitude").as("latitude"), col("vehicle.geoData.longitude").as("longitude"))

  val numericFeatures: Array[String] = Array("latitude", "longitude")
  val assembler: VectorAssembler =
    new VectorAssembler().setInputCols(numericFeatures).setHandleInvalid("skip").setOutputCol("features")

  val query: StreamingQuery = df.writeStream
    .outputMode(OutputMode.Append())
    .trigger(Trigger.ProcessingTime("20 seconds"))
    .foreachBatch { (batchDF: DataFrame, batchId: Long) =>
      if (!batchDF.isEmpty) {
        val possibleKs: List[Int] = (2 to 10).toList
        val gridSearchResults: List[(Double, Array[linalg.Vector], Int)] = possibleKs.map(k => {
          val kmeans: KMeans               = new KMeans().setK(k)
          val pipeline: Pipeline           = new Pipeline().setStages(Array(assembler, kmeans))
          val pipelineModel: PipelineModel = pipeline.fit(batchDF)
          // Make predictions
          val predictions: DataFrame = pipelineModel.transform(batchDF)

          // Evaluate clustering by computing Silhouette score
          val evaluator: ClusteringEvaluator = new ClusteringEvaluator()

          val silhouette: Double                   = evaluator.evaluate(predictions)
          val clusterCenters: Array[linalg.Vector] = pipelineModel.stages.last.asInstanceOf[KMeansModel].clusterCenters

          (silhouette, clusterCenters, k)
        })

        val bestModel: (Double, Array[linalg.Vector], Int) = gridSearchResults.maxBy(_._1)
        logger.info(s"Silhouette with squared euclidean distance = ${bestModel._1}")

        // Show the result
        logger.info(s"Cluster Centers (k = ${bestModel._3}): ")
        bestModel._2.foreach(logger.info)
      }
    }
    .start()

  logger.info("Listening to topic.")
  query.awaitTermination(600000)
  logger.info("Done, quitting.")
  spark.stop()
}
