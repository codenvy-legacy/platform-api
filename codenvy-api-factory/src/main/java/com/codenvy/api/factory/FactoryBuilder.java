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
package com.codenvy.api.factory;

import com.codenvy.api.core.ApiException;
import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.factory.FactoryParameter;
import com.codenvy.api.factory.converter.IdCommitConverter;
import com.codenvy.api.factory.converter.LegacyConverter;
import com.codenvy.api.factory.converter.ProjectNameConverter;
import com.codenvy.api.factory.converter.ProjectTypeConverter;
import com.codenvy.api.factory.converter.WorkspaceNameConverter;
import com.codenvy.api.factory.dto.Factory;
import com.codenvy.api.factory.dto.FactoryV1_0;
import com.codenvy.api.factory.dto.FactoryV1_1;
import com.codenvy.api.factory.dto.FactoryV1_2;
import com.codenvy.api.factory.dto.FactoryV2_0;
import com.codenvy.api.factory.dto.Variable;
import com.codenvy.api.project.shared.dto.ImportSourceDescriptor;
import com.codenvy.commons.lang.Strings;
import com.codenvy.commons.lang.URLEncodedUtils;
import com.codenvy.dto.server.DtoFactory;
import com.codenvy.dto.shared.DTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.codenvy.api.core.factory.FactoryParameter.FactoryFormat;
import static com.codenvy.api.core.factory.FactoryParameter.FactoryFormat.ENCODED;
import static com.codenvy.api.core.factory.FactoryParameter.FactoryFormat.NONENCODED;
import static com.codenvy.api.core.factory.FactoryParameter.Obligation;
import static com.codenvy.api.core.factory.FactoryParameter.Version;
import static com.codenvy.api.factory.FactoryConstants.INVALID_PARAMETER_MESSAGE;
import static com.codenvy.api.factory.FactoryConstants.INVALID_VERSION_MESSAGE;
import static com.codenvy.api.factory.FactoryConstants.MISSING_MANDATORY_MESSAGE;
import static com.codenvy.api.factory.FactoryConstants.PARAMETRIZED_ENCODED_ONLY_PARAMETER_MESSAGE;
import static com.codenvy.api.factory.FactoryConstants.PARAMETRIZED_ILLEGAL_PARAMETER_VALUE_MESSAGE;
import static com.codenvy.api.factory.FactoryConstants.PARAMETRIZED_INVALID_PARAMETER_MESSAGE;
import static com.codenvy.api.factory.FactoryConstants.PARAMETRIZED_INVALID_TRACKED_PARAMETER_MESSAGE;
import static com.codenvy.api.factory.FactoryConstants.UNPARSABLE_FACTORY_MESSAGE;
import static java.lang.String.format;

/**
 * Tool to easy convert Factory object to nonencoded version or
 * to json version and vise versa.
 * Also it provides factory parameters compatibility.
 *
 * @author Sergii Kabashniuk
 * @author Alexander Garagatyi
 */
