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
package com.codenvy.core.api.user;

import com.codenvy.core.api.concurrent.ThreadLocalPropagateContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * User state holder.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
public class UserState {
    private static final ThreadLocal<UserState> userStateHolder = new ThreadLocal<>();

    static {
        ThreadLocalPropagateContext.addThreadLocal(userStateHolder);
    }

    /** Get state of current user. */
    public static UserState get() {
        return userStateHolder.get();
    }

    /** Set state for current user. Typically should be called at the start of user request. */
    public static void set(UserState state) {
        userStateHolder.set(state);
    }

    /** Reset state for current user. Typically should be called at the end of user request. */
    public static void reset() {
        userStateHolder.remove();
    }

    private final User                user;
    private final Map<String, Object> attributes;

    public UserState(User user) {
        this.user = user;
        attributes = new HashMap<>();
    }

    public User getUser() {
        return user;
    }

    /**
     * Set runtime attribute.
     *
     * @param name
     *         name of attribute
     * @param value
     *         value of attribute
     * @return previous value of attribute {@code name} or {@code null} it attribute was not set
     */
    public Object setAttribute(String name, Object value) {
        return attributes.put(name, value);
    }

    /**
     * Get attribute.
     *
     * @param name
     *         name of attribute
     * @return value of attribute {@code name} or {@code null} it attribute is not set
     */
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    /** Get names of runtime attributes related to current user. Modification of returned {@code Set} must not effect internal {@code Set}. */
    public Set<String> getAttributeNames() {
        return new HashSet<>(attributes.keySet());
    }

    /**
     * Remove attribute.
     *
     * @param name
     *         name of attribute
     * @return previous value of attribute {@code name} or {@code null} it attribute was not set
     */
    public Object removeAttribute(String name) {
        return attributes.remove(name);
    }
}
