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
package com.codenvy.api.project.shared;

/**
 * Describes Attribute that may be defined for project type.
 *
 * @author andrew00x
 */
public class AttributeDescription {
    private final String name;

    /**
     * Creates new AttributeDescription with specified <code>name</code>.
     *
     * @throws IllegalArgumentException
     *         If {@code name} is {@code null} or empty
     */
    public AttributeDescription(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Null name is not allowed. ");
        }
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Name may not be empty. ");
        }
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