@Singleton
public class FactoryBuilder extends NonEncodedFactoryBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(FactoryService.class);

    /** List contains all possible implementation of factory legacy converters. */
    static final List<LegacyConverter> LEGACY_CONVERTERS;

    static {
        List<LegacyConverter> l = new ArrayList<>(4);
        l.add(new IdCommitConverter());
        l.add(new ProjectNameConverter());
        l.add(new ProjectTypeConverter());
        l.add(new WorkspaceNameConverter());
        LEGACY_CONVERTERS = Collections.unmodifiableList(l);
    }

    private final SourceProjectParametersValidator sourceProjectParametersValidator;

    @Inject
    public FactoryBuilder(SourceProjectParametersValidator sourceProjectParametersValidator) {
        this.sourceProjectParametersValidator = sourceProjectParametersValidator;
    }

    /**
     * Build factory from query string of non-encoded factory URI and validate compatibility.
     *
     * @param uri
     *         - uri with non-encoded factory parameters.
     * @return - Factory object represented by given factory URI.
     */
    public Factory buildEncoded(URI uri) throws ApiException {
        if (uri == null) {
            throw new ConflictException("Passed in invalid query parameters.");
        }
        Map<String, Set<String>> queryParams = URLEncodedUtils.parse(uri, "UTF-8");

        Factory factory = buildDtoObject(queryParams, "", Factory.class);

        // there is unsupported parameters in query
        if (!queryParams.isEmpty()) {
            String nameInvalidParams = queryParams.keySet().iterator().next();
            throw new ConflictException(format(PARAMETRIZED_INVALID_PARAMETER_MESSAGE, nameInvalidParams, factory.getV()));
        } else if (null == factory) {
            throw new ConflictException(MISSING_MANDATORY_MESSAGE);
        }

        checkValid(factory, NONENCODED);
        return factory;
    }

    /**
     * Build factory from json of encoded factory and validate compatibility.
     *
     * @param json
     *         - json Reader from encoded factory.
     * @return - Factory object represented by given factory json.
     */
    public Factory buildEncoded(Reader json) throws IOException, ApiException {
        Factory factory = DtoFactory.getInstance().createDtoFromJson(json, Factory.class);
        checkValid(factory, ENCODED);
        return factory;
    }

    /**
     * Build factory from json of encoded factory and validate compatibility.
     *
     * @param json
     *         - json string from encoded factory.
     * @return - Factory object represented by given factory json.
     */
    public Factory buildEncoded(String json) throws ApiException {
        Factory factory = DtoFactory.getInstance().createDtoFromJson(json, Factory.class);
        checkValid(factory, ENCODED);
        return factory;
    }

    /**
     * Build factory from json of encoded factory and validate compatibility.
     *
     * @param json
     *         - json  InputStream from encoded factory.
     * @return - Factory object represented by given factory json.
     */
    public Factory buildEncoded(InputStream json) throws IOException, ApiException {
        Factory factory = DtoFactory.getInstance().createDtoFromJson(json, Factory.class);
        checkValid(factory, ENCODED);
        return factory;
    }

    /**
     * Validate factory compatibility.
     *
     * @param factory
     *         - factory object to validate
     * @param sourceFormat
     *         - is it encoded factory or not
     * @throws ApiException
     */
    public void checkValid(Factory factory, FactoryFormat sourceFormat) throws ApiException {
        if (null == factory) {
            throw new ConflictException(UNPARSABLE_FACTORY_MESSAGE);
        }
        if (factory.getV() == null) {
            throw new ConflictException(INVALID_VERSION_MESSAGE);
        }

        Version v;
        try {
            v = Version.fromString(factory.getV());
        } catch (IllegalArgumentException e) {
            throw new ConflictException(INVALID_VERSION_MESSAGE);
        }

        String orgid = null;

        Class usedFactoryVersionMethodProvider;
        switch (v) {
            case V1_0:
                usedFactoryVersionMethodProvider = FactoryV1_0.class;
                break;
            case V1_1:
                usedFactoryVersionMethodProvider = FactoryV1_1.class;
                orgid = factory.getOrgid();
                break;
            case V1_2:
                usedFactoryVersionMethodProvider = FactoryV1_2.class;
                orgid = factory.getOrgid();
                break;
            case V2_0:
                usedFactoryVersionMethodProvider = FactoryV2_0.class;
                orgid = factory.getCreator() != null ? factory.getCreator().getAccountId() : null;
                break;
            default:
                throw new ConflictException(INVALID_VERSION_MESSAGE);
        }
        orgid = Strings.emptyToNull(orgid);

        validateCompatibility(factory, Factory.class, usedFactoryVersionMethodProvider, v, sourceFormat, orgid, "");
    }

    /**
     * Convert factory of given version to the latest factory format.
     *
     * @param factory
     *         - given factory.
     * @return - factory in latest format.
     * @throws com.codenvy.api.core.ApiException
     */
    public Factory convertToLatest(Factory factory) throws ApiException {
        Factory resultFactory = DtoFactory.getInstance().clone(factory);
        resultFactory.setV("2.0");
        for (LegacyConverter converter : LEGACY_CONVERTERS) {
            converter.convert(resultFactory);
        }

        return resultFactory;
    }

    /**
     * Convert factory of given version to the V1.2 factory format.
     *
     * @param factory
     *         - given factory.
     * @return - factory in V1_2 format.
     * @throws com.codenvy.api.core.ApiException
     */
    public Factory convertToV1_2(Factory factory) throws ApiException {
        Factory resultFactory = DtoFactory.getInstance().clone(factory);
        resultFactory.setV("1.2");
        for (LegacyConverter converter : LEGACY_CONVERTERS) {
            converter.convertToV1_2(resultFactory);
        }

        return resultFactory;
    }

    /**
     * Validate compatibility of factory parameters.
     *
     * @param object
     *         - object to validate factory parameters
     * @param methodsProvider
     *         - class that provides methods with {@link com.codenvy.api.core.factory.FactoryParameter}
     *         annotations
     * @param allowedMethodsProvider
     *         - class that provides allowed methods
     * @param version
     *         - version of factory
     * @param sourceFormat
     *         - factory format
     * @param orgid
     *         - orgid of a factory
     * @param parentName
     *         - parent parameter queryParameterName
     * @throws com.codenvy.api.core.ApiException
     */
    void validateCompatibility(Object object,
                               Class methodsProvider,
                               Class allowedMethodsProvider,
                               Version version,
                               FactoryFormat sourceFormat,
                               String orgid,
                               String parentName) throws ApiException {
        // get all methods recursively
        for (Method method : methodsProvider.getMethods()) {
            FactoryParameter factoryParameter = method.getAnnotation(FactoryParameter.class);
            // is it factory parameter
            if (factoryParameter != null) {
                String fullName = (parentName.isEmpty() ? "" : (parentName + ".")) + factoryParameter.queryParameterName();
                // check that field is set
                Object parameterValue;
                try {
                    parameterValue = method.invoke(object);
                } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
                    // should never happen
                    LOG.error(e.getLocalizedMessage(), e);
                    throw new ConflictException(INVALID_PARAMETER_MESSAGE);
                }

                // if value is null or empty collection or default value for primitives
                if (ValueHelper.isEmpty(parameterValue)) {
                    // field must not be a mandatory, unless it's ignored or deprecated or doesn't suit to the version
                    if (Obligation.MANDATORY.equals(factoryParameter.obligation()) &&
                        factoryParameter.deprecatedSince().compareTo(version) > 0 &&
                        factoryParameter.ignoredSince().compareTo(version) > 0 &&
                        method.getDeclaringClass().isAssignableFrom(allowedMethodsProvider)) {
                        throw new ConflictException(MISSING_MANDATORY_MESSAGE);
                    }
                } else if (!method.getDeclaringClass().isAssignableFrom(allowedMethodsProvider)) {
                    throw new ConflictException(format(PARAMETRIZED_INVALID_PARAMETER_MESSAGE, fullName, version));
                } else {
                    // is parameter deprecated
                    if (factoryParameter.deprecatedSince().compareTo(version) <= 0) {
                        throw new ConflictException(format(PARAMETRIZED_INVALID_PARAMETER_MESSAGE, fullName, version));
                    }

                    if (factoryParameter.setByServer()) {
                        throw new ConflictException(format(PARAMETRIZED_INVALID_PARAMETER_MESSAGE, fullName, version));
                    }

                    // check that field satisfies format rules
                    if (!FactoryFormat.BOTH.equals(factoryParameter.format()) && !factoryParameter.format().equals(sourceFormat)) {
                        throw new ConflictException(format(PARAMETRIZED_ENCODED_ONLY_PARAMETER_MESSAGE, fullName));
                    }

                    // check tracked-only fields
                    if (null == orgid && factoryParameter.trackedOnly()) {
                        throw new ConflictException(format(PARAMETRIZED_INVALID_TRACKED_PARAMETER_MESSAGE, fullName));
                    }

                    // use recursion if parameter is DTO object
                    if (method.getReturnType().isAnnotationPresent(DTO.class)) {
                        // validate inner objects such Git ot ProjectAttributes
                        validateCompatibility(parameterValue, method.getReturnType(), method.getReturnType(), version, sourceFormat,
                                              orgid, fullName);
                    } else if (Map.class.isAssignableFrom(method.getReturnType())) {
                        Type tp = ((ParameterizedType)method.getGenericReturnType()).getActualTypeArguments()[1];

                        Class secMapParamClass;
                        if (tp instanceof ParameterizedType) {
                            secMapParamClass = (Class)((ParameterizedType)tp).getRawType();
                        } else {
                            secMapParamClass = (Class)tp;
                        }
                        if (String.class.equals(secMapParamClass)) {
                            if (ImportSourceDescriptor.class.equals(methodsProvider)) {
                                sourceProjectParametersValidator.validate((ImportSourceDescriptor)object, version);
                            }
                        } else if (List.class.equals(secMapParamClass)) {
                            // do nothing
                        } else {
                            if (secMapParamClass.isAnnotationPresent(DTO.class)) {
                                Map<Object, Object> map = (Map)parameterValue;
                                for (Map.Entry<Object, Object> entry : map.entrySet()) {
                                    validateCompatibility(entry.getValue(), secMapParamClass, secMapParamClass, version, sourceFormat,
                                                          orgid, fullName + "." + (String)entry.getKey());
                                }
                            } else {
                                throw new RuntimeException("This type of fields is not supported by factory.");
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Build dto object with {@link com.codenvy.api.core.factory.FactoryParameter} annotations on its methods.
     *
     * @param queryParams
     *         - source of parameters to parse
     * @param parentName
     *         - queryParameterName of parent object. Allow provide support of nested parameters such as
     *         projectattributes.pname
     * @param cl
     *         - class of the object to build.
     * @return - built object
     * @throws com.codenvy.api.core.ApiException
     */
    private <T> T buildDtoObject(Map<String, Set<String>> queryParams,
                                 String parentName,
                                 Class<T> cl) throws ApiException {
        T result = DtoFactory.getInstance().createDto(cl);
        boolean returnNull = true;
        // get all methods of object recursively
        for (Method method : cl.getMethods()) {
            FactoryParameter factoryParameter = method.getAnnotation(FactoryParameter.class);
            try {
                if (factoryParameter != null) {
                    final String queryParameterName = factoryParameter.queryParameterName();
                    // define full queryParameterName of parameter to be able retrieving nested parameters
                    String fullName = (parentName.isEmpty() ? "" : parentName + ".") + queryParameterName;
                    Class<?> returnClass = method.getReturnType();

                    if (factoryParameter.format() == FactoryFormat.ENCODED) {
                        if (queryParams.containsKey(fullName)) {
                            throw new ConflictException(format(PARAMETRIZED_ENCODED_ONLY_PARAMETER_MESSAGE, fullName));
                        } else {
                            continue;
                        }
                    }

                    //PrimitiveTypeProducer
                    Object param = null;
                    if (queryParams.containsKey(fullName)) {
                        Set<String> values;
                        if (null == (values = queryParams.remove(fullName)) || values.size() != 1) {
                            throw new ConflictException(format(PARAMETRIZED_ILLEGAL_PARAMETER_VALUE_MESSAGE, fullName,
                                                               null != values ? values.toString() : "null"));
                        }
                        param = ValueHelper.createValue(returnClass, values);
                        if (null == param) {
                            if ("variables".equals(fullName) || "actions.findReplace".equals(fullName)) {
                                try {
                                    param = DtoFactory.getInstance().createListDtoFromJson(values.iterator().next(), Variable.class);
                                } catch (Exception e) {
                                    throw new ConflictException(
                                            format(PARAMETRIZED_ILLEGAL_PARAMETER_VALUE_MESSAGE, fullName, values.toString()));
                                }
                            } else {
                                // should never happen
                                throw new ConflictException(
                                        format(PARAMETRIZED_ILLEGAL_PARAMETER_VALUE_MESSAGE, fullName, values.toString()));
                            }
                        }
                    } else if (returnClass.isAnnotationPresent(DTO.class)) {
                        // use recursion if parameter is DTO object
                        param = buildDtoObject(queryParams, fullName, returnClass);
                    } else if (Map.class.isAssignableFrom(returnClass)) {
                        Type tp = ((ParameterizedType)method.getGenericReturnType()).getActualTypeArguments()[1];

                        Class secMapParamClass;
                        if (tp instanceof ParameterizedType) {
                            secMapParamClass = (Class)((ParameterizedType)tp).getRawType();
                        } else {
                            secMapParamClass = (Class)tp;
                        }
                        String mapEntryPrefix = fullName + ".";
                        Map<String, Object> map;
                        if (Map.class == returnClass) {
                            map = new HashMap<>();
                        } else {
                            map = (Map)returnClass.newInstance();
                        }
                        if (String.class.equals(secMapParamClass)) {
                            for (Map.Entry<String, Set<String>> parameterEntry : queryParams.entrySet()) {
                                if (parameterEntry.getKey().startsWith(mapEntryPrefix)) {
                                    map.put(parameterEntry.getKey().substring(mapEntryPrefix.length()),
                                            parameterEntry.getValue().iterator().next());
                                }
                            }
                            for (String key : map.keySet()) {
                                queryParams.remove(mapEntryPrefix + key);
                            }
                            if (!map.isEmpty()) {
                                param = map;
                            }
                        } else if (List.class.equals(secMapParamClass)) {
                            for (Map.Entry<String, Set<String>> parameterEntry : queryParams.entrySet()) {
                                if (parameterEntry.getKey().startsWith(mapEntryPrefix)) {
                                    map.put(parameterEntry.getKey().substring(mapEntryPrefix.length()),
                                            new ArrayList<>(parameterEntry.getValue()));
                                }
                            }
                            for (String key : map.keySet()) {
                                queryParams.remove(mapEntryPrefix + key);
                            }
                            if (!map.isEmpty()) {
                                param = map;
                            }
                        } else {
                            if (secMapParamClass.isAnnotationPresent(DTO.class)) {
                                final Map<String, Map<String, Set<String>>> dtosQueries = new HashMap<>();
                                for (Map.Entry<String, Set<String>> parameterEntry : queryParams.entrySet()) {
                                    if (parameterEntry.getKey().startsWith(mapEntryPrefix) &&
                                        parameterEntry.getKey().length() > mapEntryPrefix.length()) {
                                        final String currentKey = parameterEntry.getKey().substring(mapEntryPrefix.length());
                                        final int i = currentKey.indexOf('.');
                                        if (i != -1) {
                                            String dtoKey = currentKey.substring(0, i);
                                            Map<String, Set<String>> dtoMap;
                                            if ((dtoMap = dtosQueries.get(dtoKey)) == null) {
                                                dtosQueries.put(dtoKey, dtoMap = new HashMap<>());
                                            }
                                            dtoMap.put(parameterEntry.getKey(), parameterEntry.getValue());
                                        }
                                    }
                                }
                                for (Map.Entry<String, Map<String, Set<String>>> dtoEntry : dtosQueries.entrySet()) {
                                    Object dto = buildDtoObject(queryParams, mapEntryPrefix + dtoEntry.getKey(), secMapParamClass);
                                    map.put(dtoEntry.getKey(), dto);
                                }
                                if (!map.isEmpty()) {
                                    param = map;
                                }
                            }
                        }
                    }
                    if (param != null) {
                        // call appropriate setter to set current parameter
                        String setterMethodName =
                                "set" + Character.toUpperCase(method.getName().substring(3).charAt(0)) + method.getName().substring(4);
                        Method setterMethod = cl.getMethod(setterMethodName, returnClass);
                        setterMethod.invoke(result, param);
                        returnNull = false;
                    }
                }
            } catch (ApiException e) {
                throw e;
            } catch (Exception e) {
                LOG.error(e.getLocalizedMessage(), e);
                throw new ConflictException(
                        "Can't validate '" + (parentName.isEmpty() ? "" : parentName + ".") + factoryParameter.queryParameterName() +
                        "' parameter."
                );
            }
        }

        return returnNull ? null : result;
    }

    @Override
    protected String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    @Override
    protected String toJson(List<Variable> dto) {
        return DtoFactory.getInstance().toJson(dto);
    }
}
