package com.kylergib.wavelinktp;


import com.christophecvb.touchportal.TouchPortalPlugin;
import com.christophecvb.touchportal.annotations.*;
import com.christophecvb.touchportal.helpers.PluginHelper;
import com.christophecvb.touchportal.helpers.ReceivedMessageHelper;
import com.christophecvb.touchportal.model.*;
import com.google.gson.JsonObject;
import com.kylergib.wavelinktp.model.*;
import org.json.JSONObject;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;




@Plugin(version = BuildConfig.VERSION_CODE, colorDark = "#203060", colorLight = "#4070F0", name = "Wave Link Plugin")
public class WaveLinkPlugin extends TouchPortalPlugin implements TouchPortalPlugin.TouchPortalPluginListener,
        MonitorAppThread.AppOpenCallback, WaveLinkCallback {
    public static CountDownLatch latch = new CountDownLatch(1);
    public static WaveLinkClient client;
    public static WaveLinkPlugin waveLinkPlugin;
    public final static Logger LOGGER = Logger.getLogger(TouchPortalPlugin.class.getName());
    private String currentIp;
    private Boolean firstRun;
    private static boolean appIsOpen;
    private MonitorAppThread monitorAppThread;

    @Override
    public void onAppOpened() {
        LOGGER.log(Level.INFO, "Wave Link opened");
        appIsOpen = true;
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, e.toString());
        }
        try {
            if (currentIp.equals("localhost") || currentIp.equals("127.0.0.1")) {
                connectToWaveLink();
                int retry = 0;
                while (!client.isOpen()) {

                    connectToWaveLink();
                    if (retry > 3) break;
                    retry += 1;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e.toString());
        }
    }

    @Override
    public void onAppClosed() {
        LOGGER.log(Level.WARNING, "Wave Link closed");
        appIsOpen = false;
        if (client.isOpen()) client.close();
    }


    private enum Categories {
        /**
         * Category definitions
         */
        @Category(name = "Wave Link Outputs", imagePath = "images/wavesound.png")
        WaveLinkOutputs,
        @Category(name = "Wave Link Inputs", imagePath = "images/wavesound.png")
        WaveLinkInputs,
        @Category(name = "Wave Link Input Levels", imagePath = "images/wavesound.png")
        WaveLinkInputLevels,
        @Category(name = "Wave Link Mics", imagePath = "images/wavesound.png")
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

    public static void main(String... args) {

        if (args != null && args.length == 1) {
            if (PluginHelper.COMMAND_START.equals(args[0])) {
                // Initialize the Plugin
                waveLinkPlugin = new WaveLinkPlugin();
                waveLinkPlugin.connectThenPairAndListen(waveLinkPlugin);

            }
        }
    }


    @Action(description = "Connect/Reconnect to Wave Link", categoryId = "WaveLinkOutputs", name="Connect/Reconnect to Wave Link")
    private void connectToWaveLink() throws Exception {
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Trying to connect to IP: " + ipSetting);
        //start port at 1824, if wave link is not on 1824 it will connect to the next point and then stop at 1835.
        firstRun = true;
        int port = 1824;

        client = new WaveLinkClient(ipSetting, port);
        while (true) {
            latch.await();
            Thread.sleep(100); //need to sleep, or it will try to connect to the next port too quickly
            if (!client.isOpen()) {

                if (port < 1829) { //stopping at 29 because i do not believe it goes past there and el gato uses 1834 for camera hub, so just trying to prevent errors.

                    client = new WaveLinkClient(ipSetting, port);
                    port = port + 1;
                    latch = new CountDownLatch(1);
                } else {
                    break;
                }
            } else {
                break;
            }
        }
        if (client.isOpen()) {
            client.setConfigCallback(this);
            WaveLinkPlugin.LOGGER.log(Level.INFO, "Getting Config from Wave Link");
            SwitchState localSwitch = new SwitchState(Status.localPackageName, null, -1, "Local");
            SwitchState streamSwitch = new SwitchState(Status.streamPackageName, null, -1, "Stream");
            Status.switchStates.add(localSwitch);
            Status.switchStates.add(streamSwitch);
            Status.switchMap.put("localMixer", Status.localPackageName);
            Status.switchMap.put("streamMixer", Status.streamPackageName);

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
        }

    }


    //Output actions
    @Action(description = "Mute/Unmute/Toggle for Monitor Mix or Stream Mix", format = "Set Mute: {$value$} on mixer: {$mixerId$}", categoryId = "WaveLinkOutputs", name="Mute/Unmute Output")
    private void actionSetOutputMute(@Data(valueChoices = {"true","false","toggle"}) String[] value, @Data(valueChoices = {"Local","Stream", "Both"}) String[] mixerId) {
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Action actionSetOutputMute received: " + value[0] + " - " + mixerId[0]);
        //gets output of mixers
        for (SwitchState switchState : Status.switchStates) {
            Boolean setMute;

            //if mixerID is local or both, then it will mute/unmute. if set to toggle it will just toggle it.
            if (switchState.getMixerId().equals(Status.localPackageName) && (mixerId[0].equals("Both") || mixerId[0].equals("Local"))) {
                if (value[0].equals("toggle")) {
                    setMute = !switchState.getMuted();
                } else {
                    setMute = Boolean.valueOf(value[0]);
                }
                String localMute = "unmuted";
                if (setMute) {
                    localMute = "muted";
                }
                WaveLinkActions.setOutputConfig(Status.localPackageName,"Output Mute",setMute);
                switchState.setMuted(setMute);
                waveLinkPlugin.sendStateUpdate(WaveLinkPluginConstants.WaveLinkOutputs.States.LocalMixOut.ID,localMute);

                //if mixerID is stream or both, then it will mute/unmute. if set to toggle it will just toggle it.
            } else if (switchState.getMixerId().equals(Status.streamPackageName) && (mixerId[0].equals("Both") || mixerId[0].equals("Stream"))) {
                if (value[0].equals("toggle")) {
                    setMute = !switchState.getMuted();
                } else {
                    setMute = Boolean.valueOf(value[0]);
                }
                String streamMute = "unmuted";
                if (setMute) {
                    streamMute = "muted";
                }
                WaveLinkActions.setOutputConfig(Status.streamPackageName,"Output Mute",setMute);
                switchState.setMuted(setMute);
                waveLinkPlugin.sendStateUpdate(WaveLinkPluginConstants.WaveLinkOutputs.States.StreamMixOut.ID,streamMute);
            }
        }
    }

    @Action(format = "Select Monitor Mix Output {$choices$}", categoryId = "WaveLinkOutputs", name="Select Monitor Mix Output")
    private void selectMonitorMixOutput(@Data(stateId = "outputList") String[] choices) {
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Action actionSetOutputMute received: " + choices[0]);
        Status.selectMonitorMixOutputRunning = true;
        LOGGER.log(Level.INFO, "Action with Outputs received: " + choices[0]);
        if (!Status.selectedOutput.equals(choices[0])) {
            Status.selectedOutput = choices[0];
            WaveLinkActions.setMonitorMixOutput(choices[0]);

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
            WaveLinkActions.setOutputConfig(Status.localPackageName,"Output Level",volumeValue);
        }
        if (outputMixerId[0].equals("Stream") || outputMixerId[0].equals("Both")) {
            WaveLinkActions.setOutputConfig(Status.streamPackageName,"Output Level",volumeValue);
        }
    }
    @Action(format = "Switch which mix you are monitoring (Local or Stream) \n The active output being monitored is highlighted in greet ot the bottom right of Wave Link. {$monitorId$}",
            categoryId = "WaveLinkOutputs", name="Switch Monitoring Mix")
    private void actionSetSwitchOutput(@Data(valueChoices = {"Local","Stream"}) String[] monitorId) {
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Action actionSetSwitchOutput");
        String monitorValue = null;
        //current switch is local and wants to switch to stream.
        if (Status.switchStateValue.equals(Status.localPackageName) && !monitorId[0].equals("Local")) {
            WaveLinkActions.setSwitchOutput();
            monitorValue = "Stream";
            Status.switchStateValue = Status.streamPackageName;
        } else if (Status.switchStateValue.equals(Status.streamPackageName) && !monitorId[0].equals("Stream")) {
            WaveLinkActions.setSwitchOutput();
            monitorValue = "Local";
            Status.switchStateValue = Status.localPackageName;
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
        Status.allInputs.stream().filter(input -> isInput(input, choices[0])).forEach(input -> {
            Boolean setFilter;
            if (isLocalMixer(mixerId[0])) {
                if (value[0].equals("toggle")) {
                    setFilter = !input.getPluginBypassLocal();
                } else {
                    setFilter = Boolean.valueOf(value[0]);
                }
                WaveLinkActions.setFilterByPass(input.getIdentifier(), setFilter, Status.localPackageName);
                input.setPluginBypassLocal(setFilter);
                waveLinkPlugin.sendStateUpdate("com.kylergib.wavelinktp.WaveLinkPlugin.WaveLinkInputs.state." + input.getName().replace(" ", "") + "LocalFilterBypass", input.getPluginBypassLocal());

            }
            if (isStreamMixer(mixerId[0])) {
                if (value[0].equals("toggle")) {
                    setFilter = !input.getPluginBypassStream();
                } else {
                    setFilter = Boolean.valueOf(value[0]);
                }
                WaveLinkActions.setFilterByPass(input.getIdentifier(), setFilter, Status.streamPackageName);
                input.setPluginBypassStream(setFilter);
                waveLinkPlugin.sendStateUpdate("com.kylergib.wavelinktp.WaveLinkPlugin.WaveLinkInputs.state." + input.getName().replace(" ", "") + "StreamFilterBypass", input.getPluginBypassStream());

            }

        });


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
        Status.allInputs.stream().filter(input -> isInput(input, choices[0])).forEach(input -> {
            Boolean newValueLocal;
            Boolean newValueStream;
            if (isLocalMixer(mixerId[0])) {

                if (value[0].equals("toggle")) {
                    newValueLocal = !input.getLocalMixerMuted();
                } else {
                    newValueLocal = Boolean.valueOf(value[0]);
                }
                WaveLinkActions.setInputConfig(input.getIdentifier(), Status.localPackageName, "Mute", newValueLocal);
                input.setLocalMixerMuted(newValueLocal);
                String mutedValue = "unmuted";
                if (newValueLocal) {
                    mutedValue = "muted";
                }
                String inputStateId = "com.kylergib.wavelinktp.WaveLinkPlugin.WaveLinkInputs.state." + input.getName().replace(" ","") + "Local";
                waveLinkPlugin.sendStateUpdate(inputStateId, mutedValue);
                System.out.println(inputStateId + " - " + mutedValue);
            }
            if (isStreamMixer(mixerId[0])) {
                if (value[0].equals("toggle")) {
                    newValueStream = !input.getStreamMixerMuted();
                } else {
                    newValueStream = Boolean.valueOf(value[0]);
                }
                String mutedValue = "unmuted";
                if (newValueStream) {
                    mutedValue = "muted";
                }
                WaveLinkActions.setInputConfig(input.getIdentifier(), Status.streamPackageName, "Mute", newValueStream);
                input.setStreamMixerMuted(newValueStream);
                String inputStateId = "com.kylergib.wavelinktp.WaveLinkPlugin.WaveLinkInputs.state." + input.getName().replace(" ","") + "Stream";
                waveLinkPlugin.sendStateUpdate(inputStateId, mutedValue);
                System.out.println(inputStateId + " - " + mutedValue);

            }
        });
    }
    /**
     * Action to set input to be muted or not, can be toggled
     *
     * @param value Integer
     * @param choices list of strings
     */

    @Action(description = "Set filter to be active/inactive for input", format = "Set input: {$choices$} for filter: {$filters$} to {$value$}",
            categoryId = "WaveLinkInputs", name="Enable/Disable input filter")
    private void actionSetInputFilterActive(@Data(stateId = "inputList")  String[] choices, @Data(valueChoices = {"true","false","toggle"}) String[] value,
                                    @Data(stateId = "filterList")  String[] filters) {
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Action actionSetInputFilter received: " + value[0] + " - " + choices[0] +
                " - " + filters[0]);
        Status.allInputs.stream().filter(input -> isInput(input, choices[0])).forEach(input -> {



                //below will only run if input has the filter
                input.getPlugins().stream().filter(inputPlugin ->
                        isInputPlugin(inputPlugin,filters[0]))
                        .forEach(inputPlugin -> {

                    Boolean newValue;
                    if (value[0].equals("toggle")) {
                        newValue = !inputPlugin.getIsActive();
                    } else {
                        newValue = Boolean.valueOf(value[0]);
                    }
                    System.out.println(String.format("%s", inputPlugin.getName(), inputPlugin.getIsActive()));
                    System.out.println(String.format("%s", input.getName()));

                    String stateId = input.getName().replace(" ","") + "Filter" + inputPlugin.getName();
                    String stateIdValue;
                    if (newValue) stateIdValue = "active";
                    else stateIdValue = "inactive";




                    inputPlugin.setIsActive(newValue);
                    WaveLinkActions.setInputFilter(input.getIdentifier(),inputPlugin.getFilterID(),newValue);
                            waveLinkPlugin.sendStateUpdate("com.kylergib.wavelinktp.WaveLinkPlugin.WaveLinkInputs.state." + stateId,stateIdValue);
                    System.out.println(stateId + " - " + stateIdValue);

                });

        });
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
        Status.allInputs.stream().filter(input -> isInput(input, choices[0])).forEach(input -> {
            if (isLocalMixer(mixerId[0])) {
                WaveLinkActions.setInputConfig(input.getIdentifier(),Status.localPackageName,"Volume",integerValue);
                input.setLocalMixerLevel(integerValue);
                Status.setInputValue(input.getName(),integerValue,"Local");
            }
            if (isStreamMixer(mixerId[0])) {
                WaveLinkActions.setInputConfig(input.getIdentifier(), Status.streamPackageName, "Volume", integerValue);
                input.setStreamMixerLevel(integerValue);
                Status.setInputValue(input.getName(),integerValue,"Stream");
            }
        });
    }

    /**
     * Action to update inputs and outputs
     *
     */

    @Action(format = "Update inputs/outputs/mics",
            categoryId = "WaveLinkInputs", name="Update inputs/outputs/mics")
    public void actionUpdatePuts() throws InterruptedException {
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Action actionUpdatePuts");
        updateInputs();

        updateMics();
        updateOutputs();
        updateInputValues();
    }

    public void updateInputs() throws InterruptedException {
        if (!firstRun) {
            Status.allInputs.clear();
            Status.getInput();
            WaveJson getInputConfigs = new WaveJson("getInputConfigs", 16);
            client.send(getInputConfigs.getJsonString());
        }
        //adds all inputs in a list to send to show in touch portal
        int numIndex = 0;
        String[] allInputsString = new String[Status.allInputs.size()];

        for (Input input: Status.allInputs) {
            allInputsString[numIndex] = input.getName();
            numIndex = numIndex + 1;



            //create dynamic states and set it to null
            if (!Status.sentStates.contains(input.getName().replace(" ",""))) {
                waveLinkPlugin.sendCreateState("WaveLinkInputs", input.getName().replace(" ", "") + "Local", input.getName() + " Local", "null");
                waveLinkPlugin.sendCreateState("WaveLinkInputs", input.getName().replace(" ", "") + "Stream", input.getName() + " Stream", "null");
                //create states for filters
                waveLinkPlugin.sendCreateState("WaveLinkInputs", input.getName().replace(" ", "") + "LocalFilterBypass", input.getName() + " Local Filter Bypass", "null");
                waveLinkPlugin.sendCreateState("WaveLinkInputs", input.getName().replace(" ", "") + "StreamFilterBypass", input.getName() + " Stream Filter Bypass", "null");

                Status.sentStates.add(input.getName().replace(" ",""));
                System.out.println("Sent state: " + input.getName().replace(" ",""));

                //TODO: adding states for each input
                //NEW
                input.getPlugins().forEach(inputPlugin -> {
                    String inputFilterStateId = input.getName().replace(" ", "") + "Filter" + inputPlugin.getName();
                    String inputFilterStateDescription = input.getName() + " " + inputPlugin.getName() + " (Filter)";
                    String stateIdValue;
                    if (inputPlugin.getIsActive()) stateIdValue = "active";
                    else stateIdValue = "inactive";

                    waveLinkPlugin.sendCreateState("WaveLinkInputs", inputFilterStateId,
                            inputFilterStateDescription, stateIdValue);


                });
            }



        }


        waveLinkPlugin.sendChoiceUpdate(WaveLinkPluginConstants
                .WaveLinkInputs.States.InputList.ID, allInputsString);

    }

    public void updateInputValues() {
        Status.allInputs.forEach(input -> {

            String localMutedValue = "unmuted";
            if (input.getLocalMixerMuted()) {
                localMutedValue = "muted";
            }

            String streamMutedValue = "unmuted";
            if (input.getStreamMixerMuted()) {
                streamMutedValue = "muted";
            }

            //updates states with values of muted or unmuted so it will update any buttons when the plugin loads
            waveLinkPlugin.sendStateUpdate("com.kylergib.wavelinktp.WaveLinkPlugin.WaveLinkInputs.state." + input.getName().replace(" ","") + "Local",localMutedValue);
            waveLinkPlugin.sendStateUpdate("com.kylergib.wavelinktp.WaveLinkPlugin.WaveLinkInputs.state." + input.getName().replace(" ","") + "Stream",streamMutedValue);

            waveLinkPlugin.sendStateUpdate("com.kylergib.wavelinktp.WaveLinkPlugin.WaveLinkInputs.state." + input.getName().replace(" ", "") + "LocalFilterBypass", input.getPluginBypassLocal());
            waveLinkPlugin.sendStateUpdate("com.kylergib.wavelinktp.WaveLinkPlugin.WaveLinkInputs.state." + input.getName().replace(" ", "") + "StreamFilterBypass", input.getPluginBypassStream());

            Status.setInputValue(input.getName(), input.getLocalMixerLevel(), "Local");
            Status.setInputValue(input.getName(), input.getStreamMixerLevel(), "Stream");
        });

    }
    public void updateOutputs() {
        System.out.println("STARTING OUTPUTS UPDATE");
        if (!firstRun) {
            Status.allOutputs.clear();
            Status.getOutputs();
            WaveJson getOutputs = new WaveJson("getOutputs", 15);
            client.send(getOutputs.getJsonString());

            WaveJson getSwitchState = new WaveJson("getSwitchState", 13);
            client.send(getSwitchState.getJsonString());
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, e.toString());
            }
        }

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

    }

    public void updateMics() {
        //clears the list of inputs/outputs/mics so it can get updated info
        if (!firstRun) {
            Status.allMics.clear();
            Status.getMicrophone();
            WaveJson getMicrophoneConfig = new WaveJson("getMicrophoneConfig", 12);
            client.send(getMicrophoneConfig.getJsonString());
        }
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
    @Action(description = "Change mic gain, choose if you want to set it", format = "Mic: {$choices$} - {$options$} Gain: {$value$}",
            categoryId = "WaveLinkMics", name="Change Mic Gain")
    private void actionSetMicrophoneGain(@Data(stateId = "micList") String[] choices, @Data(minValue = 0,maxValue = 40) Integer value,
                                         @Data(valueChoices = {"Set"}, defaultValue = "Set") String[] options) {
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Action actionSetMicrophone received: " + " - " + choices[0] + " - " + value + " - " + options[0]);
        WaveLinkActions.setMicOutputEnhancement("gain",choices[0],options[0],value);
    }

    @Action(description = "Change mic output volume, choose if you want to set it", format = "Mic: {$choices$} - {$options$} Volume: {$value$}",
            categoryId = "WaveLinkMics", name="Change Mic Output Volume")
    private void actionSetMicrophoneOutputVolume(@Data(stateId = "micList") String[] choices, @Data(minValue = 0,maxValue = 40) Integer value,
                                                 @Data(valueChoices = {"Set"}, defaultValue = "Set") String[] options) {
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Action actionSetMicrophoneOutputVolume received: " + choices[0] + " - " + value + " - " + options[0]);
        WaveLinkActions.setMicOutputEnhancement("outputVolume",choices[0],options[0],value);
    }

    @Action(description = "Change mic PC/Mic mix volume, choose if you want to set it\n(0 is is closer to mic and 100 is closer to PC)", format = "Mic: {$choices$} - {$options$} Volume: {$value$}",
            categoryId = "WaveLinkMics", name="Change Mic PC/Mic Mix Volume")
    private void actionSetMicPcMix(@Data(stateId = "micList") String[] choices, @Data(minValue = 0,maxValue = 40) Integer value,
                                   @Data(valueChoices = {"Set"}, defaultValue = "Set") String[] options) {
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Action actionSetMicPcMix received: " + choices[0] + " - " + value + " - " + options[0]);
        WaveLinkActions.setMicOutputEnhancement("balance",choices[0],options[0],value);
    }

    @Action(format = "Toggle Clip Guard for Mic: {$choices$}",
            categoryId = "WaveLinkMics", name="Toggle Clip Guard")
    private void actionSetMicrophoneClipGuard(@Data(stateId = "micList") String[] choices) {
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Action actionSetMicrophoneClipGuard received: " + choices[0]);
        WaveLinkActions.setMicOutputEnhancement("Microphone ClipGuard",choices[0],"Set",0);

    }

    @Action(format = "Toggle Low Cut Filter for Mic: {$choices$}",
            categoryId = "WaveLinkMics", name="Toggle Low Cut Filter")
    private void actionSetMicrophoneLowCut(@Data(stateId = "micList") String[] choices) {
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Action actionSetMicrophoneLowCut received: " + choices[0]);
        WaveLinkActions.setMicOutputEnhancement("Microphone LowCut",choices[0],"Set",0);

    }

    @Action(format = "Toggle Mute for Mic: {$choices$}",
            categoryId = "WaveLinkMics", name="Toggle Mute")
    private void actionSetMicrophoneMute(@Data(stateId = "micList") String[] choices) {
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Action actionSetMicrophoneMute received: " + choices[0]);
        WaveLinkActions.setMicOutputEnhancement("Microphone Mute",choices[0],"Set",0);
    }

    /**
     * State that has a list of all inputs for monitor mix
     */
    @State(valueChoices = {}, defaultValue = "", categoryId = "WaveLinkInputs")
    private String[] inputList;

    @State(valueChoices = {}, defaultValue = "", categoryId = "WaveLinkInputs")
    private String[] filterList;


    /**
     * State that has a list of all outputs for monitor mix
     */
    @State(valueChoices = {}, defaultValue = "", categoryId = "WaveLinkOutputs")
    private String[] outputList;



    @State(defaultValue = "none", categoryId = "WaveLinkOutputs")
    private String selectedOutput;

    @State(defaultValue = "", categoryId = "WaveLinkOutputs",desc = "Local Mixer Output")
    private String localMixOut;

    @State(defaultValue = "", categoryId = "WaveLinkOutputs",desc = "Stream Mixer Output")
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
    private void outputVolumeConnector(@ConnectorValue Integer volumeValue, @Data (valueChoices = {"Local","Stream"}) String[] outputMixerId) {
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Connector outputVolumeConnector received: " + volumeValue + " - " + outputMixerId[0]);
        if (outputMixerId[0].equals("Local") || outputMixerId[0].equals("Both")) {
            WaveLinkActions.setOutputConfig(Status.localPackageName,"Output Level",volumeValue);
        }
        if (outputMixerId[0].equals("Stream") || outputMixerId[0].equals("Both")) {
            WaveLinkActions.setOutputConfig(Status.streamPackageName,"Output Level",volumeValue);
        }

    }

    /**
     * Connector for input volume
     */
    @Connector(format = "Input: {$choices$} - Mixer: {$mixerId$}", categoryId = "WaveLinkInputs", name="Input Volume", id="inputVolumeConnector")
    private void inputVolumeConnector(@ConnectorValue Integer value, @Data (stateId = "inputList") String[] choices,
                                      @Data (valueChoices = {"Local", "Stream", "Both"}) String[] mixerId) {
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Connector inputVolumeConnector received: " + choices[0] + " - " + value + " - " + mixerId[0]);
        Status.allInputs.stream().filter(input -> isInput(input, choices[0])).forEach(input -> {
            System.out.println("THIS IS PLUGIN ID - " + WaveLinkPluginConstants.ID );
            System.out.println("THIS IS CONNECT ID - " + WaveLinkPluginConstants.WaveLinkInputs.Connectors.InputVolumeConnector.ID );
            System.out.println("THIS IS MIXER ID - " + WaveLinkPluginConstants.WaveLinkInputs.Connectors.InputVolumeConnector.MixerId.ID);


            if (isLocalMixer(mixerId[0])) {
                WaveLinkActions.setInputConfig(input.getIdentifier(),Status.localPackageName,"Volume",value);
            }
            if (isStreamMixer(mixerId[0])) {
                WaveLinkActions.setInputConfig(input.getIdentifier(),Status.streamPackageName,"Volume",value);
            }

        });
    }
    /**
     * Connector for mic gain volume
     */
//    @Connector(format = "Mic Gain Volume - Mic: {$choices$}", categoryId = "WaveLinkMics", name="Gain Volume",id="gainVolumeConnector")
//    private void micGainVolume(@ConnectorValue Integer gainValue, @Data(stateId = "micList") String[] choices) {
//        WaveLinkPlugin.LOGGER.log(Level.INFO, "Connector micGainConnector received: " + choices[0] + " - " + gainValue);
//        WaveLinkActions.setMicOutputEnhancement("gain",choices[0],"Set",gainValue);
//    }
    /**
     * Connector for mic output volume (headphone javck on mic)
     */
//    @Connector(format = "Mic Output Volume (Headphone jack on El Gato Mic) - Mic: {$choices$}", categoryId = "WaveLinkMics", name="Mic Output Volume",id="micOutputVolumeConnector")
//    private void micOutputVolume(@ConnectorValue Integer micOutputValue, @Data(stateId = "micList") String[] choices) {
//        WaveLinkPlugin.LOGGER.log(Level.INFO, "Connector micOutputVolumeConnector received: " + choices[0] + " - " + micOutputValue);
//        WaveLinkActions.setMicOutputEnhancement("outputVolume",choices[0],"Set",micOutputValue);
//    }
    /**
     * Connector for pc/mic mix volume
     */
//    @Connector(format = "Mic/PC Mix Volume (0-Mic and 100-PC) - Mic: {$choices$}", categoryId = "WaveLinkMics", name="Mic/PC Mix Volume",id="micPcVolumeConnector")
//    private void pcMicMixVolume(@ConnectorValue Integer pcMicValue, @Data(stateId = "micList") String[] choices) {
//        WaveLinkPlugin.LOGGER.log(Level.INFO, " - Connector pcMicMixVolumeConnector received: " + choices[0] + " - " + pcMicValue);
//        WaveLinkActions.setMicOutputEnhancement("balance",choices[0],"Set",pcMicValue);
//
//    }




    /**
     * Called when the Socket connection is lost or the plugin has received the close Message
     */
    public void onDisconnected(Exception exception) {
        // Socket connection is lost or plugin has received close message
        client.close();
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Disconnected");
        monitorAppThread.requestStop();
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
        currentIp = ipSetting;
        boolean updateAvailable = checkForUpdate();
        if (updateAvailable) {
            waveLinkPlugin.sendShowNotification(
                    WaveLinkPluginConstants.WaveLinkInputs.ID + ".updateNotification",
                    "Update is available. ",
                    "You are on version: " + BuildConfig.VERSION_CODE + " and an update is available on GitHub",
                    new TPNotificationOption[]{
                            new TPNotificationOption(WaveLinkPluginConstants.WaveLinkInputs.ID + ".updateNotification.options.openLink", "Open Link")
                    });


        }
        if (monitorAppThread == null && (ipSetting.equals("localhost") || ipSetting.equals("127.0.0.1"))) {
            monitorAppThread = new MonitorAppThread(this);
            monitorAppThread.start();
        } else {
            try {
                connectToWaveLink();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.toString());
            }
        }

    }

    /**
     * Called when a List Change Message is received
     */
    public void onListChanged(TPListChangeMessage tpListChangeMessage) {
        boolean isInputFilter = tpListChangeMessage.actionId.equals(
                "com.kylergib.wavelinktp.WaveLinkPlugin.WaveLinkInputs.action.actionSetInputFilterActive");
        boolean isFirstOption = tpListChangeMessage.listId.equals("com.kylergib.wavelinktp.WaveLinkPlugin.WaveLinkInputs.state.inputList");

        if (isInputFilter && isFirstOption) {
            String inputName = tpListChangeMessage.value;
            List<String> filterList = new ArrayList<>();

            Status.allInputs.stream().filter(input -> isInput(input,inputName)).forEach(input -> {
                input.getPlugins().forEach(inputPlugin -> {
                    filterList.add(inputPlugin.getName());
                });
            });

            waveLinkPlugin.sendChoiceUpdate(WaveLinkPluginConstants
                    .WaveLinkInputs.States.FilterList.ID, filterList.toArray(new String[0]), true);
//            }
        }


    }

    /**
     * Called when a Broadcast Message is received
     */
    public void onBroadcast(TPBroadcastMessage tpBroadcastMessage) { }

    /**
     * Called when a Settings Message is received
     */
    public void onSettings(TPSettingsMessage tpSettingsMessage) {
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Plugin Settings Changed");
        WaveLinkPlugin.LOGGER.log(Level.INFO, currentIp);
        WaveLinkPlugin.LOGGER.log(Level.INFO, ipSetting);
        WaveLinkPlugin.LOGGER.log(Level.INFO, String.valueOf(currentIp.equals(ipSetting)));
        if (!currentIp.equals(ipSetting)) {
            currentIp = ipSetting;
            boolean monitorActive = (monitorAppThread != null && monitorAppThread.isAlive());
            boolean isLocalhost = (currentIp.equals("localhost") || currentIp.equals("127.0.0.1"));

            if (client.isOpen()) {
                client.close();
                WaveLinkPlugin.LOGGER.log(Level.INFO, "Closed previous connection to Wave Link");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    LOGGER.log(Level.WARNING, e.toString());
                }
            }

            if (monitorActive && !isLocalhost) {
                monitorAppThread.requestStop();
                LOGGER.log(Level.INFO, "requested monitor to stop");
            } else if (isLocalhost && !monitorActive) {
                monitorAppThread = new MonitorAppThread(this);
                monitorAppThread.start();
                LOGGER.log(Level.INFO, "requested monitor to start");
                return;

            }

            try {
                int retry = 0;
                while (!client.isOpen()) {

                    connectToWaveLink();
                    if (retry > 3) break;
                    retry += 1;
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.toString());
            }
        }
    }

    @Override
    public void onNotificationOptionClicked(TPNotificationOptionClickedMessage tpNotificationOptionClickedMessage) {
        if (tpNotificationOptionClickedMessage.notificationId.equals(WaveLinkPluginConstants.WaveLinkInputs.ID + ".updateNotification")) {
            if (tpNotificationOptionClickedMessage.optionId.equals(WaveLinkPluginConstants.WaveLinkInputs.ID + ".updateNotification.options.openLink")) {

                LOGGER.log(Level.INFO, "Update option clicked");
                //TODO: redirect to github
                String url = "https://github.com/kylergib/WaveLinkPluginTouchPortal";

                // Create a URI object from the URL
                URI uri = null;
                try {
                    uri = new URI(url);
                } catch (URISyntaxException e) {
                    LOGGER.log(Level.WARNING, e.toString());
                }
                // Check if the Desktop API is supported on the current platform
                if (Desktop.isDesktopSupported()) {
                    // Get the desktop instance
                    Desktop desktop = Desktop.getDesktop();

                    // Check if the desktop can browse the URI
                    if (desktop.isSupported(Desktop.Action.BROWSE)) {
                        // Open the URL in the default browser
                        try {
                            desktop.browse(uri);
                        } catch (IOException e) {
                            LOGGER.log(Level.WARNING, e.toString());
                        }
                    }
                }
            }
        }
    }

    /**
     * Called when plugin receives all the configs needed from Wave Link
     */
    @Override
    public void onConfigsReceived() {
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Received all Wave Link configs");
        try {
            waveLinkPlugin.actionUpdatePuts();
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, e.toString());
        }
        firstRun = false;
    }

    private boolean isInput(Input input, String choice) {
        return input.getName().equals(choice);
    }
    private boolean isInputPlugin(InputPlugin inputPlugin, String choice) {
        return inputPlugin.getName().equals(choice);
    }
    private boolean isLocalMixer(String mixerId) {
        return mixerId.equals("Local") || mixerId.equals("Both");
    }
    private boolean isStreamMixer(String mixerId) {
        return mixerId.equals("Stream") || mixerId.equals("Both");
    }

    public boolean checkForUpdate() {
        //TODO: replace with right info after commit
        String repositoryOwner = "kylergib";
        String repositoryName = "WaveLinkPluginTouchPortal";

        try {
            URL url = new URL("https://api.github.com/repos/" + repositoryOwner + "/" + repositoryName + "/releases/latest");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

            if (conn.getResponseCode() != 200) {
                LOGGER.log(Level.INFO, "Failed : HTTP Error code : " + conn.getResponseCode());
                return false;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            String output;
            StringBuilder response = new StringBuilder();

            while ((output = br.readLine()) != null) {
                response.append(output);
            }

            conn.disconnect();
            JSONObject responseJSON = new JSONObject(response.toString());

            List<String> version = Arrays.asList(responseJSON.getString("tag_name").split("\\."));
            int newestVersion = 0;
            if (version.size() == 3) {
                newestVersion = (Integer.valueOf(version.get(0)) * 1000) +
                        (Integer.valueOf(version.get(1)) * 100) +
                        (Integer.valueOf(version.get(2)));
            }
            int currentVersion = Integer.valueOf(String.valueOf(BuildConfig.VERSION_CODE));

            LOGGER.log(Level.INFO, "Newest version available is: " + newestVersion);
            LOGGER.log(Level.INFO, "Current version is: " + currentVersion);
            return currentVersion < newestVersion;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


}
