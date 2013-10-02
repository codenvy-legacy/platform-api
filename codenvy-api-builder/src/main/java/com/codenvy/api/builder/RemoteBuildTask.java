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
import com.codenvy.api.builder.internal.dto.BuildTaskDescriptor;
import com.codenvy.api.core.rest.HttpJsonHelper;
import com.codenvy.api.core.rest.RemoteException;
import com.codenvy.api.core.rest.shared.dto.Link;

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
    private final String baseUrl;
    private final String builder;
    private final Long   taskId;

    /* Package visibility, not expected to be created by api users.
       They should use RemoteBuilder instead and get an instance of remote task. */
    RemoteBuildTask(String baseUrl, String builder, Long taskId) {
        this.baseUrl = baseUrl;
        this.builder = builder;
        this.taskId = taskId;
    }

    /**
     * Get actual status of remote build process.
     *
     * @return status of remote build process
     * @throws com.codenvy.api.core.rest.RemoteAccessException
     *         if get a response from remote API which may not be parsed to BuildTaskDescriptor
     * @throws java.io.IOException
     *         if any i/o error occurs when try to access remote builder
     * @throws com.codenvy.api.core.rest.RemoteException
     *         if some other error occurs on remote server
     */
    public BuildTaskDescriptor getStatus() throws IOException, RemoteException {
        //"status/{builder}/{id}"
        return HttpJsonHelper.get(BuildTaskDescriptor.class, baseUrl + "/status/" + builder + '/' + taskId);
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
    public BuildTaskDescriptor cancel() throws IOException, RemoteException, BuilderException {
        final Link link = getLink(Constants.LINK_REL_CANCEL);
        if (link == null) {
            throw new BuilderException("Can't cancel task. Task is not started yet or already done.");
        }
        return HttpJsonHelper.request(BuildTaskDescriptor.class, link);
    }

    public void readLogs(ProxyResponse proxyResponse) throws IOException, RemoteException, BuilderException {
        final Link link = getLink(Constants.LINK_REL_VIEW_LOG);
        if (link == null) {
            throw new BuilderException("Logs are not available.");
        }
        proxyRequest(link.getHref(), link.getMethod(), proxyResponse);
    }

    public void readReport(ProxyResponse proxyResponse) throws IOException, BuilderException, RemoteException {
        final Link link = getLink(Constants.LINK_REL_VIEW_REPORT);
        if (link == null) {
            throw new BuilderException("Report is not available.");
        }
        proxyRequest(link.getHref(), link.getMethod(), proxyResponse);
    }

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

    private Link getLink(String rel) throws IOException, RemoteException {
        for (Link link : getStatus().getLinks()) {
            if (rel.equals(link.getRel())) {
                return link;
            }
        }
        return null;
    }

}
