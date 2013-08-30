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
package com.codenvy.api.resources.shared;

import com.codenvy.api.vfs.shared.Item;
import com.codenvy.api.vfs.shared.Lock;

import java.util.List;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public abstract class VirtualFileSystemConnector {
    private final String name;

    public VirtualFileSystemConnector(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract Folder getRoot();

    public abstract Resource getResource(Folder parent, String name);

    public abstract List<Resource> getChildResources(Folder parent);

    public abstract File createFile(Folder parent, String name);

    public abstract Folder createFolder(Folder parent, String name);

    public abstract Project createProject(String name);

    public abstract Project createProject(Project parent, String name);

    public abstract void delete(Resource resource);

    public abstract String getContent(File file);

    public abstract void updateContent(File file, String data, String contentType);

    public abstract Attributes getAttributes(Resource resource);

    public abstract void updateAttributes(Attributes attributes);

    public abstract Resource rename(Resource resource, String newname, String contentType);

    public abstract Resource move(Resource resource, Folder newparent);

    public abstract Lock lock(File file, long timeout);

    public abstract void unlock(File file, String lockToken);

    public abstract AccessControlList loadACL(Resource resource);

    public abstract void updateACL(AccessControlList acl);

    public abstract Item getVfsItem(Resource resource);
}
