/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2013] Codenvy, S.A.
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
