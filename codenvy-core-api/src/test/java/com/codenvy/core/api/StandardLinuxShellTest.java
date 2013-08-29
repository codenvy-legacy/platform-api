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
package com.codenvy.core.api;

import com.codenvy.core.api.util.CommandLine;
import com.codenvy.core.api.util.DefaultShellFactory;

import org.testng.Assert;
import org.testng.annotations.Test;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public class StandardLinuxShellTest {

    @Test
    public void testEscapeSpaces() throws Exception {
        CommandLine cmd = new CommandLine().add("ls", "-l", "/home/andrew/some dir");
        final String[] line = new DefaultShellFactory.StandardLinuxShell().createShellCommand(cmd);
        final String[] expected = {"/bin/bash", "-cl", "ls -l /home/andrew/some\\ dir"};
        Assert.assertEquals(line, expected);
    }

    @Test
    public void testEscapeControls() {
        CommandLine cmd = new CommandLine().add("ls", "-l", "/home/andrew/c|r>a$z\"y'dir&");
        final String[] line = new DefaultShellFactory.StandardLinuxShell().createShellCommand(cmd);
        final String[] expected = {"/bin/bash", "-cl", "ls -l /home/andrew/c\\|r\\>a\\$z\\\"y\\'dir\\&"};
        Assert.assertEquals(line, expected);
    }

    @Test
    public void testEscapeSpecCharacters() {
        CommandLine cmd = new CommandLine().add("ls", "-l", "/home/andrew/some\n\r\t\b\fdir");
        final String[] line = new DefaultShellFactory.StandardLinuxShell().createShellCommand(cmd);
        final String[] expected = {"/bin/bash", "-cl", "ls -l /home/andrew/some\\n\\r\\t\\b\\fdir"};
        Assert.assertEquals(line, expected);
    }
}
