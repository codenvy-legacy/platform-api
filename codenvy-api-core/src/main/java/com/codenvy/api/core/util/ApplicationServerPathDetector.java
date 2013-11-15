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
package com.codenvy.api.core.util;

/**
 * Helper class that can resolve root directory of application server.
 * NOTE: It may support only limited set of application servers.
 *
 * @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a>
 */
public class ApplicationServerPathDetector {
    /**
     * Find root directory of application server.
     *
     * @return root directory of application server or {@code null} if type of application server is not supported
     */
    public static java.io.File findRoot() {
        final String catalinaHome = System.getProperty("catalina.home");
        if (catalinaHome != null) {
            return new java.io.File(catalinaHome);
        }
        return null;
    }

    private ApplicationServerPathDetector() {
    }
}
