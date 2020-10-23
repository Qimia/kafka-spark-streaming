package io.qimia.kafka.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CarStateData {
    @JsonProperty
    public String state;

    public CarStateData() {
    }

    public CarStateData(String state) {
        this.state = state;
    }
}
