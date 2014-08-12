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
package com.codenvy.api.user.server.dao;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Eugene Voevodin
 */
public class Profile {

    private String              id;
    private String              userId;
    private Map<String, String> attributes;
    private Map<String, String> preferences;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Profile withId(String id) {
        this.id = id;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Profile withUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public Map<String, String> getAttributes() {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public Profile withAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
        return this;
    }

    public Map<String, String> getPreferences() {
        if (preferences == null) {
            preferences = new HashMap<>();
        }
        return preferences;
    }

    public void setPreferences(Map<String, String> preferences) {
        this.preferences = preferences;
    }

    public Profile withPreferences(Map<String, String> preferences) {
        this.preferences = preferences;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Profile profile = (Profile)o;

        if (attributes != null ? !attributes.equals(profile.attributes) : profile.attributes != null) return false;
        if (id != null ? !id.equals(profile.id) : profile.id != null) return false;
        if (preferences != null ? !preferences.equals(profile.preferences) : profile.preferences != null) return false;
        if (userId != null ? !userId.equals(profile.userId) : profile.userId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (userId != null ? userId.hashCode() : 0);
        result = 31 * result + (attributes != null ? attributes.hashCode() : 0);
        result = 31 * result + (preferences != null ? preferences.hashCode() : 0);
        return result;
    }
}
