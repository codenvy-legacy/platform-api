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
package com.codenvy.dto.server;

import com.codenvy.commons.lang.cache.Cache;
import com.codenvy.commons.lang.cache.LoadingValueSLRUCache;
import com.codenvy.commons.lang.cache.SynchronizedCache;
import com.codenvy.dto.shared.JsonArray;
import com.codenvy.dto.shared.JsonStringMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides implementations of DTO interfaces.
 *
 * @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a>
 */
public final class DtoFactory {
    private static final Gson gson = new GsonBuilder().serializeNulls().create();

    private static final Cache<Type, ParameterizedType> listTypeCache = new SynchronizedCache<>(
            new LoadingValueSLRUCache<Type, ParameterizedType>(100, 100) {
                @Override
                protected ParameterizedType loadValue(Type type) {
                    return ParameterizedTypeImpl.newParameterizedType(List.class, type);
                }
            });
    private static final Cache<Type, ParameterizedType> mapTypeCache  = new SynchronizedCache<>(
            new LoadingValueSLRUCache<Type, ParameterizedType>(100, 100) {
                @Override
                protected ParameterizedType loadValue(Type type) {
                    return ParameterizedTypeImpl.newParameterizedType(Map.class, String.class, type);
                }
            });


    private static final DtoFactory INSTANCE = new DtoFactory();

    public static DtoFactory getInstance() {
        return INSTANCE;
    }

    private final Map<Class<?>, DtoProvider<?>> providers = new ConcurrentHashMap<>();

    /**
     * Creates new instance of class which implements specified DTO interface.
     *
     * @param dtoInterface
     *         DTO interface
     * @throws IllegalArgumentException
     *         if can't provide any implementation for specified interface
     */
    public <T> T createDto(Class<T> dtoInterface) {
        return getDtoProvider(dtoInterface).newInstance();
    }

    //

    /**
     * Creates new instance of class which implements specified DTO interface, parses specified JSON string and uses parsed data for
     * initializing fields of DTO object.
     *
     * @param json
     *         JSON data
     * @param dtoInterface
     *         DTO interface
     * @throws IllegalArgumentException
     *         if can't provide any implementation for specified interface
     */
    public <T> T createDtoFromJson(String json, Class<T> dtoInterface) {
        return getDtoProvider(dtoInterface).fromJson(json);
    }

    /**
     * Creates new instance of class which implements specified DTO interface, parses specified JSON data and uses parsed data for
     * initializing fields of DTO object.
     *
     * @param json
     *         JSON data
     * @param dtoInterface
     *         DTO interface
     * @throws IllegalArgumentException
     *         if can't provide any implementation for specified interface
     * @throws IOException
     *         if an i/o error occurs
     */
    public <T> T createDtoFromJson(Reader json, Class<T> dtoInterface) throws IOException {
        DtoProvider<T> dtoProvider = getDtoProvider(dtoInterface);
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(json);
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        return dtoProvider.fromJson(sb.toString());
    }

    /**
     * Creates new instance of class which implements specified DTO interface, parses specified JSON data and uses parsed data for
     * initializing fields of DTO object.
     *
     * @param json
     *         JSON data
     * @param dtoInterface
     *         DTO interface
     * @throws IllegalArgumentException
     *         if can't provide any implementation for specified interface
     * @throws IOException
     *         if an i/o error occurs
     */
    public <T> T createDtoFromJson(InputStream json, Class<T> dtoInterface) throws IOException {
        return createDtoFromJson(new InputStreamReader(json), dtoInterface);
    }

    //

    /**
     * Parses the JSON data from the specified sting into list of objects of the specified type.
     *
     * @param json
     *         JSON data
     * @param dtoInterface
     *         DTO interface
     * @return list of DTO
     * @throws IllegalArgumentException
     *         if can't provide any implementation for specified interface
     */
    @SuppressWarnings("unchecked")
    public <T> JsonArray<T> createListDtoFromJson(String json, Class<T> dtoInterface) {
        final DtoProvider<T> dtoProvider = getDtoProvider(dtoInterface);
        return new JsonArrayImpl((List)gson.fromJson(json, listTypeCache.get(dtoProvider.getImplClass())));
    }


