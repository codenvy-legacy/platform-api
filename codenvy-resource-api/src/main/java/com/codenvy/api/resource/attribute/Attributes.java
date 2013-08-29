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
package com.codenvy.api.resource.attribute;

import com.codenvy.api.resource.Resource;
import com.codenvy.api.resource.VirtualFileSystemConnector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public class Attributes {
    /** Name of content type attribute. */
    public static final String CONTENT_TYPE     = "content_type";
    public static final String DESCRIPTION      = "description";
    /** Name of resource type attribute. */
    public static final String PROJECT_TYPE     = "project_type";
    /** Name of last update time attribute. */
    public static final String LAST_UPDATE_TIME = "last_update_time";
    /** Name of builder which should be user to build resource. */
    public static final String BUILDER          = "builder";
    /** Name of runner which should be user to run resource. */
    public static final String RUNNER           = "runner";
    /** Name of server to deploy resource. Runner may not support customization of server. */
    public static final String SERVER_NAME      = "server_name";

    private final VirtualFileSystemConnector connector;
    private final List<Attribute<?>>         attributes;
    private final Resource                   resource;

    public Attributes(VirtualFileSystemConnector connector, Resource resource) {
        this.connector = connector;
        this.resource = resource;
        attributes = new ArrayList<>(4);
    }

    public Attribute getAttribute(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Attribute name may not ne null");
        }
        for (Attribute<?> attr : attributes) {
            if (name.equals(attr.getName())) {
                return attr;
            }
        }
        final Attribute<?> attr = VirtualFileSystemConnector.getAttributeProvider(name).getAttribute(connector.getVfsItem(resource));
        attributes.add(attr);
        return attr;
    }

    public void save() {
        connector.updateAttributes(resource, this);
    }

    public List<Attribute<?>> getUpdates() {
        if (attributes.isEmpty()) {
            return Collections.emptyList();
        }
        final List<Attribute<?>> l = new ArrayList<>(4);
        for (Attribute<?> attr : attributes) {
            if (attr.isUpdated()) {
                l.add(attr);
            }
        }
        return l;
    }
}
