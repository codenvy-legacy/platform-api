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
