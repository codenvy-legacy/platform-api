/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.workspace.server.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Eugene Voevodin
 */
public class Workspace {

    private boolean         temporary;
    private String          id;
    private String          name;
    private String          accountId;
    private List<Attribute> attributes;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Workspace withId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Workspace withName(String name) {
        this.name = name;
        return this;
    }

    public boolean isTemporary() {
        return temporary;
    }

    public void setTemporary(boolean temporary) {
        this.temporary = temporary;
    }

    public Workspace withTemporary(boolean temporary) {
        this.temporary = temporary;
        return this;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public Workspace withAccountId(String accountId) {
        this.accountId = accountId;
        return this;
    }

    public List<Attribute> getAttributes() {
        if (attributes == null) {
            attributes = new ArrayList<>();
        }
        return attributes;
    }

    public void setAttributes(List<Attribute> attributes) {
        this.attributes = attributes;
    }

    public Workspace withAttributes(List<Attribute> attributes) {
        this.attributes = attributes;
        return this;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + Objects.hashCode(temporary);
        hash = 31 * hash + Objects.hashCode(id);
        hash = 31 * hash + Objects.hashCode(name);
        hash = 31 * hash + Objects.hashCode(accountId);
        hash = 31 * hash + Objects.hashCode(getAttributes());
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Workspace)) {
            return false;
        }
        final Workspace other = (Workspace)obj;
        return temporary == other.temporary &&
               Objects.equals(id, other.id) &&
               Objects.equals(name, other.name) &&
               Objects.equals(accountId, other.accountId) &&
               Objects.equals(getAttributes(), other.getAttributes());
    }
}
