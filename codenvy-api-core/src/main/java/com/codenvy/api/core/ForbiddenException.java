package com.codenvy.api.core;

import com.codenvy.api.core.rest.shared.dto.ServiceError;

/** @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a> */
@SuppressWarnings("serial")
public final class ForbiddenException extends ApiException {
    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(ServiceError serviceError) {
        super(serviceError);
    }
}
