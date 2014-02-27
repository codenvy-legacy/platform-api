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
package com.codenvy.api.factory.compatibility;

import com.codenvy.api.factory.FactoryUrlException;
import com.codenvy.api.factory.dto.Factory;
import com.codenvy.api.factory.dto.ProjectAttributes;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Alexander Garagatyi
 */
public class V1_1Support extends V1_0Support {
    private static final Map<String, ParameterState> support;

    static {
        support = new HashMap<>();
        support.put("idcommit", ParameterState.UNSUPPORTED);
        support.put("pname", ParameterState.UNSUPPORTED);
        support.put("ptype", ParameterState.UNSUPPORTED);
        support.put("projectattributes.pname", ParameterState.SUPPORTED);
        support.put("projectattributes.ptype", ParameterState.SUPPORTED);
        support.put("style", ParameterState.SUPPORTED);
        support.put("description", ParameterState.SUPPORTED);
        support.put("contactmail", ParameterState.SUPPORTED);
        support.put("author", ParameterState.SUPPORTED);
        support.put("openfile", ParameterState.SUPPORTED);
        support.put("orgid", ParameterState.SUPPORTED);
        support.put("affiliateid", ParameterState.SUPPORTED);
        support.put("vcsinfo", ParameterState.SUPPORTED);
        support.put("vcsbranch", ParameterState.SUPPORTED);
        support.put("variables", ParameterState.SUPPORTED);
        support.put("validsince", ParameterState.IGNORED);
        support.put("validuntil", ParameterState.IGNORED);
        support.put("welcome", ParameterState.SUPPORTED);
        support.put("wname", ParameterState.IGNORED);
    }

    @Override
    public void set(Factory factory, String key, String value) throws FactoryUrlException {
        ParameterState state = support.get(key);
        if (null == state) {
            super.set(factory, key, value);
            return;
        }
        switch (state) {
            case SUPPORTED:
                doSet(factory, key, value);
                break;
            case IGNORED:
                return;
            case UNSUPPORTED:
                throw new FactoryUrlException("Parameter " + key + " is unsupported.");
        }
    }

    @Override
    public void validate(Factory factory) {
    }

    private void doSet(Factory factory, String key, String value) {
        ProjectAttributes attributes;
        switch (key) {
            case "v":
                factory.setV("1.1");
                break;
            // TODO
        }
    }
}
