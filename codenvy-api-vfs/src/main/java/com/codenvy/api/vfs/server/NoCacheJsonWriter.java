/*******************************************************************************
* Copyright (c) 2012-2014 Codenvy, S.A.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* Codenvy, S.A. - initial API and implementation
*******************************************************************************/
package com.codenvy.api.vfs.server;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Add Cache-Control response header. For write JSON content use JSON provider embedded in REST framework if any.
 *
 * @author <a href="mailto:aparfonov@exoplatform.com">Andrey Parfonov</a>
 */
@Provider
@Produces({MediaType.APPLICATION_JSON})
public class NoCacheJsonWriter<T> implements MessageBodyWriter<T> {
    @Context
    private Providers providers;

    @SuppressWarnings("rawtypes")
    private MessageBodyWriter writer;

    private static final ThreadLocal<MessageBodyWriter> writerContext = new ThreadLocal<>();

    /**
     * @see javax.ws.rs.ext.MessageBodyWriter#isWriteable(java.lang.Class, java.lang.reflect.Type,
     *      java.lang.annotation.Annotation[], javax.ws.rs.core.MediaType)
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        if (null != writerContext.get()) {
            // Avoid recursively check the same type of writer in current thread.
            // It forces JAX-RS framework look embedded JSON writer if any.
            // If we got such writer then use it for writing body.
            return false;
        } else {
            try {
                writerContext.set(this);
                return null != (writer = providers.getMessageBodyWriter(type, genericType, annotations, mediaType));
            } finally {
                writerContext.remove();
            }
        }
    }

    /**
     * @see javax.ws.rs.ext.MessageBodyWriter#getSize(java.lang.Object, java.lang.Class, java.lang.reflect.Type,
     *      java.lang.annotation.Annotation[], javax.ws.rs.core.MediaType)
     */
    @Override
    public long getSize(T t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    /**
     * @see javax.ws.rs.ext.MessageBodyWriter#writeTo(java.lang.Object, java.lang.Class, java.lang.reflect.Type,
     *      java.lang.annotation.Annotation[], javax.ws.rs.core.MediaType, javax.ws.rs.core.MultivaluedMap,
     *      java.io.OutputStream)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void writeTo(T t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                        MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException,
                                                                                                      WebApplicationException {
        if (writer == null) {
            // Be sure writer available.
            throw new WebApplicationException(
                    Response
                            .status(Response.Status.NOT_ACCEPTABLE)
                            .entity("Not found writer for " + type + " and MIME type " + httpHeaders.getFirst(HttpHeaders.CONTENT_TYPE))
                            .type(MediaType.TEXT_PLAIN).build());
        }

        // Add Cache-Control before start write body.
        httpHeaders.putSingle(HttpHeaders.CACHE_CONTROL, "public, no-cache, no-store, no-transform");

        writer.writeTo(t, type, genericType, annotations, mediaType, httpHeaders, entityStream);
    }
}
