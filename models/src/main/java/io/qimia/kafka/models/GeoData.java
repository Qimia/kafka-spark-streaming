package io.qimia.kafka.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GeoData {
    @JsonProperty
    public Double latitude;

    @JsonProperty
    public Double longitude;

    public GeoData() {
    }

    public GeoData(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
