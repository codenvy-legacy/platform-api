package com.codenvy.api.core;

import com.codenvy.api.core.rest.shared.dto.ServiceError;

/** @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a> */
@SuppressWarnings("serial")
public final class NotFoundException extends ApiException {
    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(ServiceError serviceError) {
        super(serviceError);
    }
}
