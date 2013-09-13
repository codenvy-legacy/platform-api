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
package com.codenvy.api.core.rest.dto;

import com.codenvy.api.core.rest.dto.DtoType;
import com.codenvy.api.core.rest.dto.DtoTypes;

/**
 * Describes error which may be serialized to JSON format with {@link com.codenvy.api.core.rest.ApiExceptionMapper}
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @see com.codenvy.api.core.rest.ApiException
 * @see com.codenvy.api.core.rest.ApiExceptionMapper
 */
@DtoType(DtoTypes.SERVICE_ERROR_TYPE)
public class ServiceError {
    /** Error message. */
    private String message;
    /** Error code. Code should help client to understand type of error. */
    private int    code;

    public ServiceError(String message, int code) {
        this.message = message;
        this.code = code;
    }

    public ServiceError() {
    }

    /**
     * Get error message.
     *
     * @return error message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Set error message.
     *
     * @param message
     *         error message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Get error code. Code should help client to understand type of error.
     *
     * @return error code
     */
    public int getCode() {
        return code;
    }

    /**
     * Set error code. Code should help client to understand type of error.
     *
     * @param code
     *         error code
     */
    public void setCode(int code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return "ServiceError{" +
               "message='" + message + '\'' +
               ", code=" + code +
               '}';
    }
}
