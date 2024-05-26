package com.kylergib.wavelinktp.model;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ApiHandler {
    private final BlockingQueue<Info> queue;
    private final int rate; // Requests per second
    private final Thread workerThread;
    private final HandlerCallback callback;
    public ApiHandler(int rate, HandlerCallback callback) {
        this.rate = rate;
        this.queue = new LinkedBlockingQueue<>();
        this.workerThread = new Thread(this::processQueue);
        this.workerThread.start();
        this.callback = callback;
    }
    public void addStateCreate(StateCreateInfo info) {
        queue.add(info);
    }
    public void addStateUpdate(StateUpdateInfo info) {
        queue.add(info);
    }
    public void addConnectorUpdate(ConnectorUpdateInfo info) {
        queue.add(info);
    }

    public void clearQueue() {
        queue.clear();
    }
    private void processQueue() {
        while (true) {
            try {
                Info update = queue.take();
                if (update instanceof StateCreateInfo) {
                    // create info callback
                    callback.onStateCreate((StateCreateInfo) update);
                } else if (update instanceof StateUpdateInfo) {
                    // state update info
                    callback.onStateUpdate((StateUpdateInfo) update);
                } else if (update instanceof ConnectorUpdateInfo) {
                    callback.onConnectorUpdate((ConnectorUpdateInfo) update);

                    Thread.sleep(1000 / rate); // Delay between requests
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

        }
    }
    public void stop() {
        workerThread.interrupt();
    }
}
