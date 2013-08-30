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
package com.codenvy.api.resources.server.attribute;

import com.codenvy.api.resources.shared.Attribute;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public class AttributeImpl<T> implements Attribute<T> {
    private final String  name;
    private final String  displayName;
    private final boolean readOnly;
    private final boolean persistent;
    private       T       value;
    private       boolean updated;

    public AttributeImpl(String name, String displayName, boolean readOnly, boolean persistent, T value) {
        this.name = name;
        this.displayName = displayName;
        this.readOnly = readOnly;
        this.persistent = persistent;
        this.value = value;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public boolean isPersistent() {
        return persistent;
    }

    @Override
    public boolean isUpdated() {
        return updated;
    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public void setValue(T value) {
        this.value = value;
        updated = true;
    }

    @Override
    public String toString() {
        return "Attribute{" +
               "name='" + name + '\'' +
               ", readOnly=" + readOnly +
               ", persistent=" + persistent +
               ", value=" + value +
               '}';
    }
}
