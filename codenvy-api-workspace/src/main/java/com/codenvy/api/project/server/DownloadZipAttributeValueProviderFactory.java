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
package com.codenvy.api.project.server;

import com.codenvy.api.project.shared.AttributeValueProvider;
import com.codenvy.api.vfs.shared.dto.Project;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides URL for download project as zip bundle.
 *
 * @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a>
 */
public final class DownloadZipAttributeValueProviderFactory extends AttributeValueProviderFactory {
    public static final String ATTRIBUTE = "sources_download_url";

    @Override
    public String getName() {
        return ATTRIBUTE;
    }

    @Override
    public AttributeValueProvider newInstance(final Project project) {
        return new AttributeValueProvider() {
            @Override
            public List<String> getValues() {
                final List<String> list = new ArrayList<>(1);
                list.add(project.getLinks().get(com.codenvy.api.vfs.shared.dto.Link.REL_EXPORT).getHref());
                return list;
            }

            @Override
            public void setValues(List<String> value) {
                // Nothing to do. Value is not persistent.
            }
        };
    }
}
