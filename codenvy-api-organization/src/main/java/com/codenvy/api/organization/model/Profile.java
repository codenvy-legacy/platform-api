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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * POJO representation of profile entity contained in {@link User_old} . Each user always has
 * only one profile. Profile is usually used as a container of user related attributes.
 */
public class Profile extends AbstractOrganizationUnit {
    /** Map containing attributes */
    private Map<String, String> attributes = new LinkedHashMap<>();

    public final Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public final void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    //===================================================

    public void setAttribute(String name, String value) {
        attributes.put(name, value);
    }

    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    public String getAttribute(String name) {
        return attributes.get(name);
    }


    public String getAttribute(String name, String defaultValue) {
        String val = getAttribute(name);
        return (val == null) ? defaultValue : val;
    }
    //===================================================

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Profile profile = (Profile)o;

        if (attributes != null ? !attributes.equals(profile.attributes) : profile.attributes != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return attributes != null ? attributes.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Profile{" + "attributes=" + attributes + '}';
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(attributes.size());
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            out.writeUTF(entry.getKey());
            out.writeUTF(entry.getValue());
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        int attributesNumber = in.readInt();
        for (int i = 0; i < attributesNumber; ++i) {
            attributes.put(in.readUTF(), in.readUTF());
        }
    }
}
