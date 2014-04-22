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

import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.ForbiddenException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.core.UnauthorizedException;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.rest.shared.dto.ServiceError;
import com.codenvy.api.core.util.Pair;
import com.codenvy.commons.env.EnvironmentContext;
import com.codenvy.commons.user.User;
import com.codenvy.dto.server.DtoFactory;
import com.google.common.io.CharStreams;
import com.google.common.io.InputSupplier;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

/**
 * Provides helper method to send HTTP requests with JSON content.
 *
 * @author andrew00x
 */
public class HttpJsonHelper {
    @SuppressWarnings("unchecked")
    private static final Pair<String, ?>[] EMPTY = new Pair[0];


    public static <DTO> DTO request(Class<DTO> dtoInterface, Link link, Object body, Pair<String, ?>... parameters)
            throws IOException, ServerException, NotFoundException, ForbiddenException, UnauthorizedException, ConflictException {
        return request(dtoInterface, link.getHref(), link.getMethod(), body, parameters);
    }

    public static <DTO> DTO request(Class<DTO> dtoInterface, Link link, Pair<String, ?>... parameters)
            throws IOException, ServerException, UnauthorizedException, ForbiddenException, NotFoundException, ConflictException {
        return request(dtoInterface, link, null, parameters);
    }

    public static <DTO> DTO request(Class<DTO> dtoInterface, Link link)
            throws IOException, ServerException, NotFoundException, ForbiddenException, UnauthorizedException, ConflictException {
        return request(dtoInterface, link, EMPTY);
    }

    public static <DTO> List<DTO> requestArray(Class<DTO> dtoInterface, Link link, Object body, Pair<String, ?>... parameters)
            throws IOException, ServerException, NotFoundException, ForbiddenException, UnauthorizedException, ConflictException {
        return requestArray(dtoInterface, link.getHref(), link.getMethod(), body, parameters);
    }

    public static <DTO> List<DTO> requestArray(Class<DTO> dtoInterface, Link link, Pair<String, ?>... parameters)
            throws IOException, ServerException, UnauthorizedException, ForbiddenException, NotFoundException, ConflictException {
        return requestArray(dtoInterface, link, null, parameters);
    }

    public static <DTO> List<DTO> requestArray(Class<DTO> dtoInterface, Link link)
            throws IOException, ServerException, NotFoundException, ForbiddenException, UnauthorizedException, ConflictException {
        return requestArray(dtoInterface, link, EMPTY);
    }

    public static String requestString(String url,
                                       String method,
                                       Object body,
                                       Pair<String, ?>... parameters)
            throws IOException, ServerException, ForbiddenException, NotFoundException, UnauthorizedException, ConflictException {
        final String authToken = getAuthenticationToken();
        if ((parameters != null && parameters.length > 0) || authToken != null) {
            final StringBuilder sb = new StringBuilder();
            sb.append(url);
            sb.append('?');
            if (authToken != null) {
                sb.append("token=");
                sb.append(authToken);
                sb.append('&');
            }
            if (parameters != null && parameters.length > 0) {
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
            }
            url = sb.toString();
        }
        final HttpURLConnection conn = (HttpURLConnection)new URL(url).openConnection();
        conn.setConnectTimeout(30 * 1000);
        conn.setReadTimeout(30 * 1000);
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
                final InputStream fIn = in;
                final String str = CharStreams.toString(new InputSupplier<Reader>() {
                    @Override
                    public Reader getInput() throws IOException {
                        return new InputStreamReader(fIn);
                    }
                });
                final String contentType = conn.getContentType();
                if (contentType != null && contentType.startsWith("application/json")) {
                    final ServiceError serviceError = DtoFactory.getInstance().createDtoFromJson(str, ServiceError.class);
                    if (serviceError.getMessage() != null) {
                        if (responseCode == Response.Status.FORBIDDEN.getStatusCode()) {
                            throw new ForbiddenException(serviceError);
                        } else if (responseCode == Response.Status.NOT_FOUND.getStatusCode()) {
                            throw new NotFoundException(serviceError);
                        } else if (responseCode == Response.Status.UNAUTHORIZED.getStatusCode()) {
                            throw new UnauthorizedException(serviceError);
                        } else if (responseCode == Response.Status.CONFLICT.getStatusCode()) {
                            throw new ConflictException(serviceError);
                        } else if (responseCode == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                            throw new ServerException(serviceError);
                        }
                        throw new ServerException(serviceError);
                    }
                }
                // Can't parse content as json or content has format other we expect for error.
                throw new IOException(String.format("Failed access: %s, method: %s, response code: %d, message: %s",
                                                    UriBuilder.fromUri(url).replaceQuery("token").build(), method, responseCode, str));
            }
            final String contentType = conn.getContentType();
            if (!(contentType == null || contentType.startsWith("application/json"))) {
                throw new IOException("Unsupported type of response from remote server, 'application/json' expected. ");
            }

