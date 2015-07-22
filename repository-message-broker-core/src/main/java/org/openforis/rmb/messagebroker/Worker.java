package org.openforis.rmb.messagebroker;

import org.openforis.rmb.messagebroker.monitor.*;
import org.openforis.rmb.messagebroker.spi.MessageProcessingStatus;
import org.openforis.rmb.messagebroker.spi.MessageProcessingUpdate;
import org.openforis.rmb.messagebroker.spi.MessageRepository;

@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
final class Worker<T> {
    private final MessageRepository repository;
    private final Throttler throttler;
    private final Monitors monitors;
    private final T message;
    private final MessageConsumer<T> consumer;
    private final KeepAlive keepAlive;

    private MessageProcessingUpdate<T> update;

    public Worker(MessageRepository repository,
                  Throttler throttler,
                  Monitors monitors,
                  MessageProcessingUpdate<T> update,
                  T message) {
        this.repository = repository;
        this.throttler = throttler;
        this.monitors = monitors;
        this.update = update;
        this.message = message;
        consumer = update.consumer;
        keepAlive = createKeepAlive();
    }

    public void consume() throws InterruptedException, MessageUpdateConflict {
        notifyOnWorkerStart();
        Exception e = tryToConsume();
        if (e == null)
            return;

        while (notReachedMaxRetries()) {
            retry(e);
            e = tryToConsume();
            if (e == null)
                return;
        }
        failed(e);
    }

    private synchronized boolean notReachedMaxRetries() {
        return update.retries <= consumer.maxRetries;
    }

    private synchronized void notifyOnWorkerStart() {
        boolean timedOutMessage = update.fromState == MessageProcessingStatus.State.TIMED_OUT;
        if (timedOutMessage)
            monitors.onEvent(new ConsumingTimedOutMessageEvent(update, message));
        else
            monitors.onEvent(new ConsumingNewMessageEvent(update, message));
    }


    private Exception tryToConsume() {
        try {
            consumer.consume(message, keepAlive);
            completed();
            return null;
        } catch (RuntimeException e) {
            return e;
        }
    }

    // Since keep-alive can be sent from a separate thread, this.update is shared mutable state
    // All access to it has been synchronized
    private synchronized void keepAlive() {
        updateRepo(update.processing());
        monitors.onEvent(new MessageKeptAliveEvent(update, message));
    }

    private synchronized void retry(Exception e) throws InterruptedException {
        updateRepo(update.retry(e.getMessage()));
        monitors.onEvent(new ThrottlingMessageRetryEvent(update, message, e));
        throttler.throttle(update.retries, consumer, keepAlive);
        monitors.onEvent(new RetryingMessageConsumptionEvent(update, message, e));
    }

    private synchronized void failed(Exception e) {
        updateRepo(update.failed(e.getMessage()));
        monitors.onEvent(new MessageConsumptionFailedEvent(update, message, e));
    }

    private synchronized void completed() {
        updateRepo(update.completed());
        monitors.onEvent(new MessageConsumedEvent(update, message));
    }

    private void updateRepo(MessageProcessingUpdate<T> update) {
        if (!repository.update(update))
            throw new MessageUpdateConflict(update);
        this.update = update;
    }

    private KeepAlive createKeepAlive() {
        return new KeepAlive() {
            public void send() { // Can be invoked from a separate thread
                keepAlive();
            }
        };
    }

    static class MessageUpdateConflict extends RuntimeException {
        final MessageProcessingUpdate<?> update;

        MessageUpdateConflict(MessageProcessingUpdate<?> update) {
            this.update = update;
        }
    }
}
