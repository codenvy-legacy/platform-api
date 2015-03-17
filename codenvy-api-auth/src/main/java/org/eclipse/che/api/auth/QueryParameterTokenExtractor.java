/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2015] Codenvy, S.A. 
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
package org.eclipse.che.api.auth;

import javax.servlet.http.HttpServletRequest;

/**
 * Extract authentication token from query parameter 'token'.
 *
 * @author Sergii Kabashniuk
 */
public class QueryParameterTokenExtractor implements TokenExtractor {

    @Override
    public String getToken(HttpServletRequest req) {
        String query = req.getQueryString();
        if (query != null) {
            int start = query.indexOf("&token=");
            if (start != -1 || query.startsWith("token=")) {
                int end = query.indexOf('&', start + 7);
                if (end == -1) {
                    end = query.length();
                }
                if (end != start + 7) {
                    return query.substring(start + 7, end);
                }
            }
        }
        return null;
    }
}
