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

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

/** @author andrew00x */
public final class HttpServletProxyResponse implements HttpOutputProvider {
    private final HttpServletResponse httpServletResponse;

    public HttpServletProxyResponse(HttpServletResponse httpServletResponse) {
        this.httpServletResponse = httpServletResponse;
    }

    @Override
    public void setStatus(int status) {
        httpServletResponse.setStatus(status);
    }

    @Override
    public void addHttpHeader(String name, String value) {
        httpServletResponse.addHeader(name, value);
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return httpServletResponse.getOutputStream();
    }
}
