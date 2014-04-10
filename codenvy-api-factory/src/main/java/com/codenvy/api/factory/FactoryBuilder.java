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

import javax.inject.Singleton;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URLEncoder;
import java.util.*;

import static com.codenvy.api.factory.parameter.FactoryParameter.Obligation;
import static com.codenvy.api.factory.parameter.FactoryParameter.Version;

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

    private static final String INVALID_PARAMETER_MESSAGE                      =
            "Passed in an invalid parameter.  You either provided a non-valid parameter, or that parameter is not accepted for this Factory version.  For more information, please visit http://docs.codenvy.com/user/creating-factories/factory-parameter-reference/.";
    private static final String INVALID_VERSION_MESSAGE                        =
            "You have provided an inaccurate or deprecated Factory Version.  For more information, please visit: http://docs.codenvy.com/user/creating-factories/factory-parameter-reference/.";
    private static final String MISSING_MANDATORY_MESSAGE                      =
            "You are missing a mandatory parameter.  For more information, please visit: http://docs.codenvy.com/user/creating-factories/factory-parameter-reference/.";
    private static final String PARAMETRIZED_INVALID_TRACKED_PARAMETER_MESSAGE =
            "You have provided a Tracked Factory parameter %s, and you do not have a valid orgId %s.  You could have provided the wrong code, your subscription has expired, or you do not have a valid subscription account.  Please contact info@codenvy.com with any questions.";
    private static final String PARAMETRIZED_INVALID_PARAMETER_MESSAGE         =
            "You have provided an invalid parameter %s for this version of Factory parameters %s.  For more information, please visit: http://docs.codenvy.com/user/creating-factories/factory-parameter-reference/.";
    private static final String PARAMETRIZED_ENCODED_ONLY_PARAMETER_MESSAGE    =
            "You submitted a parameter that can only be submitted through an encoded Factory URL %s.  For more information, please visit: http://docs.codenvy.com/user/creating-factories/factory-parameter-reference/.";
    private static final String PARAMETRIZED_ILLEGAL_PARAMETER_VALUE_MESSAGE   =
            "The parameter %s has a value submitted %s with a value that is unexpected. For more information, please visit: http://docs.codenvy.com/user/creating-factories/factory-parameter-reference/.";

    /** List contains all possible implementation of factory legacy converters. */
    static final List<LegacyConverter> LEGACY_CONVERTERS;

    static {
        List<LegacyConverter> l = new ArrayList<>(6);
        l.add(new IdCommitConverter());
        l.add(new ProjectNameConverter());
        l.add(new ProjectTypeConverter());
        l.add(new WorkspacNameConverter());
        LEGACY_CONVERTERS = Collections.unmodifiableList(l);
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
     * @param uri
     *         - uri with factory parameters.
     * @return - Factory object represented by given factory string.
     */
    public Factory buildNonEncoded(URI uri) throws FactoryUrlException {
        if (uri == null) {
            throw new FactoryUrlException("Passed in invalid query parameters.");
        }
        Map<String, Set<String>> queryParams = URLEncodedUtils.parse(uri, "UTF-8");

        Factory factory = buildDtoObject(queryParams, "", Factory.class);

        // there is unsupported parameters in query
        if (!queryParams.isEmpty()) {
            String nameInvalidParams = queryParams.keySet().iterator().next();
            throw new FactoryUrlException(String.format(PARAMETRIZED_INVALID_PARAMETER_MESSAGE, nameInvalidParams, factory.getV()));
        } else if (null == factory) {
            throw new FactoryUrlException(MISSING_MANDATORY_MESSAGE);
        }

        checkValid(factory, FactoryFormat.NONENCODED);
        return factory;
    }


    /**
     * Build factory from json.
     *
     * @param json
     *         - json  Reader from encoded factory.
     * @return - Factory object represented by given factory string.
     */
    public Factory buildEncoded(Reader json) throws IOException, FactoryUrlException {
        Factory factory = DtoFactory.getInstance().createDtoFromJson(json, Factory.class);

        checkValid(factory, FactoryFormat.ENCODED);
        return factory;
    }

    /**
     * Build factory from json.
     *
     * @param json
     *         - json  string from encoded factory.
     * @return - Factory object represented by given factory string.
     */
    public Factory buildEncoded(String json) throws FactoryUrlException {
        Factory factory = DtoFactory.getInstance().createDtoFromJson(json, Factory.class);

        checkValid(factory, FactoryFormat.ENCODED);
        return factory;
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
        checkValid(factory, FactoryFormat.ENCODED);
        return factory;
    }

    /**
     * Validate factory compatibility.
     *
     * @param factory
     *         - factory object to validate
     * @param sourceFormat
     *         - is it encoded factory or not
     * @throws FactoryUrlException
     */
    public void checkValid(Factory factory, FactoryFormat sourceFormat) throws FactoryUrlException {
        if (factory.getV() == null) {
            throw new FactoryUrlException(INVALID_VERSION_MESSAGE);
        }

        Version v;
        try {
            v = Version.fromString(factory.getV());
        } catch (IllegalArgumentException e) {
            throw new FactoryUrlException(INVALID_VERSION_MESSAGE);
        }

        String orgid = factory.getOrgid() != null && !factory.getOrgid().isEmpty() ? factory.getOrgid() : null;

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
                throw new FactoryUrlException(INVALID_VERSION_MESSAGE);
        }


        validateCompatibility(factory, Factory.class, usedFactoryVersion, v, sourceFormat, orgid, "");

    }

    /**
     * Convert factory of given version to the latest factory format.
     *
     * @param factory
     *         - given factory.
     * @return - factory in latest format.
     * @throws FactoryUrlException
     */
    public Factory convertToLatest(Factory factory) throws FactoryUrlException {
        Factory resultFactory = DtoFactory.getInstance().clone(factory);
        resultFactory.setV("1.2");
        for (LegacyConverter converter : LEGACY_CONVERTERS) {
            converter.convert(resultFactory);
        }

        return resultFactory;
    }


    /**
     * Validate compatibility of factory parameters.
     *
     * @param object
     *         - object to validate factory parameters
     * @param methodsProvider
     *         - class that provides methods with {@link com.codenvy.api.factory.parameter.FactoryParameter}
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
     * @throws FactoryUrlException
     */
    private void validateCompatibility(Object object, Class methodsProvider, Class allowedMethodsProvider,
                                       Version version,
                                       FactoryFormat sourceFormat, String orgid,
                                       String parentName) throws FactoryUrlException {
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
                } catch (IllegalAccessException | InvocationTargetException e) {
                    // should never happen
                    LOG.error(e.getLocalizedMessage(), e);
                    throw new FactoryUrlException(INVALID_PARAMETER_MESSAGE);
                }

                // if value is null or empty collection or default value for primitives
                if (ValueHelper.isEmpty(parameterValue)) {
                    // field must not be a mandatory, unless it's ignored or deprecated
                    if (Obligation.MANDATORY.equals(factoryParameter.obligation()) &&
                        factoryParameter.deprecatedSince().compareTo(version) > 0 &&
                        factoryParameter.ignoredSince().compareTo(version) > 0) {
                        throw new FactoryUrlException(MISSING_MANDATORY_MESSAGE);
                    }
                } else if (!method.getDeclaringClass().isAssignableFrom(allowedMethodsProvider)) {
                    throw new FactoryUrlException(String.format(PARAMETRIZED_INVALID_PARAMETER_MESSAGE, fullName, version));
                } else {
                    // is parameter deprecated
                    if (factoryParameter.deprecatedSince().compareTo(version) <= 0) {
                        throw new FactoryUrlException(String.format(PARAMETRIZED_INVALID_PARAMETER_MESSAGE, fullName, version));
                    }

                    if (factoryParameter.setByServer()) {
                        throw new FactoryUrlException(String.format(PARAMETRIZED_INVALID_PARAMETER_MESSAGE, fullName, version));
                    }

                    // check that field satisfies format rules
                    if (!FactoryFormat.BOTH.equals(factoryParameter.format()) &&
                        !factoryParameter.format().equals(sourceFormat)) {
                        throw new FactoryUrlException(String.format(PARAMETRIZED_ENCODED_ONLY_PARAMETER_MESSAGE, fullName));
                    }

                    // check tracked-only fields
                    if (orgid == null && factoryParameter.trackedOnly()) {
                        throw new FactoryUrlException(String.format(PARAMETRIZED_INVALID_TRACKED_PARAMETER_MESSAGE, fullName, orgid));
                    }


                    // use recursion if parameter is DTO object
                    if (parameterValue.getClass().isAnnotationPresent(DTO.class)) {
                        // validate inner objects such Git ot ProjectAttributes
                        validateCompatibility(parameterValue, method.getReturnType(), method.getReturnType(), version, sourceFormat,
                                              orgid, fullName);
                    }

                }
            }
        }
    }


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

        if (factory.getVcsinfo()) {
            builder.append("&vcsinfo=").append(true);
        }

        if (factory.getOpenfile() != null) {
            builder.append("&openfile=").append(factory.getOpenfile());
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

        if (factory.getOrgid() != null) {
            builder.append("&orgid=").append(factory.getOrgid());
        }

        if (factory.getAffiliateid() != null) {
            builder.append("&affiliateid=").append(factory.getAffiliateid());
        }

        if (factory.getVcsbranch() != null) {
            builder.append("&vcsbranch=").append(factory.getVcsbranch());
        }

        if (factory.getVariables() != null) {
            builder.append("&variables=").append(encode(DtoFactory.getInstance().toJson(factory.getVariables())));
        }

        if (factory.getImage() != null) {
            builder.append("&image=").append(factory.getImage());
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
            if (restriction.getMaxsessioncount() > 0) {
                builder.append("&restriction.maxsessioncount=").append(restriction.getMaxsessioncount());
            }
            if (restriction.getRefererhostname() != null) {
                builder.append("&restriction.refererhostname=").append(restriction.getRefererhostname());
            }

            if (restriction.getPassword() != null) {
                builder.append("&restriction.password=").append(restriction.getPassword());
            }

            if (restriction.getRestrictbypassword()) {
                builder.append("&restriction.restrictbypassword=").append(true);
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
     * Build dto object with {@link com.codenvy.api.factory.parameter.FactoryParameter} annotations on its methods.
     *
     * @param queryParams
     *         - source of parameters to parse
     * @param parentName
     *         - queryParameterName of parent object. Allow provide support of nested parameters such
     *         projectattributes.pname
     * @param cl
     *         - class of the object to build.
     * @return - built object
     * @throws FactoryUrlException
     */
    private <T> T buildDtoObject(Map<String, Set<String>> queryParams, String parentName, Class<T> cl)
            throws FactoryUrlException {
        T result = DtoFactory.getInstance().createDto(cl);
        boolean returnNull = true;
        // get all methods of object recursively
        for (Method method : cl.getMethods()) {
            FactoryParameter factoryParameter = method.getAnnotation(FactoryParameter.class);
            try {
                if (factoryParameter != null) {
                    // define full queryParameterName of parameter to be able retrieving nested parameters
                    String fullName = (parentName.isEmpty() ? "" : parentName + ".") + factoryParameter.queryParameterName();
                    Class<?> returnClass = method.getReturnType();

                    if (factoryParameter.format() == FactoryFormat.ENCODED) {
                        if (queryParams.containsKey(fullName)) {
                            throw new FactoryUrlException(String.format(PARAMETRIZED_ENCODED_ONLY_PARAMETER_MESSAGE, fullName));
                        } else {
                            continue;
                        }
                    }

                    //PrimitiveTypeProducer
                    Object param = null;
                    if (queryParams.containsKey(fullName)) {
                        Set<String> values;
                        if ((values = queryParams.remove(fullName)) == null || values.size() != 1) {
                            throw new FactoryUrlException(String.format(PARAMETRIZED_ILLEGAL_PARAMETER_VALUE_MESSAGE, fullName,
                                                                        null != values ? values.toString() : "null"));
                        }
                        param = ValueHelper.createValue(returnClass, values);
                        if (param == null) {
                            if ("variables".equals(fullName)) {
                                try {
                                    param = DtoFactory.getInstance().createListDtoFromJson(values.iterator().next(), Variable.class);
                                } catch (Exception e) {
                                    throw new FactoryUrlException(
                                            String.format(PARAMETRIZED_ILLEGAL_PARAMETER_VALUE_MESSAGE, fullName, values.toString()));
                                }
                            } else {
                                // should never happen
                                throw new FactoryUrlException(
                                        String.format(PARAMETRIZED_ILLEGAL_PARAMETER_VALUE_MESSAGE, fullName, values.toString()));
                            }
                        }
                    } else if (returnClass.isAnnotationPresent(DTO.class)) {
                        // use recursion if parameter is DTO object
                        param = buildDtoObject(queryParams, fullName, returnClass);
                    }
                    if (param != null) {
                        // call appropriate setter to set current parameter
                        String setterMethodName =
                                "set" + Character.toUpperCase(factoryParameter.queryParameterName().charAt(0)) +
                                factoryParameter.queryParameterName().substring(1);
                        Method setterMethod = cl.getMethod(setterMethodName, returnClass);
                        setterMethod.invoke(result, param);
                        returnNull = false;
                    }
                }
            } catch (FactoryUrlException e) {
                throw e;
            } catch (Exception e) {
                LOG.error(e.getLocalizedMessage(), e);
                throw new FactoryUrlException(
                        "Can't validate '" + factoryParameter.queryParameterName() + "' parameter.");
            }
        }

        return returnNull ? null : result;
    }
}
