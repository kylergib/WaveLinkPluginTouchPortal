package com.kylergib.wavelinktp.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
/**
 * Current status for things inside of wave link
 */

public abstract class Status {
    //info about app, probably just need this for debugging
    public static JSONObject applicationInfo;
    public static String selectedOutput;
    public static String selectedOutputFinished;
    public static Boolean selectMonitorMixOutputRunning = false;


    public static JSONObject microphoneConfig;

    //i think this is what the output is listening to on bottom right hand corner of wavelink
    public static JSONObject switchState; //returns {"id":13,"result":{"value":"com.elgato.mix.local"}}
    public static ArrayList<SwitchState> switchStates = new ArrayList<>();
    public static HashMap<String, String> switchMap = new HashMap<>();

    public static JSONObject outputConfig;
    public static JSONObject outputs;

    // all inputs (music, brave, game capture, etc
    public static JSONObject inputConfigs;

    public static ArrayList<Output> allOutputs = new ArrayList<>();
    public static ArrayList<Input> allInputs = new ArrayList<>();
    public static ArrayList<Microphone> allMics = new ArrayList<>();

    public static OutputConfig outputStatus;
    public static ArrayList<JSONObject> receiveMethods = new ArrayList<>();
    public static ArrayList<String> methodString = new ArrayList<>();
    public static String switchStateValue;

    public static Output currentOutputLocal;

    public static String currentOutputSwitch;

    public static void getSwitchState() {
        // what you are listening to in bottom right hand corner of wave link
        JSONObject result = (JSONObject) switchState.get("result");
        switchStateValue = (String) result.get("value");

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
            } else if (switchState.getMixerId().equals(switchMap.get("streamMixer"))) {
                switchState.setLevel(streamMixerLevel);
                switchState.setMuted(streamMixerMuted);
            }

        }



    }

    public static void getInput() {
        //TODO: does not update when an input is removed
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

            //LEFTOFF
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

}
