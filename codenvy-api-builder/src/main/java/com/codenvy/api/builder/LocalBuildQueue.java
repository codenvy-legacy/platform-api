/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2014] Codenvy, S.A. 
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

import com.codenvy.api.builder.internal.Builder;
import com.codenvy.api.builder.internal.BuilderRegistry;
import com.codenvy.api.builder.internal.dto.BaseBuilderRequest;
import com.codenvy.api.builder.internal.dto.BuilderDescriptor;
import com.codenvy.api.core.notification.EventService;
import com.codenvy.api.core.rest.shared.ParameterType;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.rest.shared.dto.LinkParameter;
import com.codenvy.dto.server.DtoFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Implementation of BuildQueue that looks up Builder's on startup. Usage of this class assumes {@link
 * com.codenvy.api.builder.internal.SlaveBuilderService} is deployed on the same host with {@link com.codenvy.api.builder.BuilderService}.
 *
 * @author andrew00x
 */
// DON'T USE IT IN CLOUD INFRASTRUCTURE.
@Singleton
public final class LocalBuildQueue extends BuildQueue {
    private static final Logger LOG = LoggerFactory.getLogger(LocalBuildQueue.class);

    private final BuilderRegistry builders;
    private final int             slaveBuilderPort;

    // work-around to be bale configure port.
    static class SlaveBuilderPortHolder {
        @com.google.inject.Inject(optional = true)
        @Named("builder.queue.local.slave_builder_port")
        private int port = 8080;
    }

    /**
     * @param baseProjectApiUrl
     *         project api url. Configuration parameter that points to the Project API location. If such parameter isn't specified than use
     *         the same base URL as builder API has, e.g. suppose we have builder API at URL: <i>http://codenvy.com/api/builder/my_workspace</i>,
     *         in this case base URL is <i>http://codenvy.com/api</i> so we will try to find project API at URL:
     *         <i>http://codenvy.com/api/project/my_workspace</i>
     * @param maxTimeInQueue
     *         max time for request to be in queue in seconds. Configuration parameter that sets max time (in seconds) which request may be
     *         in this queue. After this time the results of build may be removed.
     * @param timeout
     *         build timeout. Configuration parameter that provides build timeout is seconds. After this time build may be terminated.
     */
    @Inject
    public LocalBuildQueue(@Nullable @Named("project.base_api_url") String baseProjectApiUrl,
                           @Named("builder.queue.max_time_in_queue") int maxTimeInQueue,
                           @Named("builder.queue.build_timeout") int timeout,
                           BuilderSelectionStrategy builderSelector,
                           EventService eventService,
                           SlaveBuilderPortHolder portHolder,
                           BuilderRegistry builders) {
        super(baseProjectApiUrl, maxTimeInQueue, timeout, builderSelector, eventService);
        this.builders = builders;
        this.slaveBuilderPort = portHolder.port;
    }

    @Override
    protected RemoteBuilder getBuilder(BaseBuilderRequest request) throws BuilderException {
        final Builder builder = builders.get(request.getBuilder());
        if (builder == null) {
            throw new BuilderException("There is no any builder to process this request. ");
        }
        final String baseUrl = String.format("http://localhost:%d/api/internal/builder", slaveBuilderPort);
        final List<Link> links = new LinkedList<>();
        links.add(DtoFactory.getInstance().createDto(Link.class)
                            .withRel("builder state")
                            .withProduces("application/json")
                            .withHref(baseUrl + "/state")
                            .withMethod("GET")
                            .withParameters(Arrays.asList(DtoFactory.getInstance().createDto(LinkParameter.class)
                                                                    .withName("builder")
                                                                    .withDescription("Name of the builder")
                                                                    .withType(ParameterType.String)
                                                                    .withRequired(true))));
        links.add(DtoFactory.getInstance().createDto(Link.class)
                            .withRel("available builders")
                            .withProduces("application/json")
                            .withHref(baseUrl + "/available")
                            .withMethod("GET"));
        links.add(DtoFactory.getInstance().createDto(Link.class)
                            .withRel("analyze dependencies")
                            .withProduces("application/json")
                            .withConsumes("application/json")
                            .withHref(baseUrl + "/dependencies")
                            .withMethod("POST"));
        links.add(DtoFactory.getInstance().createDto(Link.class)
                            .withRel("build")
                            .withProduces("application/json")
                            .withConsumes("application/json")
                            .withHref(baseUrl + "/build")
                            .withMethod("POST"));
        final RemoteBuilder rbuilder = new RemoteBuilder(baseUrl,
                                                         DtoFactory.getInstance().createDto(BuilderDescriptor.class)
                                                                   .withName(builder.getName())
                                                                   .withDescription(builder.getDescription()),
                                                         links
        );
        LOG.debug("Use slave builder {} at {}", rbuilder.getName(), rbuilder.getBaseUrl());
        return rbuilder;
    }
}
