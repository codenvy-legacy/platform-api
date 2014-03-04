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
import com.codenvy.api.analytics.dto.MetricValueListDTO;
import com.codenvy.api.core.rest.RemoteException;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.util.Pair;
import com.codenvy.commons.lang.IoUtil;
import com.codenvy.dto.server.DtoFactory;

import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Implementation provides means to perform remote REST requests to receive analytics data from remote rest service.
 *
 * @author Dmitry Kuleshov
 * @author Anatoliy Bazko
 */
public class RemoteMetricHandler implements MetricHandler {

    private static final String BASE_NAME = RemoteMetricHandler.class.getName();
    private static final String PROXY_URL = BASE_NAME + ".proxy-url";

    private String proxyUrl;

    public RemoteMetricHandler(Properties properties) {
        this.proxyUrl = properties.getProperty(PROXY_URL);
        if (this.proxyUrl == null) {
            throw new IllegalArgumentException("Not defined mandatory property " + PROXY_URL);
        }
    }

    @Override
    public MetricValueDTO getValue(String metricName,
                                   Map<String, String> executionContext,
                                   UriInfo uriInfo) {
        String proxyUrl = getProxyURL("getValue", metricName);
        try {
            List<Pair<String, String>> pairs = mapToParisList(executionContext);
            return request(MetricValueDTO.class,
                           proxyUrl,
                           "GET",
                           null,
                           pairs.toArray(new Pair[pairs.size()]));
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
    
    @Override
    public MetricValueListDTO getValues(List<String> metricNames,
                                   Map<String, String> executionContext,
                                   UriInfo uriInfo) {
        String proxyUrl = getProxyURL("getValues", "");
        try {
            List<Pair<String, String>> pairs = mapToParisList(executionContext);
            return request(MetricValueListDTO.class,
                           proxyUrl,
                           "POST",
                           metricNames,
                           pairs.toArray(new Pair[pairs.size()]));
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public MetricInfoDTO getInfo(String metricName, UriInfo uriInfo) {
        String proxyUrl = getProxyURL("getInfo", metricName);
        try {
            MetricInfoDTO metricInfoDTO = request(MetricInfoDTO.class, proxyUrl, "GET", null);
            updateLinks(uriInfo, metricInfoDTO);
            return metricInfoDTO;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public MetricInfoListDTO getAllInfo(UriInfo uriInfo) {
        String proxyUrl = getProxyURL("getAllInfo", "");
        try {
            MetricInfoListDTO metricInfoListDTO = request(MetricInfoListDTO.class, proxyUrl, "GET", null);
            updateLinks(uriInfo, metricInfoListDTO);
            return metricInfoListDTO;
        } catch (Exception e) {
            throw new RuntimeException(
                    "We have received an error code from the server. For some reason, " +
                    "we are unable to generate the list of metrics.");
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

    private String getProxyURL(String methodName, String metricName) {
        String path = getMethod(methodName).getAnnotation(Path.class).value();
        return proxyUrl + "/" + path.replace("{name}", metricName);
    }

    private <DTO> DTO request(Class<DTO> dtoInterface,
                              String proxyUrl,
                              String method,
                              Object body,
                              Pair<String, ?>... parameters) throws IOException, RemoteException {

        if (parameters != null && parameters.length > 0) {
            final StringBuilder sb = new StringBuilder();
            sb.append(proxyUrl);
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
            proxyUrl = sb.toString();
        }
        final HttpURLConnection conn = (HttpURLConnection)new URL(proxyUrl).openConnection();
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
                throw new IOException(IoUtil.readAndCloseQuietly(in));
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
        statusLink.setHref(servicePathBuilder
                                   .clone()
                                   .path("analytics")
                                   .path(getMethod("getValue"))
                                   .build(metricName, "name")
                                   .toString());
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
