/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 * [2012] - [$today.year] Codenvy, S.A. 
 * All Rights Reserved.
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

import com.codenvy.api.project.server.exceptions.SourceImporterNotFoundException;
import com.codenvy.api.project.shared.dto.ImportSourceDescriptor;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;

import org.junit.Test;
import org.junit.Assert;
import org.junit.Before;

import java.io.IOException;
import java.util.Collections;

/**
 * @author Vitaly Parfonov
 */
public class SourceImporterExtensionRegistryTest {

    private SourceImporterExtensionRegistry importers;

    @Before
    public void setUp() {
        importers = new SourceImporterExtensionRegistry(Collections.<SourceImporterExtension>emptySet());
        importers.register(new SourceImporterExtension() {
            @Override
            public String getType() {
                return "my_importer";
            }

            @Override
            public void importSource(String workspace, String projectName, ImportSourceDescriptor importSourceDescriptor)
                    throws IOException, VirtualFileSystemException {
            }
        });
    }

    @Test
    public void testRegister() throws Exception {
        Assert.assertNotNull(importers.getImporterTypes());
        Assert.assertEquals(importers.getImporterTypes().size(), 1);
        importers.register(new SourceImporterExtension() {
            @Override
            public String getType() {
                return "new_importer";
            }

            @Override
            public void importSource(String workspace, String projectName, ImportSourceDescriptor importSourceDescriptor)
                    throws IOException, VirtualFileSystemException {

            }
        });
    }

    @Test
    public void testGetImport() throws Exception {
        Assert.assertNotNull(importers.getImporterTypes());
        Assert.assertNotNull(importers.getImporter("my_importer"));
        Assert.assertEquals(importers.getImporter("my_importer").getType(), "my_importer");
    }

    @Test(expected=SourceImporterNotFoundException.class)
    public void testGetImportNotExist() throws Exception {
        Assert.assertNotNull(importers.getImporterTypes());
        importers.getImporter("not_exist");
    }

    @Test
    public void testGetImporterTypes() throws Exception {
        Assert.assertNotNull(importers.getImporterTypes());
        Assert.assertEquals(importers.getImporterTypes().size(), 1);
        Assert.assertEquals(importers.getImporterTypes().get(0), "my_importer");
    }
}
