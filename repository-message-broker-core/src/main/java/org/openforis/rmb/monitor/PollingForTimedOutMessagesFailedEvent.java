package org.openforis.rmb.monitor;

import org.openforis.rmb.util.Is;

public class PollingForTimedOutMessagesFailedEvent implements Event {
    public final Exception exception;

    public PollingForTimedOutMessagesFailedEvent(Exception exception) {
        Is.notNull(exception, "exception must not be null");
        this.exception = exception;
    }

    public String toString() {
        return "PollingForTimedOutMessagesFailedEvent{" +
                "exception=" + exception +
                '}';
    }
}