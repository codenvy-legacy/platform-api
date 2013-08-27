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
package com.codenvy.api.tools;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public final class Pair<A, B> {

    public static <K, V> Pair<K, V> of(K k, V v) {
        return new Pair<>(k, v);
    }

    public final A first;
    public final B second;

    private final int hashCode;

    public Pair(A first, B second) {
        this.first = first;
        this.second = second;
        int hashCode = 7;
        hashCode = hashCode * 31 + (first == null ? 0 : first.hashCode());
        hashCode = hashCode * 31 + (second == null ? 0 : second.hashCode());
        this.hashCode = hashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Pair<?, ?>)) {
            return false;
        }
        final Pair other = (Pair)o;
        return (first == null ? other.first == null : first.equals(other.first)) &&
               (second == null ? other.second == null : second.equals(other.second));
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return "{first=" + first + ", second=" + second + '}';
    }
}