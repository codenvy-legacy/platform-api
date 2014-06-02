/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2014] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.api.core.notification;

import com.codenvy.commons.lang.NamedThreadFactory;

import org.everrest.websockets.client.BaseClientMessageListener;
import org.everrest.websockets.client.WSClient;
import org.everrest.websockets.message.JsonMessageConverter;
import org.everrest.websockets.message.MessageConversionException;
import org.everrest.websockets.message.MessageConverter;
import org.everrest.websockets.message.RESTfulOutputMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Receives event over websocket and publish them to the local EventsService.
 *
 * @author andrew00x
 */
@Singleton
public final class WSocketEventBusClient extends WSocketEventBus {
    private static final Logger LOG = LoggerFactory.getLogger(WSocketEventBusClient.class);

    private static final long wsConnectionTimeout = 2000;

    private final EventService                         eventService;
    private final String[]                             remoteEventServices;
    private final MessageConverter                     messageConverter;
    private final ConcurrentMap<URI, Future<WSClient>> connections;
    private final AtomicBoolean                        start;

    private ExecutorService executor;

    @Inject
    WSocketEventBusClient(EventService eventService,
                          @Nullable @Named("notification.event_bus_urls") String[] remoteEventServices,
                          @Nullable EventPropagationPolicy policy) {
        super(eventService, policy);
        this.eventService = eventService;
        this.remoteEventServices = remoteEventServices;

        messageConverter = new JsonMessageConverter();
        connections = new ConcurrentHashMap<>();
        start = new AtomicBoolean(false);
    }

    @PostConstruct
    void start() {
        if (start.compareAndSet(false, true)) {
            super.start();
            if (remoteEventServices != null && remoteEventServices.length > 0) {
                executor = Executors.newCachedThreadPool(new NamedThreadFactory("WSocketEventBusClient", true));
                for (String service : remoteEventServices) {
                    try {
                        executor.execute(new ConnectTask(new URI(service)));
                    } catch (URISyntaxException e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
            }
        }
    }

    @Override
    protected void propagate(Object event) {
        for (Future<WSClient> future : connections.values()) {
            if (future.isDone()) {
                try {
                    future.get().send(messageConverter.toString(Messages.clientMessage(event)));
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }
    }

    @PreDestroy
    void stop() {
        if (start.compareAndSet(true, false) && remoteEventServices != null && remoteEventServices.length > 0) {
            executor.shutdownNow();
        }
    }

    private void connect(final URI wsUri) throws IOException {
        Future<WSClient> clientFuture = connections.get(wsUri);
        if (clientFuture == null) {
            FutureTask<WSClient> newFuture = new FutureTask<>(new Callable<WSClient>() {
                @Override
                public WSClient call() throws IOException, MessageConversionException {
                    WSClient wsClient = new WSClient(wsUri, new WSocketListener(wsUri));
                    wsClient.connect(wsConnectionTimeout);
                    return wsClient;
                }
            });
            clientFuture = connections.putIfAbsent(wsUri, newFuture);
            if (clientFuture == null) {
                clientFuture = newFuture;
                newFuture.run();
            }
        }
        boolean connected = false;
        try {
            clientFuture.get(); // wait for connection
            connected = true;
        } catch (ExecutionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof Error) {
                throw (Error)cause;
            } else if (cause instanceof RuntimeException) {
                throw (RuntimeException)cause;
            } else if (cause instanceof IOException) {
                throw (IOException)cause;
            }
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            if (!connected) {
                connections.remove(wsUri);
            }
        }
    }

    private class WSocketListener extends BaseClientMessageListener {
        final URI wsUri;

        WSocketListener(URI wsUri) {
            this.wsUri = wsUri;
        }

        @Override
        public void onClose(int status, String message) {
            connections.remove(wsUri);
            LOG.debug("Close connection to {}. ", wsUri);
            if (start.get()) {
                executor.execute(new ConnectTask(wsUri));
            }
        }

        @Override
        public void onMessage(String data) {
            try {
                final Object event = Messages
                        .restoreEventFromBroadcastMessage(messageConverter.fromString(data, RESTfulOutputMessage.class));
                if (event != null) {
                    eventService.publish(event);
                }
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }

        @Override
        public void onOpen(WSClient client) {
            LOG.debug("Open connection to {}. ", wsUri);
            try {
                client.send(messageConverter.toString(Messages.subscribeChannelMessage()));
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    private class ConnectTask implements Runnable {
        final URI wsUri;

        ConnectTask(URI wsUri) {
            this.wsUri = wsUri;
        }

        @Override
        public void run() {
            for (; ; ) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                try {
                    connect(wsUri);
                    return;
                } catch (IOException e) {
                    LOG.error(String.format("Failed connect to %s", wsUri), e);
                    synchronized (this) {
                        try {
                            wait(wsConnectionTimeout * 2);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }
            }
        }
    }
}
