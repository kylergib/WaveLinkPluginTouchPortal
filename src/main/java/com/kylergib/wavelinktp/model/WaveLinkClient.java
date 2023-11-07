package com.kylergib.wavelinktp.model;

import com.kylergib.wavelinktp.WaveLinkPlugin;
import com.kylergib.wavelinktp.WaveLinkPluginConstants;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Client that connects to wave link through sockets
 */

public class WaveLinkClient extends WebSocketClient {
    private int port;
    private String host;
    private int configsReceived;
    private boolean connectedToWaveLink;

//    private final static Logger LOGGER = Logger.getLogger(WaveLinkClient.class.getName());

    private WaveLinkCallback waveLinkCallback;
    private int realTimeUpdateCounter = 10;

//    public interface connectedToWaveLink {
//        void onConnected();
//        void onDisconnected();
//        void connectedToDifferentApp();
//    }

    public WaveLinkClient(String ipAddress, int port, WaveLinkCallback waveLinkCallback) throws Exception {
        super(new URI("ws://" + ipAddress + ":" + port));
        this.waveLinkCallback = waveLinkCallback;
        this.connectedToWaveLink = false;
        this.port = port;
        this.host = ipAddress;
        this.configsReceived = 0;
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Trying to connect to: " + ipAddress + " using port: " + port);
        connect();

    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Connected to Wave Link on Port: " + port);
        WaveLinkPlugin.latch.countDown();
        WaveJson getAppInfo = new WaveJson("getApplicationInfo", 11);
        send(getAppInfo.getJsonString());
    }

