package com.codenvy.api.core.rest;

import com.codenvy.commons.lang.IoUtil;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.nio.file.Files;

/** @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a> */
public class FileAdapterTest {
    private FileAdapter dir;
    private FileAdapter file;

    @BeforeTest
    public void setUp() throws Exception {
        dir = new FileAdapter(createDirectory("___dir"), "");
        file = new FileAdapter(createFile("___file.txt"), "___file.txt");
    }

    @AfterTest
    public void tearDown() {
        IoUtil.deleteRecursive(dir.getIoFile());
        file.getIoFile().delete();
    }

    @Test
    public void testGetChildDirectory() throws Exception {
        FileAdapter child = dir.getChild("test.txt");
        Assert.assertTrue(child.exists());
    }

    @Test(expectedExceptions = InvalidArgumentException.class)
    public void testGetChildDirectoryInvalidPath() throws Exception {
        dir.getChild("../test.txt");
    }

    @Test(expectedExceptions = InvalidArgumentException.class)
    public void testGetChildFile() throws Exception {
        file.getChild("test.txt");
    }

    private java.io.File createDirectory(String name) throws Exception {
        java.io.File dir = new java.io.File(
                new java.io.File(Thread.currentThread().getContextClassLoader().getResource(".").toURI()).getParentFile(),
                name);
        if (!(dir.exists() || dir.mkdirs())) {
            Assert.fail("Unable create directory");
        }
        Files.createFile(new java.io.File(dir, "test.txt").toPath());
        return dir;
    }

    private java.io.File createFile(String name) throws Exception {
        java.io.File root = new java.io.File(Thread.currentThread().getContextClassLoader().getResource(".").toURI()).getParentFile();
        return Files.createFile(new java.io.File(root, name).toPath()).toFile();
    }
}
