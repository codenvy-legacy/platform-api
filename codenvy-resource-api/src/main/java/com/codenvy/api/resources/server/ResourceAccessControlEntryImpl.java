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
package com.codenvy.api.resources.server;

import com.codenvy.api.resources.shared.ResourceAccessControlEntry;
import com.codenvy.api.vfs.shared.AccessControlEntry;
import com.codenvy.api.vfs.shared.Principal;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public class ResourceAccessControlEntryImpl implements ResourceAccessControlEntry {
    private final Principal             principal;
    private       ObservableSet<String> permissions;
    private       boolean               updated;

    public ResourceAccessControlEntryImpl(AccessControlEntry entry) {
        this(entry.getPrincipal(), entry.getPermissions());
    }

    public ResourceAccessControlEntryImpl(Principal principal, Set<String> permissions) {
        this.principal = principal;
        this.permissions = new ObservableSet<>(new HashSet<>(permissions), this);
    }

    public ResourceAccessControlEntryImpl(Principal principal) {
        this.principal = principal;
    }

    @Override
    public Principal getPrincipal() {
        return principal;
    }

    @Override
    public Collection<String> getPermissions() {
        if (permissions == null) {
            permissions = new ObservableSet<>(new HashSet<String>(4), this);
        }
        return permissions;
    }

    @Override
    public void setPermissions(Collection<String> permissions) {
        if (permissions == null) {
            this.permissions = null;
        } else {
            this.permissions = new ObservableSet<>(new HashSet<>(permissions), this);
        }
        updated |= true;
    }

    @Override
    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }

    @Override
    public boolean isUpdated() {
        return updated;
    }

    private static class ObservableSet<T> implements Set<T> {
        final Set<T>                         set;
        final ResourceAccessControlEntryImpl ace;

        ObservableSet(Set<T> set, ResourceAccessControlEntryImpl ace) {
            this.set = set;
            this.ace = ace;
        }

        @Override
        public int size() {
            return set.size();
        }

        @Override
        public boolean isEmpty() {
            return set.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return set.contains(o);
        }

        @Override
        public Iterator<T> iterator() {
            return new Iterator<T>() {

                private final Iterator<T> it = set.iterator();

                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public T next() {
                    return it.next();
                }

                @Override
                public void remove() {
                    it.remove();
                    ace.updated |= true;
                }
            };
        }

        @Override
        public Object[] toArray() {
            return set.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return set.toArray(a);
        }

        @Override
        public boolean add(T o) {
            boolean res = set.add(o);
            ace.updated |= res;
            return res;
        }

        @Override
        public boolean remove(Object o) {
            boolean res = set.remove(o);
            ace.updated |= res;
            return res;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return set.containsAll(c);
        }

        @Override
        public boolean addAll(Collection<? extends T> c) {
            boolean res = set.addAll(c);
            ace.updated |= res;
            return res;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            boolean res = set.retainAll(c);
            ace.updated |= res;
            return res;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            boolean res = set.removeAll(c);
            ace.updated |= res;
            return res;
        }

        @Override
        public void clear() {
            set.clear();
            ace.updated |= true;
        }

        @Override
        public String toString() {
            return set.toString();
        }

        @Override
        public boolean equals(Object obj) {
            return set.equals(obj);
        }

        @Override
        public int hashCode() {
            return set.hashCode();
        }
    }
}
