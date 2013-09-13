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

import com.codenvy.commons.json.JsonHelper;
import com.codenvy.commons.json.JsonParseException;
import com.codenvy.commons.lang.IoUtil;

import java.io.IOException;
import java.io.InputStream;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public class JsonDto {
    public static JsonDto wrap(Object o) {
        if (o == null) {
            throw new DtoException("Null object is not allowed.");
        }
        final DtoType dtoType = o.getClass().getAnnotation(DtoType.class);
        if (dtoType == null) {
            throw new DtoException(String.format("Type '%s' it is not annotated with '%s'", o.getClass(), DtoType.class));
        }
        return new JsonDto(dtoType.value(), JsonHelper.toJson(o));
    }

    public static JsonDto create(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        try {
            return JsonHelper.fromJson(content, JsonDto.class, null);
        } catch (JsonParseException e) {
            throw new DtoException(e);
        }
    }

    public static JsonDto create(InputStream content) {
        try {
            return create(IoUtil.readAndCloseQuietly(content));
        } catch (IOException e) {
            throw new DtoException(e);
        }
    }

    //

    private int    type;
    private String data;

    public JsonDto(int type, String data) {
        this.type = type;
        this.data = data;
    }

    public JsonDto() {
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @SuppressWarnings("unchecked")
    public <T> T cast() {
        if (data == null) {
            return null;
        }
        final Class<?> dtoType = DtoTypesRegistry.getDto(type);
        if (dtoType == null) {
            throw new DtoException(String.format("Unknown DTO type '%d'", type));
        }
        try {
            return (T)JsonHelper.fromJson(data, dtoType, null);
        } catch (JsonParseException e) {
            throw new DtoException(e);
        }
    }
}
