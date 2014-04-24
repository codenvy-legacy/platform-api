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
            case CHANGE:
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
