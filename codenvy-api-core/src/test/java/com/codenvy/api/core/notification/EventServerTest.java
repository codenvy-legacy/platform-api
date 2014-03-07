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

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author andrew00x
 */
public class EventServerTest {
    private EventService bus;

    @BeforeMethod
    public void setUp() {
        bus = new EventService();
    }

    @Test
    public void testSimpleEvent() {
        final List<Object> events = new ArrayList<>();
        bus.subscribe("test", new MessageReceiver<Date>() {
            @Override
            public void onEvent(String channel, Date data) {
                events.add(data);
            }
        });
        bus.subscribe("test", new MessageReceiver<String>() {
            @Override
            public void onEvent(String channel, String data) {
                events.add(data);
            }
        });
        bus.subscribe("test", new MessageReceiver<Long>() {
            @Override
            public void onEvent(String channel, Long data) {
                events.add(data);
            }
        });
        Date date = new Date();
        bus.publish("test", date);
        bus.publish("test", "hello");
        bus.publish("test", 123L);
        Assert.assertEquals(events.size(), 3);
        Assert.assertTrue(events.contains(date));
        Assert.assertTrue(events.contains("hello"));
        Assert.assertTrue(events.contains(123L));
        // ignored
        bus.publish("test", new Object());
        Assert.assertEquals(events.size(), 3);
    }

    static interface I {
    }

    static class Listener implements I, MessageReceiver<String> {
        final List<String> events = new ArrayList<>();

        @Override
        public void onEvent(String channel, String data) {
            events.add(String.format("%s:%s", channel, data));
        }
    }

    static class ExtListener extends Listener {
    }

    @Test
    public void testRegisterHierarchicalListener() {
        ExtListener listener = new ExtListener();
        bus.subscribe("test", listener);
        bus.publish("test", "hello");
        Assert.assertEquals(listener.events.size(), 1);
        Assert.assertEquals(listener.events.get(0), "test:hello");
    }

    static class Event {
        String data;

        Event() {
            this("event");
        }

        Event(String data) {
            this.data = data;
        }

        @Override
        public String toString() {
            return data;
        }
    }

    static class ExtEvent extends Event {
        ExtEvent() {
            super("ext_event");
        }
    }

    @Test
    public void testHierarchicalEvent() {
        final List<String> events = new ArrayList<>();
        // register two listeners.
        // 1. Accept only Event type.
        bus.subscribe("test", new MessageReceiver<Event>() {
            @Override
            public void onEvent(String channel, Event data) {
                events.add(String.format("1:%s:%s", channel, data));
            }
        });

        // 2. Accept Event and ExtEvent types.
        bus.subscribe("test", new MessageReceiver<ExtEvent>() {
            @Override
            public void onEvent(String channel, ExtEvent data) {
                events.add(String.format("2:%s:%s", channel, data));
            }
        });

        bus.publish("test", new Event());
        Assert.assertEquals(events.size(), 1);
        Assert.assertEquals(events.get(0), "1:test:event");
        events.clear();
        bus.publish("test", new ExtEvent());
        Assert.assertEquals(events.size(), 2);
        Assert.assertTrue(events.contains("1:test:ext_event"));
        Assert.assertTrue(events.contains("2:test:ext_event"));
    }

    @Test
    public void testUnsubscribe() {
        final List<String> events = new ArrayList<>();
        MessageReceiver<Event> l = new MessageReceiver<Event>() {
            @Override
            public void onEvent(String channel, Event data) {
                events.add(String.format("%s:%s", channel, data));
            }
        };
        bus.subscribe("test", l);
        bus.publish("test", new Event());
        Assert.assertEquals(events.size(), 1);
        bus.unsubscribe("test", l);
        events.clear();
        bus.publish("test", new Event());
        Assert.assertEquals(events.size(), 0);
    }
}
