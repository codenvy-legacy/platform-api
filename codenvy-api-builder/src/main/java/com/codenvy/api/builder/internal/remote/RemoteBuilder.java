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
import com.codenvy.api.builder.internal.dto.BaseBuilderRequest;
import com.codenvy.api.builder.internal.dto.BuildRequest;
import com.codenvy.api.builder.internal.dto.BuildTaskDescriptor;
import com.codenvy.api.builder.internal.dto.BuilderDescriptor;
import com.codenvy.api.builder.internal.dto.BuilderDtoTypes;
import com.codenvy.api.builder.internal.dto.DependencyRequest;
import com.codenvy.api.builder.internal.dto.SlaveBuilderState;
import com.codenvy.api.core.rest.HttpHelper;
import com.codenvy.api.core.rest.RemoteException;
import com.codenvy.api.core.rest.UnknownRemoteException;
import com.codenvy.api.core.rest.dto.JsonDto;
import com.codenvy.api.core.rest.dto.Link;
import com.codenvy.api.core.util.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents remote {@code Builder}.
 * <p/>
 * Usage:
 * <pre>
 *     String baseUrl = ...
 *     String builderName = ... // e.g. 'maven-builder'
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
    private final String                  baseUrl;
    private final Map<String, List<Link>> linksMap;
    private final String                  name;
    private final String                  description;
    private final int                     hashCode;

    private volatile long lastUsage = -1;

    /* Package visibility, not expected to be created by api users.
    They should use RemoteBuilderFactory to get an instance of RemoteBuilder. */
    RemoteBuilder(String baseUrl, BuilderDescriptor builderDescriptor, List<Link> links) {
        this.baseUrl = baseUrl;
        name = builderDescriptor.getName();
        description = builderDescriptor.getDescription();
        linksMap = new HashMap<>();
        for (Link link : links) {
            List<Link> list = linksMap.get(link.getRel());
            if (list == null) {
                linksMap.put(link.getRel(), list = new ArrayList<>());
            }
            list.add(link);
        }
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

    public RemoteBuildTask perform(BuildRequest request) throws IOException, RemoteException {
        final Link link = getLink(Constants.LINK_REL_BUILD);
        if (link == null) {
            throw new UnknownRemoteException("Unable get URL for starting remote process");
        }
        return doRequest(link, request);
    }

    public RemoteBuildTask perform(DependencyRequest request)
            throws UnknownRemoteException, IOException, RemoteException {
        final Link link = getLink(Constants.LINK_REL_DEPENDENCIES_ANALYSIS);
        if (link == null) {
            throw new UnknownRemoteException("Unable get URL for starting remote process");
        }
        return doRequest(link, request);
    }

    private RemoteBuildTask doRequest(Link link, BaseBuilderRequest request) throws IOException, RemoteException {
        final JsonDto dto = HttpHelper.request(link, request);
        if (dto == null || dto.getType() != BuilderDtoTypes.BUILD_TASK_DESCRIPTOR_TYPE) {
            throw new UnknownRemoteException("Invalid response from remote server");
        }
        final BuildTaskDescriptor build = dto.cast();
        Link statusLink = null;
        List<Link> links = build.getLinks();
        for (int i = 0; statusLink == null && i < links.size(); i++) {
            if (Constants.LINK_REL_GET_STATUS.equals(links.get(i).getRel())) {
                statusLink = links.get(i);
            }
        }
        if (statusLink != null) {
            lastUsage = System.currentTimeMillis();
            return new RemoteBuildTask(statusLink);
        }
        throw new UnknownRemoteException("Invalid response from remote server. Link for checking status of remote task is no available");
    }

    public SlaveBuilderState getRemoteBuilderState() throws IOException, RemoteException {
        final Link stateLink = getLink(Constants.LINK_REL_BUILDER_STATE);
        if (stateLink == null) {
            throw new UnknownRemoteException("Unable get URL for getting state of a remote builder");
        }
        final JsonDto dto = HttpHelper.request(stateLink, Pair.of("builder", name));
        if (dto == null || dto.getType() != BuilderDtoTypes.BUILDER_STATE_TYPE) {
            throw new UnknownRemoteException("Invalid response from remote server");
        }
        return dto.cast();
    }

    private Link getLink(String rel) {
        final List<Link> list = linksMap.get(rel);
        return list == null || list.isEmpty() ? null : list.get(0);
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
