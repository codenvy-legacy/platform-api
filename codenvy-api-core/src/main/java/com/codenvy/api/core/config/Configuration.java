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
package com.codenvy.api.core.config;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Container for configuration parameters.
 *
 * @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a>
 */
public class Configuration {

    private final Properties properties;

    /** An empty configuration. */
    public Configuration() {
        this(new Properties());
    }

    /** A new configuration based of specified properties. */
    public Configuration(Properties properties) {
        this.properties = properties;
    }

    /**
     * Copy constructor.
     *
     * @param other
     *         other configuration from which to copy settings
     */
    public Configuration(Configuration other) {
        this.properties = other == null ? new Properties() : (Properties)other.properties.clone();
    }

    public enum MergePolicy {
        RETAIN, OVERRIDE
    }

    public void merge(Configuration other, MergePolicy policy) {
        if (policy == MergePolicy.OVERRIDE) {
            properties.putAll(other.getProperties());
        } else {
            synchronized (properties) {
                for (Map.Entry<Object, Object> entry : other.properties.entrySet()) {
                    if (!properties.containsKey(entry.getKey())) {
                        properties.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
    }

    /**
     * Get underlying properties. Useful when user need to store properties in file.
     * <pre>
     *     Configuration cfg = ...
     *     // add parameters
     *     Writer w = ...
     *     cfg.getProperties().store(w, "my configuration");
     *     ...
     * </pre>
     *
     * @return properties
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * Get the value of the {@code name} property, {@code null} if there is no such property.
     *
     * @param name
     *         the property name
     * @return the value of the {@code name} property, or {@code null} if there is no such property
     */
    public String get(String name) {
        return properties.getProperty(name);
    }

    /**
     * Get the value of the {@code name} property. If there is no such property, then {@code defaultValue} is returned.
     *
     * @param name
     *         property name
     * @param defaultValue
     *         default value
     * @return property value, or {@code defaultValue} there is no such property
     */
    public String get(String name, String defaultValue) {
        return properties.getProperty(name, defaultValue);
    }

    /**
     * Set the {@code value} of the {@code name} property. Removes property from configuration if {@code value} is {@code null}.
     *
     * @param name
     *         property name
     * @param value
     *         property value
     * @throws IllegalArgumentException
     *         if {code name} is {@code null}
     */
    public void set(String name, String value) {
        if (name == null) {
            throw new IllegalArgumentException("The name of property may not be null. ");
        }
        if (value == null) {
            properties.remove(name);
        } else {
            properties.setProperty(name, value);
        }
    }

    /**
     * Set property if it is not set.
     *
     * @param name
     *         the property name
     * @param value
     *         the new value
     */
    public void setIfNotSet(String name, String value) {
        synchronized (properties) {
            if (get(name) == null) {
                set(name, value);
            }
        }
    }

    /**
     * Get the value of the {@code name} property as {@code boolean}. If there is no such property then {@code defaultValue} is returned.
     * If property is set then use {@link Boolean#parseBoolean(String)} to getting {@code boolean} value.
     *
     * @param name
     *         property name
     * @param defaultValue
     *         default value
     * @return property value as {@code double}, or {@code defaultValue}
     */
    public boolean getBoolean(String name, boolean defaultValue) {
        final String str = get(name);
        return str == null ? defaultValue : Boolean.parseBoolean(str);
    }

    /**
     * Set the value of the {@code name} property to {@code boolean}.
     *
     * @param name
     *         property name
     * @param value
     *         {@code boolean} value of the property
     */
    public void setBoolean(String name, boolean value) {
        set(name, String.valueOf(value));
    }

    /**
     * Get the value of the {@code name} property as {@code int}. If there is no such property, or it cannot be converted to {@code int},
     * then {@code defaultValue} is returned.
     *
     * @param name
     *         property name
     * @param defaultValue
     *         default value
     * @return property value as {@code int}, or {@code defaultValue}
     */
    public int getInt(String name, int defaultValue) {
        final String str = get(name);
        if (str == null) {
            return defaultValue;
        }
        return Integer.parseInt(str);
    }

    /**
     * Set the value of the {@code name} property to {code int}.
     *
     * @param name
     *         property name
     * @param value
     *         {@code int} value of the property
     */
    public void setInt(String name, int value) {
        set(name, String.valueOf(value));
    }


    /**
     * Get the value of the {@code name} property as {@code long}. If there is no such property, or it cannot be converted to {@code
     * long}, then {@code defaultValue} is returned.
     *
     * @param name
     *         property name
     * @param defaultValue
     *         default value
     * @return property value as {@code int}, or {@code long}
     */
    public long getLong(String name, long defaultValue) {
        final String str = get(name);
        if (str == null) {
            return defaultValue;
        }
        return Long.parseLong(str);
    }

    /**
     * Set the value of the {@code name} property to {@code long}.
     *
     * @param name
     *         property name
     * @param value
     *         {@code long} value of the property
     */
    public void setLong(String name, long value) {
        set(name, String.valueOf(value));
    }

    /**
     * Get the value of the {@code name} property as {@code float}. If there is no such property, or it cannot be converted to {@code
     * float}, then {@code defaultValue} is returned.
     *
     * @param name
     *         property name
     * @param defaultValue
     *         default value
     * @return property value as {@code float}, or {@code defaultValue}
     */
    public float getFloat(String name, float defaultValue) {
        final String str = get(name);
        if (str == null) {
            return defaultValue;
        }
        return Float.parseFloat(str);
    }

    /**
     * Set the value of the {@code name} property to {@code float}.
     *
     * @param name
     *         property name
     * @param value
     *         property value
     */
    public void setFloat(String name, float value) {
        set(name, String.valueOf(value));
    }

    /**
     * Get the value of the {@code name} property as {@code double}. If there is no such property, or it cannot be converted to {@code
     * double}, then {@code defaultValue} is returned.
     *
     * @param name
     *         property name
     * @param defaultValue
     *         default value
     * @return property value as {@code double}, or {@code defaultValue}
     */
    public double getDouble(String name, double defaultValue) {
        final String str = get(name);
        if (str == null) {
            return defaultValue;
        }
        return Double.parseDouble(str);
    }

    /**
     * Set the value of the {@code name} property to {@code double}.
     *
     * @param name
     *         property name
     * @param value
     *         property value
     */
    public void setDouble(String name, double value) {
        set(name, String.valueOf(value));
    }

    /**
     * Get the comma delimited values of the {@code name} property as {code List} of {@code String}. If no such property is specified then
     * empty {@code List} is returned.
     * <p/>
     * Note: Trims leading and trailing whitespace on each value.
     *
     * @param name
     *         property name
     * @return property value as {@code List} of {@code String}
     */
    public List<String> getStrings(String name) {
        final String str = get(name);
        return str == null ? Collections.<String>emptyList() : split(str, ',');
    }

    /**
     * Set the {@code List} of {@code String} values for the {@code name} property as comma delimited values.
     *
     * @param name
     *         property name
     * @param values
     *         {@code List} of {@code String} value of the property
     */
    public void setStrings(String name, List<String> values) {
        if (values == null) {
            set(name, null);
        } else {
            set(name, join(values, ','));
        }
    }

    private List<String> split(String raw, char ch) {
        final List<String> list = new ArrayList<>();
        int n = 0;
        int p;
        while ((p = raw.indexOf(ch, n)) != -1) {
            list.add(raw.substring(n, p).trim());
            n = p + 1;
        }
        list.add(raw.substring(n).trim());
        return list;
    }

    private String join(List<String> list, char ch) {
        final StringBuilder buff = new StringBuilder();
        for (String str : list) {
            if (buff.length() > 0) {
                buff.append(ch);
            }
            buff.append(str);
        }
        return buff.toString();
    }

    /**
     * Get the value of the {@code name} property as {@code java.io.File}. If there is no such property then {@code defaultValue} is
     * returned. If property is set it must follow requirements specified for {@link java.io.File#File(java.net.URI)}.
     *
     * @param name
     *         property name
     * @param defaultValue
     *         default value
     * @return property value as {@code java.io.File}, or {@code defaultValue}
     * @throws IllegalArgumentException
     *         if property value is invalid
     * @see java.io.File#File(java.net.URI)
     */
    public java.io.File getFile(String name, java.io.File defaultValue) {
        final String str = get(name);
        if (str == null) {
            return defaultValue;
        }
        return new java.io.File(URI.create(str));
    }

    /**
     * Set the value of the {@code name} property as {@code java.io.File}.
     *
     * @param name
     *         property name
     * @param value
     *         {@code java.io.File} value of the property
     * @see #getFile(String, java.io.File)
     */
    public void setFile(String name, java.io.File value) {
        if (value == null) {
            set(name, null);
        } else {
            set(name, value.getAbsoluteFile().toURI().toString());
        }
    }

    /**
     * Get names of all properties. Modifications to the returned {@code Set} will not affect the internal {@code Set}.
     *
     * @return names of properties
     */
    public Set<String> getNames() {
        return properties.stringPropertyNames();
    }

    /**
     * Returns the number of properties in the configuration.
     *
     * @return number of properties in the configuration
     */
    public int size() {
        return properties.size();
    }

    /** Clears configuration. */
    public void clear() {
        properties.clear();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append('\n');
        for (String name : getNames()) {
            sb.append(name);
            sb.append('=');
            sb.append(get(name));
            sb.append('\n');
        }
        sb.append('}');
        sb.append('\n');
        return sb.toString();
    }
}