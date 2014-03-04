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
package com.codenvy.api.builder;

import com.codenvy.api.builder.internal.Constants;
import com.codenvy.api.builder.internal.dto.BaseBuilderRequest;
import com.codenvy.api.builder.internal.dto.BuildRequest;
import com.codenvy.api.builder.dto.BuildTaskDescriptor;
import com.codenvy.api.builder.internal.dto.BuilderDescriptor;
import com.codenvy.api.builder.internal.dto.DependencyRequest;
import com.codenvy.api.builder.internal.dto.SlaveBuilderState;
import com.codenvy.api.core.rest.HttpJsonHelper;
import com.codenvy.api.core.rest.RemoteException;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.util.Pair;
import com.codenvy.dto.server.DtoFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents remote {@code Builder}.
 * <p/>
 * Usage:
 * <pre>
 *     String baseUrl = ...
 *     String builderName = ... // e.g. 'maven'
 *     RemoteBuilderFactory factory = new RemoteBuilderFactory(baseUrl);
 *     RemoteBuilder builder = factory.getRemoteBuilder(builderName);
 *     BuildRequest request = ...
 *     RemoteBuildTask remote = builder.perform(request);
 *     // do something with RemoteBuildTask
 *     // e.g. check status
 *     System.out.println(remote.getDescriptor());
 * </pre>
 *
 * @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a>
 */
public class RemoteBuilder {
    private final String     baseUrl;
    private final List<Link> links;
    private final String     name;
    private final String     description;
    private final int        hashCode;

    private volatile long lastUsage = -1;

    /* Package visibility, not expected to be created by api users.
    They should use RemoteBuilderFactory to get an instance of RemoteBuilder. */
    RemoteBuilder(String baseUrl, BuilderDescriptor builderDescriptor, List<Link> links) {
        this.baseUrl = baseUrl;
        name = builderDescriptor.getName();
        description = builderDescriptor.getDescription();
        this.links = new ArrayList<>(links);
        int hashCode = 7;
        hashCode = hashCode * 31 + baseUrl.hashCode();
        hashCode = hashCode * 31 + name.hashCode();
        this.hashCode = hashCode;
    }

    public final String getBaseUrl() {
        return baseUrl;
    }

    /** @see com.codenvy.api.builder.internal.Builder#getName() */
    public final String getName() {
        return name;
    }

    /** @see com.codenvy.api.builder.internal.Builder#getDescription() */
    public final String getDescription() {
        return description;
    }

    public long getLastUsageTime() {
        return lastUsage;
    }

    public RemoteBuildTask perform(BuildRequest request) throws IOException, RemoteException, BuilderException {
        final Link link = getLink(Constants.LINK_REL_BUILD);
        if (link == null) {
            throw new BuilderException("Unable get URL for starting remote process");
        }
        return doRequest(link, request);
    }

    public RemoteBuildTask perform(DependencyRequest request) throws IOException, RemoteException, BuilderException {
        final Link link = getLink(Constants.LINK_REL_DEPENDENCIES_ANALYSIS);
        if (link == null) {
            throw new BuilderException("Unable get URL for starting remote process");
        }
        return doRequest(link, request);
    }

    private RemoteBuildTask doRequest(Link link, BaseBuilderRequest request) throws IOException, RemoteException {
        final BuildTaskDescriptor build = HttpJsonHelper.request(BuildTaskDescriptor.class, link, request);
        lastUsage = System.currentTimeMillis();
        return new RemoteBuildTask(baseUrl, request.getBuilder(), build.getTaskId());
    }

    public SlaveBuilderState getRemoteBuilderState() throws IOException, RemoteException, BuilderException {
        final Link stateLink = getLink(Constants.LINK_REL_BUILDER_STATE);
        if (stateLink == null) {
            throw new BuilderException("Unable get URL for getting state of a remote builder");
        }
        return HttpJsonHelper.request(SlaveBuilderState.class, stateLink, Pair.of("builder", name));
    }

    private Link getLink(String rel) {
        for (Link link : links) {
            if (rel.equals(link.getRel())) {
                // create copy of link since we pass it outside from this class
                return DtoFactory.getInstance().clone(link);
            }
        }
        return null;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RemoteBuilder)) {
            return false;
        }
        RemoteBuilder other = (RemoteBuilder)o;
        return baseUrl.equals(other.baseUrl) && name.equals(other.name);
    }

    @Override
    public final int hashCode() {
        return hashCode;
    }
}
