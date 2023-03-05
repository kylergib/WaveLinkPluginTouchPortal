package com.kylergib.wavelinktp.model;

import com.kylergib.wavelinktp.WaveLinkPlugin;
import org.json.JSONObject;

/**
 * Different TP Plugin actions to send to wave link app
 */

public abstract class WaveLinkActions {

    public static void setFilterByPass(String inputIdentifier, Boolean value, String mixerId) {
        WaveJson testJson = new WaveJson("setFilterBypass",17);
        JSONObject params = new JSONObject();
        params.put("identifier",inputIdentifier);
        params.put("mixerID", mixerId);
        params.put("value", value);
        testJson.setParams(params);
        WaveLinkPlugin.client.send(testJson.getJsonString());

    }

    public static void setMicOutputEnhancement(String property, String micName, String option, Object value) {
        String identifier = null;
        for (Microphone mic: Status.allMics) {
            if (mic.getName().equals(micName)) {
                identifier = mic.getIdentifier();
            } else {
                return;
            }
        }
        WaveJson testJson = new WaveJson("setMicrophoneConfig",26);
        JSONObject params = new JSONObject();
        params.put("identifier",identifier);
        params.put("property",property);

        //decides if it should increment the volume or not
        if (option.equals("Set")) {

            params.put("isAdjustVolume", false);
        } else {
            params.put("isAdjustVolume", true);
            if (!option.equals("Increase")) {
                value = (Integer) value * -1;
            }
        }
        params.put("value", value);
        testJson.setParams(params);
        WaveLinkPlugin.client.send(testJson.getJsonString());
        System.out.println(micName + " - " + value + " - " + option);
    }

    public static void setOutputConfig(String mixerID, String property, Object value) {
        WaveJson testJson = new WaveJson("setOutputConfig",18);
        JSONObject params = new JSONObject();
        params.put("property",property);
        params.put("mixerID", mixerID);
        params.put("value", value);
        params.put("forceLink", false);
        testJson.setParams(params);
        WaveLinkPlugin.client.send(testJson.getJsonString());

    }

    public static void setInputConfig(String inputIdentifier, String mixerID, String property, Object value) {

        WaveJson testJson = new WaveJson("setInputConfig",17);
        JSONObject params = new JSONObject();
        params.put("identifier",inputIdentifier);
        params.put("property",property);
        params.put("mixerID", mixerID);
        params.put("value", value);
        params.put("forceLink", false);
        testJson.setParams(params);
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
        WaveLinkPlugin.client.send(testJson.getJsonString());
    }
}
