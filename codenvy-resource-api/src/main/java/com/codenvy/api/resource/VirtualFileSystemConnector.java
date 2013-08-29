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
package com.codenvy.api.resource;

import com.codenvy.api.resource.attribute.AttributeProvider;
import com.codenvy.api.resource.attribute.Attributes;
import com.codenvy.api.resource.attribute.vfs.ContentTypeProvider;
import com.codenvy.api.resource.attribute.vfs.LastUpdateTimeProvider;
import com.codenvy.api.resource.attribute.vfs.ProjectTypeProvider;
import com.codenvy.api.resource.attribute.vfs.SimpleAttributeProvider;

import com.codenvy.api.vfs.shared.Item;
import com.codenvy.api.vfs.shared.Lock;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public abstract class VirtualFileSystemConnector {
    private static Map<String, AttributeProvider<?>> attributeProviders = new HashMap<>();

    public static void addAttributeProvider(AttributeProvider<?> attributeProvider) {
        attributeProviders.put(attributeProvider.getName(), attributeProvider);
    }

    public static Set<String> getAttributeProviderNames() {
        return new HashSet<>(attributeProviders.keySet());
    }

    public static AttributeProvider<?> getAttributeProvider(String name) {
        AttributeProvider<?> attrProv = attributeProviders.get(name);
        if (attrProv == null) {
            attrProv = new SimpleAttributeProvider(name);
        }
        return attrProv;
    }

    static {
        addAttributeProvider(new ContentTypeProvider());
        addAttributeProvider(new ProjectTypeProvider());
        addAttributeProvider(new LastUpdateTimeProvider());
    }

    private final String name;

    public VirtualFileSystemConnector(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract Folder getRoot();

    public abstract Resource getResource(Folder parent, String name);

    public abstract Resource[] getChildResources(Folder parent);

    public abstract File createFile(Folder parent, String name);

    public abstract Folder createFolder(Folder parent, String name);

    public abstract Project createProject(String name);

    public abstract Project createProject(Project parent, String name);

    public abstract void delete(Resource resource);

    public abstract InputStream getContentStream(File file) throws IOException;

    public abstract void updateContentStream(File file, InputStream data, String contentType) throws IOException;

    public abstract Attributes getAttributes(Resource resource);

    public abstract void updateAttributes(Resource resource, Attributes attributes);

    public abstract Resource rename(Resource resource, String newname, String contentType);

    public abstract Resource move(Resource resource, Folder newparent);

    public abstract Lock lock(File file);

    public abstract void unlock(File file, String lockToken);

    public abstract Item getVfsItem(Resource resource);
}
