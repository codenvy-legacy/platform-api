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
package com.codenvy.api.core.rest;

import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.ForbiddenException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.core.UnauthorizedException;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.rest.shared.dto.ServiceError;
import com.codenvy.commons.env.EnvironmentContext;
import com.codenvy.commons.lang.Pair;
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

    /**
     * Implementation  HttpJsonHelper methods.
     */
    private static HttpJsonHelperImpl httpJsonHelperImpl = new HttpJsonHelperImpl();


    //==============================================================
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
        return httpJsonHelperImpl.requestString(url, method, body, parameters);
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
        return httpJsonHelperImpl.request(dtoInterface, url, method, body, parameters);
    }

    public static <DTO> List<DTO> requestArray(Class<DTO> dtoInterface,
                                               String url,
                                               String method,
                                               Object body,
                                               Pair<String, ?>... parameters)
            throws IOException, ServerException, UnauthorizedException, ForbiddenException, NotFoundException, ConflictException {
        return httpJsonHelperImpl.requestArray(dtoInterface, url, method, body, parameters);
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

    /**
     * Execute all request from HttpJsonHelper throw single method  requestString.
     */
    public static class HttpJsonHelperImpl {

        public <DTO> DTO request(Class<DTO> dtoInterface,
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

        public <DTO> List<DTO> requestArray(Class<DTO> dtoInterface,
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

        private String getAuthenticationToken() {
            User user = EnvironmentContext.getCurrent().getUser();
            if (user != null) {
                return user.getToken();
            }
            return null;
        }

        public String requestString(String url,
                                    String method,
                                    Object body,
                                    Pair<String, ?>... parameters)
                throws IOException, ServerException, ForbiddenException, NotFoundException, UnauthorizedException, ConflictException {
            final String authToken = getAuthenticationToken();
            if ((parameters != null && parameters.length > 0) || authToken != null) {
                final UriBuilder ub = UriBuilder.fromUri(url);
                if (authToken != null) {
                    ub.queryParam("token", authToken);
                }
                if (parameters != null && parameters.length > 0) {
                    for (Pair<String, ?> parameter : parameters) {
                        String name = URLEncoder.encode(parameter.first, "UTF-8");
                        String value = parameter.second == null ? null : URLEncoder
                                .encode(String.valueOf(parameter.second), "UTF-8");
                        ub.replaceQueryParam(name, value);
                    }
                }
                url = ub.build().toString();
            }
            final HttpURLConnection conn = (HttpURLConnection)new URL(url).openConnection();
            conn.setConnectTimeout(60 * 1000);
            conn.setReadTimeout(60 * 1000);
            try {
                conn.setRequestMethod(method);
                if (body != null) {
                    conn.addRequestProperty("content-type", "application/json");
                    conn.setDoOutput(true);

                    if ("DELETE".equals(method)) { //to avoid jdk bug described here http://bugs.java.com/view_bug.do?bug_id=7157360
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("X-HTTP-Method-Override", "DELETE");
                    }

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
                    throw new IOException("We received an error response from the Codenvy server." +
                                          " Retry the request. If this issue continues, contact. support.");
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
    }
}
