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
package com.codenvy.api.analytics.dto;

import com.codenvy.dto.shared.DTO;

/**
 * Simple interface to contain metric info.
 *
 * @author <a href="mailto:dkuleshov@codenvy.com">Dmitry Kuleshov</a>
 */
@DTO
public interface MetricRolesAllowedDTO {
    String getRolesAllowed();

    void setRolesAllowed(String rolesAllowed);
}

