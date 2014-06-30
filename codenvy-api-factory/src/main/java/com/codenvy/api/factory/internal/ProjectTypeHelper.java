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
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.nio.file.Files.exists;


/**
 * Helper for C2 to C3 project conversion. Maps C2 project type to appropriate
 * C3 project properties object, or produce docker file for unsupported types.
 *
 */
public class ProjectTypeHelper {

    private static String TEMPLATE_PHP;
    private static String TEMPLATE_JS;
    private static String TEMPLATE_SPRING;
    private static String TEMPLATE_ROR;
    private static String TEMPLATE_PY;
    private static String TEMPLATE_NJS;

    static {
        try {
            String conf = System.getProperty("codenvy.local.conf.dir");
            TEMPLATE_PHP     = readFile(conf + "php.template", Charset.defaultCharset());
            TEMPLATE_JS      = readFile(conf + "js.template", Charset.defaultCharset());
            TEMPLATE_SPRING  = readFile(conf + "spring.template", Charset.defaultCharset());
            TEMPLATE_ROR     = readFile(conf + "ror.template", Charset.defaultCharset());
            TEMPLATE_PY      = readFile(conf + "python.template", Charset.defaultCharset());
            TEMPLATE_NJS     = readFile(conf + "nodejs.template", Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static final String PROJECT_TYPE_MAVEN = "maven";
    private static final String PROJECT_TYPE_UNKNOWN = "unknown";

    public static  ProjectProperties projectTypeToDescription(String projectType) {

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

    public static String getRunnerTemplate(String projectType) {

        switch (projectType) {
            case "PHP": {
                return TEMPLATE_PHP;
            }
            case "JavaScript": {
                return TEMPLATE_JS;
            }
            case "Spring": {
                return TEMPLATE_SPRING;
            }
            case "Rails": {
                return TEMPLATE_ROR;
            }
            case "Python": {
                return TEMPLATE_PY;
            }
            case "nodejs": {
                return TEMPLATE_NJS;
            }
            default: {
                return null;
            }
        }
    }

    private static String readFile(String path, Charset encoding) throws IOException
    {
        if (exists(Paths.get(path))) {
            byte[] encoded = Files.readAllBytes(Paths.get(path));
            return new String(encoded, encoding);
        } else {
            return null;
        }
    }
}
