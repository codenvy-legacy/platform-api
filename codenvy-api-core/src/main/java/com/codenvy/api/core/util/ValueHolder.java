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
package com.codenvy.api.core.util;

/**
 * Holder for a value of type <code>T</code>.
 *
 * @author andrew00x
 */
public final class ValueHolder<T> {
    private T value;

    public ValueHolder(T value) {
        this.value = value;
    }

    public ValueHolder() {
    }

    public synchronized T get() {
        return value;
    }

    public synchronized void set(T value) {
        this.value = value;
    }
}
