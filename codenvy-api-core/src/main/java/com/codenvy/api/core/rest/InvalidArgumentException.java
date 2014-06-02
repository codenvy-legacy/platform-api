package com.codenvy.api.core.rest;

import com.codenvy.api.core.ApiException;
import com.codenvy.api.core.rest.shared.dto.ServiceError;

/** @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a> */
@SuppressWarnings("serial")
public final class InvalidArgumentException extends ApiException {
    public InvalidArgumentException(String message) {
        super(message);
    }

    public InvalidArgumentException(ServiceError serviceError) {
        super(serviceError);
    }
}
