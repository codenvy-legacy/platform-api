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
package com.codenvy.api.core;

import com.codenvy.api.core.rest.shared.dto.ServiceError;
import com.codenvy.dto.server.DtoFactory;

/**
 * Base class for all API errors.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
@SuppressWarnings("serial")
public class ApiException extends Exception {
    private final ServiceError serviceError;

    public ApiException(ServiceError serviceError) {
        super(serviceError.getMessage());
        this.serviceError = serviceError;
    }

    public ApiException(String message) {
        super(message);

        this.serviceError = createError(message);
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
        this.serviceError = createError(message);
    }

    public ApiException(Throwable cause) {
        super(cause);
        this.serviceError = createError(cause.getMessage());
    }

    public ServiceError getServiceError() {
        return serviceError;
    }

    private ServiceError createError(String message) {
        ServiceError dto = DtoFactory.getInstance().createDto(ServiceError.class);
        dto.setMessage(message);
        return dto;
    }
}
