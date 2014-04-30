package com.codenvy.api.runner;

import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.ForbiddenException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.core.UnauthorizedException;
import com.codenvy.api.core.rest.HttpJsonHelper;
import com.codenvy.api.core.rest.HttpOutputMessage;
import com.codenvy.api.core.rest.OutputProvider;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.runner.dto.ApplicationProcessDescriptor;
import com.google.common.io.ByteStreams;
import com.google.common.io.InputSupplier;
import com.google.common.io.OutputSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Representation of remote application process.
 *
 * @author andrew00x
 */
public class RemoteRunnerProcess {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteRunnerProcess.class);

    private final String baseUrl;
    private final String runner;

    private final Long processId;
    private final long created;

    RemoteRunnerProcess(String baseUrl, String runner, Long processId) {
        this.baseUrl = baseUrl;
        this.runner = runner;
        this.processId = processId;
        created = System.currentTimeMillis();
    }

    public Long getProcessId() {
        return processId;
    }

    public long getCreationTime() {
        return created;
    }

    /**
     * Get actual status of remote application process.
     *
     * @return status of remote application process
     * @throws RunnerException
     *         if an error occurs
     * @throws NotFoundException
     *         if can't get status of remote process because isn't available anymore, e.g. its already removed on remote server
     */
    public ApplicationProcessDescriptor getApplicationProcessDescriptor() throws RunnerException, NotFoundException {
        try {
            return HttpJsonHelper.get(ApplicationProcessDescriptor.class, baseUrl + "/status/" + runner + '/' + processId);
        } catch (IOException e) {
            throw new RunnerException(e);
        } catch (ServerException | UnauthorizedException | ForbiddenException | ConflictException e) {
            throw new RunnerException(e.getServiceError());
        }
    }

    /**
     * Stop a remote application process.
     *
     * @return status of remote application process after the call
     * @throws RunnerException
     *         if an error occurs
     * @throws NotFoundException
     *         if can't stop remote application because isn't available anymore, e.g. its already removed on remote server
     */
    public ApplicationProcessDescriptor stop() throws RunnerException, NotFoundException {
        final ApplicationProcessDescriptor descriptor = getApplicationProcessDescriptor();
        final Link link = getLink(com.codenvy.api.runner.internal.Constants.LINK_REL_STOP, descriptor);
        if (link == null) {
            switch (descriptor.getStatus()) {
                case STOPPED:
                case CANCELLED:
                    LOG.debug("Can't stop process, status is {}", descriptor.getStatus());
                    return descriptor;
                default:
                    throw new RunnerException("Can't stop application. Link for stop application is not available");
            }
        }
        try {
            return HttpJsonHelper.request(ApplicationProcessDescriptor.class, link);
        } catch (IOException e) {
            throw new RunnerException(e);
        } catch (ServerException | UnauthorizedException | ForbiddenException | ConflictException e) {
            throw new RunnerException(e.getServiceError());
        }
    }

    public void readLogs(final OutputProvider output) throws IOException, RunnerException, NotFoundException {
        final ApplicationProcessDescriptor descriptor = getApplicationProcessDescriptor();
        final Link link = getLink(com.codenvy.api.runner.internal.Constants.LINK_REL_VIEW_LOG, descriptor);
        if (link == null) {
            throw new RunnerException("Logs are not available.");
        }
        final HttpURLConnection conn = (HttpURLConnection)new URL(link.getHref()).openConnection();
        conn.setConnectTimeout(30 * 1000);
        conn.setReadTimeout(30 * 1000);
        conn.setRequestMethod(link.getMethod());
        try {
            if (output instanceof HttpOutputMessage) {
                HttpOutputMessage httpOutput = (HttpOutputMessage)output;
                httpOutput.setStatus(conn.getResponseCode());
                final String contentType = conn.getContentType();
                if (contentType != null) {
                    httpOutput.addHttpHeader("Content-Type", contentType);
                }
            }
            ByteStreams.copy(new InputSupplier<InputStream>() {
                                 @Override
                                 public InputStream getInput() throws IOException {
                                     return conn.getInputStream();
                                 }
                             },
                             new OutputSupplier<OutputStream>() {
                                 @Override
                                 public OutputStream getOutput() throws IOException {
                                     return output.getOutputStream();
                                 }
                             }
                            );
        } finally {
            conn.disconnect();
        }
    }

    private Link getLink(String rel, ApplicationProcessDescriptor descriptor) {
        for (Link link : descriptor.getLinks()) {
            if (rel.equals(link.getRel())) {
                return link;
            }
        }
        return null;
    }
}
