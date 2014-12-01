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

import com.codenvy.commons.lang.Pair;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** @author andrew00x */
public final class HttpServletProxyResponse implements HttpOutputMessage {
    private final HttpServletResponse        httpServletResponse;
    private final List<Pattern>              mimeTypePatterns;
    private final List<Pair<String, String>> rewriteRules;
    private       String                     contentType;

    public HttpServletProxyResponse(HttpServletResponse httpServletResponse) {
        this.httpServletResponse = httpServletResponse;
        mimeTypePatterns = Collections.emptyList();
        rewriteRules = Collections.emptyList();
    }

    public HttpServletProxyResponse(HttpServletResponse httpServletResponse,
                                    List<Pattern> mimeTypePatterns,
                                    List<Pair<String, String>> rewriteRules) {
        this.rewriteRules = rewriteRules;
        this.mimeTypePatterns = mimeTypePatterns;
        this.httpServletResponse = httpServletResponse;
    }

    @Override
    public void setStatus(int status) {
        httpServletResponse.setStatus(status);
    }

    @Override
    public void setContentType(String contentType) {
        setHttpHeader("Content-Type", contentType);
    }

    @Override
    public void addHttpHeader(String name, String value) {
        if ("content-type".equals(name.toLowerCase())) {
            contentType = value;
        }
        httpServletResponse.addHeader(name, value);
    }

    @Override
    public void setHttpHeader(String name, String value) {
        if ("content-type".equals(name.toLowerCase())) {
            contentType = value;
        }
        httpServletResponse.setHeader(name, value);
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if (contentType != null) {
            for (Pattern mimeTypePattern : mimeTypePatterns) {
                Matcher matcher = mimeTypePattern.matcher(contentType);
                if (matcher.matches()) {
                    return new CachingOutputStream(httpServletResponse);
                }
            }
        }
        return httpServletResponse.getOutputStream();
    }

    private class CachingOutputStream extends OutputStream {

        final HttpServletResponse httpServletResponse;
        ByteArrayOutputStream cache;
        Writer                writer;

        CachingOutputStream(HttpServletResponse httpServletResponse) {
            this.httpServletResponse = httpServletResponse;
        }

        @Override
        public void write(int b) throws IOException {
            if (cache == null) {
                cache = new ByteArrayOutputStream();
            }
            cache.write(b);
            if (b == '\n' || b == '\r') {
                final String translatedLine = translateLine();
                getWriter().write(translatedLine);
                cache.reset();
            }
        }

        String translateLine() {
            String translatedLine = cache.toString();
            for (Pair<String, String> rewriteRule : rewriteRules) {
                translatedLine = translatedLine.replaceAll(rewriteRule.first, rewriteRule.second);
            }
            return translatedLine;
        }

        Writer getWriter() throws IOException {
            if (writer == null) {
                writer = httpServletResponse.getWriter();
            }
            return writer;
        }

        @Override
        public void flush() throws IOException {
            if (cache != null) {
                final String translatedLine = translateLine();
                final Writer myWriter = getWriter();
                myWriter.write(translatedLine);
                writer.flush();
                cache.reset();
            } else if (writer != null) {
                writer.flush();
            }
        }

        @Override
        public void close() throws IOException {
            flush();
            if (writer != null) {
                writer.close();
            }
        }
    }
}
