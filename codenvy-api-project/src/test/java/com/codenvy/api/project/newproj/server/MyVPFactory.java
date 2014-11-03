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

import com.codenvy.api.project.newproj.AttributeValue;
import com.codenvy.api.project.newproj.ValueType;
import com.codenvy.api.project.server.Project;


import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

/**
 * @author gazarenkov
 */
@Singleton
public class MyVPFactory implements ValueProviderFactory {


    @Override
    public ValueProvider newInstance(Project project) {
        return new MyValueProvider();

    }


    public static class MyValueProvider implements ValueProvider {
        @Override
        public AttributeValue getValue() {
            return new AttributeValue() {

                List <String> values = new ArrayList<>();


                @Override
                public String getString() {
                    return "gena";
                }

                @Override
                public void setString() {

                }

                @Override
                public List<String> getList() {
                    values.add("gena");
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
        public void setValue(AttributeValue value) {

        }
    }
}
