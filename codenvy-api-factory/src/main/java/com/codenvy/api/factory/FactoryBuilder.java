/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2014] Codenvy, S.A. 
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
package com.codenvy.api.factory;

import com.codenvy.api.factory.dto.*;
import com.codenvy.api.factory.parameter.*;
import com.codenvy.commons.lang.URLEncodedUtils;
import com.codenvy.dto.server.DtoFactory;
import com.codenvy.dto.shared.DTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import java.util.*;

import static com.codenvy.api.factory.parameter.FactoryParameter.*;
import static com.codenvy.api.factory.parameter.FactoryParameter.Format.ENCODED;
import static com.codenvy.api.factory.parameter.FactoryParameter.Format.NONENCODED;
import static com.codenvy.api.factory.parameter.FactoryParameterConverter.DefaultFactoryParameterConverter;

/**
 * Tool to easy convert Factory object to nonencoded version or
 * to json version and vise versa.
 * Also it provides factory parameters compatibility.
 *
 * @author Sergii Kabashniuk
 * @author Alexander Garagatyi
 */
@Singleton
public class FactoryBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(FactoryService.class);

    @Inject
    private IgnoreConverter ignoreConverter;

    private void buildNonEncoded(FactoryV1_0 factory, StringBuilder builder) {
        builder.append("v=").append(factory.getV());
        builder.append("&vcs=").append(factory.getVcs());
        builder.append("&vcsurl=").append(encode(factory.getVcsurl()));
        if (factory.getCommitid() != null) {
            builder.append("&commitid=").append(factory.getCommitid());
        }
        if (factory.getIdcommit() != null && factory.getCommitid() == null) {
            builder.append("&commitid=").append(factory.getIdcommit());
        }
        if (factory.getPname() != null) {
            builder.append("&pname=").append(factory.getPname());
        }
        if (factory.getPtype() != null) {
            builder.append("&ptype=").append(factory.getPtype());
        }

        if (factory.getAction() != null) {
            builder.append("&action=").append(factory.getAction());
        }

        if (factory.getWname() != null) {
            builder.append("&wname=").append(factory.getWname());
        }

    }

    private void buildNonEncoded(FactoryV1_1 factory, StringBuilder builder) {
        buildNonEncoded(((FactoryV1_0)factory), builder);
        ProjectAttributes projectattributes = factory.getProjectattributes();
        if (projectattributes != null) {
            if (projectattributes.getPname() != null) {
                builder.append("&projectattributes.pname=").append(projectattributes.getPname());
            }

            if (projectattributes.getPtype() != null) {
                builder.append("&projectattributes.ptype=").append(projectattributes.getPtype());
            }
        }

        if (factory.getContactmail() != null) {
            builder.append("&contactmail=").append(factory.getContactmail());
        }

        if (factory.getAuthor() != null) {
            builder.append("&author=").append(factory.getAuthor());
        }

        if (factory.getOpenfile() != null) {
            builder.append("&openfile=").append(factory.getOpenfile());
        }

        if (factory.getOrgid() != null) {
            builder.append("&orgid=").append(factory.getOrgid());
        }

        if (factory.getAffiliateid() != null) {
            builder.append("&affiliateid=").append(factory.getAffiliateid());
        }
        if (factory.getVcsinfo()) {
            builder.append("&vcsinfo=true");
        }
        if (factory.getVcsbranch() != null) {
            builder.append("&vcsbranch=").append(factory.getVcsbranch());
        }
        if (factory.getVariables() != null) {
            builder.append("&variables=").append(encode(DtoFactory.getInstance().toJson(factory.getVariables())));
        }

    }

    private void buildNonEncoded(FactoryV1_2 factory, StringBuilder builder) {
        buildNonEncoded(((FactoryV1_1)factory), builder);
        Restriction restriction = factory.getRestriction();
        if (restriction != null) {
            if (restriction.getValidsince() > 0) {
                builder.append("&restriction.validsince=").append(restriction.getValidsince());
            }
            if (restriction.getValiduntil() > 0) {
                builder.append("&restriction.validuntil=").append(restriction.getValiduntil());
            }
            if (restriction.getValidsessioncount() > 0) {
                builder.append("&restriction.validsessioncount=").append(restriction.getValidsessioncount());
            }
            if (restriction.getRefererhostname() != null) {
                builder.append("&restriction.refererhostname=").append(restriction.getRefererhostname());
            }

            if (restriction.getPassword() != null) {
                builder.append("&restriction.password=").append(restriction.getPassword());
            }

            if (restriction.getRestrictbypassword() != null) {
                builder.append("&restriction.restrictbypassword=").append(restriction.getRestrictbypassword());
            }
        }
        Git git = factory.getGit();
        if (git != null) {

            if (git.getConfigbranchmerge() != null) {
                builder.append("&git.configbranchmerge=").append(encode(git.getConfigbranchmerge()));
            }

            if (git.getConfigpushdefault() != null) {
                builder.append("&git.configpushdefault=").append(git.getConfigpushdefault());
            }
            if (git.getConfigremoteoriginfetch() != null) {
                builder.append("&git.configremoteoriginfetch=").append(encode(git.getConfigremoteoriginfetch()));
            }
        }

    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    /**
     * Convert factory to nonencoded version.
     *
     * @param factory
     *         - factory object.
     * @return - query part of url of nonencoded version
     */
    public String buildNonEncoded(Factory factory) {
        StringBuilder result = new StringBuilder();
        buildNonEncoded(factory, result);
        return result.toString();
    }

    /**
     * Build factory from query string and validate compatibility.
     *
     * @param queryString
     *         - query string from nonencoded factory.
     * @return - Factory object represented by given factory string.
     */
    public Factory buildNonEncoded(String queryString) throws FactoryUrlException {
        if (queryString == null) {
            throw new FactoryUrlException("Query string is invalid.");
        }
        Map<String, Set<String>> queryParams;
        try {
            // question character allow parse url correctly
            queryParams = URLEncodedUtils.parse(new URI("?" + queryString), "UTF-8");
        } catch (URISyntaxException e) {
            throw new FactoryUrlException("Query string is invalid.");
        }

        Factory factory = buildDtoObject(queryParams, "", Factory.class);

        // there is unsupported parameters in query
        if (!queryParams.isEmpty()) {
            throw new FactoryUrlException("Unsupported parameters are found: " + queryParams.keySet().toString());
        }

        return validateFactoryCompatibility(factory, false);
    }

    /**
     * Build dto object with {@link com.codenvy.api.factory.parameter.FactoryParameter} annotations on its methods.
     *
     * @param queryParams
     *         - source of parameters to parse
     * @param parentName
     *         - name of parent object. Allow provide support of nested parameters such projectattributes.pname
     * @param cl
     *         - class of the object to build.
     * @return - built object
     * @throws FactoryUrlException
     */
    private <T> T buildDtoObject(Map<String, Set<String>> queryParams, String parentName, Class<T> cl) throws FactoryUrlException {
        T result = DtoFactory.getInstance().createDto(cl);
        boolean returnNull = true;
        // get all methods of object recursively
        for (Method method : cl.getMethods()) {
            FactoryParameter factoryParameter = method.getAnnotation(FactoryParameter.class);
            try {
                if (factoryParameter != null && !factoryParameter.format().equals(ENCODED)) {
                    // define full name of parameter to be able retrieving nested parameters
                    String fullName = parentName.isEmpty() ? factoryParameter.name() : parentName + "." + factoryParameter.name();
                    Class<?> returnClass = method.getReturnType();

                    Object param = null;
                    if (queryParams.containsKey(fullName)) {
                        Set<String> values;
                        if ((values = queryParams.remove(fullName)) == null || values.size() != 1) {
                            throw new FactoryUrlException("Value of parameter '" + fullName + "' is illegal.");
                        }

                        if (String.class.equals(returnClass)) {
                            param = values.iterator().next();
                        } else if (Boolean.class.equals(returnClass)) {
                            param = Boolean.parseBoolean(values.iterator().next());
                        } else if (Long.class.equals(returnClass)) {
                            param = Long.parseLong(values.iterator().next());
                            // hack: variables it's an encoded list of jsons
                        } else if ("variables".equals(fullName)) {
                            param = DtoFactory.getInstance().createListDtoFromJson(values.iterator().next(), Variable.class);
                        } else {
                            // should never happen
                            throw new FactoryUrlException("Unknown parameter '" + fullName + "'.");
                        }
                    } else if (returnClass.isAnnotationPresent(DTO.class)) {
                        // use recursion if parameter is DTO object
                        param = buildDtoObject(queryParams, fullName, returnClass);
                    }
                    if (param != null) {
                        // call appropriate setter to set current parameter
                        String setterMethodName =
                                "set" + Character.toUpperCase(factoryParameter.name().charAt(0)) + factoryParameter.name().substring(1);
                        Method setterMethod = cl.getMethod(setterMethodName, returnClass);
                        setterMethod.invoke(result, param);
                        returnNull = false;
                    }
                }
            } catch (FactoryUrlException e) {
                throw e;
            } catch (Exception e) {
                LOG.error(e.getLocalizedMessage(), e);
                throw new FactoryUrlException("Can't validate '" + factoryParameter.name() + "' parameter.");
            }
        }

        return returnNull ? null : result;
    }

    /**
     * Build factory from query string.
     *
     * @param json
     *         - json  Reader from encoded factory.
     * @return - Factory object represented by given factory string.
     */
    public Factory buildEncoded(Reader json) throws IOException, FactoryUrlException {
        Factory factory = DtoFactory.getInstance().createDtoFromJson(json, Factory.class);

        return validateFactoryCompatibility(factory, true);
    }

    /**
     * Build factory from query string.
     *
     * @param json
     *         - json  string from encoded factory.
     * @return - Factory object represented by given factory string.
     */
    public Factory buildEncoded(String json) throws FactoryUrlException {
        Factory factory = DtoFactory.getInstance().createDtoFromJson(json, Factory.class);

        return validateFactoryCompatibility(factory, true);
    }

    /**
     * Build factory from json.
     *
     * @param json
     *         - json  InputStream from encoded factory.
     * @return - Factory object represented by given factory string.
     */
    public Factory buildEncoded(InputStream json) throws IOException, FactoryUrlException {
        Factory factory = DtoFactory.getInstance().createDtoFromJson(json, Factory.class);

        return validateFactoryCompatibility(factory, true);
    }

    /**
     * Validate factory compatibility, convert parameters to new format if they have changed placement.
     *
     * @param factory
     *         - factory object to validate
     * @param encoded
     *         - is it encoded factory or not
     * @return - factory object with modern parameters
     * @throws FactoryUrlException
     */
    protected Factory validateFactoryCompatibility(Factory factory, boolean encoded) throws FactoryUrlException {
        Version v = Version.fromString(factory.getV());
        boolean tracked = factory.getOrgid() != null && !factory.getOrgid().isEmpty();

        Class usedFactoryVersion;
        switch (v) {
            case V1_0:
                usedFactoryVersion = FactoryV1_0.class;
                break;
            case V1_1:
                usedFactoryVersion = FactoryV1_1.class;
                break;
            case V1_2:
                usedFactoryVersion = FactoryV1_2.class;
                break;
            default:
                throw new FactoryUrlException("Unknown factory version " + factory.getV());
        }

        return (Factory)validateCompatibility(factory, Factory.class, usedFactoryVersion, v, encoded ? ENCODED : NONENCODED, tracked, "");
    }

    /**
     * Validate compatibility, convert parameters to new format if they have defined converter.
     *
     * @param object
     *         - object to validate factory parameters
     * @param methodsProvider
     *         - class that provides methods with {@link com.codenvy.api.factory.parameter.FactoryParameter} annotations
     * @param allowedMethodsProvider
     *         - class that provides allowed methods
     * @param version
     *         - version of factory
     * @param sourceFormat
     *         - factory format
     * @param orgIdIsPresent
     *         - flag that indicates is factory tracked
     * @param parentName
     *         - parent parameter name
     * @return - the latest version of validated factory
     * @throws FactoryUrlException
     */
    private Object validateCompatibility(Object object, Class methodsProvider, Class allowedMethodsProvider, Version version,
                                         Format sourceFormat, boolean orgIdIsPresent,
                                         String parentName) throws FactoryUrlException {

        // list of parameter converters that must be executed after checking parameters because
        List<FactoryParameterConverter> converters = new LinkedList<>();
        // get all methods recursively
        for (Method method : methodsProvider.getMethods()) {
            FactoryParameter factoryParameter = method.getAnnotation(FactoryParameter.class);
            // is it factory parameter
            if (factoryParameter != null) {
                String parameterName = factoryParameter.name();
                // check that field is set
                Object methodParameter;
                try {
                    methodParameter = method.invoke(object);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    LOG.error(e.getLocalizedMessage(), e);
                    throw new FactoryUrlException(String.format("Can't validate '%s' parameter%s.", parameterName,
                                                                parameterName.isEmpty() ? "" : (" of " + parentName)));
                }

                // if value is null or default value for primitives
                if (null == methodParameter || (Collection.class.isAssignableFrom(methodParameter.getClass())) ||
                    (Boolean.class.equals(methodParameter.getClass()) && (Boolean)methodParameter == false) ||
                    ((Long.class.equals(methodParameter.getClass()) && (Long)methodParameter == 0))) {
                    // field mustn't be a mandatory, unless it's ignored or deprecated
                    if (Obligation.MANDATORY.equals(factoryParameter.obligation()) &&
                        factoryParameter.deprecatedSince().compareTo(version) > 0 &&
                        factoryParameter.ignoredSince().compareTo(version) > 0) {
                        throw new FactoryUrlException("Parameter " + parameterName + " is mandatory.");
                    }
                } else if (!method.getDeclaringClass().isAssignableFrom(allowedMethodsProvider)) {
                    throw new FactoryUrlException("Parameter " + parameterName + " is unsupported for this version of factory.");
                } else {
                    // is parameter deprecated
                    if (factoryParameter.deprecatedSince().compareTo(version) <= 0) {
                        throw new FactoryUrlException("Parameter " + parameterName + " is deprecated.");
                    }

                    // is parameter ignored
                    if (factoryParameter.ignoredSince().compareTo(version) <= 0) {
                        ignoreConverter.convert(method, factoryParameter, object);
                        continue;
                    }

                    // is parameter ignored
                    if (factoryParameter.setByServer()) {
                        throw new FactoryUrlException("Parameter " + parameterName + " can't be set by user.");
                    }

                    // check that field satisfies format rules
                    if (!Format.BOTH.equals(factoryParameter.format()) && !factoryParameter.format().equals(sourceFormat)) {
                        throw new FactoryUrlException("Parameter " + parameterName + " is unsupported for this type of factory.");
                    }

                    // check tracked-only fields
                    if (!orgIdIsPresent && factoryParameter.trackedOnly()) {
                        throw new FactoryUrlException("Parameter " + parameterName + " can't be used without 'orgid'.");
                    }

                    if (!DefaultFactoryParameterConverter.class.equals(factoryParameter.converter())) {
                        try {
                            FactoryParameterConverter converter =
                                    factoryParameter.converter().getConstructor(Object.class).newInstance(object);
                            converters.add(converter);
                        } catch (Exception e) {
                            LOG.error(e.getLocalizedMessage(), e);
                            throw new FactoryUrlException(String.format("Can't validate '%s' parameter%s.", parameterName,
                                                                        parameterName.isEmpty() ? "" : (" of " + parentName)));
                        }
                    }

                    // use recursion if parameter is DTO object
                    if (methodParameter.getClass().isAnnotationPresent(DTO.class)) {

                        // validate inner objects such Git ot ProjectAttributes
                        validateCompatibility(methodParameter, method.getReturnType(), method.getReturnType(), version, sourceFormat,
                                              orgIdIsPresent, (parentName.isEmpty() ? "" : (parentName + ".")) + parameterName);
                    }

                }
            }
        }

        for (FactoryParameterConverter converter : converters) {
            converter.convert();
        }

        return object;
    }
}
