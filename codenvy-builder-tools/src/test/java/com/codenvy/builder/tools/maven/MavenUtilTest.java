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

import org.testng.Assert;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/** @author andrew00x */
public class MavenUtilTest {
    @Test
    public void testReadPom() throws IOException {
        URL pom = Thread.currentThread().getContextClassLoader().getResource("test-pom.xml");
        Assert.assertNotNull(pom);
        MavenProjectModel model = MavenUtils.readModel(new File(pom.getFile()));
        Assert.assertEquals(model.getGroupId(), "parent");
        Assert.assertEquals(model.getArtifactId(), "a");
        Assert.assertEquals(model.getVersion(), "x.x.x");
        MavenProjectModel parent = model.getParent();
        Assert.assertEquals(parent.getGroupId(), "parent");
        Assert.assertEquals(parent.getArtifactId(), "parent");
        Assert.assertEquals(parent.getVersion(), "x.x.x");
        List<MavenDependency> dependencies = model.getDependencies();
        Assert.assertEquals(dependencies.size(), 1);
        MavenDependency dependency = dependencies.get(0);
        Assert.assertEquals(dependency.getGroupId(), "x");
        Assert.assertEquals(dependency.getArtifactId(), "y");
        Assert.assertEquals(dependency.getVersion(), "z");
    }

    @Test
    public void testWrite() throws Exception {
        File workDir = new File(System.getProperty("workDir"));
        File pom = new File(workDir, "testWrite-pom.xml");
        List<MavenDependency> deps = new ArrayList<>(1);
        deps.add(new MavenDependency("x", "y", "z", "test"));
        MavenProjectModel model = new MavenProjectModel("a", "b", "c", null, null, "test pom", null, deps, pom, workDir);
        MavenUtils.writeModel(model);
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document dom = documentBuilder.parse(pom);
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();
        Assert.assertEquals(xpath.evaluate("/project/groupId", dom, XPathConstants.STRING), "a");
        Assert.assertEquals(xpath.evaluate("/project/artifactId", dom, XPathConstants.STRING), "b");
        Assert.assertEquals(xpath.evaluate("/project/version", dom, XPathConstants.STRING), "c");
        Assert.assertEquals(xpath.evaluate("/project/description", dom, XPathConstants.STRING), "test pom");
        NodeList depsNodeList = (NodeList)xpath.evaluate("/project/dependencies", dom, XPathConstants.NODESET);
        Assert.assertEquals(depsNodeList.getLength(), 1);
        Node node = depsNodeList.item(0);
        Assert.assertEquals(xpath.evaluate("dependency/groupId", node, XPathConstants.STRING), "x");
        Assert.assertEquals(xpath.evaluate("dependency/artifactId", node, XPathConstants.STRING), "y");
        Assert.assertEquals(xpath.evaluate("dependency/version", node, XPathConstants.STRING), "z");
        Assert.assertEquals(xpath.evaluate("dependency/scope", node, XPathConstants.STRING), "test");
    }

    @Test
    public void testWriteToFile() throws Exception {
        File workDir = new File(System.getProperty("workDir"));
        File pom = new File(workDir, "testWrite-pom.xml");
        File pom2 = new File(workDir, "testWriteToFile-pom.xml");
        List<MavenDependency> deps = new ArrayList<>(1);
        deps.add(new MavenDependency("x", "y", "z", "test"));
        MavenProjectModel model = new MavenProjectModel("a", "b", "c", null, null, "test pom", null, deps, pom, workDir);
        MavenUtils.writeModel(model, pom2);
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document dom = documentBuilder.parse(pom2);
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();
        Assert.assertEquals(xpath.evaluate("/project/groupId", dom, XPathConstants.STRING), "a");
        Assert.assertEquals(xpath.evaluate("/project/artifactId", dom, XPathConstants.STRING), "b");
        Assert.assertEquals(xpath.evaluate("/project/version", dom, XPathConstants.STRING), "c");
        Assert.assertEquals(xpath.evaluate("/project/description", dom, XPathConstants.STRING), "test pom");
        NodeList depsNodeList = (NodeList)xpath.evaluate("/project/dependencies", dom, XPathConstants.NODESET);
        Assert.assertEquals(depsNodeList.getLength(), 1);
        Node node = depsNodeList.item(0);
        Assert.assertEquals(xpath.evaluate("dependency/groupId", node, XPathConstants.STRING), "x");
        Assert.assertEquals(xpath.evaluate("dependency/artifactId", node, XPathConstants.STRING), "y");
        Assert.assertEquals(xpath.evaluate("dependency/version", node, XPathConstants.STRING), "z");
        Assert.assertEquals(xpath.evaluate("dependency/scope", node, XPathConstants.STRING), "test");
    }

    @Test
    public void testAddDependency() throws Exception {
        File workDir = new File(System.getProperty("workDir"));
        File pom = new File(workDir, "testAddDependency-pom.xml");
        MavenProjectModel model = new MavenProjectModel("a", "b", "c", null, null, "test pom", null, null, pom, workDir);
        MavenUtils.writeModel(model);
        MavenUtils.addDependency(pom, new MavenDependency("x", "y", "z", "test"));
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document dom = documentBuilder.parse(pom);
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();
        NodeList depsNodeList = (NodeList)xpath.evaluate("/project/dependencies", dom, XPathConstants.NODESET);
        Assert.assertEquals(depsNodeList.getLength(), 1);
        Node node = depsNodeList.item(0);
        Assert.assertEquals(xpath.evaluate("dependency/groupId", node, XPathConstants.STRING), "x");
        Assert.assertEquals(xpath.evaluate("dependency/artifactId", node, XPathConstants.STRING), "y");
        Assert.assertEquals(xpath.evaluate("dependency/version", node, XPathConstants.STRING), "z");
        Assert.assertEquals(xpath.evaluate("dependency/scope", node, XPathConstants.STRING), "test");
    }

    @Test
    public void testAddDependencyWithModel() throws Exception {
        File workDir = new File(System.getProperty("workDir"));
        File pom = new File(workDir, "testAddDependencyWithModel-pom.xml");
        MavenProjectModel model = new MavenProjectModel("a", "b", "c", null, null, "test pom", null, null, pom, workDir);
        MavenUtils.writeModel(model);
        MavenUtils.addDependency(pom, new MavenProjectModel("x", "y", "z", null, null, "test dependency pom", null, null, null, null));
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document dom = documentBuilder.parse(pom);
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();
        NodeList depsNodeList = (NodeList)xpath.evaluate("/project/dependencies", dom, XPathConstants.NODESET);
        Assert.assertEquals(depsNodeList.getLength(), 1);
        Node node = depsNodeList.item(0);
        Assert.assertEquals(xpath.evaluate("dependency/groupId", node, XPathConstants.STRING), "x");
        Assert.assertEquals(xpath.evaluate("dependency/artifactId", node, XPathConstants.STRING), "y");
        Assert.assertEquals(xpath.evaluate("dependency/version", node, XPathConstants.STRING), "z");
        // there is no 'scope' in this case
        Assert.assertEquals(xpath.evaluate("dependency/scope", node, XPathConstants.STRING), "");
    }
}
