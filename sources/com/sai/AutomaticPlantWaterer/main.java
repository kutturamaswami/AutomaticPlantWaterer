package com.sai.AutomaticPlantWaterer;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinAnalogInput;
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
import com.pi4j.io.gpio.event.GpioPinAnalogValueChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerAnalog;
import com.pi4j.io.gpio.event.GpioPinEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.io.gpio.event.PinEventType;
//import com.pi4j.gpio.extension.ads.ADS1115GpioProvider;
//import com.pi4j.gpio.extension.ads.ADS1115Pin;
//import com.pi4j.gpio.extension.ads.ADS1x15GpioProvider.ProgrammableGainAmplifierValue;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;

public class main {
    static final GpioController gpio = GpioFactory.getInstance();
    /*static final ADS1115GpioProvider adsProvider = new ADS1115GpioProvider(I2CBus.BUS_1, ADS1115GpioProvider.ADS1115_ADDRESS_0x48);
    static GpioPinAnalogInput myInputs[] = {
            gpio.provisionAnalogInputPin(adsProvider, ADS1115Pin.INPUT_A0, "ADS A0"),
            gpio.provisionAnalogInputPin(adsProvider, ADS1115Pin.INPUT_A1, "ADS A1"),
            gpio.provisionAnalogInputPin(adsProvider, ADS1115Pin.INPUT_A2, "ADS A2"),
            gpio.provisionAnalogInputPin(adsProvider, ADS1115Pin.INPUT_A3, "ADS A3"),
    };
    adsProvider.setProgrammableGainAmplifier(ProgrammableGainAmplifierValue.PGA_4_096V, ADS1115Pin.ALL);
    adsProvider.setEventThreshold(500, ADS1115Pin.ALL);
    adsProvider.setMonitorInterval(100);
    static GpioPinListenerAnalog adsListener = new GpioPinListenerAnalog() {
        @Override
        public void handleGpioPinAnalogValueChangeEvent(GpioPinAnalogValueChangeEvent event) {
            double value = event.getValue();
            double percent = ((value * 100) / ADS1115GpioProvider.ADS1115_RANGE_MAX_VALUE);
            double voltage = adsProvider.getProgrammableGainAmplifier(event.getPin()).getVoltage() * (percent/100);
        }
    };
    myInputs[0].addListener(adsListener);*/

    static final GpioPinDigitalOutput gpio1 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, "GPIO 1 - RELAY 1", PinState.LOW);
    static final GpioPinDigitalOutput gpio2 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, "GPIO 2 - RELAY 2", PinState.LOW);
    static final GpioPinDigitalOutput gpio3 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03, "GPIO 3 - RELAY 3", PinState.LOW);
    static final GpioPinDigitalOutput gpio4 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_04, "GPIO 4 - RELAY 4", PinState.LOW);
    static final GpioPinDigitalOutput gpio5 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_05, "GPIO 5 - RELAY 5 - PUMP", PinState.LOW);

    public static void main(String[] args)throws java.lang.InterruptedException, java.io.IOException {
        ScheduledExecutorService tmt = Executors.newScheduledThreadPool(1);
        tmt.scheduleAtFixedRate(() -> {
            runSchedule();
        }, 0, 30, TimeUnit.MINUTES);
    }
    private static void runSchedule() {
        System.out.println("Starting watering cycle at: " + System.currentTimeMillis());
        double[] moistures = checkMoistures(); //moistures of each plant- range of 0.00 to 1.00
        Plant[] plants = makePlants(); //get array of all plants from JSON file
        boolean needsWatering = false;
        for(int a = 0; a<plants.length; a++) { //check if watering is needed; minimizes pump time
            if(moistures[a]<plants[a].getMinMoisture()) { //check if current moisture is below minimum
                needsWatering = true;
            }
        }

        if(needsWatering) { //only starts pump if watering is needed
            gpio5.setState(true); //turn pump on
            try {
                Thread.sleep(1000); //give pump 3 second runup time
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (int a = 0; a < plants.length; a++) {
                if (moistures[a] < plants[a].getMinMoisture()) { //check if specific plant needs watering
                    waterPlant(plants[a]); //water it
                }
            }
            gpio5.setState(false); //turn pump off
        }
        needsWatering = false;
    }

    private static double[] checkMoistures() {
        //check moistures through ADC GPIO
        return new double [] {1.00, 0.00, 1.00, 1.00};
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
        System.out.println("Watering plant name: " + plant.getName() + " number: " + plant.getNumber() + " for milliseconds: " + timeOpen);
        changeValveState(plant, true); //open valve
        try {
            Thread.sleep(timeOpen);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
        changeValveState(plant, false); //close valve
    }

    private static long convertToTime(int mLs) {
        return 4000; //return time valve needs to be open to deliver certain amount of water, obtained empirically
    }

    private static void changeValveState(Plant plant, boolean state) {
        switch(plant.getNumber()) {
            case 0:
                gpio1.setState(state);
                break;
            case 1:
                gpio2.setState(state);
                break;
            case 2:
                gpio3.setState(state);
                break;
            case 3:
                gpio4.setState(state);
                break;
        }
        System.out.println("Set plant name: " + plant.getName() + " number: " + plant.getNumber() + " to state " + state);
    }
}