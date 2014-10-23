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

import com.codenvy.api.factory.dto.Actions;
import com.codenvy.api.factory.dto.Author;
import com.codenvy.api.factory.dto.Factory;
import com.codenvy.api.factory.dto.FactoryV1_0;
import com.codenvy.api.factory.dto.FactoryV1_1;
import com.codenvy.api.factory.dto.FactoryV1_2;
import com.codenvy.api.factory.dto.FactoryV2_0;
import com.codenvy.api.factory.dto.Git;
import com.codenvy.api.factory.dto.Policies;
import com.codenvy.api.factory.dto.ProjectAttributes;
import com.codenvy.api.factory.dto.Restriction;
import com.codenvy.api.project.shared.dto.RunnerSource;
import com.codenvy.api.project.shared.dto.Source;
import com.codenvy.api.factory.dto.Workspace;
import com.codenvy.api.project.shared.dto.ImportSourceDescriptor;
import com.codenvy.api.project.shared.dto.NewProject;
import com.codenvy.api.project.shared.dto.RunnerConfiguration;
import com.codenvy.api.project.shared.dto.RunnersDescriptor;
import com.codenvy.api.vfs.shared.dto.ReplacementSet;

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
     * @throws java.lang.RuntimeException
     *         if v is null, empty or illegal.
     */
    // TODO affiliateId
    public String buildNonEncoded(Factory factory) {
        if (null == factory.getV() || factory.getV().isEmpty()) {
            throw new RuntimeException("Factory version can't be null or empty");
        }
        StringBuilder result = new StringBuilder();
        switch (factory.getV()) {
            case "1.0":
                buildNonEncoded((FactoryV1_0)factory, result);
                break;
            case "1.1":
                buildNonEncoded((FactoryV1_1)factory, result);
                break;
            case "1.2":
                buildNonEncoded((FactoryV1_2)factory, result);
                break;
            case "2.0":
                buildNonEncoded((FactoryV2_0)factory, result);
                break;
            default:
                throw new RuntimeException("Factory version '" + factory.getV() + "' not found");
        }
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

        if (factory.getVcsinfo() != null) {
            builder.append("&vcsinfo=").append(factory.getVcsinfo());
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
            if (restriction.getValidsince() != null && restriction.getValidsince() > 0) {
                builder.append("&restriction.validsince=").append(restriction.getValidsince());
            }
            if (restriction.getValiduntil() != null && restriction.getValiduntil() > 0) {
                builder.append("&restriction.validuntil=").append(restriction.getValiduntil());
            }
            if (restriction.getMaxsessioncount() != null && restriction.getMaxsessioncount() > 0) {
                builder.append("&restriction.maxsessioncount=").append(restriction.getMaxsessioncount());
            }
            if (restriction.getRefererhostname() != null) {
                builder.append("&restriction.refererhostname=").append(restriction.getRefererhostname());
            }

            if (restriction.getPassword() != null) {
                builder.append("&restriction.password=").append(restriction.getPassword());
            }

            if (restriction.getRestrictbypassword() != null && restriction.getRestrictbypassword()) {
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
        final Source source = factory.getSource();
        if (null != source) {
            final ImportSourceDescriptor sourceDescriptor = source.getProject();
            if (null != sourceDescriptor) {
                appendIfNotNull(builder, "&source.project.type=", sourceDescriptor.getType(), false);
                appendIfNotNull(builder, "&source.project.location=", sourceDescriptor.getLocation(), true);
                if (sourceDescriptor.getParameters() != null) {
                    for (Map.Entry<String, String> entry : sourceDescriptor.getParameters().entrySet()) {
                        builder.append("&source.project.parameters.")
                               .append(encode(entry.getKey()))
                               .append("=")
                               .append(encode(entry.getValue()));
                    }
                }
            }
            if (source.getRunners() != null) {
                for (Map.Entry<String, RunnerSource> runnerSource : source.getRunners().entrySet()) {
                    final String prefix = "&source.runners." + encode(runnerSource.getKey());
                    builder.append(prefix)
                           .append(".location=")
                           .append(encode(runnerSource.getValue().getLocation()));
                    if (runnerSource.getValue().getParameters() != null) {
                        for (Map.Entry<String, String> parameter : runnerSource.getValue().getParameters().entrySet()) {
                            builder.append(prefix)
                                   .append(".parameters.")
                                   .append(encode(parameter.getKey()))
                                   .append("=").append(encode(parameter.getValue()));
                        }
                    }
                }
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
                builder.append("&workspace.attributes.")
                       .append(encode(entry.getKey()))
                       .append("=")
                       .append(encode(entry.getValue()));
            }
        }

        final NewProject project = factory.getProject();
        if (project != null) {
            appendIfNotNull(builder, "&project.name=", project.getName(), true);
            appendIfNotNull(builder, "&project.description=", project.getDescription(), true);
            appendIfNotNull(builder, "&project.type=", project.getType(), true);
            appendIfNotNull(builder, "&project.visibility=", project.getVisibility(), false);
            if (project.getBuilders() != null) {
                appendIfNotNull(builder, "&project.builders.default=", project.getBuilders().getDefault(), true);
            }
            final RunnersDescriptor rDescriptor = project.getRunners();
            if (null != rDescriptor) {
                appendIfNotNull(builder, "&project.runners.default=", rDescriptor.getDefault(), true);
                if (rDescriptor.getConfigs() != null) {
                    for (Map.Entry<String, RunnerConfiguration> rConf : rDescriptor.getConfigs().entrySet()) {
                        final String prefix = "&project.runners.configs." + encode(rConf.getKey());
                        if (rConf.getValue().getRam() > 0) {
                            builder.append(prefix)
                                   .append(".ram=")
                                   .append(rConf.getValue().getRam());
                        }
                        if (rConf.getValue().getVariables() != null) {
                            final String vPrefix = prefix + ".variables";
                            for (Map.Entry<String, String> vars : rConf.getValue().getVariables().entrySet()) {
                                builder.append(vPrefix)
                                       .append(".")
                                       .append(encode(vars.getKey()))
                                       .append("=")
                                       .append(encode(vars.getValue()));
                            }
                        }
                        if (rConf.getValue().getOptions() != null) {
                            final String oPrefix = prefix + ".options";
                            for (Map.Entry<String, String> options : rConf.getValue().getOptions().entrySet()) {
                                builder.append(oPrefix)
                                       .append(".")
                                       .append(encode(options.getKey()))
                                       .append("=")
                                       .append(encode(options.getValue()));
                            }
                        }
                    }
                }
            }
            if (project.getAttributes() != null) {
                for (Map.Entry<String, List<String>> attribute : project.getAttributes().entrySet()) {
                    final String prefix = "&project.attributes." + encode(attribute.getKey());
                    for (String attrValue : attribute.getValue()) {
                        builder.append(prefix)
                               .append("=")
                               .append(encode(attrValue));
                    }
                }
            }
        }

        final Policies policies = factory.getPolicies();
        if (policies != null) {
            appendIfNotNull(builder, "&policies.validSince=", policies.getValidSince(), false);
            appendIfNotNull(builder, "&policies.validUntil=", policies.getValidUntil(), false);
            appendIfNotNull(builder, "&policies.refererHostname=", policies.getRefererHostname(), true);
        }

        final Actions actions = factory.getActions();
        if (actions != null) {
            appendIfNotNull(builder, "&actions.openFile=", actions.getOpenFile(), true);
            appendIfNotNull(builder, "&actions.warnOnClose=", actions.getWarnOnClose(), false);
            if (actions.getFindReplace() != null && !actions.getFindReplace().isEmpty()) {
                builder.append("&actions.findReplace=")
                       .append(encode(toJson(actions.getFindReplace())));
            }
        }
    }

    private void appendIfNotNull(StringBuilder sb, String key, Object value, boolean encodeValue) {
        if (value != null) {
            if (encodeValue) {
                value = encode(String.valueOf(value));
            }
            sb.append(key).append(String.valueOf(value));
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
    protected abstract String toJson(List<ReplacementSet> dto);
}
