package com.codenvy.api.runner;

import com.codenvy.api.core.ServerException;
import com.codenvy.api.core.rest.shared.dto.ServiceError;

/**
 * @author andrew00x
 */
@SuppressWarnings("serial")
public class RunnerException extends ServerException {
    public RunnerException(String message) {
        super(message);
    }

    public RunnerException(ServiceError serviceError) {
        super(serviceError);
    }

    public RunnerException(Throwable cause) {
        super(cause);
    }

    public RunnerException(String message, Throwable cause) {
        super(message, cause);
    }
}
