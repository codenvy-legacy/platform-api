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

import org.everrest.websockets.WSConnectionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

/**
 * @author andrew00x
 */
@Singleton
@Path("event-bus")
public final class WSocketEventBusServer extends WSocketEventBus {
    private static final Logger LOG = LoggerFactory.getLogger(WSocketEventBusServer.class);

    private final EventService eventService;

    @Inject
    WSocketEventBusServer(EventService eventService, @Nullable EventPropagationPolicy policy) {
        super(eventService, policy);
        this.eventService = eventService;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void event(String message) {
        try {
            final Object event = Messages.restoreEventFromClientMessage(message);
            if (event != null) {
                eventService.publish(event);
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    @Override
    protected void propagate(Object event) {
        try {
            WSConnectionContext.sendMessage(Messages.broadcastMessage(event));
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }
}
