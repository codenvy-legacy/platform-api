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

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;

/**
 * @author andrew00x
 */
public abstract class WSocketEventBus {
    private final EventService           eventService;
    private final EventPropagationPolicy policy;

    protected WSocketEventBus(EventService eventService, @Nullable EventPropagationPolicy policy) {
        this.eventService = eventService;
        this.policy = policy;
    }

    @PostConstruct
    void start() {
        if (!(policy == null)) {
            eventService.subscribe(new EventSubscriber<Object>() {
                @Override
                public void onEvent(Object event) {
                    if (policy.shouldPropagated(event)) {
                        propagate(event);
                    }
                }
            });
        }
    }

    protected abstract void propagate(Object event);
}
