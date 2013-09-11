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
package com.codenvy.api.vfs.server.util;

import com.codenvy.api.vfs.server.VirtualFile;
import com.codenvy.api.vfs.server.VirtualFileFilter;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;

/**
 * Provides factory method to create AND, OR filters based on set of VirtualFileFilter.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
public class VirtualFileFilters {

    public static VirtualFileFilter createAndFilter(VirtualFileFilter... filters) {
        if (filters == null || filters.length < 2) {
            throw new IllegalArgumentException("At least two filters required. ");
        }
        VirtualFileFilter[] copy = new VirtualFileFilter[filters.length];
        System.arraycopy(filters, 0, copy, 0, filters.length);
        return new AndChangeEventFilter(copy);
    }

    private static class AndChangeEventFilter implements VirtualFileFilter {
        final VirtualFileFilter[] filters;

        AndChangeEventFilter(VirtualFileFilter[] filters) {
            this.filters = filters;
        }

        @Override
        public boolean accept(VirtualFile file) throws VirtualFileSystemException {
            for (VirtualFileFilter filter : filters) {
                if (!filter.accept(file)) {
                    return false;
                }
            }
            return true;
        }
    }

    public static VirtualFileFilter createOrFilter(VirtualFileFilter... filters) {
        if (filters == null || filters.length < 2) {
            throw new IllegalArgumentException("At least two filters required. ");
        }
        VirtualFileFilter[] copy = new VirtualFileFilter[filters.length];
        System.arraycopy(filters, 0, copy, 0, filters.length);
        return new OrChangeEventFilter(copy);
    }

    private static class OrChangeEventFilter implements VirtualFileFilter {
        final VirtualFileFilter[] filters;

        OrChangeEventFilter(VirtualFileFilter[] filters) {
            this.filters = filters;
        }

        @Override
        public boolean accept(VirtualFile file) throws VirtualFileSystemException {
            for (VirtualFileFilter filter : filters) {
                if (filter.accept(file)) {
                    return true;
                }
            }
            return false;
        }
    }

    private VirtualFileFilters() {
    }
}
