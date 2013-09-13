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
package com.codenvy.api.builder.internal.remote;

import com.codenvy.api.builder.internal.Constants;
import com.codenvy.api.builder.internal.dto.BuilderDtoTypes;
import com.codenvy.api.builder.internal.dto.BuildStatus;
import com.codenvy.api.builder.internal.dto.BuildTaskDescriptor;
import com.codenvy.api.core.rest.HttpHelper;
import com.codenvy.api.core.rest.dto.Link;
import com.codenvy.api.core.rest.RemoteException;
import com.codenvy.api.core.rest.UnknownRemoteException;
import com.codenvy.api.core.rest.dto.JsonDto;

import java.io.IOException;
import java.util.List;

/**
 * Representation of remote builder's task.
 *
 * @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a>
 */
public class RemoteBuildTask {
    private final Link statusLink;

    /* Package visibility, not expected to be created by api users.
       They should use RemoteBuilder instead and get an instance of remote task. */
    RemoteBuildTask(Link statusLink) {
        this.statusLink = statusLink;
    }

    /**
     * Get actual status of remote build process.
     *
     * @return status of remote build process
     * @throws com.codenvy.api.core.rest.UnknownRemoteException
     *         if get a response from remote API which may not be parsed to BuildTaskDescriptor
     * @throws java.io.IOException
     *         if any i/o error occurs when try to access remote builder
     * @throws com.codenvy.api.core.rest.RemoteException
     *         if some other error occurs on remote server
     */
    public BuildTaskDescriptor getDescriptor() throws UnknownRemoteException, IOException, RemoteException {
        final JsonDto dto = HttpHelper.request(statusLink);
        if (dto == null || dto.getType() != BuilderDtoTypes.BUILD_TASK_DESCRIPTOR_TYPE) {
            throw new UnknownRemoteException("Invalid response from remote server");
        }
        return dto.cast();
    }

    /**
     * Cancel a remote build process.
     *
     * @return status of remote build process after the call
     * @throws java.io.IOException
     *         if any i/o error occurs when try to access remote builder
     * @throws com.codenvy.api.core.rest.RemoteException
     *         if some other error occurs on remote server
     */
    public BuildTaskDescriptor cancel() throws UnknownRemoteException, IOException, RemoteException {
        final BuildTaskDescriptor build = getDescriptor();
        if (build.getStatus() == BuildStatus.IN_QUEUE || build.getStatus() == BuildStatus.IN_PROGRESS) {
            Link cancelLink = null;
            final List<Link> links = build.getLinks();
            for (int i = 0; cancelLink == null && i < links.size(); i++) {
                if (Constants.LINK_REL_CANCEL.equals(links.get(i).getRel())) {
                    cancelLink = links.get(i);
                }
            }
            // If remote task is not done yet (success or fail) link for cancellation must be available!
            if (cancelLink == null) {
                throw new UnknownRemoteException("Unable get URL for cancellation remote task");
            }
            final JsonDto dto = HttpHelper.request(cancelLink);
            if (dto == null || dto.getType() != BuilderDtoTypes.BUILD_TASK_DESCRIPTOR_TYPE) {
                throw new UnknownRemoteException("Invalid response from remote server");
            }
            return dto.cast();
        }
        // Return status to caller as is. Cannot cancel task which is already done.
        return build;
    }
}
