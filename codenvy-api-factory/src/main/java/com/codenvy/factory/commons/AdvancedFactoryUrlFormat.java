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

import com.codenvy.commons.lang.UrlUtils;
import com.codenvy.factory.client.FactoryClient;
import com.codenvy.factory.client.impl.HttpFactoryClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * Advanced version of <code>FactoryUrlFormat</code>.
 * This implementation suggest that factory url contain version and id
 */
public class AdvancedFactoryUrlFormat implements FactoryUrlFormat {
    private static final Logger LOG = LoggerFactory.getLogger(AdvancedFactoryUrlFormat.class);
    // client for retrieving factory parameters from storage
    private final FactoryClient factoryClient;

    public AdvancedFactoryUrlFormat() {
        this.factoryClient = new HttpFactoryClient();
    }

    AdvancedFactoryUrlFormat(FactoryClient factoryClient) {
        this.factoryClient = factoryClient;
    }

    @Override
    public AdvancedFactoryUrl parse(URL url) throws FactoryUrlException {
//        String[] factoryUrlParts = url.getPath().split("-");
//        if (factoryUrlParts.length < 2) {
//            throw new FactoryUrlInvalidFormatException(SimpleFactoryUrlFormat.DEFAULT_MESSAGE);
//        }
        try {
            String factoryId = null;
            List<String> values =  UrlUtils.getQueryParameters(url).get("id");
            if (values != null)
                factoryId = values.get(0);
            else
                throw new FactoryUrlInvalidFormatException(SimpleFactoryUrlFormat.DEFAULT_MESSAGE);
            AdvancedFactoryUrl factoryUrl = factoryClient.getFactory(url, factoryId);

            if (factoryUrl == null) {
                LOG.error("Can't find factory with id {}", factoryId);
                throw new FactoryUrlInvalidArgumentException(SimpleFactoryUrlFormat.DEFAULT_MESSAGE);
            }

            // check mandatory parameters
            if (!"1.1".equals(factoryUrl.getVersion())) {
                throw new FactoryUrlInvalidFormatException(SimpleFactoryUrlFormat.DEFAULT_MESSAGE);
            }
            // check that vcs value is correct (only git is supported for now)
            if (!"git".equals(factoryUrl.getVcs())) {
                throw new FactoryUrlInvalidArgumentException(
                        "Parameter vcs has illegal value. Only \"git\" is supported for now.");
            }
            if (factoryUrl.getVcsUrl() == null || factoryUrl.getVcsUrl().isEmpty()) {
                throw new FactoryUrlInvalidArgumentException(SimpleFactoryUrlFormat.DEFAULT_MESSAGE);
            }
            if (factoryUrl.getCommitId() == null || factoryUrl.getCommitId().isEmpty()) {
                throw new FactoryUrlInvalidArgumentException(SimpleFactoryUrlFormat.DEFAULT_MESSAGE);
            }

            SimpleFactoryUrlFormat.checkRepository(factoryUrl.getVcsUrl());

            return factoryUrl;
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw new FactoryUrlException("We cannot locate your project. Please try again or contact us.");
        }
    }

}
