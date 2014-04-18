package com.codenvy.api.core;

import com.codenvy.api.core.rest.shared.dto.ServiceError;

/** @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a> */
@SuppressWarnings("serial")
public final class ServerException extends ApiException {

    public ServerException(String message) {
        super(message);
    }

    public ServerException(ServiceError serviceError) {
        super(serviceError);
    }

    public ServerException(String message, Throwable cause) {
        super(message, cause);
    }
}
