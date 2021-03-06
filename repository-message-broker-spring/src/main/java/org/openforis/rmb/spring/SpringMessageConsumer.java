package org.openforis.rmb.spring;

import org.openforis.rmb.KeepAliveMessageHandler;
import org.openforis.rmb.MessageConsumer;
import org.openforis.rmb.MessageHandler;
import org.openforis.rmb.spi.ThrottlingStrategy;
import org.openforis.rmb.util.Is;
import org.springframework.beans.factory.InitializingBean;

import java.util.concurrent.TimeUnit;

public final class SpringMessageConsumer<M> implements InitializingBean {
    private final MessageConsumer.Builder<M> builder;
    private MessageConsumer<M> consumer;

    private Integer messagesHandledInParallel;
    private Integer retries;
    private ThrottlingStrategy throttlingStrategy;
    private Integer timeoutSeconds;

    public SpringMessageConsumer(String consumerId, MessageHandler<M> messageHandler) {
        this(consumerId, messageHandler, null);
    }

    public SpringMessageConsumer(String consumerId, KeepAliveMessageHandler<M> keepAliveMessageHandler) {
        this(consumerId, null, keepAliveMessageHandler);
    }

    private SpringMessageConsumer(String consumerId, MessageHandler<M> messageHandler, KeepAliveMessageHandler<M> keepAliveMessageHandler) {
        Is.hasText(consumerId, "consumerId must be specified");
        if (messageHandler == null && keepAliveMessageHandler == null)
            throw new IllegalArgumentException("messageHandler must not be null");
        builder = messageHandler == null
                ? MessageConsumer.builder(consumerId, keepAliveMessageHandler)
                : MessageConsumer.builder(consumerId, messageHandler);
    }

    MessageConsumer<M> getDelegate() {
        return consumer;
    }

    public void afterPropertiesSet() throws Exception {
        if (messagesHandledInParallel != null) {
            if (messagesHandledInParallel < 1)
                throw new IllegalArgumentException("A consumer must have a messagesHandledInParallel of at least one");
            builder.messagesHandledInParallel(messagesHandledInParallel);
        }

        if (throttlingStrategy == null && retries != null && retries > 0)
            builder.retry(retries, ThrottlingStrategy.ExponentialBackoff.upTo(1, TimeUnit.MINUTES));
        else if (throttlingStrategy != null && (retries == null || retries < 1))
            builder.retryUntilSuccess(throttlingStrategy);
        else if (throttlingStrategy != null && retries > 0)
            builder.retry(retries, throttlingStrategy);
        else if (throttlingStrategy != null && retries < 1)
            builder.retryUntilSuccess(throttlingStrategy);

        if (timeoutSeconds != null)
            builder.timeout(timeoutSeconds, TimeUnit.SECONDS);

        consumer = builder.build();
    }

    public void setMessagesHandledInParallel(int messagesHandledInParallel) {
        Is.greaterThenZero(messagesHandledInParallel, "A consumer must be able to handle at least one message at a time");
        this.messagesHandledInParallel = messagesHandledInParallel;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public void setThrottlingStrategy(ThrottlingStrategy throttlingStrategy) {
        this.throttlingStrategy = throttlingStrategy;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        Is.greaterThenZero(messagesHandledInParallel, "Timeout must be at least one second");
        this.timeoutSeconds = timeoutSeconds;
    }
}