            return CharStreams.toString(new InputSupplier<Reader>() {
                @Override
                public Reader getInput() throws IOException {
                    return new InputStreamReader(conn.getInputStream());
                }
            });
        } finally {
            conn.disconnect();
        }
    }

    private static String getAuthenticationToken() {
        User user = EnvironmentContext.getCurrent().getUser();
        if (user != null) {
            return user.getToken();
        }
        return null;
    }

    /**
     * Sends HTTP request to specified {@code url}.
     * <p/>
     * <p/>
     * type of expected response. If server returns some content we try parse it and restore object of the specified type from it.
     * Specified interface must be annotated with &#064DTO.
     *
     * @param url
     *         URL to send request
     * @param method
     *         HTTP method
     * @param body
     *         body of request. Object must implements DTO interface (interface must be annotated with &#064DTO).
     * @param parameters
     *         additional query parameters.
     * @return instance of {@code dtoInterface} which represents JSON response from the server
     * @throws ServerException
     *         if server returns error response in supported JSON format, see {@link ServiceError}
     * @throws IOException
     *         if any other error occurs
     * @see com.codenvy.dto.shared.DTO
     */
    public static <DTO> DTO request(Class<DTO> dtoInterface,
                                    String url,
                                    String method,
                                    Object body,
                                    Pair<String, ?>... parameters)
            throws IOException, ServerException, UnauthorizedException, ForbiddenException, NotFoundException, ConflictException {
        final String str = requestString(url, method, body, parameters);
        if (dtoInterface != null) {
            return DtoFactory.getInstance().createDtoFromJson(str, dtoInterface);
        }
        return null;

    }

    public static <DTO> List<DTO> requestArray(Class<DTO> dtoInterface,
                                               String url,
                                               String method,
                                               Object body,
                                               Pair<String, ?>... parameters)
            throws IOException, ServerException, UnauthorizedException, ForbiddenException, NotFoundException, ConflictException {
        final String str = requestString(url, method, body, parameters);
        if (dtoInterface != null) {
            return DtoFactory.getInstance().createListDtoFromJson(str, dtoInterface);
        }
        return null;
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
     * @throws ServerException
     *         if server returns error response in supported JSON format, see {@link ServiceError}
     * @throws IOException
     *         if any other error occurs
     * @see com.codenvy.dto.shared.DTO
     */
    public static <DTO> DTO get(Class<DTO> dtoInterface, String url, Pair<String, ?>... parameters)
            throws IOException, ServerException, NotFoundException, ForbiddenException, UnauthorizedException, ConflictException {
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
     * @throws ServerException
     *         if server returns error response in supported JSON format, see {@link ServiceError}
     * @throws IOException
     *         if any other error occurs
     * @see com.codenvy.dto.shared.DTO
     */
    public static <DTO> DTO post(Class<DTO> dtoInterface, String url, Object body, Pair<String, ?>... parameters)
            throws IOException, ServerException, NotFoundException, ForbiddenException, UnauthorizedException, ConflictException {
        return request(dtoInterface, url, "POST", body, parameters);
    }

    /**
     * Sends PUT request to specified {@code url}.
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
     * @throws ServerException
     *         if server returns error response in supported JSON format, see {@link ServiceError}
     * @throws IOException
     *         if any other error occurs
     * @see com.codenvy.dto.shared.DTO
     */
    public static <DTO> DTO put(Class<DTO> dtoInterface, String url, Object body, Pair<String, ?>... parameters)
            throws IOException, ServerException, NotFoundException, ForbiddenException, UnauthorizedException, ConflictException {
        return request(dtoInterface, url, "PUT", body, parameters);
    }

    /**
     * Sends OPTIONS request to specified {@code url}.
     *
     * @param dtoInterface
     *         type of expected response. If server returns some content we try parse it and restore object of the specified type from it.
     *         Specified interface must be annotated with &#064DTO.
     * @param url
     *         URL to send request
     * @param parameters
     *         additional query parameters.
     * @return instance of {@code dtoInterface} which represents JSON response from the server
     * @throws ServerException
     *         if server returns error response in supported JSON format, see {@link ServiceError}
     * @throws IOException
     *         if any other error occurs
     * @see com.codenvy.dto.shared.DTO
     */
    public static <DTO> DTO options(Class<DTO> dtoInterface, String url, Pair<String, ?>... parameters)
            throws IOException, ServerException, NotFoundException, ForbiddenException, UnauthorizedException, ConflictException {
        return request(dtoInterface, url, "OPTIONS", null, parameters);
    }

    /**
     * Sends DELETE request to specified {@code url}.
     *
     * @param dtoInterface
     *         type of expected response. If server returns some content we try parse it and restore object of the specified type from it.
     *         Specified interface must be annotated with &#064DTO.
     * @param url
     *         URL to send request
     * @param parameters
     *         additional query parameters.
     * @return instance of {@code dtoInterface} which represents JSON response from the server
     * @throws ServerException
     *         if server returns error response in supported JSON format, see {@link ServiceError}
     * @throws IOException
     *         if any other error occurs
     * @see com.codenvy.dto.shared.DTO
     */
    public static <DTO> DTO delete(Class<DTO> dtoInterface, String url, Pair<String, ?>... parameters)
            throws IOException, ServerException, NotFoundException, ForbiddenException, UnauthorizedException, ConflictException {
        return request(dtoInterface, url, "DELETE", null, parameters);
    }

    private HttpJsonHelper() {
    }
}
