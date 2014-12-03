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
import com.codenvy.api.project.newproj.AttributeValue;
import com.codenvy.api.project.server.InvalidValueException;
import com.codenvy.api.project.server.ValueProvider;
import com.codenvy.api.project.server.ValueStorageException;


/**
 * @author gazarenkov
 */
public class Variable extends AbstractAttribute {

    protected ValueProvider valueProvider = null;
    protected AttributeValue value = null;


    public Variable(String projectType, String name, String description, boolean required, ValueProvider valueProvider) {
        this(projectType, name, description, required);
        this.valueProvider = valueProvider;
    }

    public Variable(String projectType, String name, String description, boolean required, AttributeValue value) {
        this(projectType, name, description, required);
        this.value = value;
    }

    public Variable(String projectType, String name, String description, boolean required) {
        super(projectType, name, description, required, true);
    }

    @Override
    public final AttributeValue getValue() throws ValueStorageException {
        return (valueProvider != null)?new AttributeValue(valueProvider.getValues()):value;
    }

    public final void setValue(AttributeValue value) throws InvalidValueException, ValueStorageException {
        if(valueProvider != null)
            this.valueProvider.setValues(value.getList());
        else
            this.value = value;
    }

}
