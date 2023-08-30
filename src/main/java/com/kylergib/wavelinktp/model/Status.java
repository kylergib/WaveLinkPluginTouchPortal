package com.kylergib.wavelinktp.model;

import com.kylergib.wavelinktp.WaveLinkPlugin;
import com.kylergib.wavelinktp.WaveLinkPluginConstants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Base64;
/**
 * Current status for things inside of wave link
 */

public abstract class Status {
    //info about app, probably just need this for debugging
    //TODO: clean up
    public static JSONObject applicationInfo;
    public static String selectedOutput;
    public static String selectedOutputFinished;
    public static Boolean selectMonitorMixOutputRunning = false;


    public static JSONObject microphoneConfig;

    //i think this is what the output is listening to on bottom right hand corner of wavelink
    public static JSONObject switchState;
    public static ArrayList<SwitchState> switchStates = new ArrayList<>();
    public static HashMap<String, String> switchMap = new HashMap<>();

    public static JSONObject outputConfig;
    public static JSONObject outputs;

    // all inputs (music, brave, game capture, etc
    public static JSONObject inputConfigs;

    public static ArrayList<Output> allOutputs = new ArrayList<>();
    public static ArrayList<Input> allInputs = new ArrayList<>();
    public static ArrayList<String> allFilters = new ArrayList<>();
    public static ArrayList<Microphone> allMics = new ArrayList<>();

    public static OutputConfig outputStatus;
    public static ArrayList<JSONObject> receiveMethods = new ArrayList<>();
    public static ArrayList<String> methodString = new ArrayList<>();
    public static String switchStateValue;

    public static Output currentOutputLocal;

    public static String currentOutputSwitch;

    public static final String localPackageName = "com.elgato.mix.local";
    public static final String streamPackageName = "com.elgato.mix.stream";

    public static ArrayList<String> sentStates = new ArrayList<>();


    public static void getSwitchState() {
        // what you are listening to in bottom right hand corner of wave link
        JSONObject result = (JSONObject) switchState.get("result");
        switchStateValue = (String) result.get("value");

        String monitorValue = null;
        if (switchStateValue.equals(Status.streamPackageName)) {
            monitorValue = "Stream";
        } else {
            monitorValue = "Local";
        }
        WaveLinkPlugin.waveLinkPlugin.sendStateUpdate(WaveLinkPluginConstants.WaveLinkOutputs.States.MonitoredMix.ID, monitorValue);

    }

    public static void getOutputConfig() {
        JSONObject result = (JSONObject) outputConfig.get("result");
        JSONArray streamMixer = (JSONArray) result.get("streamMixer");
        Boolean streamMixerMuted = (Boolean) streamMixer.get(0);
        int streamMixerLevel = (int) streamMixer.get(1);

        JSONArray localMixer = (JSONArray) result.get("localMixer");
        Boolean localMixerMuted = (Boolean) localMixer.get(0);
        int localMixerLevel = (int) localMixer.get(1);

        for (SwitchState switchState: switchStates) {
            if (switchState.getMixerId().equals(switchMap.get("localMixer"))) {
                switchState.setLevel(localMixerLevel);
                switchState.setMuted(localMixerMuted);
                String sendMute = "unmuted";
                if (localMixerMuted) {
                    sendMute = "muted";
                }
                setOutputValue(localMixerLevel,"Local");
                WaveLinkPlugin.waveLinkPlugin.sendStateUpdate(WaveLinkPluginConstants.WaveLinkOutputs.States.LocalMixOut.ID,sendMute);
            } else if (switchState.getMixerId().equals(switchMap.get("streamMixer"))) {
                switchState.setLevel(streamMixerLevel);
                switchState.setMuted(streamMixerMuted);
                String sendMute = "unmuted";
                if (localMixerMuted) {
                    sendMute = "muted";
                }
                WaveLinkPlugin.waveLinkPlugin.sendStateUpdate(WaveLinkPluginConstants.WaveLinkOutputs.States.StreamMixOut.ID,sendMute);
                setOutputValue(streamMixerLevel, "Stream");
            }

        }



    }