    /**
     * Deserializes the JSON data from the specified reader into list of objects of the specified type.
     *
     * @param json
     *         JSON data
     * @param dtoInterface
     *         DTO interface
     * @return list of DTO
     * @throws IllegalArgumentException
     *         if can't provide any implementation for specified interface
     * @throws IOException
     *         if an i/o error occurs
     */
    @SuppressWarnings("unchecked")
    public <T> JsonArray<T> createListDtoFromJson(Reader json, Class<T> dtoInterface) throws IOException {
        final DtoProvider<T> dtoProvider = getDtoProvider(dtoInterface);
        return new JsonArrayImpl((List)gson.fromJson(json, listTypeCache.get(dtoProvider.getImplClass())));
    }

    /**
     * Deserializes the JSON data from the specified stream into list of objects of the specified type.
     *
     * @param json
     *         JSON data
     * @param dtoInterface
     *         DTO interface
     * @return list of DTO
     * @throws IllegalArgumentException
     *         if can't provide any implementation for specified interface
     * @throws IOException
     *         if an i/o error occurs
     */
    public <T> JsonArray<T> createListDtoFromJson(InputStream json, Class<T> dtoInterface) throws IOException {
        return createListDtoFromJson(new InputStreamReader(json), dtoInterface);
    }

    //

    /**
     * Parses the JSON data from the specified sting into map of objects of the specified type.
     *
     * @param json
     *         JSON data
     * @param dtoInterface
     *         DTO interface
     * @return map of DTO
     * @throws IllegalArgumentException
     *         if can't provide any implementation for specified interface
     */
    @SuppressWarnings("unchecked")
    public <T> JsonStringMap<T> createMapDtoFromJson(String json, Class<T> dtoInterface) {
        final DtoProvider<T> dtoProvider = getDtoProvider(dtoInterface);
        return new JsonStringMapImpl((Map)gson.fromJson(json, mapTypeCache.get(dtoProvider.getImplClass())));
    }


    /**
     * Deserializes the JSON data from the specified reader into map of objects of the specified type.
     *
     * @param json
     *         JSON data
     * @param dtoInterface
     *         DTO interface
     * @return map of DTO
     * @throws IllegalArgumentException
     *         if can't provide any implementation for specified interface
     * @throws IOException
     *         if an i/o error occurs
     */
    @SuppressWarnings("unchecked")
    public <T> JsonStringMap<T> createMapDtoFromJson(Reader json, Class<T> dtoInterface) throws IOException {
        final DtoProvider<T> dtoProvider = getDtoProvider(dtoInterface);
        return new JsonStringMapImpl((Map)gson.fromJson(json, mapTypeCache.get(dtoProvider.getImplClass())));
    }

    /**
     * Deserializes the JSON data from the specified stream into map of objects of the specified type.
     *
     * @param json
     *         JSON data
     * @param dtoInterface
     *         DTO interface
     * @return map of DTO
     * @throws IllegalArgumentException
     *         if can't provide any implementation for specified interface
     * @throws IOException
     *         if an i/o error occurs
     */
    public <T> JsonStringMap<T> createMapDtoFromJson(InputStream json, Class<T> dtoInterface) throws IOException {
        return createMapDtoFromJson(new InputStreamReader(json), dtoInterface);
    }

    //

    @SuppressWarnings("unchecked")
    private <T> DtoProvider<T> getDtoProvider(Class<T> dtoInterface) {
        DtoProvider<?> dtoProvider = providers.get(dtoInterface);
        if (dtoProvider == null) {
            throw new IllegalArgumentException("Unknown DTO type " + dtoInterface);
        }
        return (DtoProvider<T>)dtoProvider;
    }

    /**
     * Registers DtoProvider for DTO interface.
     *
     * @param dtoInterface
     *         DTO interface
     * @param provider
     *         provider for DTO interface
     * @see DtoProvider
     */
    public void registerProvider(Class<?> dtoInterface, DtoProvider<?> provider) {
        providers.put(dtoInterface, provider);
    }

    /**
     * Unregisters DtoProvider.
     *
     * @see #registerProvider(Class, DtoProvider)
     */
    public DtoProvider<?> unregisterProvider(Class<?> dtoInterface) {
        return providers.remove(dtoInterface);
    }

    /** Test weather or not this DtoFactory has any DtoProvider which can provide implementation of DTO interface. */
    public boolean hasProvider(Class<?> dtoInterface) {
        return providers.get(dtoInterface) != null;
    }

    static {
        for (DtoFactoryVisitor visitor : ServiceLoader.load(DtoFactoryVisitor.class)) {
            visitor.accept(INSTANCE);
        }
    }

    private DtoFactory() {
    }
}
