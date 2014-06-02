/*******************************************************************************
* Copyright (c) 2012-2014 Codenvy, S.A.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* Codenvy, S.A. - initial API and implementation
*******************************************************************************/
package com.codenvy.api.core.notification;

import com.codenvy.commons.lang.NameGenerator;

import org.everrest.core.impl.provider.json.JsonGenerator;
import org.everrest.core.impl.provider.json.JsonParser;
import org.everrest.core.impl.provider.json.JsonValue;
import org.everrest.core.impl.provider.json.JsonWriter;
import org.everrest.core.impl.provider.json.ObjectBuilder;
import org.everrest.core.impl.provider.json.StringValue;
import org.everrest.websockets.message.ChannelBroadcastMessage;
import org.everrest.websockets.message.InputMessage;
import org.everrest.websockets.message.Pair;
import org.everrest.websockets.message.RESTfulInputMessage;
import org.everrest.websockets.message.RESTfulOutputMessage;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

/**
 * @author andrew00x
 */
class Messages {
    static InputMessage clientMessage(Object event) throws Exception {
        RESTfulInputMessage message = new RESTfulInputMessage();
        message.setBody(toJson(event));
        message.setMethod("POST");
        message.setHeaders(new org.everrest.websockets.message.Pair[]{
                new org.everrest.websockets.message.Pair("Content-type", "application/json")});
        message.setUuid(NameGenerator.generate(null, 8));
        message.setPath("/event-bus");
        return message;
    }

    static InputMessage subscribeChannelMessage() throws Exception {
        return RESTfulInputMessage.newSubscribeChannelMessage(NameGenerator.generate(null, 8), "EventBus");
    }

    static ChannelBroadcastMessage broadcastMessage(Object event) throws Exception {
        final ChannelBroadcastMessage message = new ChannelBroadcastMessage();
        message.setBody(toJson(event));
        message.setChannel("EventBus");
        return message;
    }

    static Object restoreEventFromBroadcastMessage(RESTfulOutputMessage message) throws Exception {
        if (message != null && message.getHeaders() != null) {
            for (Pair pair : message.getHeaders()) {
                if ("x-everrest-websocket-channel".equals(pair.getName()) && "EventBus".equals(pair.getValue())) {
                    return fromJson(message.getBody());
                }
            }
        }
        return null;
    }

    static Object restoreEventFromClientMessage(String message) throws Exception {
        if (message != null) {
            return fromJson(message);
        }
        return null;
    }

    private static String toJson(Object event) throws Exception {
        final String type = event.getClass().getName();
        final JsonValue json = JsonGenerator.createJsonObject(event);
        json.addElement("$type", new StringValue(type));
        final Writer w = new StringWriter();
        json.writeTo(new JsonWriter(w));
        return w.toString();
    }

    private static Object fromJson(String json) throws Exception {
        if (json == null || json.isEmpty()) {
            return null;
        }
        final JsonParser parser = new JsonParser();
        parser.parse(new StringReader(json));
        final JsonValue node = parser.getJsonObject();
        final JsonValue typeNode = node.getElement("$type");
        final String type;
        if (typeNode == null || (type = typeNode.getStringValue()) == null) {
            return null;
        }
        return ObjectBuilder.createObject(Class.forName(type), node);
    }

    private Messages() {
    }
}
