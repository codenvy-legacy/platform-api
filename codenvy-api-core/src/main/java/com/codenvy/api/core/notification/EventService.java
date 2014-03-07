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

import com.codenvy.commons.lang.cache.Cache;
import com.codenvy.commons.lang.cache.LoadingValueSLRUCache;
import com.codenvy.commons.lang.cache.SynchronizedCache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dispatchers events to listeners. Usage example:
 * <pre>
 *     EventService bus = new EventService();
 *     bus.subscribe("test", new MessageReceiver&lt;String&gt;() {
 *         &#64;Override
 *         public void onEvent(String channel, String data) {
 *             System.out.println(channel + ":" + data);
 *         }
 *     });
 *     bus.publish("test", "hello");
 * </pre>
 * Code above prints "test:hello" to the output.
 *
 * @author andrew00x
 */
@Singleton
public final class EventService {
    private static final Logger LOG = LoggerFactory.getLogger(EventService.class);

    private static final int CACHE_NUM  = 1 << 2;
    private static final int CACHE_MASK = CACHE_NUM - 1;
    private static final int SEG_SIZE   = 16;

    private final Cache<Class<?>, Set<Class<?>>>[]              typeCache;
    private final ConcurrentMap<String, List<ListenerWithType>> listenersByChannel;

    @SuppressWarnings("unchecked")
    public EventService() {
        listenersByChannel = new ConcurrentHashMap<>();
        typeCache = new Cache[CACHE_NUM];
        for (int i = 0; i < CACHE_NUM; i++) {
            typeCache[i] = new SynchronizedCache<>(new LoadingValueSLRUCache<Class<?>, Set<Class<?>>>(SEG_SIZE, SEG_SIZE) {
                @Override
                protected Set<Class<?>> loadValue(Class<?> eventClass) throws RuntimeException {
                    LinkedList<Class<?>> parents = new LinkedList<>();
                    Set<Class<?>> classes = new HashSet<>();
                    parents.add(eventClass);
                    while (!parents.isEmpty()) {
                        Class<?> clazz = parents.pop();
                        classes.add(clazz);
                        Class<?> parent = clazz.getSuperclass();
                        if (parent != null) {
                            parents.add(parent);
                        }
                        Class<?>[] interfaces = clazz.getInterfaces();
                        if (interfaces.length > 0) {
                            Collections.addAll(parents, interfaces);
                        }
                    }
                    return classes;
                }
            });
        }
    }

    /**
     * Publish event {@code data} to the {@code channel}.
     *
     * @param channel
     *         channel for publishing
     * @param data
     *         event
     */
    @SuppressWarnings("unchecked")
    public void publish(String channel, Object data) {
        if (channel == null) {
            throw new IllegalArgumentException("Invalid name of channel.");
        }
        if (data == null) {
            throw new IllegalArgumentException("Null event.");
        }
        final List<ListenerWithType> wrappers = listenersByChannel.get(channel);
        if (wrappers != null && !wrappers.isEmpty()) {
            final Class<?> eventClass = data.getClass();
            for (Class<?> clazz : typeCache[eventClass.hashCode() & CACHE_MASK].get(eventClass)) {
                for (ListenerWithType wrapper : wrappers) {
                    if (wrapper.eventType.equals(clazz)) {
                        try {
                            wrapper.listener.onEvent(channel, data);
                        } catch (RuntimeException e) {
                            LOG.error(e.getMessage(), e);
                        }
                    }
                }
            }
        }
    }

    /**
     * Subscribe {@code listener} to the {@code channel}.
     *
     * @param channel
     *         channel name
     * @param listener
     *         event listener
     * @throws java.lang.IllegalArgumentException
     *         if {@code channel} is {@code null}
     */
    public void subscribe(String channel, MessageReceiver<?> listener) {
        if (channel == null) {
            throw new IllegalArgumentException("Invalid name of channel.");
        }
        Class<?> eventType = null;
        Class<?> clazz = listener.getClass();
        while (clazz != null && eventType == null) {
            for (Type type : clazz.getGenericInterfaces()) {
                if (type instanceof ParameterizedType) {
                    final ParameterizedType parameterizedType = (ParameterizedType)type;
                    final Type rawType = parameterizedType.getRawType();
                    if (MessageReceiver.class == rawType) {
                        final Type[] typeArguments = parameterizedType.getActualTypeArguments();
                        if (typeArguments.length == 1) {
                            if (typeArguments[0] instanceof Class) {
                                eventType = (Class)typeArguments[0];
                            }
                        }
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
        if (eventType == null) {
            throw new IllegalArgumentException(String.format("Unable determine type of events processed by %s", listener));
        }
        List<ListenerWithType> wrappers = listenersByChannel.get(channel);
        if (wrappers == null) {
            List<ListenerWithType> newWrappers = new CopyOnWriteArrayList<>();
            wrappers = listenersByChannel.putIfAbsent(channel, newWrappers);
            if (wrappers == null) {
                wrappers = newWrappers;
            }
        }
        wrappers.add(new ListenerWithType(eventType, listener));
    }

    /**
     * Unsubscribe {@code listener} from the {@code channel}.
     *
     * @param channel
     *         channel name
     * @param listener
     *         event listener
     * @throws java.lang.IllegalArgumentException
     *         if {@code channel} is {@code null}
     */
    public void unsubscribe(String channel, MessageReceiver<?> listener) {
        if (channel == null) {
            throw new IllegalArgumentException("Invalid name of channel.");
        }
        final List<ListenerWithType> wrappers = listenersByChannel.get(channel);
        if (wrappers != null && !wrappers.isEmpty()) {
            List<ListenerWithType> toRemove = new LinkedList<>();
            for (ListenerWithType wrapper : wrappers) {
                if (listener.equals(wrapper.listener)) {
                    toRemove.add(wrapper);
                }
            }
            wrappers.removeAll(toRemove);
        }
    }

    private static class ListenerWithType {
        final Class<?>        eventType;
        final MessageReceiver listener;

        ListenerWithType(Class<?> eventType, MessageReceiver<?> listener) {
            this.eventType = eventType;
            this.listener = listener;
        }
    }
}
