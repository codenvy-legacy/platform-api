package com.codenvy.api.builder;

import com.codenvy.api.core.ServerException;
import com.codenvy.api.core.rest.shared.dto.ServiceError;

/**
 * @author andrew00x
 */
@SuppressWarnings("serial")
public class BuilderException extends ServerException {
    public BuilderException(String message) {
        super(message);
    }

    public BuilderException(ServiceError serviceError) {
        super(serviceError);
    }

    public BuilderException(Throwable cause) {
        super(cause);
    }

    public BuilderException(String message, Throwable cause) {
        super(message, cause);
    }
}
