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
package com.codenvy.api.core.rest;

import com.codenvy.api.core.rest.dto.DtoTypes;
import com.codenvy.api.core.rest.dto.JsonDto;
import com.codenvy.api.core.rest.dto.Link;
import com.codenvy.api.core.rest.dto.ServiceError;
import com.codenvy.api.core.util.Pair;
import com.codenvy.commons.json.JsonHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public class HttpHelper {
    public static final int CONNECTION_TIMEOUT = 30 * 1000;
    public static final int READ_TIMEOUT       = 30 * 1000;

    public static JsonDto request(Link link, Object body, Pair<String, ?>... parameters) throws IOException, RemoteException {
        return request(link.getHref(), link.getMethod(), body, link.getConsumes(), parameters);
    }

    public static JsonDto request(Link link, Pair<String, ?>... parameters) throws IOException, RemoteException {
        return request(link, null, parameters);
    }

    @SuppressWarnings("unchecked")
    private static final Pair<String, ?>[] EMPTY = new Pair[0];

    public static JsonDto request(Link link) throws IOException, RemoteException {
        return request(link, EMPTY);
    }

    public static JsonDto request(String url, String method, Object body, String contentType, Pair<String, ?>... parameters)
            throws IOException, RemoteException {
        if (parameters != null && parameters.length > 0) {
            final StringBuilder sb = new StringBuilder();
            sb.append(url);
            sb.append('?');
            for (int i = 0, l = parameters.length; i < l; i++) {
                String name = URLEncoder.encode(parameters[i].first, "UTF-8");
                String value = parameters[i].second == null ? null : URLEncoder.encode(String.valueOf(parameters[i].second), "UTF-8");
                if (i > 0) {
                    sb.append('&');
                }
                sb.append(name);
                if (value != null) {
                    sb.append('=');
                    sb.append(value);
                }
            }
            url = sb.toString();
        }
        final HttpURLConnection conn = (HttpURLConnection)new URL(url).openConnection();
        conn.setConnectTimeout(CONNECTION_TIMEOUT);
        conn.setConnectTimeout(READ_TIMEOUT);
        try {
            conn.setRequestMethod(method);
            if (body != null) {
                conn.addRequestProperty("content-type", contentType);
                conn.setDoOutput(true);
                try (OutputStream output = conn.getOutputStream()) {
                    output.write(JsonHelper.toJson(JsonDto.wrap(body)).getBytes());
                }
            }

            try (InputStream input = conn.getInputStream()) {
                final JsonDto jsonDto = JsonDto.create(input);
                if (jsonDto != null) {
                    if (jsonDto.getType() == DtoTypes.SERVICE_ERROR_TYPE) {
                        final ServiceError error = jsonDto.cast();
                        throw new RemoteException(error);
                    }
                }
                return jsonDto;
            }
        } finally {
            conn.disconnect();
        }
    }

    private HttpHelper() {
    }
}
