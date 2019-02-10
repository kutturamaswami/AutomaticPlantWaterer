package com.sai.AutomaticPlantWaterer;

public class Plant {
    private String name; //friendly name
    private int number; //number in line from right to left
    private double minMoisture; //moisture 0.00-1.00
    private int moistureIncrement; //mL to add every watering

    Plant() {
    }

    Plant(String nName, int nNumber, double nMinMoisture, int nMoistureIncrement) {
        name = nName;
        number = nNumber;
        minMoisture = nMinMoisture;
        moistureIncrement = nMoistureIncrement;
    }

    String getName() {
        return name;
    }

    double getMinMoisture() {
        return minMoisture;
    }

    int getMoistureIncrement() {
        return moistureIncrement;
    }

    @Override
    public String toString() {
        return "Plant{" +
                "name='" + name + '\'' +
                ", number=" + number +
                ", minMoisture=" + minMoisture +
                ", moistureIncrement=" + moistureIncrement +
                "}";
    }
}