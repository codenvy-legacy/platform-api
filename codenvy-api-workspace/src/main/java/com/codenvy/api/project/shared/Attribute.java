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
import java.util.List;

/**
 * Attribute of Project.
 * NOTE that this class is not thread-safe. If multiple threads access an this instance concurrently, and at least one of the threads
 * modifies this instance, then access to this instance must be synchronized with some external mechanism.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @see ValueProvider
 */
public class Attribute {
    private static class DefaultValueProvider implements ValueProvider {
        List<String> values;

        DefaultValueProvider(List<String> values) {
            if (!(values == null || values.isEmpty())) {
                this.values = new ArrayList<>(values);
            }
        }

        DefaultValueProvider(String value) {
            if (value != null) {
                this.values = new ArrayList<>(1);
                this.values.add(value);
            }
        }

        public List<String> getValues() {
            if (values == null) {
                values = new ArrayList<>(2);
            }
            return values;
        }

        public void setValues(List<String> values) {
            if (this.values == null) {
                if (values != null) {
                    this.values = values;
                }
            } else {
                this.values.clear();
                if (!(values == null || values.isEmpty())) {
                    this.values.addAll(values);
                }
            }
        }
    }

    private final ValueProvider valueProvider;
    private final String        name;

    /**
     * Creates new Attribute with specified <code>name</code> and use specified <code>ValueProvider</code> to reading and updating
     * value of this Attribute.
     *
     * @throws IllegalArgumentException
     *         If {@code name} is {@code null} or empty
     */
    public Attribute(String name, ValueProvider valueProvider) {
        if (name == null) {
            throw new IllegalArgumentException("Null name is not allowed. ");
        }
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Name may not be empty. ");
        }
        this.name = name;
        this.valueProvider = valueProvider;
    }

    /** Creates new Attribute. */
    public Attribute(String name, String value) {
        this(name, new DefaultValueProvider(value));
    }

    /** Creates new Attribute. */
    public Attribute(String name, List<String> values) {
        this(name, new DefaultValueProvider(values));
    }

    /** Get name of this attribute. */
    public String getName() {
        return name;
    }

    /**
     * Get single value of attribute. If attribute has more than one value this method returns only first value.
     * <p/>
     * This method is shortcut for:
     * <pre>
     *    String name = ...
     *    Attribute attribute = ...;
     *    List&lt;String&gt; values = attribute.getValues();
     *    if (values != null && !values.isEmpty())
     *       return values.get(0);
     *    else
     *       return null;
     * </pre>
     *
     * @return current value of attribute
     */
    public final String getValue() {
        final List<String> values = getValues();
        return !(values == null || values.isEmpty()) ? values.get(0) : null;
    }

    /**
     * Get all values of attribute. Modifications to the returned {@code List} will not affect the internal {@code List}.
     *
     * @return current value of attribute
     */
    public final List<String> getValues() {
        return valueProvider.getValues();
    }

    /**
     * Set single value of attribute.
     *
     * @param value
     *         new value of attribute
     */
    public final void setValue(String value) {
        if (value != null) {
            final List<String> list = new ArrayList<>(1);
            list.add(value);
            valueProvider.setValues(list);
        } else {
            valueProvider.setValues(null);
        }
    }

    /**
     * Set values of attribute.
     *
     * @param values
     *         new value of attribute
     */
    public final void setValues(List<String> values) {
        if (values != null) {
            valueProvider.setValues(new ArrayList<>(values));
        } else {
            valueProvider.setValues(null);
        }
    }
}
