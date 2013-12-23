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
package com.codenvy.builder.tools.maven;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * A smattering of useful methods to work with the Maven POM.
 *
 * @author <a href="mailto:azatsarynnyy@codenvy.com">Artem Zatsarynnyy</a>
 */
public class MavenUtils {
    /** Internal Maven POM reader. */
    private static MavenXpp3Reader pomReader = new MavenXpp3Reader();
    /** Internal Maven POM writer. */
    private static MavenXpp3Writer pomWriter = new MavenXpp3Writer();

    /** Not instantiable. */
    private MavenUtils() {
    }

    /**
     * Writes a specified {@link MavenProjectModel} to the path from which this model has been read.
     *
     * @param model
     *         model to write
     * @throws IllegalArgumentException
     *         if path to write a model is unknown
     */
    public static void writeModel(MavenProjectModel model) {
        try {
            if (model.getPomFile() != null) {
                pomWriter.write(Files.newOutputStream(model.getPomFile().toPath()), model2InternalModel(model));
            } else {
                throw new IllegalStateException("Unable to write a model. Unknown path.");
            }
        } catch (IOException | XmlPullParserException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Writes a specified {@link MavenProjectModel} to the specified {@link Path}.
     *
     * @param model
     *         model to write
     * @param path
     *         path to the file to write a model
     * @throws IllegalArgumentException
     *         if any error occurred while reading an original model or writing a specified model
     */
    public static void writeModelToPath(MavenProjectModel model, Path path) {
        try {
            pomWriter.write(Files.newOutputStream(path), model2InternalModel(model));
        } catch (IOException | XmlPullParserException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns an execution command to launch Maven. If Maven home
     * environment variable isn't set then 'mvn' will be returned
     * since it's assumed that 'mvn' should be in PATH variable.
     *
     * @return an execution command to launch Maven
     */
    public static String getMavenExecCommand() {
        final File mvnHome = getMavenHome();
        if (mvnHome != null) {
            final String mvn = "bin" + File.separatorChar + "mvn";
            return new File(mvnHome, mvn).getAbsolutePath(); // use Maven home directory if it's set
        } else {
            return "mvn"; // otherwise 'mvn' should be in PATH variable
        }
    }

    /**
     * Returns Maven home directory.
     *
     * @return Maven home directory
     */
    public static File getMavenHome() {
        final String m2HomeEnv = System.getenv("M2_HOME");
        if (m2HomeEnv == null) {
            return null;
        }
        final File m2Home = new File(m2HomeEnv);
        return m2Home.exists() ? m2Home : null;
    }

    /**
     * Converts the specified {@link MavenProjectModel} to the internal {@link Model}.
     * <p/>
     * If a specified model contains information about an original pom.xml file,
     * from which this model has been read, then method returns an internal model
     * that will contain all information from a specified model supplemented with
     * a data from original pom.xml file.
     * Otherwise method returns an internal model which will contain only information
     * from the specified model.
     *
     * @param model
     *         model to convert
     * @return internal model or empty
     * @throws IOException
     * @throws XmlPullParserException
     */
    private static Model model2InternalModel(MavenProjectModel model) throws IOException, XmlPullParserException {
        final Model internalModel;
        if (model.getPomFile() != null) {
            final Path pomFilePath = model.getPomFile().toPath();
            Reader reader = Files.newBufferedReader(pomFilePath, Charset.forName("UTF-8"));
            internalModel = pomReader.read(reader, true);
        } else {
            internalModel = new Model();
        }

        return fillInternalModel(internalModel, model);
    }

    private static Model fillInternalModel(Model internalModel, MavenProjectModel model) {
        internalModel.setGroupId(model.getGroupId());
        internalModel.setArtifactId(model.getArtifactId());
        internalModel.setVersion(model.getVersion());
        internalModel.setDescription(model.getDescription());
        internalModel.setPackaging(model.getPackaging());
        internalModel.setName(model.getName());

        // add all dependencies
        List<MavenDependency> dependencies = model.getDependencies();
        if (dependencies != null && dependencies.size() > 0) {
            for (MavenDependency dependency : dependencies) {
                Dependency internalDependency = new Dependency();
                internalDependency.setGroupId(dependency.getGroupId());
                internalDependency.setArtifactId(dependency.getArtifactId());
                internalDependency.setVersion(dependency.getVersion());
                internalDependency.setScope(dependency.getScope());

                internalModel.addDependency(internalDependency);
            }
        }

        return internalModel;
    }
}
