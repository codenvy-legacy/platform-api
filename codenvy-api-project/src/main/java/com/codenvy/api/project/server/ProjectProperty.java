/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2014] Codenvy, S.A. 
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
package com.codenvy.api.project.server;

import java.util.List;

/**
 * @author andrew00x
 */
public class ProjectProperty {
    private String       name;
    private List<String> value;

    public ProjectProperty() {
    }

    public ProjectProperty(String name, List<String> value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ProjectProperty withName(String name) {
        this.name = name;
        return this;
    }

    public List<String> getValue() {
        return value;
    }

    public void setValue(List<String> value) {
        this.value = value;
    }

    public ProjectProperty withValue(List<String> value) {
        this.value = value;
        return this;
    }

    @Override
    public String toString() {
        return "ProjectProperty{" +
               "name='" + name + '\'' +
               ", value=" + value +
               '}';
    }
}
