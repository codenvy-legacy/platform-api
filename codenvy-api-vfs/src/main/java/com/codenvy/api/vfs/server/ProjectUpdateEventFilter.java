/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 * [2012] - [2013] Codenvy, S.A.
 * All Rights Reserved.
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
import com.codenvy.api.vfs.server.observation.ChangeEvent;
import com.codenvy.api.vfs.server.observation.ChangeEvent.ChangeType;
import com.codenvy.api.vfs.server.observation.ChangeEventFilter;
import com.codenvy.api.vfs.server.observation.PathFilter;
import com.codenvy.api.vfs.server.observation.TypeFilter;
import com.codenvy.api.vfs.server.observation.VfsIDFilter;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public class ProjectUpdateEventFilter extends ChangeEventFilter {
    private final String            vfsId;
    private final String            projectId;
    private final ChangeEventFilter delegate;

    public static ProjectUpdateEventFilter newFilter(VirtualFileSystemImpl vfs, VirtualFile project) throws VirtualFileSystemException {
        final String vfsId = vfs.getInfo().getId();
        final ChangeEventFilter filter = ChangeEventFilter.createAndFilter(
                new VfsIDFilter(vfsId),
                new PathFilter(project.getPath() + "/.*"), // events for all project items
                new TypeFilter(ChangeType.CREATED,
                               ChangeType.CONTENT_UPDATED,
                               ChangeType.DELETED,
                               ChangeType.RENAMED,
                               ChangeType.MOVED));
        return new ProjectUpdateEventFilter(filter, vfsId, project.getId());
    }

    @Override
    public boolean matched(ChangeEvent event) throws VirtualFileSystemException {
        final VirtualFileSystem vfs = event.getVirtualFileSystem();
        return (vfs instanceof VirtualFileSystemImpl) && delegate.matched(event);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProjectUpdateEventFilter)) {
            return false;
        }

        ProjectUpdateEventFilter other = (ProjectUpdateEventFilter)o;

        if (vfsId == null) {
            if (other.vfsId != null) {
                return false;
            }
        } else {
            if (!vfsId.equals(other.vfsId)) {
                return false;
            }
        }

        return projectId.equals(other.projectId);
    }

    @Override
    public final int hashCode() {
        int hash = 7;
        hash = 31 * hash + (vfsId != null ? vfsId.hashCode() : 0);
        hash = 31 * hash + projectId.hashCode();
        return hash;
    }

    private ProjectUpdateEventFilter(ChangeEventFilter delegate, String vfsId, String projectId) {
        this.delegate = delegate;
        this.vfsId = vfsId;
        this.projectId = projectId;
    }
}
