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
package com.codenvy.api.project.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Default implementation of ValueProvider.
 *
 * @author andrew00x
 */
public class DefaultValueProvider implements ValueProvider {
    private List<String> values;

    public DefaultValueProvider(List<String> values) {
        if (!(values == null || values.isEmpty())) {
            this.values = new ArrayList<>(values);
        }
    }

    public DefaultValueProvider(String... values) {
        if (values != null) {
            this.values = new ArrayList<>(values.length);
            Collections.addAll(this.values, values);
        }
    }

    public DefaultValueProvider(String value) {
        if (value != null) {
            this.values = new ArrayList<>(1);
            this.values.add(value);
        }
    }

    public DefaultValueProvider() {
    }

    @Override
    public List<String> getValues() {
        if (values == null) {
            values = new ArrayList<>(2);
        }
        return values;
    }

    @Override
    public void setValues(List<String> values) {
        if (this.values == null) {
            if (values != null) {
                this.values = new ArrayList<>(values);
            }
        } else {
            this.values.clear();
            if (!(values == null || values.isEmpty())) {
                this.values.addAll(values);
            }
        }
    }
}