    public static void getInput() {
        //TODO: possible to get icon data and import icon?
        if (!allInputs.isEmpty()) {
            allInputs.clear();
        }
        int id = (int) inputConfigs.get("id");
        JSONArray resultJson = (JSONArray) inputConfigs.get("result");
        for (int i = 0; i < resultJson.length(); i++) {
            JSONObject tempJson = (JSONObject) resultJson.get(i);


            String identifier = (String) tempJson.get("identifier");
            String name = (String) tempJson.get("name");
            Boolean isAvailable = (Boolean) tempJson.get("isAvailable");

            //testing
            String iconData = (String) tempJson.get("iconData");
            byte[] decodedIcon = Base64.getDecoder().decode(iconData);


            String folderPath = "icons";
            File folder = new File(folderPath);
            if (!folder.exists()) {
                boolean created = folder.mkdirs();
                if (!created) {
                    System.out.println("Failed to create the folder.");
                    return;
                }
            }

            // Saving as a file
            if (!iconData.isEmpty()) {
                String fileName = folderPath + File.separator + name + "Image.png";
                try (FileOutputStream fos = new FileOutputStream(fileName)) {
                    fos.write(decodedIcon);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


            //end testing

            JSONArray streamMixer = (JSONArray) tempJson.get("streamMixer"); //isMuted, level, unknown
            Boolean streamMixerMuted = (Boolean) streamMixer.get(0);
            int streamMixerLevel = (Integer) streamMixer.get(1);
            Boolean pluginBypassStream = (Boolean) streamMixer.get(2);

            JSONArray localMixer = (JSONArray) tempJson.get("localMixer");
            Boolean localMixerMuted = (Boolean) localMixer.get(0);
            int localMixerLevel = (Integer) localMixer.get(1);
            Boolean pluginBypassLocal = (Boolean) localMixer.get(2);


            int inputType = (int) tempJson.get("inputType");
            ArrayList<InputPlugin> inputPlugins = new ArrayList<>();
            try {
                //input has filters

                JSONArray filters = (JSONArray) tempJson.get("filters");
                for (int z = 0; z < filters.length(); z++) {
                    JSONObject tempPlugin = (JSONObject) filters.get(z);
                    String filterId = (String) tempPlugin.get("filterID");
                    String pluginId = (String) tempPlugin.get("pluginID");
                    String pluginName = (String) tempPlugin.get("name");
                    Boolean isActive = (Boolean) tempPlugin.get("isActive");
                    InputPlugin newPlugin = new InputPlugin(filterId,pluginId,pluginName,isActive);
                    inputPlugins.add(newPlugin);
                    if (!allFilters.contains(pluginName)) {
                        allFilters.add(pluginName);
                    }
                }
            } catch (JSONException ignored) {
            }
            Input newInput = new Input(identifier,name,isAvailable,
                    streamMixerMuted,localMixerMuted,localMixerLevel,
                    streamMixerLevel,inputPlugins,inputType, pluginBypassLocal,
                    pluginBypassStream);
            if (!allInputs.isEmpty()) {
                Boolean shouldAdd = true;
                for (Input allInput : allInputs) {

                    if (allInput.getIdentifier().equals(newInput.getIdentifier())) {
                        shouldAdd = false;
                        break;
                    }
                }
                if (shouldAdd) {
                    allInputs.add(newInput);
                }
            } else {
                allInputs.add(newInput);
            }
        }

    }

    public static void getMicrophone() {
        int id = (int) microphoneConfig.get("id");
        JSONArray resultJson = (JSONArray) microphoneConfig.get("result");

        for (int i = 0; i < resultJson.length(); i++) {
            JSONObject tempJson = (JSONObject) resultJson.get(i);
            String identifier = (String) tempJson.get("identifier");
            String name = (String) tempJson.get("name");
            Boolean isClipGuardOn = (Boolean)  tempJson.get("isClipGuardOn");
            Boolean isLowCutOn = (Boolean)  tempJson.get("isLowCutOn");
            Boolean isWaveLink = (Boolean)  tempJson.get("isWaveLink");
            Boolean isWaveXLR = (Boolean)  tempJson.get("isWaveXLR");
            int lowCutType = (int) tempJson.get("lowCutType");
            Microphone newMicrophone = new Microphone(identifier,isClipGuardOn,isLowCutOn,
                    isWaveLink,isWaveXLR,lowCutType,name);
            allMics.add(newMicrophone);

        }




    }
    public static void getOutputs() {
        allOutputs.clear();
        int id = (int) outputs.get("id");
        JSONObject resultJson = (JSONObject) outputs.get("result");
        String selectedOutput = (String) resultJson.get("selectedOutput");
        JSONArray newOutputs = (JSONArray) resultJson.get("outputs");

        for (int i = 0; i < newOutputs.length(); i++) {
            JSONObject tempJson = (JSONObject) newOutputs.get(i);
            String identifier = (String) tempJson.get("identifier");
            String name = (String) tempJson.get("name");
            Boolean isSelected = false;
            Output newOutput = new Output(identifier, name, isSelected);
            if (identifier.equals(selectedOutput)) {
                newOutput.setSelected(true);
                currentOutputLocal = newOutput;
                selectedOutput = newOutput.getName();
                selectedOutputFinished = newOutput.getName();
            }


            allOutputs.add(newOutput);


        }

    }

    public static void setInputValue(String inputName, int value, String mixerName) {
        HashMap<String, Object> data = new HashMap<>();
        data.put("com.kylergib.wavelinktp.WaveLinkPlugin.WaveLinkInputs.connector.inputVolumeConnector.data.mixerId", mixerName);
        data.put("com.kylergib.wavelinktp.WaveLinkPlugin.WaveLinkInputs.state.inputList", inputName);
        WaveLinkPlugin.waveLinkPlugin.sendConnectorUpdate(WaveLinkPluginConstants.ID,WaveLinkPluginConstants.WaveLinkInputs.Connectors.InputVolumeConnector.ID,value,data);
    }

    public static void setOutputValue(int value, String mixerName) {
        HashMap<String, Object> data = new HashMap<>();
        data.put("com.kylergib.wavelinktp.WaveLinkPlugin.WaveLinkOutputs.connector.outputVolumeConnector.data.outputMixerId", mixerName);
        WaveLinkPlugin.waveLinkPlugin.sendConnectorUpdate(WaveLinkPluginConstants.ID,WaveLinkPluginConstants.WaveLinkOutputs.Connectors.OutputVolumeConnector.ID,value,data);

    }

}
