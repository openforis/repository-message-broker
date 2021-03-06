package org.openforis.rmb;

import org.openforis.rmb.RepositoryMessageBroker.Config;
import org.openforis.rmb.monitor.MessagePublishedEvent;
import org.openforis.rmb.monitor.MessageQueueCreatedEvent;
import org.openforis.rmb.spi.MessageRepository;
import org.openforis.rmb.spi.MessageSerializer;
import org.openforis.rmb.spi.TransactionSynchronizer;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

final class MessageQueueManager {
    private final MessageRepository repository;
    private final TransactionSynchronizer transactionSynchronizer;
    private final MessagePoller messagePoller;
    private final MessageSerializer messageSerializer;
    private final Monitors monitors;
    private final MessageRepositoryWatcher repositoryWatcher;

    private final Map<String, List<MessageConsumer<?>>> consumersByQueueId = new ConcurrentHashMap<String, List<MessageConsumer<?>>>();
    private final Set<String> queueIds = new HashSet<String>(); // For asserting global queue id uniqueness
    private final Set<String> consumerIds = new HashSet<String>(); // For asserting global consumer id uniqueness
    private final AtomicBoolean started = new AtomicBoolean();

    public MessageQueueManager(Config config) {
        this.repository = config.messageRepository;
        this.transactionSynchronizer = config.transactionSynchronizer;
        this.messagePoller = new MessagePoller(repository, config.messageSerializer, config.monitors);
        this.messageSerializer = config.messageSerializer;
        this.monitors = config.monitors;
        this.repositoryWatcher = new MessageRepositoryWatcher(messagePoller, config);
    }

    <M> void publish(String queueId, M message) {
        if (!started.get())
            throw new IllegalStateException("MessageBroker has not been started");
        assertInTransaction(queueId, message);
        List<MessageConsumer<?>> consumers = consumersByQueueId.get(queueId);
        repository.add(queueId, consumers, messageSerializer.serialize(message));
        monitors.onEvent(new MessagePublishedEvent(queueId, message));
        pollForMessagesOnCommit();
    }

    void registerQueue(String queueId, List<MessageConsumer<?>> consumers) {
        assertQueueIdUniqueness(queueId);
        assertConsumerUniqueness(consumers);
        repositoryWatcher.includeQueue(queueId, consumers);
        consumersByQueueId.put(queueId, consumers);
        messagePoller.registerConsumers(consumers);
        monitors.onEvent(new MessageQueueCreatedEvent(queueId, consumers));
    }

    void start() {
        started.set(true);
        repositoryWatcher.start();
    }

    void stop() {
        repositoryWatcher.stop();
        messagePoller.stop();
    }

    private void pollForMessagesOnCommit() {
        transactionSynchronizer.notifyOnCommit(new TransactionSynchronizer.CommitListener() {
            public void committed() {
                messagePoller.poll();
            }
        });
    }

    private void assertInTransaction(String queueId, Object message) {
        if (!transactionSynchronizer.isInTransaction())
            throw new IllegalStateException("Trying to publish a message outside of a transaction. " +
                    "Queue: " + queueId + ", message: " + message);
    }

    private void assertQueueIdUniqueness(String queueId) {
        if (!queueIds.add(queueId))
            throw new IllegalArgumentException("Duplicate queue id: " + queueId);
    }

    private void assertConsumerUniqueness(List<MessageConsumer<?>> consumers) {
        for (MessageConsumer<?> consumer : consumers)
            if (!consumerIds.add(consumer.id))
                throw new IllegalArgumentException("Duplicate consumer id: " + consumer.id);
    }
}
