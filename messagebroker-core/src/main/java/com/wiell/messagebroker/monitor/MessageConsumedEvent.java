package com.wiell.messagebroker.monitor;

import com.wiell.messagebroker.MessageProcessingUpdate;

public class MessageConsumedEvent<T> implements Event {
    private final MessageProcessingUpdate<T> update;
    private final T message;

    public MessageConsumedEvent(MessageProcessingUpdate<T> update, T message) {
        this.update = update;
        this.message = message;
    }

    public String toString() {
        return "MessageConsumedEvent{" +
                "update=" + update +
                ", message=" + message +
                '}';
    }
}
