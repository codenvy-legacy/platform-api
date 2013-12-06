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
import java.util.LinkedList;
import java.util.List;

/**
 * Attribute of Project. Attribute may be hierarchical; one Attribute may contain other set of child attributes.
 * NOTE that this class is not thread-safe. If multiple threads access an this instance concurrently, and at least one of the threads
 * modifies this instance, then access to this instance must be synchronized with some external mechanism.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @see AttributeValueProvider
 */
public class Attribute {
    private static class DefaultValueProvider implements AttributeValueProvider {
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

    private final AttributeValueProvider valueProvider;
    private final String                 name;

    private List<Attribute> children;
    private Attribute       parent;

    /**
     * Creates new Attribute with specified <code>name</code> and use specified <code>AttributeValueProvider</code> to reading and updating
     * value of this Attribute.
     *
     * @throws IllegalArgumentException
     *         If {@code name} is {@code null} or empty or if it contains '.' character. Character '.' is used as delimiter of attribute's
     *         hierarchy so this character may not be used in name of attribute.
     */
    public Attribute(String name, AttributeValueProvider valueProvider) {
        if (name == null) {
            throw new IllegalArgumentException("Null name is not allowed. ");
        }
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Name may not be empty. ");
        }
        if (name.indexOf('.') >= 0) {
            throw new IllegalArgumentException("Invalid name '" + name + "', character '.' is not allowed in name of attribute");
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

    /** Get simple (not hierarchical) name of this attribute. */
    public String getName() {
        return name;
    }

    /** Get full name of this attribute. Full name includes name of parent attributes, '.' is used as separator. */
    public String getFullName() {
        Attribute parent = this.parent;
        if (parent == null) {
            return name;
        }
        String name = this.name;
        LinkedList<String> elements = new LinkedList<>();
        elements.add(name);

        while (parent != null) {
            elements.addFirst(parent.name);
            parent = parent.parent;
        }

        StringBuilder sb = new StringBuilder();
        for (String el : elements) {
            if (sb.length() > 0) {
                sb.append('.');
            }
            sb.append(el);
        }
        return sb.toString();
    }

    public Attribute getParent() {
        return parent;
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

    /**
     * Get child Attribute by {@code path}. Path may be hierarchical and use '.' as separator.
     * <p/>
     * For example if path of attribute is 'a.b.c' then first check is current attribute has child attribute with name 'a'. Then check
     * attribute 'a' has child attribute with name 'b' and so on. Return {@code null} is attribute doesn't exist.
     */
    public Attribute getChild(String path) {
        if (path == null || children == null) {
            return null;
        }
        if (path.indexOf('.') < 0) {
            for (Attribute child : children) {
                if (path.equals(child.name)) {
                    return child;
                }
            }
        } else {
            final String[] elements = path.split("\\.");
            List<Attribute> level = children;
            Attribute next = null;
            for (int i = 0, k = elements.length - 1; i < elements.length && level != null; i++) {
                for (int j = 0, size = level.size(); j < size && next == null; j++) {
                    Attribute child = level.get(j);
                    if (elements[i].equals(child.name)) {
                        next = child;
                    }
                }
                if (next == null) {
                    return null;
                } else if (i == k) {
                    return next;
                } else {
                    level = next.children;
                    next = null;
                }
            }
        }
        return null;
    }

    public boolean hasChild(String path) {
        return getChild(path) != null;
    }

    /**
     * Get list of child Attribute. In case if this Attribute hasn't child this method returns empty {@code List} never {@code null}.
     * Modifications to the returned {@code List} will not affect the internal {@code List}.
     */
    public List<Attribute> getChildren() {
        return children == null ? new ArrayList<Attribute>(4) : new ArrayList<>(children);
    }

    /** Tests whether this Attribute has child Attributes. */
    public boolean hasChildren() {
        return !(children == null || children.isEmpty());
    }

    /**
     * Add child Attribute to this Attribute.
     *
     * @param child
     *         Attribute to add
     * @throws IllegalArgumentException
     *         is thrown if one of the following conditions are met:
     *         <ul>
     *         <li>If specified {@code child} is {code null}</li>
     *         <li>If attribute with the same name already exists</li>
     *         </ul>
     * @see #hasChild(String)
     */
    public void addChild(Attribute child) {
        if (child == null) {
            throw new IllegalArgumentException("Null attribute is not allowed. ");
        }
        if (children == null) {
            children = new ArrayList<>(4);
            children.add(child);
        } else {
            for (Attribute attr : children) {
                if (child.name.equals(attr.name)) {
                    throw new IllegalArgumentException("Attribute " + child.name + " already exists. ");
                }
            }
            children.add(child);
        }
        child.parent = this;
    }

    public Attribute removeChild(String path) {
        final Attribute child = getChild(path);
        if (child == null) {
            return null;
        }
        child.parent.children.remove(child);
        child.parent = null;
        return child;
    }
}
