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
package com.codenvy.api.core.rest;

import com.codenvy.api.core.rest.dto.ServiceError;

/**
 * Base class for all API errors.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
@SuppressWarnings("serial")
public class ApiException extends Exception {
    public static final int ERROR_CODE_UNKNOWN_ERROR = 1001;

    private final ServiceError serviceError;

    public ApiException(ServiceError serviceError) {
        super(serviceError == null ? null : serviceError.getMessage());
        this.serviceError = serviceError;
    }

    public ApiException(String message) {
        super(message);
        this.serviceError = new ServiceError(message, ERROR_CODE_UNKNOWN_ERROR);
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
        this.serviceError = new ServiceError(message, ERROR_CODE_UNKNOWN_ERROR);
    }

    public ApiException(Throwable cause) {
        super(cause);
        this.serviceError = new ServiceError(cause.getMessage(), ERROR_CODE_UNKNOWN_ERROR);
    }

    public ServiceError getServiceError() {
        return serviceError;
    }
}
