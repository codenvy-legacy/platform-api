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

import com.codenvy.api.builder.internal.BuilderException;
import com.codenvy.api.builder.internal.Constants;
import com.codenvy.api.builder.dto.BuildTaskDescriptor;
import com.codenvy.api.core.rest.HttpJsonHelper;
import com.codenvy.api.core.rest.ProxyResponse;
import com.codenvy.api.core.rest.RemoteException;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.util.ValueHolder;

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
 * @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a>
 */
public class RemoteBuildTask {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteBuildTask.class);

    private final String baseUrl;
    private final String builder;
    private final Long   taskId;
    private final long   created;

    /* Package visibility, not expected to be created by api users.
       They should use RemoteBuilder instead and get an instance of remote task. */
    RemoteBuildTask(String baseUrl, String builder, Long taskId) {
        this.baseUrl = baseUrl;
        this.builder = builder;
        this.taskId = taskId;
        created = System.currentTimeMillis();
    }

    /** Get date when this remote build task was started. */
    long getCreationTime() {
        return created;
    }

    /**
     * Get actual status of remote build process.
     *
     * @return status of remote build process
     * @throws java.io.IOException
     *         if any i/o error occurs when try to access remote builder
     * @throws RemoteException
     *         if some other error occurs on remote server
     */
    public BuildTaskDescriptor getBuildTaskDescriptor() throws IOException, RemoteException {
        //"status/{builder}/{id}"
        return HttpJsonHelper.get(BuildTaskDescriptor.class, baseUrl + "/status/" + builder + '/' + taskId);
    }

    /**
     * Cancel a remote build process.
     *
     * @return status of remote build process after the call
     * @throws java.io.IOException
     *         if any i/o error occurs when try to access remote builder
     * @throws RemoteException
     *         if some other error occurs on remote server
     */
    public BuildTaskDescriptor cancel() throws IOException, RemoteException, BuilderException {
        final ValueHolder<BuildTaskDescriptor> holder = new ValueHolder<>();
        final Link link = getLink(Constants.LINK_REL_CANCEL, holder);
        if (link == null) {
            switch (holder.get().getStatus()) {
                case SUCCESSFUL:
                case FAILED:
                case CANCELLED:
                    LOG.info("Can't cancel build, status is {}", holder.get().getStatus()); // TODO: debug
                    return holder.get();
                default:
                    throw new BuilderException("Can't cancel task. Cancellation link is not available");
            }
        }
        return HttpJsonHelper.request(BuildTaskDescriptor.class, link);
    }

    /**
     * Read logs of remote build process.
     *
     * @throws RemoteException
     *         if some other error occurs on remote server
     * @throws IOException
     *         if an i/o error occurs
     * @throws BuilderException
     *         if other error occurs
     */
    public void readLogs(ProxyResponse proxyResponse) throws IOException, RemoteException, BuilderException {
        final Link link = getLink(Constants.LINK_REL_VIEW_LOG, null);
        if (link == null) {
            throw new BuilderException("Logs are not available.");
        }
        proxyRequest(link.getHref(), link.getMethod(), proxyResponse);
    }

    /**
     * Read report file of remote build process.
     *
     * @throws RemoteException
     *         if some other error occurs on remote server
     * @throws IOException
     *         if an i/o error occurs
     * @throws BuilderException
     *         if other error occurs
     */
    public void readReport(ProxyResponse proxyResponse) throws IOException, BuilderException, RemoteException {
        final Link link = getLink(Constants.LINK_REL_VIEW_REPORT, null);
        if (link == null) {
            throw new BuilderException("Report is not available.");
        }
        proxyRequest(link.getHref(), link.getMethod(), proxyResponse);
    }

    /**
     * Download result of remote build process.
     *
     * @throws IOException
     *         if an i/o error occurs
     */
    public void download(String path, ProxyResponse proxyResponse) throws IOException {
        //"download/{builder}/{id}?path={path}"
        proxyRequest(baseUrl + "/download/" + builder + '/' + taskId + "?path=" + path, "GET", proxyResponse);
    }

    private void proxyRequest(String url, String method, ProxyResponse proxyResponse) throws IOException {
        final HttpURLConnection conn = (HttpURLConnection)new URL(url).openConnection();
        conn.setConnectTimeout(30 * 1000);
        conn.setConnectTimeout(30 * 1000);
        conn.setRequestMethod(method);
        try {
            proxyResponse.setStatus(conn.getResponseCode());
            final String contentType = conn.getContentType();
            if (contentType != null) {
                proxyResponse.addHttpHeader("Content-Type", contentType);
            }
            final String contentDisposition = conn.getHeaderField("Content-Disposition");
            if (contentDisposition != null) {
                proxyResponse.addHttpHeader("Content-Disposition", contentDisposition);
            }
            final OutputStream proxyOutputStream = proxyResponse.getOutputStream();
            try (InputStream input = conn.getInputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = input.read(buf)) > 0) {
                    proxyOutputStream.write(buf, 0, n);
                }
                proxyOutputStream.flush();
            }
        } finally {
            conn.disconnect();
        }
    }

    private Link getLink(String rel, ValueHolder<BuildTaskDescriptor> statusHolder) throws IOException, RemoteException {
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
