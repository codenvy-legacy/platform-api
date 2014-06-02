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
package com.codenvy.api.vfs.server;

import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;

/**
 * Filter for virtual files.
 *
 * @author andrew00x
 */
public interface VirtualFileFilter {
    /** Tests whether specified file should be included in result. */
    boolean accept(VirtualFile file) throws VirtualFileSystemException;

    VirtualFileFilter ALL = new VirtualFileFilter() {
        @Override
        public boolean accept(VirtualFile file) {
            return true;
        }
    };
}
