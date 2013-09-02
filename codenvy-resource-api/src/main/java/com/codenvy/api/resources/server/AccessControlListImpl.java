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

import com.codenvy.api.resources.shared.AccessControlList;
import com.codenvy.api.resources.shared.Resource;
import com.codenvy.api.resources.shared.ResourceAccessControlEntry;
import com.codenvy.api.resources.shared.VirtualFileSystemConnector;
import com.codenvy.api.vfs.shared.Principal;

import java.util.ArrayList;
import java.util.List;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public class AccessControlListImpl implements AccessControlList {
    private final VirtualFileSystemConnector       connector;
    private final Resource                         resource;
    private final List<ResourceAccessControlEntry> acl;

    public AccessControlListImpl(VirtualFileSystemConnector connector, List<ResourceAccessControlEntry> acl, Resource resource) {
        this.connector = connector;
        this.acl = acl;
        this.resource = resource;
    }

    @Override
    public Resource getResource() {
        return resource;
    }

    @Override
    public List<ResourceAccessControlEntry> getAll() {
        return new ArrayList<>(acl);
    }

    @Override
    public ResourceAccessControlEntry getAccessControlEntry(Principal principal) {
        for (ResourceAccessControlEntry entry : acl) {
            final Principal ePrincipal = entry.getPrincipal();
            if (ePrincipal.getName().equals(principal.getName()) && ePrincipal.getType() == principal.getType()) {
                return entry;
            }
        }
        final ResourceAccessControlEntry entry = new ResourceAccessControlEntryImpl(principal);
        acl.add(entry);
        return entry;
    }

    @Override
    public void save() {
        connector.updateACL(this);
    }

    public String dump() {
        final StringBuilder sb = new StringBuilder();
        for (ResourceAccessControlEntry entry : acl) {
            final Principal principal = entry.getPrincipal();
            sb.append(principal.getType());
            sb.append(':');
            sb.append(principal.getName());
            sb.append('\n');
            for (String p : entry.getPermissions()) {
                sb.append(' ');
                sb.append(p);
                sb.append('\n');
            }

        }
        return sb.toString();
    }
}
