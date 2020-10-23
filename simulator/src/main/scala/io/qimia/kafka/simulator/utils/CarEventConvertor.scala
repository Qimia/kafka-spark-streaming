package io.qimia.kafka.simulator.utils

import io.qimia.kafka.models.{CarStateData, GeoData, VehicleData}
import io.qimia.kafka.simulator.actors.CarEvent

object CarEventConvertor {

  /**
   * Converts an CarEvent object into VehicleData that will be sent over Kafka.
   *
   * @param event The internal CarEvent object.
   * @return The converted VehicleData object.
   */
  def convert(event: CarEvent): VehicleData = {
    val csData      = new CarStateData(event.state)
    val geoData     = new GeoData(event.location._1, event.location._2)
    val vehicleData = new VehicleData(event.timeStamp, event.vin, "TEST", geoData, csData)
    vehicleData
  }
}
