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
package com.codenvy.api.runner.internal;

import com.codenvy.api.core.rest.ServiceContext;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.util.Cancellable;
import com.codenvy.api.runner.ApplicationStatus;
import com.codenvy.api.runner.RunnerException;
import com.codenvy.api.runner.dto.ApplicationProcessDescriptor;
import com.codenvy.dto.server.DtoFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public abstract class ApplicationProcess implements Cancellable {
    private static final AtomicLong sequence = new AtomicLong(1);

    private final    Long                id;
    private final    String              runner;
    private final    RunnerConfiguration configuration;
    private volatile Throwable           error;

    public ApplicationProcess(String runner, RunnerConfiguration configuration) {
        id = sequence.getAndIncrement();
        this.configuration = configuration;
        this.runner = runner;
    }

    /**
     * Get time when process was started.
     *
     * @return time when process was started or {@code -1} if process is not started yet or if time of start is unknown
     * @throws RunnerException
     *         if an error occurs when try to check status of application process
     */
    public abstract long getStartTime() throws RunnerException;

    /**
     * Reports whether process is running or not.
     *
     * @return {@code true} if process is running and {@code false} otherwise
     * @throws RunnerException
     *         if an error occurs when try to check status of application process
     */
    public abstract boolean isRunning() throws RunnerException;

    /**
     * Reports whether process was started and stopped successfully.
     *
     * @throws RunnerException
     *         if an error occurs when try to check status of application process
     */
    public abstract boolean isDone() throws RunnerException;

    /** Stop process. */
    public abstract void stop() throws RunnerException;

    public abstract ApplicationLogger getLogger();

    /**
     * Get unique id of this process.
     *
     * @return unique id of this process
     */
    public final Long getId() {
        return id;
    }

    /**
     * Get name of runner which owns this process.
     *
     * @return name of runner which owns this process
     */
    public final String getRunner() {
        return runner;
    }

    public RunnerConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Get error of starting application process. If this method returns {@code null} then application was started successfully or is not
     * started yet.
     */
    public Throwable getError() {
        return error;
    }

    /** Set error. For internal usage only. */
    public void setError(Throwable error) {
        this.error = error;
    }

    /**
     * Stop process.
     *
     * @see #cancel()
     */
    @Override
    public void cancel() throws Exception {
        stop();
    }

    public ApplicationProcessDescriptor getDescriptor(ServiceContext restfulRequestContext) throws RunnerException {
        final ApplicationStatus status = isDone()
                                         ? error == null ? ApplicationStatus.RAN : ApplicationStatus.FAILED
                                         : isRunning() ? ApplicationStatus.RUNNING : ApplicationStatus.STOPPED;

        final List<Link> links = new ArrayList<>(3);
        final UriBuilder servicePathBuilder = restfulRequestContext.getServiceUriBuilder();
        links.add(DtoFactory.getInstance().createDto(Link.class)
                            .withRel(Constants.LINK_REL_GET_STATUS)
                            .withHref(servicePathBuilder.clone().path(SlaveRunnerService.class, "getStatus")
                                                        .build(getRunner(), getId()).toString())
                            .withMethod("GET")
                            .withProduces(MediaType.APPLICATION_JSON));
        links.add(DtoFactory.getInstance().createDto(Link.class)
                            .withRel(Constants.LINK_REL_VIEW_LOG)
                            .withHref(servicePathBuilder.clone().path(SlaveRunnerService.class, "getLogs")
                                                        .build(getRunner(), getId()).toString())
                            .withMethod("GET")
                            .withProduces(getLogger().getContentType()));
        if (ApplicationStatus.RUNNING == status) {
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withRel(Constants.LINK_REL_STOP)
                                .withHref(servicePathBuilder.clone().path(SlaveRunnerService.class, "stop")
                                                            .build(getRunner(), getId()).toString())
                                .withMethod("POST")
                                .withProduces(MediaType.APPLICATION_JSON));
        }
        return DtoFactory.getInstance().createDto(ApplicationProcessDescriptor.class)
                         .withProcessId(id)
                         .withStatus(status)
                         .withLinks(links);
    }
}
