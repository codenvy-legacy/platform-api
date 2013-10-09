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

package com.codenvy.api.factory;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/** An object that contain URL for some resource. */
public class Link implements Externalizable {
    /** Produced media type of resource described by this link. */
    private String type;

    /** URL of resource. */
    private String href;

    /** Relation attribute of link. Client may use it for choice links to retrieve specific info about resource. */
    private String rel;

    public Link() {
    }

    public Link(String type, String href, String rel) {
        this.type = type;
        this.href = href;
        this.rel = rel;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getRel() {
        return rel;
    }

    public void setRel(String rel) {
        this.rel = rel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Link link = (Link)o;

        if (href != null ? !href.equals(link.href) : link.href != null) {
            return false;
        }
        if (rel != null ? !rel.equals(link.rel) : link.rel != null) {
            return false;
        }
        if (type != null ? !type.equals(link.type) : link.type != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (href != null ? href.hashCode() : 0);
        result = 31 * result + (rel != null ? rel.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Link{" + "type='" + type + '\'' + ", href='" + href + '\'' + ", rel='" + rel + '\'' + '}';
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(type);
        out.writeUTF(href);
        out.writeUTF(rel);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        type = (String)in.readObject();
        href = in.readUTF();
        rel = in.readUTF();
    }
}
