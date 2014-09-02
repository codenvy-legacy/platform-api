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
package com.codenvy.api.project.server;

import com.codenvy.api.project.shared.RunnerEnvironmentConfiguration;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.InputStream;

/**
 * @author andrew00x
 */
public class ProjectJsonTest {
    @BeforeMethod
    public void setUp() throws Exception {
    }

    @Test
    public void testRead() throws Exception {
        InputStream json = Thread.currentThread().getContextClassLoader().getResourceAsStream("json_test/project.json");
        Assert.assertNotNull(json);
        ProjectJson projectJson;
        try {
            projectJson = ProjectJson.load(json);
        } finally {
            json.close();
        }
        Assert.assertEquals(projectJson.getProjectTypeId(), "maven");
        Assert.assertEquals(projectJson.getBuilder(), "maven");
        Assert.assertEquals(projectJson.getRunner(), "javaweb");
        Assert.assertEquals(projectJson.getDescription(), "test project");
        Assert.assertEquals(projectJson.getDefaultBuilderEnvironment(), "my_env");
        Assert.assertEquals(projectJson.getDefaultRunnerEnvironment(), "my_env");
        Assert.assertNotNull(projectJson.getBuilderEnvironmentConfigurations().get("my_env"));
        RunnerEnvironmentConfiguration runnerEnvironmentConfiguration = projectJson.getRunnerEnvironmentConfigurations().get("my_env");
        Assert.assertNotNull(runnerEnvironmentConfiguration);
        Assert.assertEquals(runnerEnvironmentConfiguration.getRequiredMemorySize(), 128);
        Assert.assertEquals(runnerEnvironmentConfiguration.getRecommendedMemorySize(), 256);
        Assert.assertEquals(projectJson.getAttributeValue("language.version"), "1.6");
        Assert.assertEquals(projectJson.getAttributeValue("framework"), "spring");
    }
}