    //chooses what to do when client receives messages from wave link
    @Override
    public void onMessage(String message) {
        JSONObject newReceive = new JSONObject(message);
//        LOGGER.log(Level.FINER, String.valueOf(newReceive));
        WaveLinkPlugin.LOGGER.log(Level.FINER, String.valueOf(newReceive));
        if (newReceive.keySet().contains("result") && newReceive.get("result").equals(null)) {
            WaveLinkPlugin.LOGGER.log(Level.WARNING, "Result was null");
            WaveLinkPlugin.LOGGER.log(Level.WARNING, String.valueOf(newReceive));
            configsReceived = configsReceived + 1;
            return;
        }
        if (newReceive.keySet().contains("method") && newReceive.get("method").equals("realTimeChanges")) {

            if (realTimeUpdateCounter > 0) {
                realTimeUpdateCounter -= 1;
                return;
            } else {
                realTimeUpdateCounter = 10;
            }
            JSONObject params = (JSONObject) newReceive.get("params");
            JSONArray mixerList = (JSONArray) params.get("MixerList");
            mixerList.forEach(mixerInput -> {
                String inputIdentifier = (String) ((JSONObject) mixerInput).get("identifier");
                BigDecimal levelLeft = ((BigDecimal) ((JSONObject) mixerInput).get("levelLeft")).setScale(2, RoundingMode.HALF_UP);
                BigDecimal levelRight = ((BigDecimal) ((JSONObject) mixerInput).get("levelRight")).setScale(2, RoundingMode.HALF_UP);

                for (Input input: Status.allInputs) {
                    if (input.getIdentifier().equals(inputIdentifier)) {
                        boolean levelChanged = false;
//                        WaveLinkPlugin.LOGGER.log(Level.FINER, String.valueOf(input.getLevelLeft() != levelLeft));
//                        WaveLinkPlugin.LOGGER.log(Level.FINER, String.valueOf(input.getLevelLeft()) + "-" +String.valueOf(levelLeft));
//                        WaveLinkPlugin.LOGGER.log(Level.FINER, String.valueOf(input.getLevelRight() != levelRight));
                        if (input.getLevelLeft() == null) {
                            input.setLevelLeft(levelLeft);
                             levelChanged = true;
                        } else if (input.getLevelLeft().compareTo(levelLeft) != 0) {
                            input.setLevelLeft(levelLeft);
                            levelChanged = true;
                        }

                        if (input.getLevelRight() == null) {
                            input.setLevelRight(levelRight);
                            levelChanged = true;
                        } else if (input.getLevelRight().compareTo(levelRight) != 0) {
                            input.setLevelRight(levelRight);
                            levelChanged = true;
                        }
                        if (levelChanged) {
//                            WaveLinkPlugin.LOGGER.log(Level.FINEST, "Input: " + input.getName() + " Left: " +
//                                    input.getLevelLeft() + " - Right: " + input.getLevelRight());
                            WaveLinkPlugin.waveLinkPlugin.sendStateUpdate("com.kylergib.wavelinktp.WaveLinkPlugin.WaveLinkLevelStates.state." + input.getLevelLeftStateId().replace(" ",""),input.getLevelLeft());
                            WaveLinkPlugin.waveLinkPlugin.sendStateUpdate("com.kylergib.wavelinktp.WaveLinkPlugin.WaveLinkLevelStates.state." + input.getLevelRightStateId().replace(" ",""),input.getLevelRight());
                        }

                    }
                }


            });

            JSONObject localMixer = (JSONObject) params.get("localMixer");
            BigDecimal localLeft = ((BigDecimal) localMixer.get("levelLeft")).setScale(2,RoundingMode.HALF_UP);
            BigDecimal localRight = ((BigDecimal) localMixer.get("levelRight")).setScale(2,RoundingMode.HALF_UP);
            boolean localLevelChanged = false;
            if (Status.localLeft == null) {
                Status.localLeft = localLeft;
                localLevelChanged = true;
            } else if (Status.localLeft.compareTo(localLeft) != 0) {
                Status.localLeft = localLeft;
                localLevelChanged = true;
            }
            if (Status.localRight == null) {
                Status.localRight = localRight;
                localLevelChanged = true;
            } else if (Status.localRight.compareTo(localRight) != 0) {
                Status.localRight = localRight;
                localLevelChanged = true;
            }
            boolean streamLevelChanged = false;
            JSONObject streamMixer = (JSONObject) params.get("streamMixer");
            BigDecimal streamLeft = (BigDecimal) streamMixer.get("levelLeft");
            BigDecimal streamRight = (BigDecimal) streamMixer.get("levelRight");
            if (Status.streamLeft == null) {
                Status.streamLeft = streamLeft;
                streamLevelChanged = true;
            } else if (Status.streamLeft.compareTo(streamLeft) != 0) {
                Status.streamLeft = streamLeft;
                streamLevelChanged = true;
            }
            if (Status.streamRight == null) {
                Status.streamRight = streamRight;
                streamLevelChanged = true;
            } else if (Status.streamRight.compareTo(streamRight) != 0) {
                Status.streamRight = streamRight;
                streamLevelChanged = true;
            }
            if (localLevelChanged) {
                WaveLinkPlugin.waveLinkPlugin.sendStateUpdate(WaveLinkPluginConstants.WaveLinkOutputs.States.LocalLeftLevel.ID,Status.localLeft);
                WaveLinkPlugin.waveLinkPlugin.sendStateUpdate(WaveLinkPluginConstants.WaveLinkOutputs.States.LocalRightLevel.ID,Status.localRight);
                System.out.println(Status.localLeft);
            }
            if (streamLevelChanged) {
                WaveLinkPlugin.waveLinkPlugin.sendStateUpdate(WaveLinkPluginConstants.WaveLinkOutputs.States.StreamLeftLevel.ID,Status.streamLeft);
                WaveLinkPlugin.waveLinkPlugin.sendStateUpdate(WaveLinkPluginConstants.WaveLinkOutputs.States.StreamRightLevel.ID,Status.streamRight);
            }

        } else {
            WaveLinkPlugin.LOGGER.log(Level.FINER, String.valueOf(newReceive));
        }



        if (newReceive.keySet().contains("id")) {
            //id in message matches what the client sent from WaveLinkPlugin class
            if ((int) newReceive.get("id") == 11) {
                // need to check what appName is, if it is "Elgato Wave Link" continue, if not need to close
                JSONObject result = newReceive.getJSONObject("result");
                if (!result.get("appName").equals("Elgato Wave Link")) {
                    WaveLinkPlugin.LOGGER.log(Level.WARNING, "Connected to wrong app. Will try another port");
                    waveLinkCallback.onConnectedToWrongApp();
                }
                connectedToWaveLink = true;
                Status.applicationInfo = newReceive;
                configsReceived = configsReceived + 1;
                waveLinkCallback.onConnectedToWaveLink();

            } else if ((int) newReceive.get("id") == 12) {
                Status.microphoneConfig = newReceive;
                Status.getMicrophone();
                configsReceived = configsReceived + 1;
            } else if ((int) newReceive.get("id") == 13) {
                Status.switchState = newReceive;
                Status.getSwitchState();
                configsReceived = configsReceived + 1;
            } else if ((int) newReceive.get("id") == 14) {
                Status.outputConfig = newReceive;
                Status.getOutputConfig();
                configsReceived = configsReceived + 1;
            } else if ((int) newReceive.get("id") == 15) {
                Status.outputs = newReceive;
                Status.getOutputs();
                configsReceived = configsReceived + 1;
            } else if ((int) newReceive.get("id") == 16) {
                Status.inputConfigs =
                        newReceive;
                Status.getInput();
                configsReceived = configsReceived + 1;




            }
            if (configsReceived == 6) {
                waveLinkCallback.onConfigsReceived();
                configsReceived = 0;
            }

        } else {

            //these messages are from when something is changed in the wave link app outside of TP

            String method = (String) newReceive.get("method");
            if (method.equals("inputMuteChanged")) {
                JSONObject params = (JSONObject) newReceive.get("params");
                String inputIdentifier = (String) params.get("identifier");
                String mixerId = (String) params.get("mixerID");
                Boolean value = (Boolean) params.get("value");
                for (Input input: Status.allInputs) {
                    if (input.getIdentifier().equals(inputIdentifier)) {
                        String sendMute = "unmuted";
                        if (value) {
                            sendMute = "muted";
                        }
                        if (mixerId.equals(Status.localPackageName)) {
                            input.setLocalMixerMuted(value);

                            WaveLinkPlugin.waveLinkPlugin.sendStateUpdate("com.kylergib.wavelinktp.WaveLinkPlugin.WaveLinkMuteStates.state." + input.getLocalMuteStateId().replace(" ",""),sendMute);


                        } else if (mixerId.equals(Status.streamPackageName)) {
                            input.setStreamMixerMuted(value);
                            WaveLinkPlugin.waveLinkPlugin.sendStateUpdate("com.kylergib.wavelinktp.WaveLinkPlugin.WaveLinkMuteStates.state." + input.getStreamMuteStateId().replace(" ",""),sendMute);
                        }
                    }
                }
            } else if (method.equals("inputVolumeChanged")) {
                JSONObject params = (JSONObject) newReceive.get("params");
                String inputIdentifier = (String) params.get("identifier");
                String mixerId = (String) params.get("mixerID");
                int value = (Integer) params.get("value");

                //TODO: make this shorter
                for (Input input: Status.allInputs) {
                    if (input.getIdentifier().equals(inputIdentifier)) {
                        if (mixerId.equals(Status.localPackageName) && input.getLocalMixerLevel() != value) {
                            input.setLocalMixerLevel(value);
                            Status.setInputValue(value,"Local", input);
                        } else if (mixerId.equals(Status.streamPackageName)) {
                            input.setStreamMixerLevel(value);
                            Status.setInputValue(value,"Stream", input);
                        }

                    }
                }
            } else if (method.equals("outputSwitched")) {
                JSONObject params = (JSONObject) newReceive.get("params");
                Status.switchStateValue = (String) params.get("value");
                String monitorValue = null;
                if (Status.switchStateValue.equals(Status.streamPackageName)) {
                    monitorValue = "Stream";
                } else {
                    monitorValue = "Local";
                }
                WaveLinkPlugin.waveLinkPlugin.sendStateUpdate(WaveLinkPluginConstants.WaveLinkOutputs.States.MonitoredMix.ID, monitorValue);

            } else if (method.equals("outputMuteChanged")) {
                JSONObject params = (JSONObject) newReceive.get("params");
                Boolean value = (Boolean) params.get("value");
                String mixerId = (String) params.get("mixerID");
                for (SwitchState switchState : Status.switchStates) {
                    if (switchState.getMixerId().equals(mixerId)) {
                        switchState.setMuted(value);
                        String sendMute = "unmuted";
                        if (switchState.getMuted()) {
                            sendMute = "muted";
                        }
                        if (switchState.getMixerName().equals("Local")) {
                            WaveLinkPlugin.waveLinkPlugin.sendStateUpdate(WaveLinkPluginConstants.WaveLinkOutputs.States.LocalMixOut.ID, sendMute);
                        } else {
                            WaveLinkPlugin.waveLinkPlugin.sendStateUpdate(WaveLinkPluginConstants.WaveLinkOutputs.States.StreamMixOut.ID, sendMute);
                        }
                    }

                }
            } else if (method.equals("outputVolumeChanged")) {
                JSONObject params = (JSONObject) newReceive.get("params");
                Integer value = (Integer) params.get("value");
                String mixerId = (String) params.get("mixerID");
                for (SwitchState switchState : Status.switchStates) {
                    if (switchState.getMixerId().equals(mixerId)) {
                        switchState.setLevel(value);
                        Status.setOutputValue(value, switchState.getMixerName());
                    }
                }

            } else if (method.equals("inputsChanged")) {
                WaveJson getInputConfigs = new WaveJson("getInputConfigs", 16);
                send(getInputConfigs.getJsonString());
                try {
                    WaveLinkPlugin.waveLinkPlugin.updateInputs();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            } else if (method.equals("filterRemoved")) {
                JSONObject params = (JSONObject) newReceive.get("params");
                String inputIdentifier = (String) params.get("identifier");
                String filterID = (String) params.get("filterID");
                for (Input input: Status.allInputs) {
                    if (input.getIdentifier().equals(inputIdentifier)) {
                        ArrayList<InputPlugin> newPlugins = new ArrayList<>();
                        for (InputPlugin plugin: input.getPlugins()) {

                            if (!plugin.getFilterID().equals(filterID)) {
                                newPlugins.add(plugin);

                            }
                        }
                        input.setPlugins(newPlugins);
                    }
                }

            } else if (method.equals("filterAdded")) {
                JSONObject params = (JSONObject) newReceive.get("params");
                String pluginID = (String) params.get("pluginID");
                String filterID = (String) params.get("filterID");
                for (Input input: Status.allInputs) {
                    if (params.get("identifier").equals(input.getIdentifier())) {
                        ArrayList<InputPlugin> allPlugins = new ArrayList<>();
                        Boolean pluginAlreadyAdded = false;
                        for (InputPlugin plugin: input.getPlugins()) {
                            if (plugin.getFilterID().equals(filterID)) {
                                pluginAlreadyAdded = true;
                            } else {
                                allPlugins.add(plugin);
                            }
                        }
                        if (!pluginAlreadyAdded) {
                            InputPlugin newPlugin = new InputPlugin((String) params.get("filterID"),
                                    pluginID, (String) params.get("name"),
                                    (Boolean) params.get("isActive"));
                            allPlugins.add(newPlugin);
                            input.setPlugins(allPlugins);
                        }

                    }
                }

            } else if (method.equals("filterBypassStateChanged")) {
                JSONObject params = (JSONObject) newReceive.get("params");
                String inputIdentifier = (String) params.get("identifier");
                String mixerID = (String) params.get("mixerID");
                Boolean value = (Boolean) params.get("value");
                for (Input input: Status.allInputs) {
                    if (input.getIdentifier().equals(inputIdentifier)) {
                        if (mixerID.equals(Status.localPackageName)) {
                            input.setPluginBypassLocal(value);
                        } else if (mixerID.equals(Status.streamPackageName)) {
                            input.setPluginBypassStream(value);
                        }
                    }
                }
            } else if (method.equals("selectedOutputChanged")) {
                JSONObject params = (JSONObject) newReceive.get("params");
                String outputIdentifier = (String) params.get("value");
                for (Output output: Status.allOutputs) {
                    if (output.getIdentifier().equals(outputIdentifier)) {
                        //TODO: make this cleaner, as i think setting both these is redundant
                        Status.currentOutputLocal = output;
                        Status.selectedOutput = output.getName();
                        WaveLinkPlugin.waveLinkPlugin.sendStateUpdate("com.kylergib.wavelinktp.WaveLinkPlugin.WaveLinkOutputs.state.selectedOutput",output.getName());
                    }
                }
            } else if (method.equals("microphoneConfigChanged")) {
                JSONObject params = (JSONObject) newReceive.get("params");
                String identifier = (String) params.get("identifier");
                String property = (String) params.get("property");
                boolean isLowCut = property.equals("Microphone LowCut");
                boolean isClipGuard = property.equals("Microphone Clipguard");
                boolean isMute = property.equals("Microphone Mute");
                if (isLowCut || isClipGuard || isMute){
                    Boolean value = (Boolean) params.get("value");
                    for (Microphone mic: Status.allMics) {
                        if (mic.getIdentifier().equals(identifier)) {
                            if (property.equals("clipGuard")) {
                                mic.setClipGuardOn(value);
                            } else if (property.equals("lowCut")) {
                                mic.setLowCutOn(value);
                            }
                        }
                    }
                }

            } else if (method.equals("inputDisabled")) {
                //TODO: see if what this does ?
                JSONObject params = (JSONObject) newReceive.get("params");
                String identifier = (String) params.get("identifier");
                for (Input input: Status.allInputs) {
                    if (input.getIdentifier().equals(identifier)) {
                        input.setAvailable(false);
                    }
                }
                WaveLinkPlugin.waveLinkPlugin.updateMics();
            } else if (method.equals("inputEnabled")) {
                JSONObject params = (JSONObject) newReceive.get("params");
                String identifier = (String) params.get("identifier");
                for (Input input: Status.allInputs) {
                    if (input.getIdentifier().equals(identifier)) {
                        input.setAvailable(true);
                    }
                }
                WaveLinkPlugin.waveLinkPlugin.updateMics();
            } else if (method.equals("filterChanged")) {
                JSONObject params = (JSONObject) newReceive.get("params");
                String identifier = (String) params.get("identifier"); //input id
                String filterId = (String) params.get("filterID"); //filter id
                boolean filterActive = (boolean) params.get("value");
                String stateValue;
                if (filterActive) stateValue = "active";
                else stateValue = "inactive";
                for (Input input: Status.allInputs) {
                    if (input.getIdentifier().equals(identifier)) {
                        for (InputPlugin inputPlugin: input.getPlugins()) {
                            if (inputPlugin.getFilterID().equals(filterId)) {
                                String stateId = "com.kylergib.wavelinktp.WaveLinkPlugin.WaveLinkInputs.state." +
                                        input.getName().replace(" ","") + "Filter" + inputPlugin.getName();

                                inputPlugin.setIsActive(filterActive);
                                WaveLinkPlugin.waveLinkPlugin.sendStateUpdate(
                                        stateId, stateValue);
                            }
                        }
                    }
                }
            }
        }

    }


    @Override
    public void onClose(int code, String reason, boolean remote) {
        WaveLinkPlugin.LOGGER.log(Level.INFO, "WebSocket connection closed: " + reason);
        WaveLinkPlugin.latch.countDown();
        if (connectedToWaveLink) {
            waveLinkCallback.onWaveLinkDisconnected();
        }
    }

    @Override
    public void onError(Exception ex) {
        WaveLinkPlugin.LOGGER.log(Level.SEVERE, String.valueOf(ex.fillInStackTrace()));

    }

    public void setConfigCallback(WaveLinkCallback waveLinkCallback) {
        this.waveLinkCallback = waveLinkCallback;
    }




}




