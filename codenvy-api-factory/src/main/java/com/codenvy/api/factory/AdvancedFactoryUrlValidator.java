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
package com.codenvy.api.factory;

/** Validate {@code AdvancedFactoryUrl} */
public class AdvancedFactoryUrlValidator {
    /**
     * Validate factory url
     *
     * @param factoryUrl
     *         - factory url to validate
     * @throws FactoryUrlException
     *         - if object is not invalid
     */
    public static void validate(AdvancedFactoryUrl factoryUrl) throws FactoryUrlException {

        // check mandatory parameters
        if (!"1.1".equals(factoryUrl.getV())) {
            throw new FactoryUrlException("Version has illegal value. Version must be equal to '1.1'");
        }
        // check that vcs value is correct (only git is supported for now)
        if (!"git".equals(factoryUrl.getVcs())) {
            throw new FactoryUrlException("Parameter vcs has illegal value. Only \"git\" is supported for now.");
        }
        if (factoryUrl.getVcsurl() == null || factoryUrl.getVcsurl().isEmpty()) {
            throw new FactoryUrlException("Vcsurl is null or empty.");
        }
    }


}
