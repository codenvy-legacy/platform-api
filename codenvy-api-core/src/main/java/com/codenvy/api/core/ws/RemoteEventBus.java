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
package com.codenvy.api.core.ws;

import com.codenvy.commons.json.JsonHelper;
import com.codenvy.commons.lang.NameGenerator;
import com.codenvy.commons.lang.NamedThreadFactory;

import org.everrest.websockets.client.BaseClientMessageListener;
import org.everrest.websockets.client.WSClient;
import org.everrest.websockets.message.InputMessage;
import org.everrest.websockets.message.JsonMessageConverter;
import org.everrest.websockets.message.MessageConverter;
import org.everrest.websockets.message.Pair;
import org.everrest.websockets.message.RESTfulOutputMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author andrew00x
 */
public class RemoteEventBus<E> {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteEventBus.class);

    private static final long connectTimeout = 1000;

    private final ExecutorService              executor;
    private final MessageConverter             messageConverter;
    private final WSClient                     bus;
    private final List<RemoteEventListener<E>> listeners;
    private final String                       name;
    private final Class<E>                     eventType;

    private volatile boolean stop;

    RemoteEventBus(String name, Class<E> eventType, String url) throws IOException {
        this.name = name;
        this.eventType = eventType;
        messageConverter = new JsonMessageConverter();
        listeners = new CopyOnWriteArrayList<>();
        executor = Executors.newCachedThreadPool(new NamedThreadFactory("REMOTE_EVENT_BUS", true));
        bus = new WSClient(URI.create(url), new WSMessageListener());
        bus.connect(connectTimeout);
    }

    public static <T> RemoteEventBus create(String name, Class<T> eventType, String url) throws IOException {
        return new RemoteEventBus<>(name, eventType, url);
    }

    public String getName() {
        return name;
    }

    public void register(RemoteEventListener<E> listener) {
        listeners.add(listener);
    }

    public void unregister(RemoteEventListener<E> listener) {
        listeners.remove(listener);
    }

    public void post(final String path, final E event) throws Exception {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                final RESTfulOutputMessage message = new RESTfulOutputMessage();
                message.setUuid(NameGenerator.generate(null, 8));
                message.setMethod("PUT");
                message.setHeaders(new Pair[]{new Pair("content-type", "application/json")});
                message.setPath(path);
                message.setBody(JsonHelper.toJson(event));
                try {
                    bus.send(messageConverter.toString(message));
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        });
    }

    public void stop() throws IOException {
        stop = true;
        bus.disconnect();
        executor.shutdownNow();
    }

    private void notifyListeners(String raw) {
        final E event;
        try {
            final InputMessage inputMessage = messageConverter.fromString(raw, InputMessage.class);
            event = JsonHelper.fromJson(inputMessage.getBody(), eventType, null);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return;
        }
        for (RemoteEventListener<E> listener : listeners) {
            try {
                listener.handleEvent(event);
            } catch (Throwable e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    private class WSMessageListener extends BaseClientMessageListener {
        @Override
        public void onMessage(String data) {
            notifyListeners(data);
        }

        @Override
        public void onClose(int status, String message) {
            if (!stop) {
                try {
                    bus.connect(connectTimeout);
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }
    }
}
