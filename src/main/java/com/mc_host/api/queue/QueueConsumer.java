package com.mc_host.api.queue;

import com.mc_host.api.model.cache.Queue;

public interface QueueConsumer {
    void start();
    void stop();
    void processItem(String item);
    Queue getQueue();
}
