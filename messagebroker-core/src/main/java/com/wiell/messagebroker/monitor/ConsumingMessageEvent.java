package com.wiell.messagebroker.monitor;

import com.wiell.messagebroker.MessageProcessingUpdate;

public abstract class ConsumingMessageEvent implements Event {
    public final MessageProcessingUpdate<?> update;
    public final Object message;

    public ConsumingMessageEvent(MessageProcessingUpdate<?> update, Object message) {
        this.update = update;
        this.message = message;
    }
}
