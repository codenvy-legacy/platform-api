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
import com.codenvy.dto.server.DtoFactory;

import javax.inject.Singleton;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * Tool to easy convert Factory object to nonencoded version or
 * to json version and vise versa
 *
 * @author Sergii Kabashniuk
 */
@Singleton
public class FactoryBuilder {
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
        if (factory.getVariable() != null) {
            builder.append("&variables=").append(encode(DtoFactory.getInstance().toJson(factory.getVariable())));
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
        /*if (queryString == null) {
            throw new FactoryUrlException("Query string is invalid.");
        }
        Map<String, Set<String>> params = null;
        try {
            params = URLEncodedUtils.parse(new URI("?" + queryString), "UTF-8");
        } catch (URISyntaxException e) {
            throw new FactoryUrlException("Query string is invalid.");
        }

        if (params.get("v") != null || params.get("v").size() != 1) {
            throw new FactoryUrlException("Parameter v is missing or has multiple values.");
        }

        FactoryCompatibilityMap compatibilityMap = FactoryCompatibilityMap.create(params.get("v").iterator().next());
        for (Map.Entry<String, Set<String>> entry : params.entrySet()) {
            String value;
            if (entry.getValue() == null || entry.getValue().size() != 1 || (value = entry.getValue().iterator().next()) == null) {
                throw new FactoryUrlException(String.format("Value of parameter %s is illegal.", entry.getKey()));
            }

            compatibilityMap.set(entry.getKey(), value);
        }

        return compatibilityMap.getFactory();*/
        return null;
    }

    /**
     * Build factory from query string.
     *
     * @param json
     *         - json  Reader from encoded factory.
     * @return - Factory object represented by given factory string.
     */
    public Factory buildEncoded(Reader json) throws IOException {
        return DtoFactory.getInstance().createDtoFromJson(json, Factory.class);
    }

    /**
     * Build factory from query string.
     *
     * @param json
     *         - json  string from encoded factory.
     * @return - Factory object represented by given factory string.
     */
    public Factory buildEncoded(String json) {
        return DtoFactory.getInstance().createDtoFromJson(json, Factory.class);
    }

    /**
     * Build factory from query string.
     *
     * @param json
     *         - json  InputStream from encoded factory.
     * @return - Factory object represented by given factory string.
     */
    public Factory buildEncoded(InputStream json) throws IOException {
        return DtoFactory.getInstance().createDtoFromJson(json, Factory.class);
    }

    public Factory validateCompatibility(Factory factory) {
        String version = factory.getV();

        Method[] methods = factory.getClass().getMethods();

        for (Method method : methods) {
            Compatibility compatibility = method.getAnnotation(Compatibility.class);
            System.out.println(method.getName());
            if (compatibility != null) {
                System.out.println(method.getName());
            }
        }

        return factory;
    }
}
