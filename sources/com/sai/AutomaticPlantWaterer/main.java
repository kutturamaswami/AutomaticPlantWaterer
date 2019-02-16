package com.sai.AutomaticPlantWaterer;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.nio.file.*;
import java.io.IOException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pi4j.io.gpio.GpioPin;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinDirection;
import com.pi4j.io.gpio.PinMode;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.trigger.GpioCallbackTrigger;
import com.pi4j.io.gpio.trigger.GpioPulseStateTrigger;
import com.pi4j.io.gpio.trigger.GpioSetStateTrigger;
import com.pi4j.io.gpio.trigger.GpioSyncStateTrigger;
import com.pi4j.io.gpio.event.GpioPinListener;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.io.gpio.event.PinEventType;
public class main {
    static final GpioController gpio = GpioFactory.getInstance();
    static final GpioPinDigitalOutput pin0 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, "Pin 0", PinState.LOW);
    static final GpioPinDigitalOutput pin1 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, "Pin 1", PinState.LOW);
    static final GpioPinDigitalOutput pin2 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03, "Pin 2", PinState.LOW);
    static final GpioPinDigitalOutput pin3 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_04, "Pin 3", PinState.LOW);
    static final GpioPinDigitalOutput pin4 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_05, "Pump Pin 4", PinState.LOW);

    public static void main(String[] args)throws java.lang.InterruptedException, java.io.IOException {
        ScheduledExecutorService tmt = Executors.newScheduledThreadPool(1);
        tmt.scheduleAtFixedRate(() -> {
            runSchedule();
        }, 0, 30, TimeUnit.MINUTES);
    }
    private static void runSchedule() {
        double[] moistures = checkMoistures(); //moistures of each plant- range of 0.00 to 1.00
        Plant[] plants = makePlants(); //get array of all plants from JSON file
        boolean needsWatering = false;
        for(int a = 0; a<plants.length; a++) { //check if watering is needed; minimizes pump time
            if(moistures[a]<plants[a].getMinMoisture()) { //check if current moisture is below minimum
                needsWatering = true;
            }
        }

        if(needsWatering) { //only starts pump if watering is needed
            pin4.setState(true); //turn pump on
            try {
                Thread.sleep(1000); //give pump 1 second runup time
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (int a = 0; a < plants.length; a++) {
                if (moistures[a] < plants[a].getMinMoisture()) { //check if specific plant needs watering
                    waterPlant(plants[a]); //water it
                }
            }
            pin4.setState(false); //turn pump off
        }
        needsWatering = false;
    }

    private static double[] checkMoistures() {
        //check moistures through ADC GPIO
        return new double [] {0.00, 0.25, 0.50, 0.75};
    }

    private static Plant[] makePlants() {
        String jsonData;
        try {
            jsonData = new String(Files.readAllBytes(Paths.get("plants.json"))); //get string of JSON file
        } catch(IOException e) {
            e.printStackTrace();
            jsonData = null; //make java happy
        }
        GsonBuilder builder = new GsonBuilder(); //GSON startup stuff
        builder.setPrettyPrinting();
        Gson gson = builder.create();
        return gson.fromJson(jsonData, Plant[].class); //turn JSON string into Plant[], return it
    }

    private static void waterPlant(Plant plant) {
        long timeOpen = convertToTime(plant.getMoistureIncrement()); //time to stay open in milliseconds
        changeValveState(plant, true); //open valve
        ScheduledExecutorService waterTimer = Executors.newScheduledThreadPool(1);
        waterTimer.scheduleAtFixedRate(() -> {
            changeValveState(plant, false); //close valve
        }, 0, timeOpen, TimeUnit.MILLISECONDS); //wait until right amount of water is dispensed
    }

    private static long convertToTime(int mLs) {
        return 557; //return time valve needs to be open to deliver certain amount of water, obtained empirically
    }

    private static void changeValveState(Plant plant, boolean state) {
        switch(plant.getNumber()) {
            case 0:
                pin0.setState(state);
                break;
            case 1:
                pin1.setState(state);
                break;
            case 2:
                pin2.setState(state);
                break;
            case 3:
                pin3.setState(state);
                break;
            case 4:
                pin4.setState(state);
                break;
        }
    }
}