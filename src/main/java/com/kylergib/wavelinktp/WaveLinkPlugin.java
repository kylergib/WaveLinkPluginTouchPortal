package com.kylergib.wavelinktp;


import com.christophecvb.touchportal.TouchPortalPlugin;
import com.christophecvb.touchportal.annotations.*;
import com.christophecvb.touchportal.helpers.PluginHelper;
import com.christophecvb.touchportal.helpers.ReceivedMessageHelper;
import com.christophecvb.touchportal.model.*;
import com.google.gson.JsonObject;
import com.kylergib.wavelinktp.model.*;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;


@Plugin(version = BuildConfig.VERSION_CODE, colorDark = "#203060", colorLight = "#4070F0", name = "Wave Link Plugin")
public class WaveLinkPlugin extends TouchPortalPlugin implements TouchPortalPlugin.TouchPortalPluginListener {
    public static CountDownLatch latch = new CountDownLatch(1);
    public static WaveLinkClient client;
    public static WaveLinkPlugin waveLinkPlugin;
    public final static Logger LOGGER = Logger.getLogger(TouchPortalPlugin.class.getName());
    //TODO: update image
    private enum Categories {
        /**
         * Category definitions
         */
        @Category(name = "Wave Link Outputs", imagePath = "images/icon-24.png")
        WaveLinkOutputs,
        @Category(name = "Wave Link Inputs", imagePath = "images/icon-24.png")
        WaveLinkInputs,
        @Category(name = "Wave Link Mics", imagePath = "images/icon-24.png")
        WaveLinkMics
    }

    /**
     * IP address setting in touch portal,
     */
    @Setting(name = "IP", defaultValue = "localhost", maxLength = 15)
    private static String ipSetting;

    /**
     * Constructor calling super
     */
    public WaveLinkPlugin() {
        super(true);// true is for paralleling Actions executions
    }

