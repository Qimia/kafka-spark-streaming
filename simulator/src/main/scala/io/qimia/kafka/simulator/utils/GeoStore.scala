package io.qimia.kafka.simulator.utils

import scala.io.Source

object GeoStore {

  /**
    * Initialises the GeoStore by reading locations from the resource file.
    *
    * @param fl Name of the file with locations.
    * @return A list of locations (latitude, longitude).
    */
  def init(fl: String): Seq[(Double, Double)] = {
    val lines: Iterator[String] = Source.fromResource(fl).getLines

    lines.map(parse).toSeq
  }

  /**
    * Parses a line into a latitude and longitude.
    *
    * @param str One line of the file as string.
    * @return Latitude, longitude.
    */
  def parse(str: String): (Double, Double) = {
    val arr = str.split(",")
    (arr(1).toDouble, arr(3).toDouble)
  }
}
