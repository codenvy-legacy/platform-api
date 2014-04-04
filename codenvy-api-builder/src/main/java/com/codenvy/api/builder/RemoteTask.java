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

import com.codenvy.api.builder.dto.BuildTaskDescriptor;
import com.codenvy.api.builder.internal.Constants;
import com.codenvy.api.core.rest.HttpJsonHelper;
import com.codenvy.api.core.rest.HttpOutputMessage;
import com.codenvy.api.core.rest.OutputProvider;
import com.codenvy.api.core.rest.RemoteException;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.util.ValueHolder;
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

    /** Get date when this remote build task was started. */
    public long getCreationTime() {
        return created;
    }

    /**
     * Get actual status of remote build process.
     *
     * @return status of remote build process
     * @throws BuilderException
     *         if an error occurs
     */
    public BuildTaskDescriptor getBuildTaskDescriptor() throws BuilderException {
        try {
            return HttpJsonHelper.get(BuildTaskDescriptor.class, String.format("%s/status/%s/%d", baseUrl, builder, taskId));
        } catch (IOException e) {
            throw new BuilderException(e);
        } catch (RemoteException e) {
            throw new BuilderException(e.getServiceError());
        }
    }

    /**
     * Cancel a remote build process.
     *
     * @return status of remote build process after the call
     * @throws BuilderException
     *         if an error occurs
     */
    public BuildTaskDescriptor cancel() throws BuilderException {
        final ValueHolder<BuildTaskDescriptor> holder = new ValueHolder<>();
        final Link link = getLink(Constants.LINK_REL_CANCEL, holder);
        if (link == null) {
            switch (holder.get().getStatus()) {
                case SUCCESSFUL:
                case FAILED:
                case CANCELLED:
                    LOG.debug("Can't cancel build, status is {}", holder.get().getStatus());
                    return holder.get();
                default:
                    throw new BuilderException("Can't cancel task. Cancellation link is not available");
            }
        }
        try {
            return HttpJsonHelper.request(BuildTaskDescriptor.class, link);
        } catch (IOException e) {
            throw new BuilderException(e);
        } catch (RemoteException e) {
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
    public void readLogs(OutputProvider output) throws IOException, BuilderException {
        final Link link = getLink(Constants.LINK_REL_VIEW_LOG, null);
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
    public void readReport(OutputProvider output) throws IOException, BuilderException {
        final Link link = getLink(Constants.LINK_REL_VIEW_REPORT, null);
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
        conn.setConnectTimeout(30 * 1000);
        conn.setConnectTimeout(30 * 1000);
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

    private Link getLink(String rel, ValueHolder<BuildTaskDescriptor> statusHolder) throws BuilderException {
        final BuildTaskDescriptor descriptor = getBuildTaskDescriptor();
        if (statusHolder != null) {
            statusHolder.set(descriptor);
        }
        for (Link link : descriptor.getLinks()) {
            if (rel.equals(link.getRel())) {
                return link;
            }
        }
        return null;
    }
}
