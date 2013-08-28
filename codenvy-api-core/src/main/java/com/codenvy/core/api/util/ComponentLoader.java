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
package com.codenvy.core.api.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Lookup instances of components by its {@code Class}. This is for internal usage. Regular users are not expected to use this class
 * directly or indirectly.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
public abstract class ComponentLoader {
    private static volatile ComponentLoader loader;

    public static void setInstance(ComponentLoader myLoader) {
        synchronized (ComponentLoader.class) {
            loader = myLoader;
        }
    }

    /**
     * Loads all available implementations of {@code componentClass}. This method is the same to:
     * <pre>
     * Class&lt;T&gt; myClass = ...
     * ComponentLoader.all(myClass, Thread.currentThread().getContextClassLoader());
     * </pre>
     *
     * @param componentClass
     *         component class
     * @param <T>
     *         type of component
     * @return set of all available implementations
     * @see #all(Class, ClassLoader)
     */
    public static <T> Collection<T> all(Class<T> componentClass) {
        return all(componentClass, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Loads all available implementations of {@code componentClass}.
     *
     * @param componentClass
     *         component class
     * @param classLoader
     *         ClassLoader
     * @param <T>
     *         type of component
     * @return set of all available implementations
     */
    public static <T> Collection<T> all(Class<T> componentClass, ClassLoader classLoader) {
        return getInstance().findComponents(componentClass, classLoader);
    }

    /**
     * Loads single implementation of {@code componentClass}. Throws {@code IllegalStateException} if there is no any implementation or
     * more then one implementation found. This method is the same to:
     * <pre>
     * Class&lt;T&gt; myClass = ...
     * ComponentLoader.one(myClass, Thread.currentThread().getContextClassLoader());
     * </pre>
     *
     * @param componentClass
     *         component class
     * @param <T>
     *         type of component
     * @return set of all available implementations
     * @throws IllegalStateException
     *         if there is no any implementation or more then one implementation found.
     * @see #one(Class, ClassLoader)
     */
    public static <T> T one(Class<T> componentClass) {
        return one(componentClass, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Loads single implementation of {@code componentClass}. Throws {@code IllegalStateException} if there is no any implementation or
     * more
     * then one implementation found.
     *
     * @param componentClass
     *         component class
     * @param classLoader
     *         ClassLoader
     * @param <T>
     *         type of component
     * @return set of all available implementations
     * @throws IllegalStateException
     *         if there is no any implementation or more then one implementation found.
     */
    public static <T> T one(Class<T> componentClass, ClassLoader classLoader) {
        final Collection<T> components = getInstance().findComponents(componentClass, classLoader);
        if (components.isEmpty()) {
            throw new IllegalStateException(String.format("No implementation found for %s", componentClass));
        }
        if (components.size() > 1) {
            throw new IllegalStateException(
                    String.format("More then one implementation found for %s, but only one must be", componentClass));
        }
        return components.iterator().next();
    }

    private static ComponentLoader getInstance() {
        ComponentLoader myLoader = loader;
        if (myLoader == null) {
            synchronized (ComponentLoader.class) {
                myLoader = loader;
                if (myLoader == null) {
                    loader = myLoader = new DefaultComponentLoader();
                }
            }
        }
        return myLoader;
    }

    protected ComponentLoader() {
    }

    protected abstract <T> Collection<T> findComponents(Class<T> componentClass, ClassLoader classLoader);


    /** Default implementation of {@code ComponentLoader}. It is based on {@link ServiceLoader}. */
    private static class DefaultComponentLoader extends ComponentLoader {
        @Override
        protected <T> Collection<T> findComponents(Class<T> componentClass, ClassLoader classLoader) {
            final Iterator<T> components = ServiceLoader.load(componentClass, classLoader).iterator();
            if (components.hasNext()) {
                Set<T> result = new LinkedHashSet<>();
                while (components.hasNext()) {
                    result.add(components.next());
                }
                return result;
            }
            return Collections.emptySet();
        }
    }
}
