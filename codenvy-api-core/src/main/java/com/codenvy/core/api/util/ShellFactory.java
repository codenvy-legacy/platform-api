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
package com.codenvy.core.api.util;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public abstract class ShellFactory {
    private static final ShellFactory INSTANCE = ComponentLoader.one(ShellFactory.class);

    public static ShellFactory getInstance() {
        return INSTANCE;
    }

    public abstract Shell getShell();

    /** Creates command line to run system command from set of arguments. */
    public static interface Shell {
        String[] createShellCommand(CommandLine args);
    }
}
