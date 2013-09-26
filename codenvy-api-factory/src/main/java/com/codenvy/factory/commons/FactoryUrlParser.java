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
package com.codenvy.factory.commons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;

/** Class to parse factory url parameters and validate them */
public class FactoryUrlParser {
    private static final Logger LOG = LoggerFactory.getLogger(FactoryUrlParser.class);

    protected static final Set<FactoryUrlFormat> factoryUrlFormats = new HashSet<>();

    /** Retrieve FactoryUrlFormat implementations */
    static {
        ServiceLoader<FactoryUrlFormat> availableUrlFormats = ServiceLoader.load(FactoryUrlFormat.class);
        for (FactoryUrlFormat factoryUrlFormat : availableUrlFormats) {
            factoryUrlFormats.add(factoryUrlFormat);
            LOG.info("Used {} as FactoryUrlFormat", factoryUrlFormat.getClass());
        }
        if (factoryUrlFormats.size() == 0) {
            LOG.error("FactoryUrlFormat implementations wasn't found");
            throw new RuntimeException("FactoryUrlFormat implementations wasn't found");
        }
    }

    /**
     * Validate and parse factory url
     *
     * @param factoryUrl
     *         - factory url to parse
     * @throws FactoryUrlInvalidFormatException
     *         - there is no format to validate such url
     * @throws FactoryUrlInvalidArgumentException
     *         - if url satisfy format, but arguments is invalid
     * @throws FactoryUrlException
     *         - if other exceptions occurs
     */
    public static FactoryUrl parse(URL factoryUrl) throws FactoryUrlException {
        FactoryUrl factoryUrlParams;
        for (FactoryUrlFormat factoryUrlFormat : factoryUrlFormats) {
            try {
                factoryUrlParams = factoryUrlFormat.parse(factoryUrl);
                if (factoryUrlParams != null) {
                    return factoryUrlParams;
                }
            } catch (FactoryUrlInvalidFormatException ignored) {
            }
        }
        throw new FactoryUrlInvalidFormatException(SimpleFactoryUrlFormat.DEFAULT_MESSAGE);
    }
}
