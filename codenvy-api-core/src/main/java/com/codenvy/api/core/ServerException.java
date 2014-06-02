package com.codenvy.api.core;

import com.codenvy.api.core.rest.shared.dto.ServiceError;

/** @author andrew00x */
@SuppressWarnings("serial")
public class ServerException extends ApiException {

    public ServerException(String message) {
        super(message);
    }

    public ServerException(ServiceError serviceError) {
        super(serviceError);
    }

    public ServerException(Throwable cause) {
        super(cause);
    }

    public ServerException(String message, Throwable cause) {
        super(message, cause);
    }
}
