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
package com.codenvy.api.resource.attribute;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public class Attribute<T> {
    private final String  name;
    private final String  displayName;
    private final boolean readOnly;
    private final boolean persistent;
    private       T       value;
    private       boolean updated;

    public Attribute(String name, String displayName, boolean readOnly, boolean persistent, T value) {
        this.name = name;
        this.displayName = displayName;
        this.readOnly = readOnly;
        this.persistent = persistent;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public boolean isUpdated() {
        return updated;
    }

    public T getValue() {
        return value;
    }

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
