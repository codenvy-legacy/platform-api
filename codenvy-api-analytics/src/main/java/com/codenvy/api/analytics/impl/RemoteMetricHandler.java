/*
 *
 * CODENVY CONFIDENTIAL
 * ________________
 *
 * [2012] - [2013] Codenvy, S.A.
 * All Rights Reserved.
 * NOTICE: All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any. The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */

package com.codenvy.api.analytics.impl;

import com.codenvy.api.analytics.AnalyticsService;
import com.codenvy.api.analytics.MetricHandler;
import com.codenvy.api.analytics.dto.Constants;
import com.codenvy.api.analytics.dto.MetricInfoDTO;
import com.codenvy.api.analytics.dto.MetricInfoListDTO;
import com.codenvy.api.analytics.dto.MetricValueDTO;
import com.codenvy.api.analytics.exception.MetricNotFoundException;
import com.codenvy.api.core.rest.RemoteException;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.rest.shared.dto.ServiceError;
import com.codenvy.api.core.util.Pair;
import com.codenvy.commons.lang.IoUtil;
import com.codenvy.dto.server.DtoFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Implementation provides means to perform remote REST requests to receive analytics data from remote rest service.
 *
 * @author <a href="mailto:dkuleshov@codenvy.com">Dmitry Kuleshov</a>
 */
public class RemoteMetricHandler implements MetricHandler {
    private static final Logger LOG = LoggerFactory.getLogger(AnalyticsService.class);

    private static final String BASE_NAME = RemoteMetricHandler.class.getName();
    private static final String LOGIN     = BASE_NAME + ".login";
    private static final String PASS      = BASE_NAME + ".pass";
    private static final String HOST      = BASE_NAME + ".host";
    private static final String PORT      = BASE_NAME + ".port";

    private String login;
    private String pass;
    private String host;
    private String port;

    public RemoteMetricHandler(Properties properties) {
        this.login = properties.getProperty(LOGIN);
        if (this.login == null) {
            throw new IllegalArgumentException("Not defined mandatory property " + LOGIN);
        }
        this.pass = properties.getProperty(PASS);
        if (this.pass == null) {
            throw new IllegalArgumentException("Not defined mandatory property " + PASS);
        }
        this.host = properties.getProperty(HOST);
        if (this.host == null) {
            throw new IllegalArgumentException("Not defined mandatory property " + HOST);
        }
        this.port = properties.getProperty(PORT);
        if (this.port == null) {
            throw new IllegalArgumentException("Not defined mandatory property " + PORT);
        }
    }

    public MetricValueDTO getValue(String metricName,
                                   Map<String, String> executionContext,
                                   UriInfo uriInfo) throws MetricNotFoundException {
        URI redirectURI = getUriBuilder(uriInfo, "getValue").build(metricName, "name");
        try {
            List<Pair<String, String>> pairs = mapToParisList(executionContext);
            return request(MetricValueDTO.class,
                           redirectURI.toString(),
                           "GET",
                           null,
                           pairs.toArray(new Pair[pairs.size()]));
        } catch (IOException | RemoteException e) {
            throw new MetricNotFoundException();
        }
    }

    public MetricInfoDTO getInfo(String metricName, UriInfo uriInfo) throws MetricNotFoundException {
        URI redirectURI = getUriBuilder(uriInfo, "getInfo").build(metricName, "name");
        try {
            MetricInfoDTO metricInfoDTO = request(MetricInfoDTO.class, redirectURI.toString(), "GET", null);
            updateLinks(uriInfo, metricInfoDTO);
            return metricInfoDTO;
        } catch (IOException | RemoteException e) {
            throw new MetricNotFoundException();
        }
    }

    public MetricInfoListDTO getAllInfo(UriInfo uriInfo) {
        URI redirectURI = getUriBuilder(uriInfo, "getAllInfo").build();
        try {
            MetricInfoListDTO metricInfoListDTO = request(MetricInfoListDTO.class, redirectURI.toString(), "GET", null);
            updateLinks(uriInfo, metricInfoListDTO);
            return metricInfoListDTO;
        } catch (IOException | RemoteException e) {
            throw new RuntimeException("Can't get generate metric info list!");
        }
    }

    private void updateLinks(UriInfo uriInfo, MetricInfoDTO metricInfoDTO) {
        metricInfoDTO.setLinks(getLinks(metricInfoDTO.getName(), uriInfo));
    }

    private void updateLinks(UriInfo uriInfo, MetricInfoListDTO metricInfoListDTO) {
        for (MetricInfoDTO metricInfoDTO : metricInfoListDTO.getMetrics()) {
            updateLinks(uriInfo, metricInfoDTO);
        }
    }

    private List<Pair<String, String>> mapToParisList(Map<String, String> executionContext) {
        List<Pair<String, String>> pairs = new ArrayList<>();
        for (Map.Entry<String, String> entry : executionContext.entrySet()) {
            pairs.add(new Pair(entry.getKey(), entry.getValue()));
        }
        return pairs;
    }

    private UriBuilder getUriBuilder(UriInfo uriInfo, String methodName) {
        return uriInfo.getBaseUriBuilder()
                      .clone()
                      .host(host)
                      .port(new Integer(port))
                      .scheme("http")
                      .path("analytics")
                      .path(getMethod(methodName));
    }

    private <DTO> DTO request(Class<DTO> dtoInterface,
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
                String value = parameters[i].second == null ? null : URLEncoder
                        .encode(String.valueOf(parameters[i].second), "UTF-8");
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
//        conn.addRequestProperty("Authorization", "Basic " + new Base64().encode((login + ":" + pass).getBytes()));
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
                if (contentType.startsWith("application/json")) {
                    final ServiceError serviceError =
                            DtoFactory.getInstance().createDtoFromJson(str, ServiceError.class);
                    if (serviceError.getMessage() != null) {
                        // Error is in format what we can understand.
                        throw new RemoteException(serviceError);
                    }
                }
                // Can't parse content as json or content has format other we expect for error.
                throw new IOException(
                        String.format("Failed access: %s, method: %s, response code: %d, message: %s", url, method,
                                      responseCode, str));
            }
            final String contentType = conn.getContentType();
            if (!contentType.startsWith("application/json")) {
                throw new IOException("Unsupported type of response from remote server. ");
            }
            try (InputStream input = conn.getInputStream()) {
                return DtoFactory.getInstance().createDtoFromJson(input, dtoInterface);
            }
        } finally {
            conn.disconnect();
        }
    }

    private static List<Link> getLinks(String metricName, UriInfo uriInfo) {
        final UriBuilder servicePathBuilder = uriInfo.getBaseUriBuilder();
        List<Link> links = new ArrayList<>();

        final Link statusLink = DtoFactory.getInstance().createDto(Link.class);
        statusLink.setRel(Constants.LINK_REL_GET_METRIC_VALUE);
        statusLink.setHref(servicePathBuilder.clone().path(getMethod("getValue")).build(metricName, "name").toString());
        statusLink.setMethod("GET");
        statusLink.setProduces(MediaType.APPLICATION_JSON);
        links.add(statusLink);
        return links;
    }

    private static Method getMethod(String name) {
        for (Method analyticsMethod : AnalyticsService.class.getMethods()) {
            if (analyticsMethod.getName().equals(name)) {
                return analyticsMethod;
            }
        }

        throw new RuntimeException("No '" + name + "' method found in " + AnalyticsService.class + "class");
    }
}
