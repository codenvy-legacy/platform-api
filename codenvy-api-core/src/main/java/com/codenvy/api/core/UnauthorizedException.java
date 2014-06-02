package com.codenvy.api.core;

import com.codenvy.api.core.rest.shared.dto.ServiceError;

/** @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a> */
@SuppressWarnings("serial")
public final class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(ServiceError serviceError) {
        super(serviceError);
    }
}
