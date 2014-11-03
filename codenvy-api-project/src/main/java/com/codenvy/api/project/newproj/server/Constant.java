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
package com.codenvy.api.project.newproj.server;

import com.codenvy.api.project.newproj.AbstractAttribute;
import com.codenvy.api.project.newproj.Attribute;
import com.codenvy.api.project.newproj.AttributeValue;
import com.codenvy.api.project.newproj.ValueType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author gazarenkov
 */

public final class Constant extends AbstractAttribute {


    private final AttributeValue value;

    public Constant(String projectType, String name, String description, AttributeValue value) {
        super(projectType, name, description, true, false);
        this.value = value;
    }

    public Constant(String projectType, String name, String description, final String str) {

        super(projectType, name, description, true, false);

        this.value = new AttributeValue() {

            List <String> values = new ArrayList<>();

            @Override
            public String getString() {
                return str;
            }

            @Override
            public void setString() {

            }

            @Override
            public List<String> getList() {
                values.add(str);
                return values;
            }

            @Override
            public void setList(List<String> list) {

            }

            @Override
            public ValueType getType() {
                return null;
            }
        };
    }

    @Override
    public AttributeValue getValue() {
        return value;
    }

}
