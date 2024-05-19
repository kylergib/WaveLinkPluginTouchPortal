package com.kylergib.wavelinktp.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;

public class WaveLink {

    private static ArrayList<Microphone> allMics = new ArrayList<>();
    private static ArrayList<Output> allOutputs = new ArrayList<>();
    private static ArrayList<Input> allInputs = new ArrayList<>();

    private SwitchState localMixer;
    private SwitchState streamMixer;
    private SwitchState activeMixer;
    public static final String localPackageName = "com.elgato.mix.local";
    public static final String streamPackageName = "com.elgato.mix.stream";

    private static Output currentOutput;
//    private SwitchState currentSwitchState;

    public WaveLink() {

        localMixer = new SwitchState(WaveLink.localPackageName, null, -1, "Local");
        streamMixer = new SwitchState(WaveLink.streamPackageName, null, -1, "Stream");

    }

    public void onMicrophoneConfig(JSONObject microphoneConfig) {

        JSONArray resultJson = (JSONArray) microphoneConfig.get("result");

        for (int i = 0; i < resultJson.length(); i++) {
            JSONObject tempJson = (JSONObject) resultJson.get(i);
            String identifier = (String) tempJson.get("identifier");
            Boolean isMicMuted = (Boolean)  tempJson.get("isMicMuted");
            BigDecimal outputVolume = (BigDecimal) tempJson.get("outputVolume");
            Boolean isClipGuardOn = (Boolean)  tempJson.get("isClipGuardOn");
            int lowCutType = tempJson.getInt("lowCutType"); //unsure what this is for
            BigDecimal gain = (BigDecimal) tempJson.get("gain");
            Boolean isWaveXLR = (Boolean)  tempJson.get("isWaveXLR");
            Boolean isWaveLink = (Boolean)  tempJson.get("isWaveLink");
//            JSONArray outputVolumeLookup = (JSONArray) tempJson.get("outputVolumeLookup"); //unsure what this is for
            BigDecimal balance = (BigDecimal) tempJson.get("balance");
            Boolean isLowCutOn = (Boolean)  tempJson.get("isLowCutOn");
            String name = (String) tempJson.get("name");
            Boolean isGainLocked = tempJson.getBoolean("isGainLocked");
//            JSONArray gainLookup = (JSONArray) tempJson.get("gainLookup"); //unsure what this is for
            Microphone newMicrophone = new Microphone(identifier,isMicMuted,outputVolume,
                    isClipGuardOn,lowCutType,gain,isWaveXLR,isWaveLink,
                    balance,isLowCutOn,name,isGainLocked);
            allMics.add(newMicrophone);
        }

    }

    public void onSwitchStateConfig(JSONObject switchState) {
        //TODO: finish later
        // what you are listening to in bottom right hand corner of wave link
        JSONObject result = (JSONObject) switchState.get("result");
        String switchStateValue = (String) result.get("value");

        String monitorValue = null;
        if (switchStateValue.equals(Status.streamPackageName)) {
            monitorValue = "Stream";
            activeMixer = streamMixer;
        } else {
            monitorValue = "Local";
            activeMixer = localMixer;
        }
        //TODO: change what is being sent to the state
//        WaveLinkPlugin.waveLinkPlugin.sendStateUpdate(WaveLinkPluginConstants.WaveLinkOutputs.States.MonitoredMix.ID, monitorValue);


    }

    public void onOutputConfig(JSONObject outputConfig) {
        JSONObject result = (JSONObject) outputConfig.get("result");
        JSONArray stream = (JSONArray) result.get("streamMixer");
        Boolean streamMixerMuted = (Boolean) stream.get(0);
        int streamMixerLevel = (int) stream.get(1);
        streamMixer.setMuted(streamMixerMuted);
        streamMixer.setLevel(streamMixerLevel);

        JSONArray local = (JSONArray) result.get("localMixer");
        Boolean localMixerMuted = (Boolean) local.get(0);
        int localMixerLevel = (int) local.get(1);
        localMixer.setMuted(localMixerMuted);
        localMixer.setLevel(localMixerLevel);

    }

