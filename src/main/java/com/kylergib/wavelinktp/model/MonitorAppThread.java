package com.kylergib.wavelinktp.model;


import com.kylergib.wavelinktp.WaveLinkPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;

import static com.kylergib.wavelinktp.WaveLinkPlugin.LOGGER;

public class MonitorAppThread extends Thread {
    private final AppOpenCallback appCallback;
    private volatile boolean stopRequested = false;
    private final String os;
    private int firstCheck = 0;

    public void requestStop() {
        stopRequested = true;
        interrupt();
    }

    public MonitorAppThread(AppOpenCallback appCallback) {
        this.appCallback = appCallback;
        os = System.getProperty("os.name").toLowerCase();
    }

    public interface AppOpenCallback {
        void onAppOpened();
        void onAppClosed();
    }

    public void run() {
        // Code to be executed in the new thread

        LOGGER.log(Level.INFO, os);
        boolean isRunning = false;
        if (os.contains("win")) {

            isRunning = isAppRunningWin();
        } else if (os.contains("mac")) {

            isRunning = isAppRunningMac();
        }
        boolean appOpenedPreviously = isRunning;
        if (!isRunning) {
            LOGGER.log(Level.WARNING, "Wave Link is not running");
        }
        else {
            LOGGER.log(Level.INFO, "Wave Link is running.");
            appCallback.onAppOpened();
        }
        int retries = 0;
        int retryCountNeeded = 100;
        while (!stopRequested) {

            if (retries % retryCountNeeded == 0) LOGGER.log(Level.INFO, "Checking if Wave Link is open");
            if (os.contains("win")) {

                isRunning = isAppRunningWin();

            } else if (os.contains("mac")) {

                isRunning = isAppRunningMac();
            }
            if (appOpenedPreviously && !isRunning) {
                if (retries % retryCountNeeded == 0) LOGGER.log(Level.INFO, "Wave Link has closed");
                appCallback.onAppClosed();
                appOpenedPreviously = false;
            } else if (!appOpenedPreviously && isRunning) {
                if (retries % retryCountNeeded == 0) LOGGER.log(Level.INFO, "Wave Link has opened");
                appOpenedPreviously = isRunning;
                appCallback.onAppOpened();
            }
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            retries += 1;
        }
    }
    public boolean isAppRunningMac() {

        try {
            Process process = Runtime.getRuntime().exec("pgrep -l WaveLink.exe");


            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                if (firstCheck == 0 ) {
                    LOGGER.log(Level.FINEST, line);

                }
                if (line.toLowerCase().contains("wavelink")) {
                    process.destroy();
                    firstCheck = 1;
                    return true;
                }
            }
            process.destroy();
            firstCheck = 1;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        firstCheck = 1;
        return false;
    }


    private boolean isAppRunningWin() {
        try {
            Process process = Runtime.getRuntime().exec("tasklist");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (firstCheck == 0 ) {
                    LOGGER.log(Level.FINEST, line);

                }
                if (line.contains("WaveLink.exe")) {
                    process.destroy();
                    firstCheck = 1;
                    return true;
                }
            }
            reader.close();
            process.destroy();
        } catch (IOException e) {
            e.printStackTrace();
        }
        firstCheck = 1;
        return false;
    }
}
