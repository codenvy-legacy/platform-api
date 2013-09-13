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

import com.codenvy.api.core.util.ComponentLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public abstract class DtoTypesRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(DtoTypesRegistry.class);

    private static final ConcurrentMap<Integer, Class<?>> DTOs = new ConcurrentHashMap<>();

    static {
        for (DtoTypesRegistry dtoTypesRegistry : ComponentLoader.all(DtoTypesRegistry.class)) {
            for (Class<?> dtoClass : dtoTypesRegistry.getDtos()) {
                registerDto(dtoClass);
            }
        }
    }

    /**
     * Register new DTO. Specified class must be annotated with annotation {@link DtoType}.
     *
     * @param dto
     *         dto class
     */
    public static void registerDto(Class<?> dto) {
        final DtoType dtoType = dto.getAnnotation(DtoType.class);
        if (dtoType == null) {
            LOG.warn("Ignore type {} it is not annotated with {}", dto, DtoType.class);
        } else {
            final Class<?> registered = DTOs.putIfAbsent(dtoType.value(), dto);
            if (registered != null) {
                final String msg = String.format("Duplicated DTO types: '%s' and '%s' have the same value in annotation '%s'",
                                                 registered, dto, dtoType);
                LOG.error(msg);
                throw new IllegalStateException(msg);
            }
        }
    }

    public static Class<?> getDto(int type) {
        return DTOs.get(type);
    }

    public static boolean unregisterDto(Class<?> dto) {
        return DTOs.values().remove(dto);
    }

    public static Class<?> unregisterDto(int type) {
        return DTOs.remove(type);
    }

    private final Set<Class<?>> dtos;

    protected DtoTypesRegistry() {
        dtos = new LinkedHashSet<>();
        addDtos(dtos);
    }

    protected abstract void addDtos(Set<Class<?>> dtos);

    public Set<Class<?>> getDtos() {
        return dtos;
    }
}
