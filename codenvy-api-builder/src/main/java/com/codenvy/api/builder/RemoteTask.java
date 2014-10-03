/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.builder;

import com.codenvy.api.builder.dto.BuildTaskDescriptor;
import com.codenvy.api.builder.internal.Constants;
import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.ForbiddenException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.core.UnauthorizedException;
import com.codenvy.api.core.rest.HttpJsonHelper;
import com.codenvy.api.core.rest.HttpOutputMessage;
import com.codenvy.api.core.rest.OutputProvider;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.dto.server.DtoFactory;
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
 * Representation of remote builder's task.
 *
 * @author andrew00x
 */
public class RemoteTask {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteTask.class);

    private final String baseUrl;
    private final String builder;
    private final Long   taskId;
    private final long   created;

    /* Package visibility, not expected to be created by api users. They should use RemoteBuilder instead and get an instance of remote task. */
    RemoteTask(String baseUrl, String builder, Long taskId) {
        this.baseUrl = baseUrl;
        this.builder = builder;
        this.taskId = taskId;
        created = System.currentTimeMillis();
    }

    /**
     * Get unique id of this task.
     *
     * @return unique id of this task
     */
    public Long getId() {
        return taskId;
    }

    /** Get date when this task was created. */
    public long getCreationTime() {
        return created;
    }

    /**
     * Get actual status of remote build process.
     *
     * @return status of remote build process
     * @throws BuilderException
     *         if an error occurs
     * @throws NotFoundException
     *         if can't get status of remote task because isn't available anymore, e.g. its already removed on remote server
     */
    public BuildTaskDescriptor getBuildTaskDescriptor() throws BuilderException, NotFoundException {
        try {
            return HttpJsonHelper.get(BuildTaskDescriptor.class, String.format("%s/status/%s/%d", baseUrl, builder, taskId));
        } catch (IOException e) {
            throw new BuilderException(e);
        } catch (ServerException | UnauthorizedException | ForbiddenException | ConflictException e) {
            throw new BuilderException(e.getServiceError());
        }
    }

    /**
     * Cancel a remote build process.
     *
     * @return status of remote build process after the call
     * @throws BuilderException
     *         if an error occurs
     * @throws NotFoundException
     *         if can't cancel remote task because isn't available anymore, e.g. its already removed on remote server
     */
    public BuildTaskDescriptor cancel() throws BuilderException, NotFoundException {
        final BuildTaskDescriptor descriptor = getBuildTaskDescriptor();
        final Link link = descriptor.getLink(Constants.LINK_REL_CANCEL);
        if (link == null) {
            switch (descriptor.getStatus()) {
                case SUCCESSFUL:
                case FAILED:
                case CANCELLED:
                    LOG.debug("Can't cancel build, status is {}", descriptor.getStatus());
                    return descriptor;
                default:
                    throw new BuilderException("Can't cancel task. Cancellation link is not available");
            }
        }
        try {
            return HttpJsonHelper.request(BuildTaskDescriptor.class, DtoFactory.getInstance().clone(link));
        } catch (IOException e) {
            throw new BuilderException(e);
        } catch (ServerException | UnauthorizedException | ForbiddenException | ConflictException e) {
            throw new BuilderException(e.getServiceError());
        }
    }

    /**
     * Copy logs of build process to specified {@code output}.
     *
     * @param output
     *         output for logs content
     * @throws IOException
     *         if an i/o error occurs
     * @throws BuilderException
     *         if other error occurs
     */
    public void readLogs(OutputProvider output) throws IOException, BuilderException, NotFoundException {
        final BuildTaskDescriptor descriptor = getBuildTaskDescriptor();
        final Link link = descriptor.getLink(Constants.LINK_REL_VIEW_LOG);
        if (link == null) {
            throw new BuilderException("Logs are not available.");
        }
        doRequest(link.getHref(), link.getMethod(), output);
    }

    /**
     * Copy report file of build process to specified {@code output}.
     *
     * @param output
     *         output for report
     * @throws IOException
     *         if an i/o error occurs
     * @throws BuilderException
     *         if other error occurs
     * @see com.codenvy.api.builder.internal.BuildResult#getBuildReport()
     */
    public void readReport(OutputProvider output) throws IOException, BuilderException, NotFoundException {
        final BuildTaskDescriptor descriptor = getBuildTaskDescriptor();
        final Link link = descriptor.getLink(Constants.LINK_REL_VIEW_REPORT);
        if (link == null) {
            throw new BuilderException("Report is not available.");
        }
        doRequest(link.getHref(), link.getMethod(), output);
    }

    /**
     * Copy file to specified {@code output}.
     *
     * @param path
     *         path to build artifact
     * @param output
     *         output for download content
     * @throws IOException
     *         if an i/o error occurs
     * @throws BuilderException
     *         if other error occurs
     * @see com.codenvy.api.builder.internal.BuildResult#getResults()
     */
    public void readFile(String path, OutputProvider output) throws IOException, BuilderException {
        doRequest(String.format("%s/download/%s/%d?path=%s", baseUrl, builder, taskId, path), "GET", output);
    }

    private void doRequest(String url, String method, final OutputProvider output) throws IOException {
        final HttpURLConnection conn = (HttpURLConnection)new URL(url).openConnection();
        conn.setConnectTimeout(60 * 1000);
        conn.setReadTimeout(60 * 1000);
        conn.setRequestMethod(method);
        try {
            if (output instanceof HttpOutputMessage) {
                HttpOutputMessage httpOutput = (HttpOutputMessage)output;
                httpOutput.setStatus(conn.getResponseCode());
                final String contentType = conn.getContentType();
                if (contentType != null) {
                    httpOutput.addHttpHeader("Content-Type", contentType);
                }
                // for download files
                final String contentDisposition = conn.getHeaderField("Content-Disposition");
                if (contentDisposition != null) {
                    httpOutput.addHttpHeader("Content-Disposition", contentDisposition);
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
}
