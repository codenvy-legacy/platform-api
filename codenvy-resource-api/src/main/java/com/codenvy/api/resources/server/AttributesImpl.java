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

import com.codenvy.api.resources.shared.Attribute;
import com.codenvy.api.resources.shared.Attributes;
import com.codenvy.api.resources.shared.Resource;
import com.codenvy.api.resources.shared.VirtualFileSystemConnector;

import java.util.ArrayList;
import java.util.List;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public class AttributesImpl implements Attributes {

    private final VirtualFileSystemConnector connector;
    private final List<Attribute<?>>         attributes;
    private final Resource                   resource;

    public AttributesImpl(VirtualFileSystemConnector connector, Resource resource) {
        this.connector = connector;
        this.resource = resource;
        attributes = new ArrayList<>(4);
    }

    @Override
    public Resource getResource() {
        return resource;
    }

    @Override
    public List<Attribute<?>> getAll() {
        return new ArrayList<>(attributes);
    }

    @Override
    public Attribute getAttribute(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Attribute name may not be null");
        }
        for (Attribute<?> attr : attributes) {
            if (name.equals(attr.getName())) {
                return attr;
            }
        }
        final Attribute<?> attr =
                AttributeProviderRegistryImpl.INSTANCE.getAttributeProvider(resource.getType(), name)
                                             .getAttribute(connector.getVfsItem(resource));
        attributes.add(attr);
        return attr;
    }

    @Override
    public void save() {
        connector.updateAttributes(this);
    }
}
