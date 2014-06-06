/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
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
