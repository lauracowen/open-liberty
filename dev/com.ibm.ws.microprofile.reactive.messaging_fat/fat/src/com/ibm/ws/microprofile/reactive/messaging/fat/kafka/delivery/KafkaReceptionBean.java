/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.delivery;

import static org.eclipse.microprofile.reactive.messaging.Acknowledgment.Strategy.MANUAL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

@ApplicationScoped
public class KafkaReceptionBean {

    public final static String CHANNEL_NAME = "reception-test-input";

    private final Queue<Message<String>> receivedMessages = new LinkedList<>();

    @Acknowledgment(MANUAL)
    @Incoming(CHANNEL_NAME)
    public CompletionStage<Void> recieveMessage(Message<String> msg) {
        synchronized (this) {
            receivedMessages.add(msg);
            this.notifyAll();
        }
        return CompletableFuture.completedFuture(null);
    }

    public List<Message<String>> getReceivedMessages(int count, Duration timeout) throws InterruptedException {
        ArrayList<Message<String>> result = new ArrayList<>();
        Duration remaining = timeout;
        long startTime = System.nanoTime();
        while (!remaining.isNegative() && result.size() < count) {
            synchronized (this) {
                while (!receivedMessages.isEmpty() && result.size() < count) {
                    result.add(receivedMessages.poll());
                }
                if (result.size() < count) {
                    this.wait(remaining.toMillis());
                }
            }
            Duration elapsed = Duration.ofNanos(System.nanoTime() - startTime);
            remaining = timeout.minus(elapsed);
        }
        assertThat("Wrong number of records fetched from kafka", result, hasSize(count));
        return result;
    }

}
