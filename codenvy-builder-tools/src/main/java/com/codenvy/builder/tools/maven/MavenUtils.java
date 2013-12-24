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
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * A smattering of useful methods to work with the Maven POM.
 *
 * @author Artem Zatsarynnyy
 * @author andrew00x
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
     * Get description of maven project.
     *
     * @param sources
     *         maven project directory. Note: Must contains pom.xml file.
     * @return description of maven project
     * @throws IOException
     *         if an i/o error occurs
     */
    public static MavenProjectModel getModel(java.io.File sources) throws IOException {
        return toMavenProjectModel(readInternalModel(new java.io.File(sources, "pom.xml")));
    }

    /**
     * Read description of maven project.
     *
     * @param pom
     *         path to pom.xml file
     * @return description of maven project
     * @throws IOException
     *         if an i/o error occurs
     */
    public static MavenProjectModel readModel(java.io.File pom) throws IOException {
        return toMavenProjectModel(readInternalModel(pom));
    }

    /**
     * Read description of maven project.
     *
     * @param reader
     *         {@link java.io.Reader} to read content of pom.xml file.
     * @return description of maven project
     * @throws IOException
     *         if an i/o error occurs
     */
    public static MavenProjectModel readModel(Reader reader) throws IOException {
        try {
            return toMavenProjectModel(pomReader.read(reader, true));
        } catch (XmlPullParserException e) {
            throw new IOException(e);
        }
    }

    /**
     * Writes a specified {@link com.codenvy.builder.tools.maven.MavenProjectModel} to the path from which this model has been read.
     *
     * @param model
     *         model to write
     * @throws IOException
     *         if an i/o error occurs
     * @throws IllegalStateException
     *         if method {@code model.getPomFile()} returns {@code null}
     */
    public static void writeModel(MavenProjectModel model) throws IOException {
        final java.io.File pom = model.getPomFile();
        if (pom == null) {
            throw new IllegalStateException("Unable to write a model. Unknown path.");
        }
        writeModel(model, pom);
    }

    /**
     * Writes a specified {@link com.codenvy.builder.tools.maven.MavenProjectModel} to the specified {@link java.io.File}.
     *
     * @param model
     *         model to write
     * @param pom
     *         path to the file to write a model
     * @throws IOException
     *         if an i/o error occurs
     */
    public static void writeModel(MavenProjectModel model, java.io.File pom) throws IOException {
        final Model internalModel = new Model();
        updateInternalModel(internalModel, model);
        try (BufferedWriter writer = Files.newBufferedWriter(pom.toPath(), Charset.forName("UTF-8"))) {
            pomWriter.write(writer, internalModel);
        }
    }

    /**
     * Add dependency to the specified pom.xml.
     *
     * @param pom
     *         pom.xml path
     * @param dependency
     *         POM of artifact to add as dependency
     * @throws java.io.IOException
     *         if an i/o error occurs
     */
    public static void addDependency(java.io.File pom, MavenProjectModel dependency) throws IOException {
        addDependency(pom, dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), null);
    }

    public static void addDependency(java.io.File pom, MavenDependency dependency) throws IOException {
        addDependency(pom, dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), dependency.getScope());
    }

    /**
     * Add dependency to the specified pom.xml.
     *
     * @param pom
     *         pom.xml path
     * @param groupId
     *         groupId
     * @param artifactId
     *         artifactId
     * @param version
     *         artifact version
     * @throws java.io.IOException
     *         if an i/o error occurs
     */
    public static void addDependency(java.io.File pom, String groupId, String artifactId, String version, String scope) throws IOException {
        final Model internalModel = readInternalModel(pom);
        final Dependency dep = new Dependency();
        dep.setGroupId(groupId);
        dep.setArtifactId(artifactId);
        dep.setVersion(version);
        dep.setScope(scope);
        internalModel.getDependencies().add(dep);
        try (BufferedWriter writer = Files.newBufferedWriter(pom.toPath(), Charset.forName("UTF-8"))) {
            pomWriter.write(writer, internalModel);
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
        final java.io.File mvnHome = getMavenHome();
        if (mvnHome != null) {
            final String mvn = "bin" + java.io.File.separatorChar + "mvn";
            return new java.io.File(mvnHome, mvn).getAbsolutePath(); // use Maven home directory if it's set
        } else {
            return "mvn"; // otherwise 'mvn' should be in PATH variable
        }
    }

    /**
     * Returns Maven home directory.
     *
     * @return Maven home directory
     */
    public static java.io.File getMavenHome() {
        final String m2HomeEnv = System.getenv("M2_HOME");
        if (m2HomeEnv == null) {
            return null;
        }
        final java.io.File m2Home = new java.io.File(m2HomeEnv);
        return m2Home.exists() ? m2Home : null;
    }

    private static Model readInternalModel(java.io.File pom) throws IOException {
        final Model model;
        try (Reader reader = Files.newBufferedReader(pom.toPath(), Charset.forName("UTF-8"))) {
            model = readInternalModel(reader);
        }
        model.setPomFile(pom);
        return model;
    }

    private static Model readInternalModel(Reader reader) throws IOException {
        try {
            return pomReader.read(reader, true);
        } catch (XmlPullParserException e) {
            throw new IOException(e);
        }
    }

    private static void updateInternalModel(Model internalModel, MavenProjectModel model) {
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
    }

    private static MavenProjectModel toMavenProjectModel(Model internalModel) {
        final Parent parent = internalModel.getParent();
        MavenProjectModel myParent = null;
        if (parent != null) {
            myParent = new MavenProjectModel(parent.getGroupId(),
                                             parent.getArtifactId(),
                                             parent.getVersion(),
                                             null,
                                             null,
                                             null,
                                             null,
                                             null,
                                             null,
                                             null);
        }
        String groupId = internalModel.getGroupId();
        if (groupId == null && parent != null) {
            groupId = parent.getGroupId();
        }
        String version = internalModel.getVersion();
        if (version == null && parent != null) {
            version = parent.getVersion();
        }
        List<MavenDependency> dependencies = new ArrayList<>();
        for (Dependency dep : internalModel.getDependencies()) {
            dependencies.add(new MavenDependency(dep.getGroupId(), dep.getArtifactId(), dep.getVersion(), dep.getScope()));
        }
        return new MavenProjectModel(groupId,
                                     internalModel.getArtifactId(),
                                     version,
                                     internalModel.getPackaging(),
                                     internalModel.getName(),
                                     internalModel.getDescription(),
                                     myParent,
                                     dependencies,
                                     internalModel.getPomFile(),
                                     internalModel.getProjectDirectory());
    }
}
