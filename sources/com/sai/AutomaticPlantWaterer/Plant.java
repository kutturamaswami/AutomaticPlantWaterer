package com.sai.AutomaticPlantWaterer;

public class Plant {
    private String name; //friendly name
    private int number; //number in line from right to left
    private double minMoisture; //moisture 0.00-1.00
    private int moistureIncrement; //mL to add every watering
    private int pin; //pin to activate valve with

    Plant() {
    }

    Plant(String nName, int nNumber, double nMinMoisture, int nMoistureIncrement, int nPin) {
        name = nName;
        number = nNumber;
        minMoisture = nMinMoisture;
        moistureIncrement = nMoistureIncrement;
        pin = nPin;
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

    int getPin() {
        return pin;
    }

    @Override
    public String toString() {
        return "Plant{" +
                "name='" + name + '\'' +
                ", number=" + number +
                ", minMoisture=" + minMoisture +
                ", moistureIncrement=" + moistureIncrement +
                ", pin=" + pin +
                '}';
    }
}