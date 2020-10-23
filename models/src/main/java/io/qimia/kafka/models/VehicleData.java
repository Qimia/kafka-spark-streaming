package io.qimia.kafka.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VehicleData {
    @JsonProperty
    public Double timeStamp;

    @JsonProperty
    public String vin;

    @JsonProperty
    public String fleet;

    @JsonProperty
    public GeoData geoData;

    @JsonProperty
    public CarStateData carStateData;

    public VehicleData() {
    }

    public VehicleData(Double timeStamp, String vin, String fleet, GeoData geoData, CarStateData carStateData) {
        this.timeStamp = timeStamp;
        this.vin = vin;
        this.fleet = fleet;
        this.geoData = geoData;
        this.carStateData = carStateData;
    }
}
