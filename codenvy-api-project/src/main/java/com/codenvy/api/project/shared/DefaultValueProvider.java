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
