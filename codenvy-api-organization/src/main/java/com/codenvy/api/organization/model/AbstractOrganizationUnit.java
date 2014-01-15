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

package com.codenvy.api.organization.model;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.LinkedHashSet;
import java.util.Set;

public abstract class AbstractOrganizationUnit implements Externalizable {
    String id;

    boolean temporary;

    Set<Link> links = new LinkedHashSet<>();

    public boolean isTemporary() {
        return temporary;
    }

    public void setTemporary(boolean temporary) {
        this.temporary = temporary;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = toLowerCase(id);
    }

    public Set<Link> getLinks() {
        return links;
    }

    public void setLinks(Set<Link> links) {
        this.links = links;
    }

    public String toLowerCase(String string) {
        if (string != null) {
            return string.toLowerCase();
        }

        return null;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        id = in.readUTF();
        temporary = in.readBoolean();
        int linksNumber = in.readInt();
        for (int i = 0; i < linksNumber; ++i) {
            Link link = new Link();
            link.readExternal(in);
            links.add(link);
        }
    }


    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(id);
        out.writeBoolean(temporary);
        out.writeInt(links.size());
        for (Link one : links) {
            one.writeExternal(out);
        }
    }
}
