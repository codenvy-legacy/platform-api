package com.codenvy.api.runner;

import com.codenvy.api.core.rest.HttpJsonHelper;
import com.codenvy.api.core.rest.ProxyResponse;
import com.codenvy.api.core.rest.RemoteException;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.runner.dto.ApplicationProcessDescriptor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a> */
public class RemoteRunnerProcess {
    private final String baseUrl;
    private final String runner;
    private final Long   processId;
    private final long   created;

    RemoteRunnerProcess(String baseUrl, String runner, Long processId) {
        this.baseUrl = baseUrl;
        this.runner = runner;
        this.processId = processId;
        created = System.currentTimeMillis();
    }

    /** Get date when this process was started. */
    public long getStartTime() {
        return created;
    }

    public ApplicationProcessDescriptor getApplicationProcessDescriptor() throws IOException, RemoteException {
        //"status/{runner}/{id}"
        return HttpJsonHelper.get(ApplicationProcessDescriptor.class, baseUrl + "/status/" + runner + '/' + processId);
    }

    public ApplicationProcessDescriptor stop() throws IOException, RemoteException, RunnerException {
        final Link link = getLink(com.codenvy.api.runner.internal.Constants.LINK_REL_STOP);
        if (link == null) {
            throw new RunnerException("Can't stop application. Application is not started yet or already stopped.");
        }
        return HttpJsonHelper.request(ApplicationProcessDescriptor.class, link);
    }

    public void readLogs(ProxyResponse proxyResponse) throws IOException, RemoteException, RunnerException {
        final Link link = getLink(com.codenvy.api.runner.internal.Constants.LINK_REL_VIEW_LOG);
        if (link == null) {
            throw new RunnerException("Logs are not available.");
        }
        final HttpURLConnection conn = (HttpURLConnection)new URL(link.getHref()).openConnection();
        conn.setConnectTimeout(30 * 1000);
        conn.setConnectTimeout(30 * 1000);
        conn.setRequestMethod(link.getMethod());
        try {
            proxyResponse.setStatus(conn.getResponseCode());
            final String contentType = conn.getContentType();
            if (contentType != null) {
                proxyResponse.addHttpHeader("Content-Type", contentType);
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
        for (Link link : getApplicationProcessDescriptor().getLinks()) {
            if (rel.equals(link.getRel())) {
                return link;
            }
        }
        return null;
    }
}
