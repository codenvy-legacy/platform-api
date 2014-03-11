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
package com.codenvy.api.runner;

import com.codenvy.api.core.notification.EventService;
import com.codenvy.api.core.notification.EventSubscriber;
import com.codenvy.api.core.rest.shared.ParameterType;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.rest.shared.dto.LinkParameter;
import com.codenvy.api.project.server.Project;
import com.codenvy.api.project.server.ProjectManager;
import com.codenvy.api.project.shared.Attribute;
import com.codenvy.api.project.shared.ProjectDescription;
import com.codenvy.api.runner.dto.ApplicationProcessDescriptor;
import com.codenvy.api.runner.internal.Runner;
import com.codenvy.api.runner.internal.RunnerEvent;
import com.codenvy.api.runner.internal.dto.RunnerDescriptor;
import com.codenvy.dto.server.DtoFactory;

import org.everrest.websockets.WSConnectionContext;
import org.everrest.websockets.message.ChannelBroadcastMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Implementation of RunQueue that looks up Runner's on startup. Usage of this class assumes {@link
 * com.codenvy.api.runner.internal.SlaveRunnerService} is deployed on the same host with {@link RunnerService}.
 *
 * @author andrew00x
 */
@Singleton
public class LocalRunQueue extends RunQueue {
    private static final Logger LOG = LoggerFactory.getLogger(LocalRunQueue.class);

    private final List<RemoteRunner> remoteRunners;
    private final EventService       eventService;
    private final ProjectManager     projectManager;

    // work-around to be bale configure port.
    static class SlaveRunnerPortHolder {
        @com.google.inject.Inject(optional = true)
        @Named("runner.queue.local.slave_runner_port")
        private int port = 8080;
    }

    /**
     * @param baseProjectApiUrl
     *         project api url. Configuration parameter that points to the Project API location. If such parameter isn't specified than use
     *         the same base URL as runner API has, e.g. suppose we have runner API at URL: <i>http://codenvy.com/api/runner/my_workspace</i>,
     *         in this case base URL is <i>http://codenvy.com/api</i> so we will try to find project API at URL:
     *         <i>http://codenvy.com/api/project/my_workspace</i>
     * @param baseBuilderApiUrl
     *         builder api url. Configuration parameter that points to the base Builder API location. If such parameter isn't specified
     *         than use the same base URL as runner API has, e.g. suppose we have runner API at URL:
     *         <i>http://codenvy.com/api/runner/my_workspace</i>, in this case base URL is <i>http://codenvy.com/api</i> so we will try to
     *         find builder API at URL: <i>http://codenvy.com/api/builder/my_workspace</i>.
     * @param defMemSize
     *         default size of memory for application in megabytes. This value used is there is nothing specified in properties of project.
     * @param maxTimeInQueue
     *         max time for request to be in queue in seconds
     * @param appLifetime
     *         application life time in seconds. After this time the application may be terminated.
     */
    @Inject
    public LocalRunQueue(@Nullable @Named("project.base_api_url") String baseProjectApiUrl,
                         @Nullable @Named("builder.base_api_url") String baseBuilderApiUrl,
                         @Named("runner.default_app_mem_size") int defMemSize,
                         @Named("runner.queue.max_time_in_queue") int maxTimeInQueue,
                         @Named("runner.queue.app_lifetime") int appLifetime,
                         RunnerSelectionStrategy runnerSelector,
                         EventService eventService,
                         SlaveRunnerPortHolder portHolder,
                         Set<Runner> runners,
                         ProjectManager projectManager) {
        super(baseProjectApiUrl, baseBuilderApiUrl, defMemSize, maxTimeInQueue, appLifetime, runnerSelector);
        this.eventService = eventService;
        this.projectManager = projectManager;
        final String baseUrl = String.format("http://localhost:%s/api/internal/runner", portHolder.port);
        final List<Link> links = new ArrayList<>();
        links.add(DtoFactory.getInstance().createDto(Link.class)
                            .withRel("runner state")
                            .withProduces("application/json")
                            .withHref(baseUrl + "/state")
                            .withMethod("GET")
                            .withParameters(Arrays.asList(DtoFactory.getInstance().createDto(LinkParameter.class)
                                                                    .withName("runner")
                                                                    .withDescription("Name of the runner")
                                                                    .withType(ParameterType.String)
                                                                    .withRequired(true))));
        links.add(DtoFactory.getInstance().createDto(Link.class)
                            .withRel("available runners")
                            .withProduces("application/json")
                            .withHref(baseUrl + "/available")
                            .withMethod("GET"));
        links.add(DtoFactory.getInstance().createDto(Link.class)
                            .withRel("run")
                            .withProduces("application/json")
                            .withConsumes("application/json")
                            .withHref(baseUrl + "/run")
                            .withMethod("POST"));
        remoteRunners = new ArrayList<>();
        for (Runner runner : runners) {
            remoteRunners.add(new RemoteRunner(baseUrl,
                                               DtoFactory.getInstance().createDto(RunnerDescriptor.class)
                                                         .withName(runner.getName())
                                                         .withDescription(runner.getDescription()),
                                               links));
        }
    }

    @PostConstruct
    @Override
    public synchronized void start() {
        super.start();
        registerRunners(null, null, remoteRunners);
        // Register listener that resend events from runner to the client (browser).
        eventService.subscribe(new EventSubscriber<RunnerEvent>() {
            @Override
            public void onEvent(RunnerEvent event) {
                try {
                    final ChannelBroadcastMessage bm = new ChannelBroadcastMessage();
                    final long id = event.getTaskId();
                    final ApplicationProcessDescriptor processDescriptor = getTask(id).getDescriptor();
                    // TODO: do need to have separate channel? Can use runner:status channel for all events?
                    bm.setChannel(String.format("runner:status:%d", id));
                    bm.setType(event.hasError() ? ChannelBroadcastMessage.Type.ERROR : ChannelBroadcastMessage.Type.NONE);
                    bm.setBody(DtoFactory.getInstance().toJson(processDescriptor));
                    WSConnectionContext.sendMessage(bm);
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        });
        eventService.subscribe(new EventSubscriber<RunnerEvent>() {
            @Override
            public void onEvent(RunnerEvent event) {
                if (!event.hasError()) {
                    try {
                        final Project project = projectManager.getProject(event.getWorkspace(), event.getProject());
                        final ProjectDescription description = project.getDescription();
                        if (RunnerEvent.EventType.STARTED.equals(event.getType())) {
                            description.setAttribute(new Attribute("runner.running", "true"));
                        } else if (RunnerEvent.EventType.STOPPED.equals(event.getType())) {
                            description.setAttribute(new Attribute("runner.running", "false"));
                        }
                        project.updateDescription(description);
                    } catch (IOException e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
            }
        });
    }

    @PreDestroy
    @Override
    public synchronized void stop() {
        unregisterRunners(remoteRunners);
        super.stop();
    }
}