    public void onOutputs(JSONObject outputs) {
        allOutputs.clear();
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
                currentOutput = newOutput;
            }
            allOutputs.add(newOutput);
        }
    }

    public void onInputConfig(JSONObject inputConfigs) {
        allInputs.clear();

        JSONArray resultJson = (JSONArray) inputConfigs.get("result");
        for (int i = 0; i < resultJson.length(); i++) {
            JSONObject tempJson = (JSONObject) resultJson.get(i);


            String identifier = (String) tempJson.get("identifier");
            Boolean isAvailable = (Boolean) tempJson.get("isAvailable");



            JSONArray streamMixer = (JSONArray) tempJson.get("streamMixer"); //isMuted, level, unknown
            Boolean streamMixerMuted = (Boolean) streamMixer.get(0);
            int streamMixerLevel = (Integer) streamMixer.get(1);
            Boolean pluginBypassStream = (Boolean) streamMixer.get(2);

            String name = (String) tempJson.get("name");

            JSONArray localMixer = (JSONArray) tempJson.get("localMixer");
            Boolean localMixerMuted = (Boolean) localMixer.get(0);
            int localMixerLevel = (Integer) localMixer.get(1);
            Boolean pluginBypassLocal = (Boolean) localMixer.get(2);


            int inputType = (int) tempJson.get("inputType");
            ArrayList<InputPlugin> inputPlugins = new ArrayList<>();
            if (tempJson.keySet().contains("filters")) {
                //input has filters
                JSONArray filters = (JSONArray) tempJson.get("filters");
                for (int z = 0; z < filters.length(); z++) {
                    JSONObject tempPlugin = (JSONObject) filters.get(z);
                    String filterId = (String) tempPlugin.get("filterID");
                    String pluginId = (String) tempPlugin.get("pluginID");
                    String pluginName = (String) tempPlugin.get("name");
                    Boolean isActive = (Boolean) tempPlugin.get("isActive");
                    InputPlugin newPlugin = new InputPlugin(filterId, pluginId, pluginName, isActive);
                    inputPlugins.add(newPlugin);

                }
            }

            Input newInput = new Input(identifier,name,isAvailable,
                    streamMixerMuted,localMixerMuted,localMixerLevel,
                    streamMixerLevel,inputPlugins,inputType, pluginBypassLocal,
                    pluginBypassStream);
            newInput.setLocalMuteStateId(newInput.getName() + " Local Mixer Mute");
            newInput.setStreamMuteStateId(newInput.getName() + " Stream Mixer Mute");
            newInput.setLocalFilterBypassStateId(newInput.getName() + " Local Filter Bypass");
            newInput.setStreamFilterBypassStateId(newInput.getName() + " Stream Filter Bypass");

            newInput.setLevelLeftStateId(newInput.getName() + " Level Left Volume");
            newInput.setLevelRightStateId(newInput.getName() + " Level Right Volume");
            newInput.setLocalVolumeStateId(newInput.getName() + " Local Volume");
            newInput.setStreamVolumeStateId(newInput.getName() + " Stream Volume");

            //TODO: idk why these were commented out
//            WaveLinkPlugin.waveLinkPlugin.sendCreateState("WaveLinkInputs", );
//            waveLinkPlugin.sendCreateState("WaveLinkInputs", input.getName().replace(" ", "") + "Stream", input.getName() + " Stream", "null");
//            //create states for filters
//            waveLinkPlugin.sendCreateState("WaveLinkInputs", input.getName().replace(" ", "") + "LocalFilterBypass", input.getName() + " Local Filter Bypass", "null");
//            waveLinkPlugin.sendCreateState("WaveLinkInputs", input.getName().replace(" ", "") + "StreamFilterBypass", input.getName() + " Stream Filter Bypass", "null");


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
//                    System.out.println("Should attempt to send");
                    //TODO: prolly needs to be readded
//                    WaveLinkPlugin.waveLinkPlugin.sendDynamicStates(newInput);
//                    System.out.println("after attempt to send");
                }
            }
            else {
                allInputs.add(newInput);
                //TODO: prolly needs to be readded
//                WaveLinkPlugin.waveLinkPlugin.sendDynamicStates(newInput);
            }
        }
    }
    //LEFT OFF: finished the config processing in new class
}
