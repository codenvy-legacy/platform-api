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

import com.google.inject.Singleton;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores available subscription services
 *
 * @author Eugene Voevodin
 */
@Singleton
public class SubscriptionServiceRegistry {

    private final Map<String, SubscriptionService> services;

    public SubscriptionServiceRegistry() {
        services = new ConcurrentHashMap<>();
    }

    public void add(SubscriptionService service) {
        services.put(service.getServiceId(), service);
    }

    public SubscriptionService get(String serviceId) {
        return services.get(serviceId);
    }

    public SubscriptionService remove(String serviceId) {
        return services.remove(serviceId);
    }

    public Set<SubscriptionService> getAll() {
        return new LinkedHashSet<>(services.values());
    }

    public void clear() {
        services.clear();
    }
}