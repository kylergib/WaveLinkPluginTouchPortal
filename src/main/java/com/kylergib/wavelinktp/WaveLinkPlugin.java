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
    public final static Logger LOGGER = Logger.getLogger(TouchPortalPlugin.class.getName());

    private static String hostname;

    public static void main(String... args) throws Exception {

        if (args != null && args.length == 1) {
            if (PluginHelper.COMMAND_START.equals(args[0])) {
                // Initialize the Plugin
                WaveLinkPlugin waveLinkPlugin = new WaveLinkPlugin();
                // Load a properties File
                waveLinkPlugin.loadProperties("plugin.config");
                // Get a property
                WaveLinkPlugin.LOGGER.log(Level.INFO, waveLinkPlugin.getProperty("samplekey"));
                // Set a property
                waveLinkPlugin.setProperty("samplekey", "Value set from Plugin new");
                // Store the properties
                waveLinkPlugin.storeProperties();
                // Initiate the connection with the Touch Portal Plugin System
                boolean connectedPairedAndListening = waveLinkPlugin.connectThenPairAndListen(waveLinkPlugin);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {}

                int port = 1824;
                WaveLinkPlugin.LOGGER.log(Level.INFO, "Trying to connect to IP: " + ipSetting);
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
                SwitchState localSwitch = new SwitchState("com.elgato.mix.local",null, -1);
                SwitchState streamSwitch = new SwitchState("com.elgato.mix.stream",null, -1);
                Status.switchStates.add(localSwitch);
                Status.switchStates.add(streamSwitch);
                Status.switchMap.put("localMixer","com.elgato.mix.local");
                Status.switchMap.put("streamMixer","com.elgato.mix.stream");


                WaveJson getAppInfo = new WaveJson("getApplicationInfo", 11);
                client.send(getAppInfo.getJsonString());
                WaveJson getMicrophoneconfig = new WaveJson("getMicrophoneConfig", 12);
                client.send(getMicrophoneconfig.getJsonString());
                WaveJson getSwitchState = new WaveJson("getSwitchState", 13);
                client.send(getSwitchState.getJsonString());
                WaveJson getOutputConfig = new WaveJson("getOutputConfig", 14);
                client.send(getOutputConfig.getJsonString());
                WaveJson getOutputs = new WaveJson("getOutputs", 15);
                client.send(getOutputs.getJsonString());
                WaveJson getInputConfigs = new WaveJson("getInputConfigs", 16);
                client.send(getInputConfigs.getJsonString());


                while (true) {
                    if (client.configsReceived == 6) {
                        break;
                    } else {
                        Thread.sleep(100);
                    }
                }

                WaveLinkPlugin.LOGGER.log(Level.INFO, "Received all Wave Link configs");
                String[] allOutputsString = new String[Status.allOutputs.size()];

                int numIndex = 0;
                for (Output output: Status.allOutputs) {

//                    System.out.println(output.getName() + " - " + output.getSelected());
                    allOutputsString[numIndex] = output.getName();
                    numIndex = numIndex + 1;
                    if (output.getSelected() == true) {
                        Status.selectedOutput = output.getName();
                    }

                }
                numIndex = 0;
                String[] allInputsString = new String[Status.allInputs.size()];
                for (Input input: Status.allInputs) {

//                    System.out.println(input.getName());
                    allInputsString[numIndex] = input.getName();
                    numIndex = numIndex + 1;

                }
                waveLinkPlugin.sendChoiceUpdate(WaveLinkPluginConstants.WaveLinkCategory.States.OutputList.ID, allOutputsString);
                waveLinkPlugin.sendChoiceUpdate(WaveLinkPluginConstants.WaveLinkCategory.States.InputList.ID, allInputsString);

            }
        }
    }
    //TODO: update image
    private enum Categories {
        /**
         * Category definitions
         */
        @Category(name = "Wave Link", imagePath = "images/icon-24.png")
        WaveLinkCategory

    }

    @Action(description = "Mute/Unmute/Toggle for Monitor Mix or Stream Mix", format = "Set Mute: {$value$} on mixer: {$mixerId$}",
            categoryId = "WaveLinkCategory", name="Mute/Unmute Output")
    private void actionSetOutputMute(@Data(valueChoices = {"true","false","toggle"}) String[] value,
                                     @Data(valueChoices = {"Local","Stream", "Both"}) String[] mixerId) {
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Action actionSetOutputMute received: " + value[0] + " - " + mixerId[0]);

        for (SwitchState switchState : Status.switchStates) {
            Boolean setMute = null;
            if (switchState.getMixerId().equals("com.elgato.mix.local") && (mixerId[0].equals("Both") || mixerId[0].equals("Local"))) {
                if (value[0].equals("toggle")) {
                    if (switchState.getMuted()) {
                        setMute = false;
                    } else {
                        setMute = true;
                    }
                } else {
                    setMute = Boolean.valueOf(value[0]);
                }
                WaveLinkActions.setOutputConfig("com.elgato.mix.local","Output Mute",setMute);
                switchState.setMuted(setMute);
            } else if (switchState.getMixerId().equals("com.elgato.mix.stream") && (mixerId[0].equals("Both") || mixerId[0].equals("Stream"))) {
                if (value[0].equals("toggle")) {
                    if (switchState.getMuted()) {
                        setMute = false;
                    } else {
                        setMute = true;
                    }
                } else {
                    setMute = Boolean.valueOf(value[0]);
                }
                WaveLinkActions.setOutputConfig("com.elgato.mix.stream","Output Mute",setMute);
                switchState.setMuted(setMute);
            }
        }
    }




    /**
     * Action example with a Data Integer and Double
     *
     * @param integerValue Integer
     */
    @Action(description = "Set input volume", format = "Set input volume of: {$choices$} to {$integerValue$} on mixer: {$mixerId$}",
            categoryId = "WaveLinkCategory", name="Set Input Volume")
    private void actionSetInputVolume(@Data(stateId = "inputList")  String[] choices, @Data(minValue = 0, maxValue = 100) Integer integerValue,
                                      @Data(valueChoices = {"Local","Stream", "Both"}) String[] mixerId) {
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Action actionSetInputVolume received: " + integerValue + " - " + choices[0] + " - " + mixerId);
        for (Input input: Status.allInputs) {
            if (input.getName().equals(choices[0])) {
                if (mixerId[0].equals("Both")) {
                    WaveLinkActions.setInputConfig(input.getIdentifier(),"com.elgato.mix.local","Volume",integerValue);
                    input.setLocalMixerLevel(integerValue);
                    WaveLinkActions.setInputConfig(input.getIdentifier(),"com.elgato.mix.stream","Volume",integerValue);
                    input.setStreamMixerLevel(integerValue);

                } else if (mixerId[0].equals("Local")) {
                    WaveLinkActions.setInputConfig(input.getIdentifier(),"com.elgato.mix.local","Volume",integerValue);
                    input.setLocalMixerLevel(integerValue);
                } else if (mixerId[0].equals("Stream")) {
                    WaveLinkActions.setInputConfig(input.getIdentifier(), "com.elgato.mix.stream", "Volume", integerValue);
                    input.setStreamMixerLevel(integerValue);
                }
            }
        }
    }

    @Action(description = "Mute Input for Monitor Mix or Stream Mix", format = "Set input: {$choices$} on mixer: {$mixerId$} to {$value$}",
            categoryId = "WaveLinkCategory", name="Mute/Unmute Input")
    private void actionSetInputMute(@Data(stateId = "inputList")  String[] choices, @Data(valueChoices = {"true","false","toggle"}) String[] value,
                                    @Data(valueChoices = {"Local","Stream", "Both"}) String[] mixerId) {
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Action actionSetInputMute received: " + value[0] + " - " + choices[0] + " - " + mixerId[0]);
        Boolean newValueLocal = null;
        Boolean newValueStream = null;

        for (Input input: Status.allInputs) {
            if (input.getName().equals(choices[0])) {
                if (mixerId[0].equals("Both")) {
                    if (value[0].equals("toggle")) {
                        if (input.getLocalMixerMuted()) {
                            newValueLocal = false;
                        } else {
                            newValueLocal = true;
                        }
                        if (input.getStreamMixerMuted()) {
                            newValueStream = false;
                        } else {
                            newValueStream = true;
                        }


                    } else {
                        newValueLocal = Boolean.valueOf(value[0]);
                        newValueStream = Boolean.valueOf(value[0]);
                    }
                    WaveLinkActions.setInputConfig(input.getIdentifier(),"com.elgato.mix.local","Mute",newValueLocal);
                    input.setLocalMixerMuted(newValueLocal);
                    WaveLinkActions.setInputConfig(input.getIdentifier(),"com.elgato.mix.stream","Mute",newValueStream);
                    input.setStreamMixerMuted(newValueStream);


                } else if (mixerId[0].equals("Local")) {
                    if (value[0].equals("toggle")) {
                        if (input.getLocalMixerMuted() == true) {
                            newValueLocal = false;
                        } else if (input.getLocalMixerMuted() == false) {
                            newValueLocal = true;
                        }
                    } else {
                        newValueLocal = Boolean.valueOf(value[0]);
                    }
                    WaveLinkActions.setInputConfig(input.getIdentifier(),"com.elgato.mix.local","Mute",newValueLocal);
                    input.setLocalMixerMuted(newValueLocal);

                } else if (mixerId[0].equals("Stream")) {
                    if (value[0].equals("toggle")) {
                        if (input.getStreamMixerMuted() == true) {
                            newValueStream = false;
                        } else if (input.getStreamMixerMuted() == false) {
                            newValueStream = true;
                        }
                    } else {
                        newValueStream = Boolean.valueOf(value[0]);
                    }
                    WaveLinkActions.setInputConfig(input.getIdentifier(), "com.elgato.mix.stream", "Mute", newValueStream);
                    input.setStreamMixerMuted(newValueStream);

                }
            }
        }
    }


    /**
     * State that has a list of all outputs for monitor mix
     */
    @State(valueChoices = {}, defaultValue = "", categoryId = "WaveLinkCategory")
    private String[] inputList;

    /**
     * State that has a list of all outputs for monitor mix
     */
    @State(valueChoices = {}, defaultValue = "", categoryId = "WaveLinkCategory")
    private String[] outputList;

    @Action(format = "Select Monitor Mix Output {$choices$}", categoryId = "WaveLinkCategory", name="Select Monitor Mix Output")
    private void selectMonitorMixOutput(@Data(stateId = "outputList") String[] choices) {
        LOGGER.log(Level.INFO, "Action with Outputs received: " + choices[0]);
//        WaveLinkActions.setInputConfig("com.hnc.Discord","com.elgato.mix.local","Volume",95);
        System.out.println(Status.selectedOutput + " - " + choices[0]);
        if (!Status.selectedOutput.equals(choices[0])) {
            WaveLinkActions.setMonitorMixOutput(choices[0]);
            Status.selectedOutput = choices[0];
        } else {
            LOGGER.log(Level.INFO, "Could not select " + choices[0] + " as output because it is already selected");
        }

    }
    /**
     * Connector example with no parameter
     */
    @Connector(format = "Mixer: {$outputMixerId$}", categoryId = "WaveLinkCategory", name="Output Volume",id="outputVolumeConnector")
    private void outputVolumeConnector(@ConnectorValue Integer volumeValue, @Data (valueChoices = {"Local","Stream","Both"}) String[] outputMixerId) {
        WaveLinkPlugin.LOGGER.log(Level.INFO, String.format(" - Connector outputVolumeConnector received: value[%d]", volumeValue));
//        System.out.println("Trying output volume " + outputMixerId[0]);
        if (outputMixerId[0].equals("Both")) {
            WaveLinkActions.setOutputConfig("com.elgato.mix.local","Output Level",volumeValue);
            WaveLinkActions.setOutputConfig("com.elgato.mix.stream","Output Level",volumeValue);
        } else if (outputMixerId[0].equals("Local")) {
            WaveLinkActions.setOutputConfig("com.elgato.mix.local","Output Level",volumeValue);
        } else if (outputMixerId[0].equals("Stream")) {
            WaveLinkActions.setOutputConfig("com.elgato.mix.stream","Output Level",volumeValue);
        }

    }

    @Connector(format = "Input: {$choices$}  -  Mixer: {$mixerId$}", categoryId = "WaveLinkCategory", name="Input Volume", id="inputVolumeConnector")
    private void inputVolumeConnector(@ConnectorValue Integer value, @Data (stateId = "inputList") String[] choices, @Data (valueChoices = {"Local", "Stream", "Both"}) String[] mixerId) {
        WaveLinkPlugin.LOGGER.log(Level.INFO, String.format(choices[0] + " - Connector inputVolumeConnector received: value[%d]", value));
        for (Input input: Status.allInputs) {
            if (input.getName().equals(choices[0])) {
                if (mixerId[0].equals("Both")) {
                    WaveLinkActions.setInputConfig(input.getIdentifier(),"com.elgato.mix.local","Volume",value);
                    WaveLinkActions.setInputConfig(input.getIdentifier(),"com.elgato.mix.stream","Volume",value);

                } else if (mixerId[0].equals("Local")) {
                    WaveLinkActions.setInputConfig(input.getIdentifier(),"com.elgato.mix.local","Volume",value);
                } else if (mixerId[0].equals("Stream")) {
                    WaveLinkActions.setInputConfig(input.getIdentifier(),"com.elgato.mix.stream","Volume",value);
                }

            }
        }
    }


    /**
     * Setting of type text definition example
     */
    @Setting(name = "IP", defaultValue = "localhost", maxLength = 15)
    private static String ipSetting;



    /**
     * Constructor calling super
     */
    public WaveLinkPlugin() {
        super(true);// true is for paralleling Actions executions
    }

    /**
     * Called when the Socket connection is lost or the plugin has received the close Message
     */
    public void onDisconnected(Exception exception) {  }

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
//        this.sendSettingUpdate(TouchPortalSampleJavaPluginConstants.Settings.ReadOnly.NAME, "Connected at " + System.currentTimeMillis(), false);

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
