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
package com.codenvy.api.factory.internal;

import com.codenvy.api.project.server.ProjectProperties;
import com.codenvy.api.project.server.ProjectProperty;
import com.google.inject.name.Named;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Singleton
public class ProjectTypeHelper {

    private final String TEMPLATE_PHP;

    @Inject
    public  ProjectTypeHelper (@Nullable @Named("runner.php.template") String templatePHP) {
        this.TEMPLATE_PHP = templatePHP;
    }

    private static final String PROJECT_TYPE_MAVEN = "maven";
    private static final String PROJECT_TYPE_UNKNOWN = "unknown";

    public  ProjectProperties projectTypeToDescription(String projectType) {

        ProjectProperties projectDescription = new ProjectProperties();
        List<ProjectProperty> outputProps = new ArrayList<>();
        switch (projectType) {
            case "Jar": {
                projectDescription.setType(PROJECT_TYPE_MAVEN);
                outputProps.add(new ProjectProperty("builder.name", Arrays.asList("maven")));
                outputProps.add(new ProjectProperty("runner.name", Arrays.asList("JavaStandalone")));
                break;
            }
            case "Servlet/JSP": {
                projectDescription.setType(PROJECT_TYPE_MAVEN);
                outputProps.add(new ProjectProperty("builder.name", Arrays.asList("maven")));
                outputProps.add(new ProjectProperty("runner.name", Arrays.asList("JavaWeb")));
                break;
            }
            case "PHP": {
                projectDescription.setType(PROJECT_TYPE_UNKNOWN);
                break;
            }
            case "JavaScript": {
                projectDescription.setType(PROJECT_TYPE_UNKNOWN);
                break;
            }
            case "Spring": {
                projectDescription.setType(PROJECT_TYPE_UNKNOWN);
                break;
            }
            case "Rails": {
                projectDescription.setType(PROJECT_TYPE_UNKNOWN);
                break;
            }
            case "Python": {
                projectDescription.setType(PROJECT_TYPE_UNKNOWN);
                break;
            }
            case "Android": {
                projectDescription.setType(PROJECT_TYPE_UNKNOWN);
                outputProps.add(new ProjectProperty("runner.name", Arrays.asList("Android")));// Will work ?
                outputProps.add(new ProjectProperty("builder.name", Arrays.asList("maven")));
                break;
            }
            case "nodejs": {
                projectDescription.setType(PROJECT_TYPE_UNKNOWN);
                break;
            }
            default: {
                projectDescription.setType(PROJECT_TYPE_UNKNOWN);
            }
        }
        projectDescription.setProperties(outputProps);
        return projectDescription;
    }

    public String getRunnerTemplate(String projectType) {

        String template = null;
        switch (projectType) {
            case "PHP": {
                template = TEMPLATE_PHP;//example
                break;
            }
            case "JavaScript": {
                //template = TEMPLATE_JS;//example
                break;
            }
            case "Spring": {
                //template = TEMPLATE_SPRING;//example
                break;
            }
            case "Rails": {
                //template = TEMPLATE_ROR;//example
                break;
            }
            case "Python": {
                //template = TEMPLATE_PY;//example
                break;
            }
            case "nodejs": {
                //template = TEMPLATE_NJS;//example
                break;
            }
            default: {
            }
        }
        return template;
    }
}
