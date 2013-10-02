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
package com.codenvy.api.core.rest.shared.dto;

import com.codenvy.dto.shared.DTO;

/**
 * Describes error which may be serialized to JSON format with {@link com.codenvy.api.core.rest.ApiExceptionMapper}
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @see com.codenvy.api.core.ApiException
 * @see com.codenvy.api.core.rest.ApiExceptionMapper
 */
@DTO
public interface ServiceError {
    /**
     * Get error message.
     *
     * @return error message
     */
    String getMessage();

    /**
     * Set error message.
     *
     * @param message
     *         error message
     */
    void setMessage(String message);
}
