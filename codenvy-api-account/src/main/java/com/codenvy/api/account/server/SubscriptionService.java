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
package com.codenvy.api.account.server;


import java.util.LinkedList;
import java.util.List;

/**
 * Base class for any service which may communicate with account via subscriptions
 *
 * @author Eugene Voevodin
 */
public abstract class SubscriptionService {

    private final List<SubscriptionHandler> handlers;
    private final String                    serviceId;
    private final String                    displayName;


    public SubscriptionService(String serviceId, String displayName) {
        this.serviceId = serviceId;
        this.displayName = displayName;
        this.handlers = new LinkedList<>();
    }

    public void addHandler(SubscriptionHandler handler) {
        handlers.add(handler);
    }

    public void removeHandler(SubscriptionHandler handler) {
        handlers.remove(handler);
    }

    public void notifyHandlers(SubscriptionEvent event) {
        switch (event.getEventType()) {
            case CREATE:
                for (SubscriptionHandler handler : handlers) {
                    handler.onCreateSubscription(event);
                }
                break;
            case REMOVE:
                for (SubscriptionHandler handler : handlers) {
                    handler.onRemoveSubscription(event);
                }
                break;
            case CHECK:
                for (SubscriptionHandler handler : handlers) {
                    handler.onCheckSubscription(event);
                }
                break;
            default:
                throw new IllegalArgumentException(String.format("Given event type %s is not supported", event.getEventType()));
        }
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getDisplayName() {
        return displayName;
    }
}
