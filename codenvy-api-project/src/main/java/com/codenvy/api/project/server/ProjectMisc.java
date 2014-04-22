/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2014] Codenvy, S.A. 
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
package com.codenvy.api.project.server;

import java.util.Properties;
import java.util.Set;

/**
 * @author andrew00x
 */
class ProjectMisc {
    static final String UPDATED = "updated";
    static final String CREATED = "created";

    private final InternalMisc data;

    ProjectMisc(Properties properties) {
        this.data = new InternalMisc(properties);
    }

    ProjectMisc() {
        this.data = new InternalMisc();
    }

    long getModificationDate() {
        return data.getLong(UPDATED, -1L);
    }

    long getCreationDate() {
        return data.getLong(CREATED, -1L);
    }

    void setModificationDate(long date) {
        data.setLong(UPDATED, date);
    }

    void setCreationDate(long date) {
        data.setLong(CREATED, date);
    }

    boolean isUpdated() {
        return data.isUpdated();
    }

    Properties asProperties() {
        return data.properties;
    }

    private static class InternalMisc {
        final Properties properties;
        boolean updated;

        boolean isUpdated() {
            synchronized (properties) {
                return updated;
            }
        }

        InternalMisc() {
            this(new Properties());
        }

        InternalMisc(Properties properties) {
            this.properties = properties;
        }

        String get(String name) {
            return properties.getProperty(name);
        }

        void set(String name, String value) {
            if (name == null) {
                throw new IllegalArgumentException("The name of property may not be null. ");
            }
            if (value == null) {
                properties.remove(name);
            } else {
                properties.setProperty(name, value);
            }
            synchronized (properties) {
                updated = true;
            }
        }

        boolean getBoolean(String name) {
            return getBoolean(name, false);
        }

        boolean getBoolean(String name, boolean defaultValue) {
            final String str = get(name);
            return str == null ? defaultValue : Boolean.parseBoolean(str);
        }

        void setBoolean(String name, boolean value) {
            set(name, String.valueOf(value));
        }

        int getInt(String name) {
            return getInt(name, 0);
        }

        int getInt(String name, int defaultValue) {
            final String str = get(name);
            if (str == null)
                return defaultValue;
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        void setInt(String name, int value) {
            set(name, String.valueOf(value));
        }

        long getLong(String name) {
            return getLong(name, 0L);
        }

        long getLong(String name, long defaultValue) {
            final String str = get(name);
            if (str == null)
                return defaultValue;
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        void setLong(String name, long value) {
            set(name, String.valueOf(value));
        }

        float getFloat(String name) {
            return getFloat(name, 0.0F);
        }

        float getFloat(String name, float defaultValue) {
            final String str = get(name);
            if (str == null)
                return defaultValue;
            try {
                return Float.parseFloat(str);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        void setFloat(String name, float value) {
            set(name, String.valueOf(value));
        }


        double getDouble(String name) {
            return getDouble(name, 0.0);
        }

        double getDouble(String name, double defaultValue) {
            final String str = get(name);
            if (str == null)
                return defaultValue;
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        void setDouble(String name, double value) {
            set(name, String.valueOf(value));
        }

        Set<String> getNames() {
            return properties.stringPropertyNames();
        }
    }
}
