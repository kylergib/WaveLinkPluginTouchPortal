package com.kylergib.wavelinktp.model;

import com.kylergib.wavelinktp.WaveLinkPlugin;
import org.json.JSONObject;

/**
 * Different TP Plugin actions to send to wave link app
 */

public abstract class WaveLinkActions {

    public static void SetMicPcMix() {

    }

    public static void setSwitchOutput() {
        actionSend(null, "switchOutput", 20);
    }

    public static void setFilterByPass(String inputIdentifier, Boolean value, String mixerId) {
        JSONObject params = new JSONObject();
        params.put("identifier",inputIdentifier);
        params.put("mixerID", mixerId);
        params.put("value", value);
        actionSend(params,"setFilterBypass",17);

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
        actionSend(params,"setMicrophoneConfig",26);
    }

    public static void setOutputConfig(String mixerID, String property, Object value) {
        JSONObject params = new JSONObject();
        params.put("property",property);
        params.put("mixerID", mixerID);
        params.put("value", value);
        params.put("forceLink", false);
        actionSend(params,"setOutputConfig",18);

    }

    public static void setInputConfig(String inputIdentifier, String mixerID, String property, Object value) {

        JSONObject params = new JSONObject();
        params.put("identifier",inputIdentifier);
        params.put("property",property);
        params.put("mixerID", mixerID);
        params.put("value", value);
        params.put("forceLink", false);
        actionSend(params,"setInputConfig",17);
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
        JSONObject params = new JSONObject();
        params.put("identifier",inputIdentifier);
        params.put("name", name);
        actionSend(params,"setSelectedOutput", 12);
    }

    private static void actionSend(JSONObject params, String method, int id) {
        WaveJson sendJson = new WaveJson(method,id);
        if (params != null) {
            sendJson.setParams(params);
        }

        WaveLinkPlugin.client.send(sendJson.getJsonString());
    }
}
