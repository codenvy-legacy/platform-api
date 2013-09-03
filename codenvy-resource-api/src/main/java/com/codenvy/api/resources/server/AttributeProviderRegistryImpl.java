/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2013] Codenvy, S.A. 
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
package com.codenvy.api.resources.server;

import com.codenvy.api.resources.shared.AttributeProvider;
import com.codenvy.api.resources.shared.AttributeProviderRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Simple singleton implementation of AttributeProviderRegistry.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
public enum AttributeProviderRegistryImpl implements AttributeProviderRegistry {
    INSTANCE;

    private final ConcurrentMap<String, Map<String, AttributeProvider<?>>> all = new ConcurrentHashMap<>(3);

    @Override
    public void addAttributeProvider(String itemType, AttributeProvider<?> attributeProvider) {
        getAttributeProvidersForType(itemType).put(attributeProvider.getName(), attributeProvider);
    }

    @Override
    public AttributeProvider<?> removeAttributeProvider(String itemType, String attributeName) {
        return getAttributeProvidersForType(itemType).get(attributeName);
    }

    @Override
    public List<AttributeProvider<?>> getAttributeProviders(String itemType) {
        return new ArrayList<>(getAttributeProvidersForType(itemType).values());
    }

    @Override
    public AttributeProvider<?> getAttributeProvider(String itemType, String attributeName) {
        return getAttributeProvidersForType(itemType).get(attributeName);
    }

    private Map<String, AttributeProvider<?>> getAttributeProvidersForType(String itemType) {
        Map<String, AttributeProvider<?>> byType = all.get(itemType);
        if (byType == null) {
            Map<String, AttributeProvider<?>> newByType = new ConcurrentHashMap<>();
            byType = all.putIfAbsent(itemType, newByType);
            if (byType == null) {
                byType = newByType;
            }
        }
        return byType;
    }
}
