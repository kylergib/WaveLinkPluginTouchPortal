package com.kylergib.wavelinktp.model;

import com.kylergib.wavelinktp.WaveLinkPlugin;
import com.kylergib.wavelinktp.WaveLinkPluginConstants;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.logging.Level;

/**
 * Client that connects to wave link through sockets
 */

public class WaveLinkClient extends WebSocketClient {
    public int isConnected = 0;
    private int port;
    private String host;
    public int configsReceived = 0;


    public WaveLinkClient(String ipAddress, int port) throws Exception {
        super(new URI("ws://" + ipAddress + ":" + port));
        this.port = port;
        this.host = ipAddress;
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Trying to connect to: " + ipAddress + " using port: " + port);
        connect();

    }
    @Override
    public void onOpen(ServerHandshake handshakedata) {
        WaveLinkPlugin.LOGGER.log(Level.INFO, "Connected to Wave Link");
        isConnected = 1;
        WaveLinkPlugin.latch.countDown();
    }

    //chooses what to do when client receives messages from wave link
    @Override
    public void onMessage(String message) {
        JSONObject newReceive = new JSONObject(message);
        if (newReceive.keySet().contains("id")) {
            //id in message matches what the client sent from WaveLinkPlugin class
            if ((int) newReceive.get("id") == 11) {
                Status.applicationInfo = new JSONObject(message);
                configsReceived = configsReceived + 1;

            } else if ((int) newReceive.get("id") == 12) {
                Status.microphoneConfig = new JSONObject(message);
                Status.getMicrophone();
                configsReceived = configsReceived + 1;
            } else if ((int) newReceive.get("id") == 13) {
                Status.switchState = new JSONObject(message);
                Status.getSwitchState();
                configsReceived = configsReceived + 1;
            } else if ((int) newReceive.get("id") == 14) {
                Status.outputConfig = new JSONObject(message);
                Status.getOutputConfig();
                configsReceived = configsReceived + 1;
            } else if ((int) newReceive.get("id") == 15) {
                Status.outputs = new JSONObject(message);
                Status.getOutputs();
                configsReceived = configsReceived + 1;
            } else if ((int) newReceive.get("id") == 16) {
                Status.inputConfigs =
                        new JSONObject(message);
                Status.getInput();
                configsReceived = configsReceived + 1;
            }
        } else {

            //these messages are from when something is changed in the wave link app outside of TP
            //auto sending to TP was not working
            String method = (String) newReceive.get("method");
            if (method.equals("inputMuteChanged")) {
                JSONObject params = (JSONObject) newReceive.get("params");
                String inputIdentifier = (String) params.get("identifier");
                String mixerId = (String) params.get("mixerID");
                Boolean value = (Boolean) params.get("value");
                for (Input input: Status.allInputs) {
                    if (input.getIdentifier().equals(inputIdentifier)) {
                        if (mixerId.equals("com.elgato.mix.local")) {
                            input.setLocalMixerMuted(value);

                        } else if (mixerId.equals("com.elgato.mix.stream")) {
                            input.setStreamMixerMuted(value);
                        }
                    }
                }
            } else if (method.equals("inputVolumeChanged")) {
                JSONObject params = (JSONObject) newReceive.get("params");
                String inputIdentifier = (String) params.get("identifier");
                String mixerId = (String) params.get("mixerID");
                int value = (Integer) params.get("value");
                for (Input input: Status.allInputs) {
                    if (input.getIdentifier().equals(inputIdentifier)) {
                        if (mixerId.equals("com.elgato.mix.local") && input.getLocalMixerLevel() != value) {
                            input.setLocalMixerLevel(value);
                        } else if (mixerId.equals("com.elgato.mix.stream")) {
                            input.setStreamMixerLevel(value);
                        }

                    }
                }
            } else if (method.equals("outputSwitched")) {
                JSONObject params = (JSONObject) newReceive.get("params");
                Status.switchStateValue = (String) params.get("value");

            } else if (method.equals("outputMuteChanged")) {
                JSONObject params = (JSONObject) newReceive.get("params");
                Boolean value = (Boolean) params.get("value");
                String mixerId = (String) params.get("mixerID");
                for (SwitchState switchState : Status.switchStates) {
                    if (switchState.getMixerId().equals(mixerId)) {
                        switchState.setMuted(value);
                    }

                }
            } else if (method.equals("outputVolumeChanged")) {
                JSONObject params = (JSONObject) newReceive.get("params");
                Integer value = (Integer) params.get("value");
                String mixerId = (String) params.get("mixerID");
                for (SwitchState switchState : Status.switchStates) {
                    if (switchState.getMixerId().equals(mixerId)) {
                        switchState.setLevel(value);
                    }
                }

            } else if (method.equals("inputsChanged")) {
                WaveJson getInputConfigs = new WaveJson("getInputConfigs", 16);
                send(getInputConfigs.getJsonString());

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

                            } else {
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
                        if (pluginAlreadyAdded == false) {
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
                        if (mixerID.equals("com.elgato.mix.local")) {
                            input.setPluginBypassLocal(value);
                        } else if (mixerID.equals("com.elgato.mix.stream")) {
                            input.setPluginBypassStream(value);
                        }
                    }
                }
            } else if (method.equals("selectedOutputChanged")) {
                JSONObject params = (JSONObject) newReceive.get("params");
                String outputIdentifier = (String) params.get("value");
                for (Output output: Status.allOutputs) {
                    if (output.getIdentifier().equals(outputIdentifier)) {
                        Status.currentOutputLocal = output;
                    }
                }
            } else if (method.equals("microphoneConfigChanged")) {
                JSONObject params = (JSONObject) newReceive.get("params");
                String identifier = (String) params.get("identifier");
                String property = (String) params.get("property");
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
            } else if (method.equals("inputDisabled")) {
                JSONObject params = (JSONObject) newReceive.get("params");
                String identifier = (String) params.get("identifier");
                for (Input input: Status.allInputs) {
                    if (input.getIdentifier().equals(identifier)) {
                        input.setAvailable(false);
                    }
                }
            } else if (method.equals("inputEnabled")) {
                JSONObject params = (JSONObject) newReceive.get("params");
                String identifier = (String) params.get("identifier");
                for (Input input: Status.allInputs) {
                    if (input.getIdentifier().equals(identifier)) {
                        input.setAvailable(true);
                    }
                }
            }
        }

    }


    @Override
    public void onClose(int code, String reason, boolean remote) {
        WaveLinkPlugin.LOGGER.log(Level.INFO, "WebSocket connection closed: " + reason);
        isConnected = -1;
        WaveLinkPlugin.latch.countDown();
    }

    @Override
    public void onError(Exception ex) {
        WaveLinkPlugin.LOGGER.log(Level.SEVERE, "Error connecting to wave link");

    }
}




