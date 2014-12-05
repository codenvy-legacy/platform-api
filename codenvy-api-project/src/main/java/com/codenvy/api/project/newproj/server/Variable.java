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
import com.codenvy.api.project.server.*;


/**
 * @author gazarenkov
 */
public class Variable extends AbstractAttribute {

    protected ValueProviderFactory valueProviderFactory = null;
    protected AttributeValue value = null;


    public Variable(String projectType, String name, String description, boolean required,
                    ValueProviderFactory valueProviderFactory) {
        this(projectType, name, description, required);
        this.valueProviderFactory = valueProviderFactory;
    }


    public Variable(String projectType, String name, String description, boolean required, AttributeValue value) {
        this(projectType, name, description, required);
        this.value = value;
    }

    public Variable(String projectType, String name, String description, boolean required) {
        super(projectType, name, description, required, true);
    }

    @Override
    public AttributeValue getValue() throws ValueStorageException {
        return value;
    }


    public final AttributeValue getValue(Project project) throws ValueStorageException {
        if(valueProviderFactory != null) {
            System.out.println("setValue >> "+getName()+" "+value.getList());
            return new AttributeValue(valueProviderFactory.newInstance(getId(), project).getValues());
        } else {
            return value;
        }
        //return (valueProvider != null)?new AttributeValue(valueProvider.getValues()):value;
    }

    public final void setValue(AttributeValue value, Project project) throws InvalidValueException, ValueStorageException {
        if(valueProviderFactory != null) {
            System.out.println("setValue >> "+getName()+" "+value.getList());
            this.valueProviderFactory.newInstance(getId(), project).setValues(value.getList());
        } else
            this.value = value;
    }

    public final ValueProviderFactory getValueProviderFactory() {
        return valueProviderFactory;
    }



}
