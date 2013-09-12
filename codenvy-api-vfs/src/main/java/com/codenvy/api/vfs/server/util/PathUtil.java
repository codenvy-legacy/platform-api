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
package com.codenvy.api.vfs.server.util;

import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemRuntimeException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public class PathUtil {
    private static final String[] EMPTY_PATH    = new String[0];
    private static final Pattern  PATH_SPLITTER = Pattern.compile("/");

    public static String[] parse(String raw) {
        String[] parsed = ((raw == null) || raw.isEmpty() || ((raw.length() == 1) && (raw.charAt(0) == '/')))
                          ? EMPTY_PATH : PATH_SPLITTER.split(raw.charAt(0) == '/' ? raw.substring(1) : raw);
        if (parsed.length == 0) {
            return parsed;
        }
        List<String> newTokens = new ArrayList<>(parsed.length);
        for (String token : parsed) {
            if ("..".equals(token)) {
                int size = newTokens.size();
                if (size == 0) {
                    throw new VirtualFileSystemRuntimeException(String.format("Invalid path '%s', '..' on root. ", raw));
                }
                newTokens.remove(size - 1);
            } else if (!".".equals(token)) {
                newTokens.add(token);
            }
        }
        return newTokens.toArray(new String[newTokens.size()]);
    }

    private PathUtil() {
    }
}
