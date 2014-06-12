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
package com.codenvy.dto.generator.maven.plugin;

import com.codenvy.dto.generator.DtoGenerator;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.util.List;

/** Mojo to run {@link DtoGenerator}. */
@Mojo(name = "generate", requiresDependencyResolution = ResolutionScope.COMPILE)
public class DtoGeneratorMojo extends AbstractMojo {
    @Parameter(property = "outputDir", required = true)
    private String outputDirectory;

    @Parameter(property = "dtoPackages", required = true)
    private String[] dtoPackages;

    @Parameter(property = "genClassName", required = true)
    private String genClassName;

    @Parameter(property = "impl", required = true)
    private String impl;

    public void execute() throws MojoExecutionException {
        DtoGenerator dtoGenerator = new DtoGenerator();
        dtoGenerator.setPackageBase(outputDirectory);
        String genFileName = genClassName.replace('.', File.separatorChar) + ".java";
        dtoGenerator.setGenFileName(outputDirectory.endsWith(File.separator) ? (outputDirectory + genFileName)
                                                                             : (outputDirectory + File.separatorChar + genFileName));
        dtoGenerator.setImpl(impl);
        dtoGenerator.setDtoPackages(dtoPackages);
        dtoGenerator.generate();
    }
}
