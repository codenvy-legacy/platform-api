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
package com.codenvy.api.resources.shared;

/**
 * Attribute of Resource.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @see AttributeProvider
 * @see AttributeProviderRegistry
 */
public interface Attribute<T> {
    /** Name of attribute. */
    String getName();

    /** Optional display name of attribute. May return {@code null}. */
    String getDisplayName();

    /** Returns {@code true} if this attribute may not be updated and {@code false} otherwise. */
    boolean isReadOnly();

    /**
     * Returns {@code true} if this attribute stored as Property in Virtual File System and {@code false} if this attribute has a
     * calculated
     * nature.
     *
     * @see com.codenvy.api.vfs.shared.Property
     */
    boolean isPersistent();

    /**
     * Report is this attribute updated through method {@link #setValue(Object)} or not. Implementation SHOULD track all updates of value
     * of
     * this attribute and report about updates with this method.
     */
    boolean isUpdated();

    /**
     * Get value of attribute.
     *
     * @return current value of attribute
     */
    T getValue();

    /**
     * Get value of attribute.
     *
     * @param value
     *         new value of attribute
     * @see #isUpdated()
     */
    void setValue(T value);
}
