package org.openforis.rmb.monitor;

import org.openforis.rmb.spi.MessageProcessingUpdate;
import org.openforis.rmb.util.Is;

public final class MessageKeptAliveEvent implements Event {
    public final MessageProcessingUpdate<?> update;
    public final Object message;

    public MessageKeptAliveEvent(MessageProcessingUpdate<?> update, Object message) {
        Is.notNull(update, "update must not be null");
        Is.notNull(message, "message must not be null");
        this.update = update;
        this.message = message;
    }

    public String toString() {
        return "MessageKeptAliveEvent{" +
                "update=" + update +
                ", message=" + message +
                '}';
    }
}
