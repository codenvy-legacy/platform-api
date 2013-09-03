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
package com.codenvy.api.resources.shared;

/** @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a> */
public abstract class Resource {
    protected final VirtualFileSystemConnector connector;
    protected final String                     type;
    private final   String                     id;
    private final   String                     name;
    private final   Folder                     parent;
    private         Attributes                 attributes;
    private         AccessControlList          acl;
    boolean valid = true;

    protected Resource(VirtualFileSystemConnector connector, Folder parent, String type, String id, String name) {
        this.connector = connector;
        this.parent = parent;
        this.type = type;
        this.id = id;
        this.name = name;
    }

    public abstract boolean isFolder();

    public abstract boolean isProject();

    public abstract boolean isFile();

    public String getId() {
        checkValid();
        return id;
    }

    public String getName() {
        checkValid();
        return name;
    }

    public String getType() {
        return type;
    }

    public String getPath() {
        checkValid();
        final Folder parent = getParent();
        if (parent != null) {
            if (parent.isRoot()) {
                return '/' + name;
            }
            return parent.getPath() + '/' + name;
        }
        return "/";
    }

    public Folder getParent() {
        checkValid();
        return parent;
    }

    public Attributes getAttributes() {
        return getAttributes(false);
    }

    public Attributes getAttributes(boolean renew) {
        checkValid();
        if (attributes == null || renew) {
            attributes = connector.getAttributes(this);
        }
        return attributes;
    }

    public Resource move(Folder newparent) {
        checkValid();
        final Resource moved = connector.move(this, newparent);
        valid = false;
        return moved;
    }

    public Resource rename(String newname, String contentType) {
        checkValid();
        final Resource renamed = connector.rename(this, newname, contentType);
        valid = false;
        return renamed;
    }

    public void delete() {
        checkValid();
        connector.delete(this);
        valid = false;
    }

    public AccessControlList getACL() {
        return getACL(false);
    }

    public AccessControlList getACL(boolean renew) {
        checkValid();
        if (acl == null || renew) {
            acl = connector.loadACL(this);
        }
        return acl;
    }

    protected final void checkValid() {
        if (!valid) {
            throw new IllegalStateException("Resource is not valid any more. Probably it was moved, renamed or deleted");
        }
    }
}
