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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public enum AttributeProviderRegistryImpl implements AttributeProviderRegistry {
    INSTANCE;

    private Map<String, AttributeProvider<?>> attributeProviders = new HashMap<>();

    public void addAttributeProvider(AttributeProvider<?> attributeProvider) {
        attributeProviders.put(attributeProvider.getName(), attributeProvider);
    }

    public List<String> getAttributeProviderNames() {
        return new ArrayList<>(attributeProviders.keySet());
    }

    public AttributeProvider<?> getAttributeProvider(String name) {
        AttributeProvider<?> attrProv = attributeProviders.get(name);
        if (attrProv == null) {
            attrProv = new SimpleAttributeProvider(name);
        }
        return attrProv;
    }
}
