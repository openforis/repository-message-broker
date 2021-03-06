package org.openforis.rmb.spi;

import org.openforis.rmb.MessageConsumer;
import org.openforis.rmb.util.Is;

import java.util.Date;
import java.util.UUID;

import static org.openforis.rmb.spi.MessageProcessingStatus.State.PROCESSING;

/**
 * Describes the processing of a message by a consumer.
 * <p>
 * Instances of this class is created through
 * {@link #create(MessageDetails, MessageConsumer, MessageProcessingStatus)}.
 * </p>
 * <p>
 * This class is immutable.
 * </p>
 *
 * @param <M> the type of the message being processed
 */
public final class MessageProcessing<M> {
    final String queueId;
    final String messageId;
    final Date publicationTime;
    final MessageConsumer<M> consumer;
    final MessageProcessingStatus.State state;
    final int retries;
    final String errorMessage;
    final Date lastUpdated;
    final String versionId;

    private MessageProcessing(MessageDetails messageDetails,
                              MessageConsumer<M> consumer,
                              MessageProcessingStatus status) {
        Is.notNull(messageDetails, "messageDetails must not be null");
        Is.notNull(consumer, "consumer must not be null");
        Is.notNull(status, "status must not be null");
        this.queueId = messageDetails.queueId;
        this.consumer = consumer;
        this.messageId = messageDetails.messageId;
        this.publicationTime = messageDetails.publicationTime;
        this.state = status.state;
        this.retries = status.retries;
        this.errorMessage = status.errorMessage;
        this.lastUpdated = status.lastUpdated;
        this.versionId = status.versionId;
    }

    /**
     * Takes the message for processing.
     *
     * @param clock the clock used to calculate current time
     * @return a {@link MessageProcessingUpdate} representing the update
     */
    public MessageProcessingUpdate<M> take(Clock clock) {
        return MessageProcessingUpdate.create(messageDetails(), consumer, status(),
                new MessageProcessingStatus(PROCESSING, retries, errorMessage, now(clock), randomUuid()));
    }

    private Date now(Clock clock) {
        return new Date(clock.millis());
    }

    private String randomUuid() {
        return UUID.randomUUID().toString();
    }

    private MessageProcessingStatus status() {
        return new MessageProcessingStatus(state, retries, errorMessage, lastUpdated, versionId);
    }

    private MessageDetails messageDetails() {
        return new MessageDetails(queueId, messageId, publicationTime);
    }

    public String toString() {
        return "MessageProcessing{" +
                "queueId='" + queueId + '\'' +
                ", messageId='" + messageId + '\'' +
                ", publicationTime=" + publicationTime +
                ", consumer=" + consumer +
                ", state=" + state +
                ", retries=" + retries +
                ", errorMessage='" + errorMessage + '\'' +
                ", versionId='" + versionId + '\'' +
                '}';
    }

    /**
     * Creates an instance.
     *
     * @param messageDetails the details about the message
     * @param consumer       the consumer to process/processing the message
     * @param status         the current status of the processing
     * @param <M>            the type of the message
     * @return the new instance
     */
    public static <M> MessageProcessing<M> create(MessageDetails messageDetails,
                                                  MessageConsumer<M> consumer,
                                                  MessageProcessingStatus status) {
        return new MessageProcessing<M>(messageDetails, consumer, status);
    }
}
