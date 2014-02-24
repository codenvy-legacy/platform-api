/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2014] Codenvy, S.A. 
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
package com.codenvy.api.project.server;

import com.codenvy.api.vfs.server.VirtualFile;
import com.codenvy.api.vfs.server.VirtualFileSystemUser;
import com.codenvy.api.vfs.server.VirtualFileSystemUserContext;
import com.codenvy.api.vfs.server.impl.memory.MemoryMountPoint;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author andrew00x
 */
public class FolderEntryTest {
    private static final String      vfsUserName   = "dev";
    private static final Set<String> vfsUserGroups = new LinkedHashSet<>(Arrays.asList("workspace/developer"));

    private MemoryMountPoint mmp;
    private VirtualFile      myVfProject;
    private VirtualFile      myVfFolder;
    private FolderEntry      myFolder;

    @BeforeMethod
    public void setUp() throws Exception {
        mmp = new MemoryMountPoint(null, new VirtualFileSystemUserContext() {
            @Override
            public VirtualFileSystemUser getVirtualFileSystemUser() {
                return new VirtualFileSystemUser(vfsUserName, vfsUserGroups);
            }
        });
        VirtualFile myVfRoot = mmp.getRoot();
        myVfProject = myVfRoot.createFolder("my_project");
        myVfProject.createFolder(".codenvy").createFile("project", null, null);
        myVfFolder = myVfProject.createFolder("test_folder");
        myVfFolder.createFile("child_file", "text/plain", new ByteArrayInputStream("to be or not to be".getBytes()));
        myVfFolder.createFolder("child_folder");
        myFolder = new FolderEntry(myVfFolder);
        Assert.assertTrue(myFolder.isFolder());
    }

    @Test
    public void testGetName() throws Exception {
        Assert.assertEquals(myFolder.getName(), myVfFolder.getName());
    }

    @Test
    public void testGetPath() throws Exception {
        Assert.assertEquals(myFolder.getPath(), myVfFolder.getPath());
    }

    @Test
    public void testGetParent() throws Exception {
        Assert.assertEquals(myFolder.getParent().getPath(), myVfProject.getPath());
    }

    @Test
    public void testRename() throws Exception {
        String name = myFolder.getName();
        String newName = name + "_renamed";
        String newPath = myVfProject.getVirtualFilePath().newPath(newName).toString();

        myFolder.rename(newName);
        Assert.assertNull(myVfProject.getChild(name));
        Assert.assertNotNull(myVfProject.getChild(newName));
        Assert.assertEquals(myFolder.getName(), newName);
        Assert.assertEquals(myFolder.getPath(), newPath);
        Assert.assertNotNull(myFolder.getChild("child_file"));
        Assert.assertNotNull(myFolder.getChild("child_folder"));
    }

    @Test
    public void testMove() throws Exception {
        VirtualFile vfProject = mmp.getRoot().createFolder("my_project_2");
        vfProject.createFolder(".codenvy").createFile("project", null, null);
        String name = myFolder.getName();
        String newPath = vfProject.getVirtualFilePath().newPath(name).toString();

        myFolder.moveTo(vfProject.getPath());
        Assert.assertNull(myVfProject.getChild(name));
        Assert.assertNotNull(vfProject.getChild(name));
        Assert.assertEquals(myFolder.getName(), name);
        Assert.assertEquals(myFolder.getPath(), newPath);
        Assert.assertNotNull(myFolder.getChild("child_file"));
        Assert.assertNotNull(myFolder.getChild("child_folder"));
    }

    @Test
    public void testCopy() throws Exception {
        VirtualFile vfProject = mmp.getRoot().createFolder("my_project_2");
        vfProject.createFolder(".codenvy").createFile("project", null, null);
        String name = myFolder.getName();
        String newPath = vfProject.getVirtualFilePath().newPath(name).toString();

        FolderEntry copy = myFolder.copyTo(vfProject.getPath());
        Assert.assertNotNull(myVfProject.getChild(name));
        Assert.assertNotNull(vfProject.getChild(name));
        Assert.assertEquals(copy.getName(), name);
        Assert.assertEquals(copy.getPath(), newPath);
        Assert.assertNotNull(myFolder.getChild("child_file"));
        Assert.assertNotNull(myFolder.getChild("child_folder"));
    }

    @Test
    public void testRemove() throws Exception {
        String name = myFolder.getName();
        myFolder.remove();
        Assert.assertFalse(myVfFolder.exists());
        Assert.assertNull(myVfProject.getChild(name));
    }

    @Test
    public void testUnzip() throws Exception {
        byte[] content = "to be or not to be".getBytes();
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ZipOutputStream zipOut = new ZipOutputStream(bout);
        zipOut.putNextEntry(new ZipEntry("folder1/"));
        zipOut.putNextEntry(new ZipEntry("folder2/"));
        zipOut.putNextEntry(new ZipEntry("folder3/"));
        zipOut.putNextEntry(new ZipEntry("folder1/file1.txt"));
        zipOut.write(content);
        zipOut.putNextEntry(new ZipEntry("folder2/file2.txt"));
        zipOut.write(content);
        zipOut.putNextEntry(new ZipEntry("folder3/file3.txt"));
        zipOut.write(content);
        zipOut.close();
        byte[] zip = bout.toByteArray();
        myFolder.unzip(new ByteArrayInputStream(zip));
        Assert.assertNotNull(myFolder.getChild("folder1"));
        Assert.assertNotNull(myFolder.getChild("folder1/file1.txt"));
        Assert.assertNotNull(myFolder.getChild("folder2"));
        Assert.assertNotNull(myFolder.getChild("folder2/file2.txt"));
        Assert.assertNotNull(myFolder.getChild("folder3"));
        Assert.assertNotNull(myFolder.getChild("folder3/file3.txt"));
    }
}
