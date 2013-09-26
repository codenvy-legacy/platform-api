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
package com.codenvy.factory.client.impl;

import com.codenvy.commons.json.JsonHelper;
import com.codenvy.commons.json.JsonParseException;
import com.codenvy.commons.lang.IoUtil;
import com.codenvy.factory.client.FactoryClient;
import com.codenvy.factory.commons.AdvancedFactoryUrl;
import com.codenvy.factory.commons.FactoryUrlException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;

/** Retrieve factory parameters over http connection. */
public class HttpFactoryClient implements FactoryClient {
    private static final Logger LOG = LoggerFactory.getLogger(HttpFactoryClient.class);

    @Override
    public AdvancedFactoryUrl getFactory(URL factoryUrl, String id) throws FactoryUrlException {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection)UriBuilder.fromUri(factoryUrl.toURI()).replacePath("/api/factory").path(id).replaceQuery(null).build()
                                                .toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setInstanceFollowRedirects(false);

            int responseCode = conn.getResponseCode();
            if (responseCode / 100 != 2) {
                InputStream errorStream = conn.getErrorStream();
                String message = errorStream != null ? IoUtil.readAndCloseQuietly(errorStream) : "";
                throw new FactoryUrlException(responseCode, message);
            }

            AdvancedFactoryUrl factory = JsonHelper.fromJson(conn.getInputStream(), AdvancedFactoryUrl.class, null);
            return factory;
        } catch (IOException | URISyntaxException | JsonParseException e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw new FactoryUrlException(e.getLocalizedMessage(), e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