    public static void main(String... args) throws Exception {

        if (args != null && args.length == 1) {
            if (PluginHelper.COMMAND_START.equals(args[0])) {
                // Initialize the Plugin
                waveLinkPlugin = new WaveLinkPlugin();

                boolean connectedPairedAndListening = waveLinkPlugin.connectThenPairAndListen(waveLinkPlugin);
                if (connectedPairedAndListening) {


                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }


                    WaveLinkPlugin.LOGGER.log(Level.INFO, "Trying to connect to IP: " + ipSetting);

                    //start port at 1824, if wave link is not on 1824 it will connect to the next point and then stop at 1835.
                    int port = 1824;
                    client = new WaveLinkClient(ipSetting, port);
                    while (true) {
                        latch.await();
                        if (client.isConnected != 1) {
                            if (port < 1835) {
                                port = port + 1;
                                client = new WaveLinkClient(ipSetting, port);
                                latch = new CountDownLatch(1);
                            } else {
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                    latch = new CountDownLatch(1);
                    WaveLinkPlugin.LOGGER.log(Level.INFO, "Getting Config from Wave Link");
                    SwitchState localSwitch = new SwitchState("com.elgato.mix.local", null, -1);
                    SwitchState streamSwitch = new SwitchState("com.elgato.mix.stream", null, -1);
                    Status.switchStates.add(localSwitch);
                    Status.switchStates.add(streamSwitch);
                    Status.switchMap.put("localMixer", "com.elgato.mix.local");
                    Status.switchMap.put("streamMixer", "com.elgato.mix.stream");

                    //sends commands to wave link to receive wave link configs (inputs/outputs, selected output, filters, etc)
                    WaveJson getAppInfo = new WaveJson("getApplicationInfo", 11);
                    client.send(getAppInfo.getJsonString());
                    WaveJson getMicrophoneConfig = new WaveJson("getMicrophoneConfig", 12);
                    client.send(getMicrophoneConfig.getJsonString());
                    WaveJson getSwitchState = new WaveJson("getSwitchState", 13);
                    client.send(getSwitchState.getJsonString());
                    WaveJson getOutputConfig = new WaveJson("getOutputConfig", 14);
                    client.send(getOutputConfig.getJsonString());
                    WaveJson getOutputs = new WaveJson("getOutputs", 15);
                    client.send(getOutputs.getJsonString());
                    WaveJson getInputConfigs = new WaveJson("getInputConfigs", 16);
                    client.send(getInputConfigs.getJsonString());

                    // makes sure that the plugin received all 6 of the configs before going further, so no errors occur.
                    WaveLinkPlugin.LOGGER.log(Level.INFO, "Waiting on all Wave Link configs");
                    while (true) {
                        if (client.configsReceived == 6) {
                            break;
                        } else {
                            Thread.sleep(100);
                        }
                    }
                    WaveLinkPlugin.LOGGER.log(Level.INFO, "Received all Wave Link configs");
                    waveLinkPlugin.actionUpdatePuts();
                }
            }
        }
    }


    //Output actions
    @Action(description = "Mute/Unmute/Toggle for Monitor Mix or Stream Mix", format = "Set Mute: {$value$} on mixer: {$mixerId$}", categoryId = "WaveLinkOutputs", name="Mute/Unmute Output")
    private void actionSetOutputMute(@Data(valueChoices = {"true","false","toggle"}) String[] value, @Data(valueChoices = {"Local","Stream", "Both"}) String[] mixerId) {
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Action actionSetOutputMute received: " + value[0] + " - " + mixerId[0]);
        //gets output of mixers
        for (SwitchState switchState : Status.switchStates) {
            Boolean setMute = null;

            //if mixerID is local or both, then it will mute/unmute. if set to toggle it will just toggle it.
            if (switchState.getMixerId().equals("com.elgato.mix.local") && (mixerId[0].equals("Both") || mixerId[0].equals("Local"))) {
                if (value[0].equals("toggle")) {
                    setMute = !switchState.getMuted();
                } else {
                    setMute = Boolean.valueOf(value[0]);
                }
                String localMute = "unmuted";
                if (!setMute) {
                    localMute = "muted";
                }
                WaveLinkActions.setOutputConfig("com.elgato.mix.local","Output Mute",setMute);
                switchState.setMuted(setMute);
                waveLinkPlugin.sendStateUpdate(WaveLinkPluginConstants.WaveLinkOutputs.States.LocalMixOut.ID,localMute);

                //if mixerID is stream or both, then it will mute/unmute. if set to toggle it will just toggle it.
            } else if (switchState.getMixerId().equals("com.elgato.mix.stream") && (mixerId[0].equals("Both") || mixerId[0].equals("Stream"))) {
                if (value[0].equals("toggle")) {
                    setMute = !switchState.getMuted();
                } else {
                    setMute = Boolean.valueOf(value[0]);
                }
                String streamMute = "unmuted";
                if (!setMute) {
                    streamMute = "muted";
                }
                WaveLinkActions.setOutputConfig("com.elgato.mix.stream","Output Mute",setMute);
                switchState.setMuted(setMute);
                waveLinkPlugin.sendStateUpdate(WaveLinkPluginConstants.WaveLinkOutputs.States.StreamMixOut.ID,streamMute);
            }
        }
    }

    @Action(format = "Select Monitor Mix Output {$choices$}", categoryId = "WaveLinkOutputs", name="Select Monitor Mix Output")
    private void selectMonitorMixOutput(@Data(stateId = "outputList") String[] choices) {
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Action actionSetOutputMute received: " + choices[0]);
        Status.selectMonitorMixOutputRunning = true;
        System.out.println("Start ACTION MONITOR");
        LOGGER.log(Level.INFO, "Action with Outputs received: " + choices[0]);
        System.out.println(Status.selectedOutput + " - " + choices[0]);
        if (!Status.selectedOutput.equals(choices[0])) {
            Status.selectedOutput = choices[0];
            WaveLinkActions.setMonitorMixOutput(choices[0]);

//            waveLinkPlugin.sendStateUpdate("com.kylergib.wavelinktp.WaveLinkPlugin.WaveLinkInput.state.SelectedOutput",Status.selectedOutput);
            waveLinkPlugin.sendStateUpdate(WaveLinkPluginConstants.WaveLinkOutputs.States.SelectedOutput.ID,Status.selectedOutput);
            LOGGER.log(Level.INFO, "Select " + choices[0] + " as output");
        } else {
            LOGGER.log(Level.INFO, "Could not select " + choices[0] + " as output because it is already selected");
        }
        Status.selectMonitorMixOutputRunning = false;
    }

    /**
     * Action to set input volume to a specific integer
     *
     */
    @Action(description = "Set output volume", format = "Set output volume of: {$outputMixerId$} to {$volumeValue$}",
            categoryId = "WaveLinkOutputs", name="Set Output Volume")
    private void actionSetOutputVolume(@Data(valueChoices = {"Local", "Stream", "Both"})  String[] outputMixerId, @Data(minValue = 0, maxValue = 100) Integer volumeValue) {
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Action actionSetInputVolume received: " + outputMixerId[0] + " - " + volumeValue);

        if (outputMixerId[0].equals("Local") || outputMixerId[0].equals("Both")) {
            WaveLinkActions.setOutputConfig("com.elgato.mix.local","Output Level",volumeValue);
        }
        if (outputMixerId[0].equals("Stream") || outputMixerId[0].equals("Both")) {
            WaveLinkActions.setOutputConfig("com.elgato.mix.stream","Output Level",volumeValue);
        }
    }
    @Action(format = "Switch which mix you are monitoring (Local or Stream)\nThe active output being monitored is highlighted in greet ot the bottom right of Wave Link",
            categoryId = "WaveLinkOutputs", name="Switch Monitoring Mix")
    private void actionSetSwitchOutput() {
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Action actionSetSwitchOutput");
        WaveLinkActions.setSwitchOutput();

        String monitorValue = null;
        if (Status.switchStateValue.equals("com.elgato.mix.local")) {
            monitorValue = "stream";
            Status.switchStateValue = "com.elgato.mix.stream";
        } else {
            monitorValue = "local";
            Status.switchStateValue = "com.elgato.mix.local";
        }

        waveLinkPlugin.sendStateUpdate(WaveLinkPluginConstants.WaveLinkOutputs.States.MonitoredMix.ID, monitorValue);
    }

    //Input Actions
    @Action(description = "Enable/Disable or toggle filter bypass for a specific input.\n(true means that the input will bypass the filter)",
            format = "Set: {$value$} on input: {$choices$} for {$mixerId$}",
            categoryId = "WaveLinkInputs", name="Input Filter Bypass")
    private void actionSetInputFilter(@Data(stateId = "inputList") String[] choices, @Data(valueChoices = {"true","false","toggle"}) String[] value,
                                      @Data(valueChoices = {"Local","Stream", "Both"}) String[] mixerId) {
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Action actionSetInputFilter received: " + choices[0] + " - " + value[0] + " - " + mixerId[0]);
        Boolean setFilter = null;
        for (Input input: Status.allInputs) {
            if (input.getName().equals(choices[0])) {
                if (mixerId[0].equals("Both") || mixerId[0].equals("Local")) {
                    if (value[0].equals("toggle")) {
                        if (input.getPluginBypassLocal()) {
                            setFilter = false;
                        } else {
                            setFilter = true;
                        }
                    } else {
                        setFilter = Boolean.valueOf(value[0]);
                    }
                    WaveLinkActions.setFilterByPass(input.getIdentifier(), setFilter, "com.elgato.mix.local");
                    input.setPluginBypassLocal(setFilter);
                }
                if (mixerId[0].equals("Both") || mixerId[0].equals("Stream")) {
                    if (value[0].equals("toggle")) {
                        if (input.getPluginBypassStream()) {
                            setFilter = false;
                        } else {
                            setFilter = true;
                        }
                    } else {
                        setFilter = Boolean.valueOf(value[0]);
                    }
                    WaveLinkActions.setFilterByPass(input.getIdentifier(), setFilter, "com.elgato.mix.stream");
                    input.setPluginBypassStream(setFilter);
                }
            }
        }
    }
    /**
     * Action to set input to be muted or not, can be toggled
     *
     * @param value Integer
     * @param choices list of strings
     * @param mixerId list of strings
     */

    @Action(description = "Mute Input for Monitor Mix or Stream Mix", format = "Set input: {$choices$} on mixer: {$mixerId$} to {$value$}",
            categoryId = "WaveLinkInputs", name="Mute/Unmute Input")
    private void actionSetInputMute(@Data(stateId = "inputList")  String[] choices, @Data(valueChoices = {"true","false","toggle"}) String[] value,
                                    @Data(valueChoices = {"Local","Stream", "Both"}) String[] mixerId) {
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Action actionSetInputMute received: " + value[0] + " - " + choices[0] + " - " + mixerId[0]);
        Boolean newValueLocal = null;
        Boolean newValueStream = null;
        //sorts list of inputs
        for (Input input: Status.allInputs) {
            if (input.getName().equals(choices[0])) {
                if (mixerId[0].equals("Local") || mixerId[0].equals("Both")) {

                    if (value[0].equals("toggle")) {
                        newValueLocal = !input.getLocalMixerMuted();
                    } else {
                        newValueLocal = Boolean.valueOf(value[0]);
                    }
                    WaveLinkActions.setInputConfig(input.getIdentifier(), "com.elgato.mix.local", "Mute", newValueLocal);
                    input.setLocalMixerMuted(newValueLocal);
                    String mutedValue = "unmuted";
                    if (!newValueLocal) {
                        mutedValue = "muted";
                    }
                    String inputStateId = "com.kylergib.wavelinktp.WaveLinkPlugin.WaveLinkInput.state." + input.getName() + "Local";
                    waveLinkPlugin.sendStateUpdate(inputStateId, mutedValue);
//                    waveLinkPlugin.sendStateUpdate(WaveLinkPluginConstants.WaveLinkInputs.States.input.getName() + "Local", newValueLocal);
//                    System.out.println(WaveLinkPluginConstants.WaveLinkInputs.States);
                    System.out.println(inputStateId + " - " + mutedValue);
                    System.out.println(Thread.currentThread());
                }

                if (mixerId[0].equals("Stream") || mixerId[0].equals("Both")) {
                    if (value[0].equals("toggle")) {
                        newValueStream = !input.getStreamMixerMuted();
                    } else {
                        newValueStream = Boolean.valueOf(value[0]);
                    }
                    String mutedValue = "unmuted";
                    if (!newValueStream) {
                        mutedValue = "muted";
                    }
                    WaveLinkActions.setInputConfig(input.getIdentifier(), "com.elgato.mix.stream", "Mute", newValueStream);
                    input.setStreamMixerMuted(newValueStream);
                    String inputStateId = "com.kylergib.wavelinktp.WaveLinkPlugin.WaveLinkInput.state." + input.getName() + "Stream";
                    waveLinkPlugin.sendStateUpdate(inputStateId, mutedValue);
                    System.out.println(inputStateId + " - " + mutedValue);

                }
            }
        }
    }



    /**
     * Action to set input volume to a specific integer
     *
     * @param integerValue Integer
     * @param choices list of strings
     * @param mixerId list of strings
     */
    @Action(description = "Set input volume", format = "Set input volume of: {$choices$} to {$integerValue$} on mixer: {$mixerId$}",
            categoryId = "WaveLinkInputs", name="Set Input Volume")
    private void actionSetInputVolume(@Data(stateId = "inputList")  String[] choices, @Data(minValue = 0, maxValue = 100) Integer integerValue,
                                      @Data(valueChoices = {"Local","Stream", "Both"}) String[] mixerId) {
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Action actionSetInputVolume received: " + integerValue + " - " + choices[0] + " - " + mixerId[0]);
        //sorts through all inputs
        for (Input input: Status.allInputs) {
            // if input is selected for choices it will set the volume of that to the integerValue on the selected mixerId
            if (input.getName().equals(choices[0])) {
                if (mixerId[0].equals("Local") || mixerId[0].equals("Both")) {
                    WaveLinkActions.setInputConfig(input.getIdentifier(),"com.elgato.mix.local","Volume",integerValue);
                    input.setLocalMixerLevel(integerValue);
                }
                if (mixerId[0].equals("Stream") || mixerId[0].equals("Both")) {
                    WaveLinkActions.setInputConfig(input.getIdentifier(), "com.elgato.mix.stream", "Volume", integerValue);
                    input.setStreamMixerLevel(integerValue);
                }
            }
        }
    }

    /**
     * Action to update inputs and outputs
     *
     */

    @Action(format = "Update inputs/outputs/mics",
            categoryId = "WaveLinkInputs", name="Update inputs/outputs/mics")
    private void actionUpdatePuts() {
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Action actionUpdatePuts");
        updateInputs();
        updateOutputs();
        updateMics();
    }

    public void updateInputs() {
        Status.allInputs.clear();
        Status.getInput();
        WaveJson getInputConfigs = new WaveJson("getInputConfigs", 16);
        client.send(getInputConfigs.getJsonString());
        //adds all inputs in a list to send to show in touch portal
        int numIndex = 0;
        String[] allInputsString = new String[Status.allInputs.size()];
        for (Input input: Status.allInputs) {
            allInputsString[numIndex] = input.getName();
            numIndex = numIndex + 1;

            //NEW
            String localMutedValue = "unmuted";
            if (input.getLocalMixerMuted()) {
                localMutedValue = "muted";
            }

            String streamMutedValue = "unmuted";
            if (input.getStreamMixerMuted()) {
                streamMutedValue = "muted";
            }


            waveLinkPlugin.sendCreateState("WaveLinkInput", input.getName() + "Local",input.getName() + " Local", localMutedValue);
            waveLinkPlugin.sendCreateState("WaveLinkInput", input.getName() + "Stream",input.getName() + " Stream", streamMutedValue);
            System.out.println(input.getLocalMixerMuted());
        }
        waveLinkPlugin.sendChoiceUpdate(WaveLinkPluginConstants.WaveLinkInputs.States.InputList.ID, allInputsString);
    }
    public void updateOutputs() {
        Status.allOutputs.clear();
        Status.getOutputs();
        WaveJson getOutputs = new WaveJson("getOutputs", 15);
        client.send(getOutputs.getJsonString());

        WaveJson getSwitchState = new WaveJson("getSwitchState", 13);
        client.send(getSwitchState.getJsonString());

        //adds all outputs in a list to send to show in touch portal
        String[] allOutputsString = new String[Status.allOutputs.size()];
        int numIndex = 0;
        for (Output output: Status.allOutputs) {
            allOutputsString[numIndex] = output.getName();
            numIndex = numIndex + 1;
            if (output.getSelected()) {
                Status.selectedOutput = output.getName();
                waveLinkPlugin.sendStateUpdate(WaveLinkPluginConstants.WaveLinkOutputs.States.SelectedOutput.ID,Status.selectedOutput);
            }
        }
        waveLinkPlugin.sendChoiceUpdate(WaveLinkPluginConstants.WaveLinkOutputs.States.OutputList.ID, allOutputsString);

        String monitorValue = null;
        if (Status.switchStateValue.equals("com.elgato.mix.local")) {
            monitorValue = "stream";
            Status.switchStateValue = "com.elgato.mix.stream";
        } else {
            monitorValue = "local";
            Status.switchStateValue = "com.elgato.mix.local";
        }
        waveLinkPlugin.sendStateUpdate(WaveLinkPluginConstants.WaveLinkOutputs.States.MonitoredMix.ID, monitorValue);
    }

    public void updateMics() {
        //clears the list of inputs/outputs/mics so it can get updated info
        Status.allMics.clear();
        Status.getMicrophone();
        WaveJson getMicrophoneConfig = new WaveJson("getMicrophoneConfig", 12);
        client.send(getMicrophoneConfig.getJsonString());
        //adds all mics in a list to send to show in touch portal
        int numIndex = 0;
        String[] allMicsString = new String[Status.allMics.size()];
        for (Microphone mic: Status.allMics) {
            allMicsString[numIndex] = mic.getName();
            numIndex = numIndex + 1;
        }
        waveLinkPlugin.sendChoiceUpdate(WaveLinkPluginConstants.WaveLinkMics.States.MicList.ID, allMicsString);
    }

    //Mic actions
    @Action(description = "Change mic gain, choose if you want to set it or increase/decrease it", format = "Mic: {$choices$} - {$options$} Gain: {$value$}",
            categoryId = "WaveLinkMics", name="Change Mic Gain")
    private void actionSetMicrophoneGain(@Data(stateId = "micList") String[] choices, @Data(minValue = 0,maxValue = 100) Integer value,
                                         @Data(valueChoices = {"Set","Increase","Decrease"}) String[] options) {
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Action actionSetMicrophone received: " + " - " + choices[0] + " - " + value + " - " + options[0]);
        WaveLinkActions.setMicOutputEnhancement("gain",choices[0],options[0],value);
    }

    @Action(description = "Change mic output volume, choose if you want to set it or increase/decrease it", format = "Mic: {$choices$} - {$options$} Volume: {$value$}",
            categoryId = "WaveLinkMics", name="Change Mic Output Volume")
    private void actionSetMicrophoneOutputVolume(@Data(stateId = "micList") String[] choices, @Data(minValue = 0,maxValue = 100) Integer value,
                                                 @Data(valueChoices = {"Set","Increase","Decrease"}) String[] options) {
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Action actionSetMicrophoneOutputVolume received: " + choices[0] + " - " + value + " - " + options[0]);
        WaveLinkActions.setMicOutputEnhancement("outputVolume",choices[0],options[0],value);
    }

    @Action(description = "Change mic PC/Mic mix volume, choose if you want to set it or increase/decrease it\n(0 is is closer to mic and 100 is closer to PC)", format = "Mic: {$choices$} - {$options$} Volume: {$value$}",
            categoryId = "WaveLinkMics", name="Change Mic PC/Mic Mix Volume")
    private void actionSetMicPcMix(@Data(stateId = "micList") String[] choices, @Data(minValue = 0,maxValue = 100) Integer value,
                                   @Data(valueChoices = {"Set","Increase","Decrease"}) String[] options) {
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Action actionSetMicPcMix received: " + choices[0] + " - " + value + " - " + options[0]);
        WaveLinkActions.setMicOutputEnhancement("balance",choices[0],options[0],value);
    }

    @Action(format = "Toggle Clip Guard for Mic: {$choices$}",
            categoryId = "WaveLinkMics", name="Toggle Clip Guard")
    private void actionSetMicrophoneClipGuard(@Data(stateId = "micList") String[] choices) {
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Action actionSetMicrophoneClipGuard received: " + choices[0]);
        WaveLinkActions.setMicOutputEnhancement("clipGuard",choices[0],"Set",0);

    }

    @Action(format = "Toggle Low Cut Filter for Mic: {$choices$}",
            categoryId = "WaveLinkMics", name="Toggle Low Cut Filter")
    private void actionSetMicrophoneLowCut(@Data(stateId = "micList") String[] choices) {
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Action actionSetMicrophoneLowCut received: " + choices[0]);
        WaveLinkActions.setMicOutputEnhancement("lowCut",choices[0],"Set",0);
    }

    /**
     * State that has a list of all inputs for monitor mix
     */
    @State(valueChoices = {}, defaultValue = "", categoryId = "WaveLinkInputs")
    private String[] inputList;

    /**
     * State that has a list of all outputs for monitor mix
     */
    @State(valueChoices = {}, defaultValue = "", categoryId = "WaveLinkOutputs")
    private String[] outputList;

    @State(defaultValue = "none", categoryId = "WaveLinkOutputs")
    private String selectedOutput;

    @State(defaultValue = "unmuted", categoryId = "WaveLinkOutputs",desc = "Local Mixer Output")
    private String localMixOut;

    @State(defaultValue = "unmuted", categoryId = "WaveLinkOutputs",desc = "Stream Mixer Output")
    private String streamMixOut;

    @State(defaultValue = "",categoryId = "WaveLinkOutputs", desc = "Monitor Mix Selected")
    private String monitoredMix;

    /**
     * State that has a list of all mics for monitor mix
     */
    @State(valueChoices = {}, defaultValue = "", categoryId = "WaveLinkMics")
    private String[] micList;


    /**
     * Connector for output volume - local or stream
     */
    @Connector(format = "Mixer: {$outputMixerId$}", categoryId = "WaveLinkOutputs", name="Output Volume",id="outputVolumeConnector")
    private void outputVolumeConnector(@ConnectorValue Integer volumeValue, @Data (valueChoices = {"Local","Stream","Both"}) String[] outputMixerId) {
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Connector outputVolumeConnector received: " + volumeValue + " - " + outputMixerId[0]);
        if (outputMixerId[0].equals("Local") || outputMixerId[0].equals("Both")) {
            WaveLinkActions.setOutputConfig("com.elgato.mix.local","Output Level",volumeValue);
        }
        if (outputMixerId[0].equals("Stream") || outputMixerId[0].equals("Both")) {
            WaveLinkActions.setOutputConfig("com.elgato.mix.stream","Output Level",volumeValue);
        }

    }

    /**
     * Connector for input volume
     */
    @Connector(format = "Input: {$choices$} - Mixer: {$mixerId$}", categoryId = "WaveLinkInputs", name="Input Volume", id="inputVolumeConnector")
    private void inputVolumeConnector(@ConnectorValue Integer value, @Data (stateId = "inputList") String[] choices, @Data (valueChoices = {"Local", "Stream", "Both"}) String[] mixerId) {
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Connector inputVolumeConnector received: " + choices[0] + " - " + value + " - " + mixerId[0]);
        for (Input input: Status.allInputs) {
            if (input.getName().equals(choices[0])) {
                if (mixerId[0].equals("Local") || mixerId[0].equals("Both")) {
                    WaveLinkActions.setInputConfig(input.getIdentifier(),"com.elgato.mix.local","Volume",value);
                }
                if (mixerId[0].equals("Stream") || mixerId[0].equals("Both")) {
                    WaveLinkActions.setInputConfig(input.getIdentifier(),"com.elgato.mix.stream","Volume",value);
                }
            }
        }
    }
    /**
     * Connector for mic gain volume
     */
    @Connector(format = "Mic Gain Volume - Mic: {$choices$}", categoryId = "WaveLinkMics", name="Gain Volume",id="gainVolumeConnector")
    private void micGainVolume(@ConnectorValue Integer gainValue, @Data(stateId = "micList") String[] choices) {
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Connector micGainConnector received: " + choices[0] + " - " + gainValue);
        WaveLinkActions.setMicOutputEnhancement("gain",choices[0],"Set",gainValue);
    }
    /**
     * Connector for mic output volume (headphone javck on mic)
     */
    @Connector(format = "Mic Output Volume (Headphone jack on El Gato Mic) - Mic: {$choices$}", categoryId = "WaveLinkMics", name="Mic Output Volume",id="micOutputVolumeConnector")
    private void micOutputVolume(@ConnectorValue Integer micOutputValue, @Data(stateId = "micList") String[] choices) {
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Connector micOutputVolumeConnector received: " + choices[0] + " - " + micOutputValue);
        WaveLinkActions.setMicOutputEnhancement("outputVolume",choices[0],"Set",micOutputValue);
    }
    /**
     * Connector for pc/mic mix volume
     */
    @Connector(format = "Mic/PC Mix Volume (0-Mic and 100-PC) - Mic: {$choices$}", categoryId = "WaveLinkMics", name="Mic/PC Mix Volume",id="micPcVolumeConnector")
    private void pcMicMixVolume(@ConnectorValue Integer pcMicValue, @Data(stateId = "micList") String[] choices) {
        WaveLinkPlugin.LOGGER.log(Level.INFO, " - Connector pcMicMixVolumeConnector received: " + choices[0] + " - " + pcMicValue);
        WaveLinkActions.setMicOutputEnhancement("balance",choices[0],"Set",pcMicValue);

    }




    /**
     * Called when the Socket connection is lost or the plugin has received the close Message
     */
    public void onDisconnected(Exception exception) {
        // Socket connection is lost or plugin has received close message
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Disconnected");
        System.exit(0);
    }

    /**
     * Called when receiving a message from the Touch Portal Plugin System
     */
    public void onReceived(JsonObject jsonMessage) {
        // Check if ReceiveMessage is an Action
        if (ReceivedMessageHelper.isTypeAction(jsonMessage)) {
            // Get the Action ID
            String receivedActionId = ReceivedMessageHelper.getActionId(jsonMessage);
            if (receivedActionId != null) {
                // Manually call the action methods which not all parameters are annotated with @Data
                switch (receivedActionId) {
                    // case ...:
                    // break;
                }
            }
        }
    }


    /**
     * Called when the Info Message is received when Touch Portal confirms our initial connection is successful
     */
    public void onInfo(TPInfoMessage tpInfoMessage) {

    }

    /**
     * Called when a List Change Message is received
     */
    public void onListChanged(TPListChangeMessage tpListChangeMessage) { }

    /**
     * Called when a Broadcast Message is received
     */
    public void onBroadcast(TPBroadcastMessage tpBroadcastMessage) { }

    /**
     * Called when a Settings Message is received
     */
    public void onSettings(TPSettingsMessage tpSettingsMessage) {
    }

    @Override
    public void onNotificationOptionClicked(TPNotificationOptionClickedMessage tpNotificationOptionClickedMessage) {

    }


}
