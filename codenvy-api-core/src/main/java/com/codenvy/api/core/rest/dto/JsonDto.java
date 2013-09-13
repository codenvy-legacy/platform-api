/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2013] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.api.core.rest.dto;

import com.codenvy.commons.lang.IoUtil;

import org.everrest.core.impl.provider.json.JsonException;
import org.everrest.core.impl.provider.json.JsonGenerator;
import org.everrest.core.impl.provider.json.JsonParser;
import org.everrest.core.impl.provider.json.JsonValue;
import org.everrest.core.impl.provider.json.JsonWriter;
import org.everrest.core.impl.provider.json.LongValue;
import org.everrest.core.impl.provider.json.ObjectBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public class JsonDto {
    @SuppressWarnings("unchecked")
    public static String toJson(Object o) {
        if (o == null) {
            throw new DtoException("Null object is not allowed.");
        }
        final DtoType dtoType = o.getClass().getAnnotation(DtoType.class);
        if (dtoType == null) {
            throw new DtoException(String.format("Type '%s' it is not annotated with '%s'", o.getClass(), DtoType.class));
        }
        try {
            final JsonValue json = JsonGenerator.createJsonObject(o);
            json.addElement("_type", new LongValue(dtoType.value()));
            final Writer w = new StringWriter();
            json.writeTo(new JsonWriter(w));
            return w.toString();
        } catch (JsonException e) {
            throw new DtoException(e);
        }
    }

    public static JsonDto fromJson(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        try {
            final JsonValue node;
            JsonParser parser = new JsonParser();
            parser.parse(new StringReader(content));
            node = parser.getJsonObject();

            final JsonValue typeNode = node.getElement("_type");
            if (typeNode == null) {
                return null;
            }
            final int type = typeNode.getIntValue();
            final Class<?> dtoType = DtoTypesRegistry.getDto(type);
            if (dtoType == null) {
                throw new DtoException(String.format("Unknown DTO type '%d'", type));
            }
            return new JsonDto(type, ObjectBuilder.createObject(dtoType, node));
        } catch (JsonException e) {
            throw new DtoException(e);
        }
    }

    public static JsonDto fromJson(InputStream content) {
        try {
            return fromJson(IoUtil.readAndCloseQuietly(content));
        } catch (IOException e) {
            throw new DtoException(e);
        }
    }

    //

    private final int    type;
    private final Object value;

    private JsonDto(int type, Object value) {
        this.type = type;
        this.value = value;
    }

    public int getType() {
        return type;
    }

    @SuppressWarnings("unchecked")
    public <T> T cast() {
        return (T)value;
    }
}
