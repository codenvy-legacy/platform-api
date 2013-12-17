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
package com.codenvy.api.core.util;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public class DefaultShellFactory extends ShellFactory {
    @Override
    public Shell getShell() {
        if (SystemInfo.isUnix()) {
            return new StandardLinuxShell();
        }
        throw new IllegalStateException("Unsupported OS");
    }

    /** Creates command line for standard command language interpreter. */
    public static class StandardLinuxShell implements Shell {

        @Override
        public String[] createShellCommand(CommandLine args) {
            final String[] array = args.asArray();
            if (array.length == 0) {
                throw new IllegalArgumentException("Command line is empty");
            }
            StringBuilder buff = new StringBuilder();
            for (String str : array) {
                if (!str.isEmpty()) {
                    if (buff.length() > 0) {
                        buff.append(' ');
                    }
                    for (int i = 0, len = str.length(); i < len; i++) {
                        char c = str.charAt(i);
                        switch (c) {
                            case ' ':
                            case '|':
                            case '>':
                            case '$':
                            case '"':
                            case '\'':
                            case '&':
                                buff.append('\\');
                                buff.append(c);
                                break;
                            case '\n':
                                buff.append('\\');
                                buff.append('n');
                                break;
                            case '\r':
                                buff.append('\\');
                                buff.append('r');
                                break;
                            case '\t':
                                buff.append('\\');
                                buff.append('t');
                                break;
                            case '\b':
                                buff.append('\\');
                                buff.append('b');
                                break;
                            case '\f':
                                buff.append('\\');
                                buff.append('f');
                                break;
                            default:
                                buff.append(c);
                                break;
                        }
                    }
                }
            }

            final String[] line = new String[3];
            line[0] = "/bin/bash";
            line[1] = "-cl";
            line[2] = buff.toString();
            return line;
        }
    }
}
