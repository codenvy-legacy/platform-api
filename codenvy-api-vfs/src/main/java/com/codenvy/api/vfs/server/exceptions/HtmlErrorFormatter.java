/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.vfs.server.exceptions;

import com.codenvy.api.vfs.shared.ExitCodes;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Factory of WebApplicationException that contains error message in HTML format.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
public final class HtmlErrorFormatter {
    public static void sendErrorAsHTML(Exception e) {
        // GWT framework (used on client side) requires result in HTML format if use HTML forms.
        if (e instanceof ItemAlreadyExistException) {
            throw new WebApplicationException(Response.ok(formatAsHtml(e.getMessage(), ExitCodes.ITEM_EXISTS),
                                                          MediaType.TEXT_HTML).build());
        } else if (e instanceof ItemNotFoundException) {
            throw new WebApplicationException(Response.ok(formatAsHtml(e.getMessage(), ExitCodes.ITEM_NOT_FOUND),
                                                          MediaType.TEXT_HTML).build());
        } else if (e instanceof InvalidArgumentException) {
            throw new WebApplicationException(Response.ok(formatAsHtml(e.getMessage(), ExitCodes.INVALID_ARGUMENT),
                                                          MediaType.TEXT_HTML).build());
        } else if (e instanceof ConstraintException) {
            throw new WebApplicationException(Response.ok(formatAsHtml(e.getMessage(), ExitCodes.CONSTRAINT),
                                                          MediaType.TEXT_HTML).build());
        } else if (e instanceof PermissionDeniedException) {
            throw new WebApplicationException(Response.ok(formatAsHtml(e.getMessage(), ExitCodes.NOT_PERMITTED),
                                                          MediaType.TEXT_HTML).build());
        } else if (e instanceof LockException) {
            throw new WebApplicationException(Response.ok(formatAsHtml(e.getMessage(), ExitCodes.LOCK_CONFLICT),
                                                          MediaType.TEXT_HTML).build());
        }
        throw new WebApplicationException(Response.ok(formatAsHtml(e.getMessage(), ExitCodes.INTERNAL_ERROR),
                                                      MediaType.TEXT_HTML).build());
    }

    private static String formatAsHtml(String message, int exitCode) {
        return String.format("<pre>Code: %d Text: %s</pre>", exitCode, message);
    }

    private HtmlErrorFormatter() {
    }
}
