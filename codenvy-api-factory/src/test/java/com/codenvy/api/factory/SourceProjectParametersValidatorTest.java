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

import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.factory.FactoryParameter;
import com.codenvy.api.project.shared.dto.ImportSourceDescriptor;
import com.codenvy.dto.server.DtoFactory;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;

public class SourceProjectParametersValidatorTest {
    private SourceProjectParametersValidator validator;

    private ImportSourceDescriptor sourceDescriptor;

    @BeforeMethod
    public void setUp() throws Exception {
        validator = new SourceProjectParametersValidator();

        sourceDescriptor = DtoFactory.getInstance().createDto(ImportSourceDescriptor.class)
                                     .withLocation("location")
                                     .withType("git")
                                     .withParameters(new HashMap<String, String>() {
                                         {
                                             put("branch", "master");
                                             put("commitId", "123456");
                                             put("keepVcs", "true");
                                             put("remoteOriginFetch", "12345");
                                             put("keepDirectory", "/src");
                                         }
                                     });
    }

    @Test
    public void shouldBeAbleValidateGitSource() throws Exception {
        validator.validate(sourceDescriptor, FactoryParameter.Version.V2_0);
    }

    @Test
    public void shouldBeAbleValidateESBWSO2Source() throws Exception {
        sourceDescriptor.setType("esbwso2");

        validator.validate(sourceDescriptor, FactoryParameter.Version.V2_0);
    }

    @Test(expectedExceptions = ConflictException.class,
          expectedExceptionsMessageRegExp = "The parameter .* has a value submitted .* with a value.*")
    public void shouldThrowExceptionIfTypeIsNotGit() throws Exception {
        validator.validate(sourceDescriptor.withType("zip"), FactoryParameter.Version.V2_0);
    }

    @Test(expectedExceptions = ConflictException.class,
          expectedExceptionsMessageRegExp = "You have provided an invalid parameter .* for this version of Factory parameters.*")
    public void shouldThrowExceptionIfUnknownParameterIsUsed() throws Exception {
        sourceDescriptor.getParameters().put("other", "value");

        validator.validate(sourceDescriptor, FactoryParameter.Version.V2_0);
    }

    @Test(expectedExceptions = ConflictException.class,
          expectedExceptionsMessageRegExp = "The parameter .* has a value submitted .* with a value that is unexpected.*")
    public void shouldThrowExceptionIfKeepVcsIsNotTrueOrFalse() throws Exception {
        sourceDescriptor.getParameters().put("keepVcs", "qwerty");

        validator.validate(sourceDescriptor, FactoryParameter.Version.V2_0);
    }
}