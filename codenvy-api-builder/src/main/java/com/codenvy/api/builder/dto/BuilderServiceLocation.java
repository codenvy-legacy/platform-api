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
package com.codenvy.api.builder.dto;

import com.codenvy.dto.shared.DTO;

/**
 * Location of {@code Builder} resource.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @see com.codenvy.api.builder.internal.SlaveBuilderService
 */
@DTO
public interface BuilderServiceLocation {
    /**
     * Get URL of this Builder. This URL may be used for direct access to the {@code Builder} functionality.
     *
     * @return resource URL
     */
    String getUrl();

    /**
     * Set URL of this Builder. This URL may be used for direct access to the {@code Builder} functionality.
     *
     * @param url
     *         resource URL
     */
    void setUrl(String url);
}
