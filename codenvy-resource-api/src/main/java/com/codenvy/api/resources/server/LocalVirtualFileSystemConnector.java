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
 * patents in process, and are public by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.api.resources.server;

import com.codenvy.api.resources.shared.AccessControlList;
import com.codenvy.api.resources.shared.Attributes;
import com.codenvy.api.resources.shared.File;
import com.codenvy.api.resources.shared.Folder;
import com.codenvy.api.resources.shared.Project;
import com.codenvy.api.resources.shared.Resource;
import com.codenvy.api.vfs.server.VirtualFileSystem;
import com.codenvy.api.vfs.shared.Item;
import com.codenvy.api.vfs.shared.Lock;

import java.io.InputStream;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public class LocalVirtualFileSystemConnector extends VirtualFileSystemConnectorImpl {
    private final VirtualFileSystem vfs;

    public LocalVirtualFileSystemConnector(String name, VirtualFileSystem vfs) {
        super(name);
        this.vfs = vfs;
    }

    @Override
    public Folder getRoot() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Resource getResource(Folder parent, String relPath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Resource[] getChildResources(Folder parent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public File createFile(Folder parent, String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Folder createFolder(Folder parent, String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Project createProject(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Project createProject(Project parent, String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(Resource resource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getContent(File file) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateContent(File file, String data, String contentType) {
        throw new UnsupportedOperationException();
    }

    public InputStream getContentStream(File file) {
        throw new UnsupportedOperationException();
    }

    public void updateContentStream(File file, InputStream data, String contentType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Attributes getAttributes(Resource resource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateAttributes(Attributes attributes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Resource rename(Resource resource, String newname, String contentType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Resource move(Resource resource, Folder newparent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Lock lock(File file, long timeout) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unlock(File file, String lockToken) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AccessControlList loadACL(Resource resource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateACL(AccessControlList acl) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Item getVfsItem(Resource resource) {
        throw new UnsupportedOperationException();
    }
}
