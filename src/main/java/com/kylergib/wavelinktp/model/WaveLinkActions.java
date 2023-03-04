package com.kylergib.wavelinktp.model;

import com.kylergib.wavelinktp.WaveLinkPlugin;
import org.json.JSONObject;

import java.util.logging.Level;

public abstract class WaveLinkActions {

    public static void setOutputConfig(String mixerID, String property, Object value) {
        WaveJson testJson = new WaveJson("setOutputConfig",18);
        JSONObject params = new JSONObject();
        params.put("property",property);
        params.put("mixerID", mixerID);
        params.put("value", value);
        params.put("forceLink", false);
        testJson.setParams(params);
//        System.out.println(testJson.getJsonString());

        WaveLinkPlugin.client.send(testJson.getJsonString());
//        WaveLinkPlugin.LOGGER.log(Level.INFO, String.format(testJson.getJsonString()));

    }

    public static void setInputConfig(String inputIdentifier, String mixerID, String property, Object value) {
//        for (Microphone mic: Status.allMics) {
//            System.out.println(mic.getName() + " - " + mic.getClipGuardOn());
//        }
        WaveJson testJson = new WaveJson("setInputConfig",17);
        JSONObject params = new JSONObject();
        params.put("identifier",inputIdentifier);
        params.put("property",property);
        params.put("mixerID", mixerID);
        params.put("value", value);
        params.put("forceLink", false);
        testJson.setParams(params);
//        System.out.println(testJson.getJsonString());
        WaveLinkPlugin.client.send(testJson.getJsonString());
    }


    public static void setMonitorMixOutput(String name) {
        String inputIdentifier = null;
        for (Output output: Status.allOutputs) {
            System.out.println(output.getName() + " - " + output.getIdentifier());
            if (output.getName().equals(name)) {
                inputIdentifier = output.getIdentifier();
                output.setSelected(true);
                Status.selectedOutput = output.getName();
            } else {
                output.setSelected(false);
            }
        }
        WaveJson testJson = new WaveJson("setSelectedOutput",12);
        JSONObject params = new JSONObject();
        params.put("identifier",inputIdentifier);
        params.put("name", name);
        testJson.setParams(params);
//        System.out.println(testJson.getJsonString());
        WaveLinkPlugin.client.send(testJson.getJsonString());
    }
}
