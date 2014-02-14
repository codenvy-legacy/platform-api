/*
 *
 * CODENVY CONFIDENTIAL
 * ________________
 *
 * [2012] - [2014] Codenvy, S.A.
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
package com.codenvy.api.analytics;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** @author Anatoliy Bazko */
public class Utils {

    private static final Pattern ADMIN_ROLE_EMAIL_PATTERN = Pattern.compile("@codenvy[.]com$");

    /** Extract the execution context from passed query parameters. */
    public static Map<String, String> extractContext(UriInfo info, String page, String perPage) {
        MultivaluedMap<String, String> parameters = info.getQueryParameters();
        Map<String, String> context = new HashMap<>(parameters.size());

        for (String key : parameters.keySet()) {
            context.put(key.toUpperCase(), parameters.getFirst(key));
        }

        if (page != null) {
            context.put("PAGE", page);
            context.put("PER_PAGE", perPage);
        }

        return context;
    }

    /** Extract the execution context from passed query parameters. */
    public static Map<String, String> extractContext(UriInfo info) {
        return extractContext(info, null, null);
    }

    public static boolean isAdmin(String email) {
        Matcher matcher = ADMIN_ROLE_EMAIL_PATTERN.matcher(email);
        return matcher.find();
    }
}
