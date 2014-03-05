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
import java.util.Map;
import java.util.Set;

import static com.codenvy.api.factory.parameter.FactoryParameter.*;
import static com.codenvy.api.factory.parameter.FactoryParameter.Format.ENCODED;

/**
 * Tool to easy convert Factory object to nonencoded version or
 * to json version and vise versa
 *
 * @author Sergii Kabashniuk
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

    private String decode(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    /**
     * Convert factory to json
     *
     * @param factory
     *         - factory object.
     * @return - json view of given factory
     */
    public String buildEncoded(Factory factory) {
        return DtoFactory.getInstance().toJson(factory);
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
     * Build factory from query string.
     *
     * @param queryString
     *         - query string from nonencoded factory.
     * @return - Factory object represented by given factory string.
     */
    public Factory buildNonEncoded(String queryString) throws FactoryUrlException {
        if (queryString == null) {
            throw new FactoryUrlException("Query string is invalid.");
        }
        Map<String, Set<String>> queryParams = null;
        try {
            queryParams = URLEncodedUtils.parse(new URI("?" + queryString), "UTF-8");
        } catch (URISyntaxException e) {
            throw new FactoryUrlException("Query string is invalid.");
        }

        Factory factory = buildFactoryDtoParameter(queryParams, "", Factory.class);

        if (!queryParams.isEmpty()) {
            throw new FactoryUrlException("Unsupported parameters is found: " + queryParams.keySet().toString());
        }

        return validateFactoryCompatibility(factory, Format.ENCODED);
        //return null;
    }

    private <T> T buildFactoryDtoParameter(Map<String, Set<String>> queryParams, String parentName, Class<T> cl)
            throws FactoryUrlException {
        // lazy initialization
        T result = DtoFactory.getInstance().createDto(cl);
        boolean returnNull = true;
        for (Method method : cl.getMethods()) {
            FactoryParameter factoryParameter = method.getAnnotation(FactoryParameter.class);
            try {
                if (factoryParameter != null && !factoryParameter.format().equals(ENCODED)) {
                    String parameterName = factoryParameter.name();
                    if (queryParams.containsKey(getName(parentName, parameterName))) {
                        Set<String> values;
                        if ((values = queryParams.remove(getName(parentName, parameterName))) == null || values.size() != 1) {
                            throw new FactoryUrlException("Value of parameter '" + getName(parentName, parameterName) + "' is illegal.");
                        }

                        Class<?> returnClass = method.getReturnType();
                        String setterMethodName =
                                "set" + Character.toUpperCase(factoryParameter.name().charAt(0)) + factoryParameter.name().substring(1);
                        Method setterMethod = cl.getMethod(setterMethodName, returnClass);

                        if (boolean.class.equals(returnClass)) {
                            setterMethod.invoke(result, Boolean.parseBoolean(values.iterator().next()));
                            returnNull = false;
                        } else if (returnClass.isPrimitive()) {
                            setterMethod.invoke(result, Long.parseLong(values.iterator().next()));
                            returnNull = false;
                        } else if (String.class.equals(returnClass)) {
                            setterMethod.invoke(result, values.iterator().next());
                            returnNull = false;
                        } else if ("variables".equals(parameterName)) {
                            setterMethod.invoke(result,
                                                DtoFactory.getInstance().createListDtoFromJson(values.iterator().next(), Variable.class));
                            returnNull = false;
                        }
                    } else {
                        Class<?> returnClass = method.getReturnType();
                        if (returnClass.isAnnotationPresent(DTO.class)) {
                            Object param = buildFactoryDtoParameter(queryParams, parameterName, returnClass);
                            if (param != null) {
                                String setterMethodName =
                                        "set" + Character.toUpperCase(factoryParameter.name().charAt(0)) + factoryParameter.name().substring(1);
                                Method setterMethod = cl.getMethod(setterMethodName, returnClass);
                                setterMethod.invoke(result, param);
                                returnNull = false;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOG.error(e.getLocalizedMessage(), e);
                throw new FactoryUrlException("Can't validate '" + factoryParameter.name() + "' parameter.");
            }
        }

        return returnNull ? null : result;
    }

    private String getName(String parent, String current) {
        return parent.isEmpty() ? current : parent + "." + current;
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
        return validateFactoryCompatibility(factory, ENCODED);
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
        return validateFactoryCompatibility(factory, ENCODED);
    }

    /**
     * Build factory from query string.
     *
     * @param json
     *         - json  InputStream from encoded factory.
     * @return - Factory object represented by given factory string.
     */
    public Factory buildEncoded(InputStream json) throws IOException, FactoryUrlException {
        Factory factory = DtoFactory.getInstance().createDtoFromJson(json, Factory.class);
        return validateFactoryCompatibility(factory, ENCODED);
    }

    public Factory validateFactoryCompatibility(Factory factory, Format sourceFormat) throws FactoryUrlException {
        Version version = Version.fromString(factory.getV());

        return (Factory)validateCompatibility(factory, version, sourceFormat, factory.getOrgid() != null && !factory.getOrgid().isEmpty(),
                                              "");
    }

    // TODO improve it, because there is a lot of work with needless methods, classes
    private Object validateCompatibility(Object object, Version version, Format sourceFormat, boolean orgIdIsPresent, String parentName)
            throws FactoryUrlException {
        for (Method method : object.getClass().getMethods()) {
            FactoryParameter factoryParameter = getAnnotation(method);
            if (factoryParameter != null) {
                String parameterName = factoryParameter.name();
                // check that field is set
                Object methodParameter;
                try {
                    methodParameter = method.invoke(object);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    LOG.error(e.getLocalizedMessage(), e);
                    throw new FactoryUrlException(String.format("Can't validate '%s' parameter%s.", parameterName,
                                                                parameterName.isEmpty() ? "" : " of " + parentName));
                }

                // if value is null
                if (null == methodParameter || (Long.class.equals(methodParameter.getClass()) && (Long)methodParameter == 0) ||
                    (Boolean.class.equals(methodParameter.getClass()) && (Boolean)methodParameter == false)) {
                    // field mustn't be mandatory, unless it's ignored or deprecated
                    if (Obligation.MANDATORY.equals(factoryParameter.obligation()) &&
                        factoryParameter.deprecatedSince().compareTo(version) > 0 &&
                        factoryParameter.ignoredSince().compareTo(version) > 0) {
                        throw new FactoryUrlException("Parameter " + parameterName + " is mandatory.");
                    }
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
                    if (orgIdIsPresent && factoryParameter.trackedOnly()) {
                        throw new FactoryUrlException("Parameter " + parameterName + " can't be used without 'orgid'.");
                    }

                    try {
                        FactoryParameterConverter converter = factoryParameter.converter().newInstance();
                        converter.convert(object);
                    } catch (InstantiationException | IllegalAccessException e) {
                        LOG.error(e.getLocalizedMessage(), e);
                        throw new FactoryUrlException(String.format("Can't validate '%s' parameter%s.", parameterName,
                                                                    parameterName.isEmpty() ? "" : " of " + parentName));
                    }

                    // validate inner objects such Git ot ProjectAttributes
                    validateCompatibility(methodParameter, version, sourceFormat, orgIdIsPresent,
                                          (parentName.isEmpty() ? "" : parentName + ".") + parameterName);
                }
            }
        }

        return object;
    }

    private static FactoryParameter getAnnotation(Method baseMethod) {
        Method method;
        Class<?>[] interfaces = baseMethod.getDeclaringClass().getInterfaces();
        for (Class<?> factoryInterface : interfaces) {
            try {
                method = factoryInterface.getDeclaredMethod(baseMethod.getName(), baseMethod.getParameterTypes());
                return method.getAnnotation(FactoryParameter.class);
            } catch (NoSuchMethodException ignored) {
            }
        }

        return null;
    }
}
