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

import java.util.HashSet;
import java.util.Set;

public class ItemReference extends AbstractOrganizationUnit {
    public ItemReference() {
    }

    public ItemReference(String id) {
        this.id = toLowerCase(id);
    }

    public ItemReference(ItemReference source) {
        this.id = (source.getId());
        Set<Link> newLinks = new HashSet<>();
        for (Link one : source.getLinks())
            newLinks.add(new Link(one.getType(), one.getHref(), one.getRel()));
        this.links = newLinks;
        this.temporary = source.isTemporary();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ItemReference that = (ItemReference)o;

        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }
        if (links != null ? !links.equals(that.links) : that.links != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (links != null ? links.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ItemReference{" + "id='" + id + '\'' + ", links=" + links + '}';
    }

}
