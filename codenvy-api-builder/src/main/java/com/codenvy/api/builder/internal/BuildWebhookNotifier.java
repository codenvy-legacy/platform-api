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
package com.codenvy.api.builder.internal;

import com.codenvy.api.core.rest.ServiceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public class BuildWebhookNotifier implements BuildTask.Callback {
    private static final int    CONNECTION_TIMEOUT = 30 * 1000;
    private static final int    READ_TIMEOUT       = 30 * 1000;
    private static final Logger LOG                = LoggerFactory.getLogger(BuildWebhookNotifier.class);

    private final URL            callbackUrl;
    private final ServiceContext builderServiceContext;

    public BuildWebhookNotifier(URL callbackUrl, ServiceContext builderServiceContext) {
        this.callbackUrl = callbackUrl;
        this.builderServiceContext = builderServiceContext;
    }

    @Override
    public void done(BuildTask task) {
//        try {
//            LOG.debug("callback request to {}", callbackUrl);
//            final HttpURLConnection conn = (HttpURLConnection)callbackUrl.openConnection();
//            conn.setConnectTimeout(CONNECTION_TIMEOUT);
//            conn.setConnectTimeout(READ_TIMEOUT);
//            try {
//                conn.setRequestMethod("POST");
//                conn.addRequestProperty("content-type", "application/json");
//                conn.setDoOutput(true);
//                try (OutputStream output = conn.getOutputStream()) {
//                    output.write(JsonDto.toJson(task.getDescriptor(builderServiceContext)).getBytes());
//                }
//                final int responseCode = conn.getResponseCode();
//                if ((responseCode / 100) != 2) {
//                    throw new IOException(String.format("Invalid response status %d from remote server. ", responseCode));
//                }
//            } finally {
//                conn.disconnect();
//            }
//        } catch (Exception e) {
//            LOG.error(e.getMessage(), e);
//        }
    }
}
