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

/**
 * @author gazarenkov
 */
public class Variable extends AbstractAttribute {

    protected ValueProvider valueProvider;


    public Variable(String projectType, String name, String description, boolean required, ValueProvider valueProvider) {
        super(projectType, name, description, required, true);
        this.valueProvider = valueProvider;
    }

    @Override
    public final AttributeValue getValue() {
        return valueProvider.getValue();
    }

    public final void setValue(AttributeValue value) {
        valueProvider.setValue(value);
    }


}
