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

import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.rest.shared.dto.ServiceError;
import com.codenvy.api.core.util.Pair;
import com.codenvy.commons.lang.IoUtil;
import com.codenvy.dto.server.DtoFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Provides helper method to send HTTP requests with JSON content.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
public class HttpJsonHelper {

    public static <DTO> DTO request(Class<DTO> dtoInterface, Link link, Object body, Pair<String, ?>... parameters)
            throws IOException, RemoteException {
        return request(dtoInterface, link.getHref(), link.getMethod(), body, parameters);
    }

    public static <DTO> DTO request(Class<DTO> dtoInterface, Link link, Pair<String, ?>... parameters) throws IOException, RemoteException {
        return request(dtoInterface, link, null, parameters);
    }

    @SuppressWarnings("unchecked")
    private static final Pair<String, ?>[] EMPTY = new Pair[0];

    public static <DTO> DTO request(Class<DTO> dtoInterface, Link link) throws IOException, RemoteException {
        return request(dtoInterface, link, EMPTY);
    }

    /**
     * Sends HTTP request to specified {@code url}.
     *
     * @param dtoInterface
     *         type of expected response. If server returns some content we try parse it and restore object of the specified type from it.
     *         Specified interface must be annotated with &#064DTO.
     * @param url
     *         URL to send request
     * @param method
     *         HTTP method
     * @param body
     *         body of request. Object must implements DTO interface (interface must be annotated with &#064DTO).
     * @param parameters
     *         additional query parameters.
     * @return instance of {@code dtoInterface} which represents JSON response from the server
     * @throws IOException
     *         if an i/o error occurs
     * @throws RemoteException
     *         if server returns error response in supported JSON format, see {@link ServiceError}
     * @throws RemoteAccessException
     *         if server returns error response in unsupported format
     * @see com.codenvy.dto.shared.DTO
     */
    public static <DTO> DTO request(Class<DTO> dtoInterface,
                                    String url,
                                    String method,
                                    Object body,
                                    Pair<String, ?>... parameters) throws IOException, RemoteException {
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
        conn.setConnectTimeout(30 * 1000);
        conn.setConnectTimeout(30 * 1000);
        try {
            conn.setRequestMethod(method);
            if (body != null) {
                conn.addRequestProperty("content-type", "application/json");
                conn.setDoOutput(true);
                try (OutputStream output = conn.getOutputStream()) {
                    output.write(DtoFactory.getInstance().toJson(body).getBytes());
                }
            }

            final int responseCode = conn.getResponseCode();
            if ((responseCode / 100) != 2) {
                InputStream in = conn.getErrorStream();
                if (in == null) {
                    in = conn.getInputStream();
                }
                final String str = IoUtil.readAndCloseQuietly(in);
                final String contentType = conn.getContentType();
                if (contentType != null && contentType.startsWith("application/json")) {
                    final ServiceError serviceError = DtoFactory.getInstance().createDtoFromJson(str, ServiceError.class);
                    if (serviceError.getMessage() != null) {
                        // Error is in format what we can understand.
                        throw new RemoteException(serviceError);
                    }
                }
                // Can't parse content as json or content has format other we expect for error.
                throw new RemoteAccessException(
                        String.format("Failed access: %s, method: %s, response code: %d, message: %s", url, method, responseCode, str));
            }
            final String contentType = conn.getContentType();
            if (!(contentType == null || contentType.startsWith("application/json"))) {
                throw new RemoteAccessException("Unsupported type of response from remote server. ");
            }
            if (dtoInterface != null) {
                try (InputStream input = conn.getInputStream()) {
                    return DtoFactory.getInstance().createDtoFromJson(input, dtoInterface);
                }
            }
            return null;
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Sends GET request to specified {@code url}.
     *
     * @param dtoInterface
     *         type of expected response. If server returns some content we try parse it and restore object of the specified type from it.
     *         Specified interface must be annotated with &#064DTO.
     * @param url
     *         URL to send request
     * @param parameters
     *         additional query parameters.
     * @return instance of {@code dtoInterface} which represents JSON response from the server
     * @throws IOException
     *         if an i/o error occurs
     * @throws RemoteException
     *         if server returns error response in supported JSON format, see {@link ServiceError}
     * @throws RemoteAccessException
     *         if server returns error response in unsupported format
     * @see com.codenvy.dto.shared.DTO
     */
    public static <DTO> DTO get(Class<DTO> dtoInterface, String url, Pair<String, ?>... parameters) throws IOException, RemoteException {
        return request(dtoInterface, url, "GET", null, parameters);
    }

    /**
     * Sends POST request to specified {@code url}.
     *
     * @param dtoInterface
     *         type of expected response. If server returns some content we try parse it and restore object of the specified type from it.
     *         Specified interface must be annotated with &#064DTO.
     * @param url
     *         URL to send request
     * @param body
     *         body of request. Object must implements DTO interface (interface must be annotated with &#064DTO).
     * @param parameters
     *         additional query parameters.
     * @return instance of {@code dtoInterface} which represents JSON response from the server
     * @throws IOException
     *         if an i/o error occurs
     * @throws RemoteException
     *         if server returns error response in supported JSON format, see {@link ServiceError}
     * @throws RemoteAccessException
     *         if server returns error response in unsupported format
     * @see com.codenvy.dto.shared.DTO
     */
    public static <DTO> DTO post(Class<DTO> dtoInterface, String url, Object body, Pair<String, ?>... parameters)
            throws IOException, RemoteException {
        return request(dtoInterface, url, "POST", body, parameters);
    }

    private HttpJsonHelper() {
    }
}
