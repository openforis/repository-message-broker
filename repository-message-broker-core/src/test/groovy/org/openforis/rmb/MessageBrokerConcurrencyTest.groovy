package org.openforis.rmb

import spock.lang.Specification

class MessageBrokerConcurrencyTest extends Specification {
    @SuppressWarnings("GroovyUnusedDeclaration")
    @Delegate QueueTestDelegate queueTestDelegate = new QueueTestDelegate()

    def messages = 1..1000

    def setup() {
        this.messagesHandledInParallel = 4
        queueTestDelegate.setUp()
    }

    def 'All messages are handled for a blocking queue'() {
        def handler = createHandler()
        def queue = blockingQueue(handler)

        when:
            publish(queue, messages)
        then:
            handler.handled(messages)
    }

    def 'All, concurrently published, messages are handled for a blocking queue'() {
        def handler = createHandler()
        def queue = blockingQueue(handler)

        when:
            publishInParallel(queue, messages)

        then:
            handler.handled(messages)
    }

    def 'All published messages are handled for a non-blocking queue'() {
        def handler = createHandler(this.messagesHandledInParallel)
        def queue = nonBlockingQueue(handler)

        when:
            publish(queue, messages)

        then:
            handler.handled(messages)
    }

    def 'All, concurrently published, messages are handled for a non-blocking queue'() {
        def handler = createHandler(this.messagesHandledInParallel)
        def queue = nonBlockingQueue(handler)

        when:
            publishInParallel(queue, messages)

        then:
            handler.handled(messages)
    }
}
