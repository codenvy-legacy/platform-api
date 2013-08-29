/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 * [2012] - [2013] Codenvy, S.A.
 * All Rights Reserved.
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

import com.codenvy.api.resource.local.LocalVirtualFileSystemConnector;
import com.codenvy.api.resource.remote.RemoteVirtualFileSystemConnector;

import org.exoplatform.ide.vfs.server.VirtualFileSystem;

/** @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a> */
public class Workspace {
    private final VirtualFileSystemConnector virtualFileSystemConnector;

    public Workspace(String name, VirtualFileSystem vfs) {
        this(new LocalVirtualFileSystemConnector(name, vfs));
    }

    public Workspace(String name, String remoteUrl) {
        this(new RemoteVirtualFileSystemConnector(name, remoteUrl));
    }

    protected Workspace(VirtualFileSystemConnector virtualFileSystemConnector) {
        this.virtualFileSystemConnector = virtualFileSystemConnector;
    }

    public String getName() {
        return virtualFileSystemConnector.getName();
    }

    public String getTariffScale() {
        return null;  //TODO
    }

    public Folder getRoot() {
        return virtualFileSystemConnector.getRoot();
    }
}
