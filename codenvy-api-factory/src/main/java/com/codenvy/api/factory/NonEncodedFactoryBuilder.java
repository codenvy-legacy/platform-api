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
import com.codenvy.api.project.shared.dto.ImportSourceDescriptor;

import java.util.List;
import java.util.Map;

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

            if (projectattributes.getRunnername() != null) {
                builder.append("&projectattributes.runnername=").append(projectattributes.getRunnername());
            }

            if (projectattributes.getRunnerenvironmentid() != null) {
                builder.append("&projectattributes.runnerenvironmentid=").append(projectattributes.getRunnerenvironmentid());
            }

            if (projectattributes.getBuildername() != null) {
                builder.append("&projectattributes.buildername=").append(projectattributes.getBuildername());
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
            builder.append("&variables=").append(encode(toJson(factory.getVariables())));
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

    private void buildNonEncoded(FactoryV2_0 factory, StringBuilder builder) {
        appendIfNotNull(builder, "v=", factory.getV(), false);
        final ImportSourceDescriptor source = factory.getSource();
        if (source != null) {
            appendIfNotNull(builder, "&source.type=", source.getType(), false);
            appendIfNotNull(builder, "&source.location=", source.getLocation(), true);
            for (Map.Entry<String, String> entry : source.getParameters().entrySet()) {
                builder.append("&source.parameters.").append(entry.getKey()).append("=").append(encode(entry.getValue()));
            }
        }

        final Author creator = factory.getCreator();
        if (creator != null) {
            appendIfNotNull(builder, "&creator.name=", creator.getName(), true);
            appendIfNotNull(builder, "&creator.email=", creator.getEmail(), true);
            appendIfNotNull(builder, "&creator.accountId=", creator.getAccountId(), false);
        }

        final Workspace workspace = factory.getWorkspace();
        if (workspace != null) {
            appendIfNotNull(builder, "&workspace.temp=", workspace.getTemp(), false);
            for (Map.Entry<String, String> entry : workspace.getAttributes().entrySet()) {
                appendIfNotNull(builder, "&workspace.attributes." + entry.getKey() + "=", entry.getValue(), true);
            }
        }

        final FactoryProject project = factory.getProject();
        if (project != null) {
            appendIfNotNull(builder, "&project.name", project.getName(), true);
            appendIfNotNull(builder, "&project.description", project.getDescription(), true);
            appendIfNotNull(builder, "&project.type", project.getProjectTypeId(), true);
            appendIfNotNull(builder, "&project.visibility", project.getVisibility(), false);
            appendIfNotNull(builder, "&project.builderType", project.getBuilder(), false);
            appendIfNotNull(builder, "&project.runnerType", project.getRunner(), false);
            appendIfNotNull(builder, "&project.defaultBuilder", project.getDefaultBuilderEnvironment(), false);
            appendIfNotNull(builder, "&project.defaultRunner", project.getDefaultRunnerEnvironment(), false);
            // TODO
//            for (Map.Entry<String, List<String>> entry : project.getAttributes().entrySet()) {
//                appendIfNotNull(builder, "&project.attributes." + entry.getKey() + "=", entry.getValue(), true);
//            }
            // TODO builder, runner environments
        }

        final Policies policies = factory.getPolicies();
        if (policies != null) {
            appendIfNotNull(builder, "&policies.validSince", String.valueOf(policies.getValidSince()), false);
            appendIfNotNull(builder, "&policies.validUntil", String.valueOf(policies.getValidUntil()), false);
            appendIfNotNull(builder, "&policies.refererHostname", policies.getRefererHostname(), true);
        }

        final Actions actions = factory.getActions();
        if (actions != null) {
            appendIfNotNull(builder, "&actions.openFile", actions.getOpenFile(), true);
            appendIfNotNull(builder, "&actions.warnOnClose", String.valueOf(actions.getWarnOnClose()), false);
            if (actions.getFindReplace() != null) {
                builder.append("&actions.findReplace").append(encode(toJson(actions.getFindReplace())));
            }
        }
    }

    private void appendIfNotNull(StringBuilder sb, String key, String value, boolean encodeValue) {
        if (value != null) {
            if (encodeValue) {
                value = encode(value);
            }
            sb.append(key).append(value);
        }
    }

    /**
     * Encode value to be used as a query parameter.
     *
     * @param value
     *         - string to encode.
     * @return - encoded value safe to use as query parameter.
     */
    protected abstract String encode(String value);

    /**
     * Convert object to json
     *
     * @param dto
     *         - initial object
     * @return - json representation of object.
     */
    protected abstract String toJson(List<Variable> dto);
}
