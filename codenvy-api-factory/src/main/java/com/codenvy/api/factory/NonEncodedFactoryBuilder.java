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

import com.codenvy.api.factory.dto.*;

import java.util.List;

/**
 * Convert factory to non encode url.
 * This class is used in GWT code directly.
 *
 * @author Sergii Kabashniuk
 */
public abstract class NonEncodedFactoryBuilder {

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


    private void buildNonEncoded(FactoryV1_0 factory, StringBuilder builder) {
        builder.append("v=").append(factory.getV());
        builder.append("&vcs=").append(factory.getVcs());
        builder.append("&vcsurl=").append(safeGwtEncode(factory.getVcsurl()));
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

        if (factory.getVariables() != null && factory.getVariables().size() > 0) {
            builder.append("&variables=").append(safeGwtEncode(safeGwtToJson(factory.getVariables())));
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
                builder.append("&git.configbranchmerge=").append(safeGwtEncode(git.getConfigbranchmerge()));
            }

            if (git.getConfigpushdefault() != null) {
                builder.append("&git.configpushdefault=").append(git.getConfigpushdefault());
            }
            if (git.getConfigremoteoriginfetch() != null) {
                builder.append("&git.configremoteoriginfetch=").append(safeGwtEncode(git.getConfigremoteoriginfetch()));
            }
        }

    }

    /**
     * Encode value to be used as a query parameter.
     * GWT have its own implementation.
     *
     * @param value
     *         - string to encode.
     * @return - encoded value safe to use as query parameter.
     */
    protected abstract String safeGwtEncode(String value);

    /**
     * Convert object to json
     * GWT have its own implementation.
     *
     * @param dto
     *         - initial object
     * @return - json representation of object.
     */
    protected abstract String safeGwtToJson(List<Variable> dto);
}
