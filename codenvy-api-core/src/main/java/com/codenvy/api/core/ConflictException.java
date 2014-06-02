package com.codenvy.api.core;

import com.codenvy.api.core.rest.shared.dto.ServiceError;

/** @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a> */
@SuppressWarnings("serial")
public final class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(ServiceError serviceError) {
        super(serviceError);
    }
}
