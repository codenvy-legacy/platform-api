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
import com.codenvy.dto.server.DtoFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Alexander Garagatyi
 */
// TODO check mandatory
// TODO check encoded
// TODO validation
public class FactoryCompatibilityMap {
    private final        FactoryVersionSupporter              versionSupporter;
    private final        Factory                              factory;
    private final static Map<String, FactoryVersionSupporter> factoryVersionsSupporters;

    static {
        factoryVersionsSupporters = new HashMap<>();
        factoryVersionsSupporters.put("1.0", new V1_0Support());
        factoryVersionsSupporters.put("1.1", new V1_1Support());
        //factoryVersionsSupporters.put("1.2", new V1_2Support());
    }

    private FactoryCompatibilityMap(String version, Factory factory) {
        if (factoryVersionsSupporters.get(version) == null) {
            throw new IllegalArgumentException("This version is unsupported.");
        }
        this.versionSupporter = factoryVersionsSupporters.get(version);
        this.factory = factory;
    }

    public static FactoryCompatibilityMap create(String version) {
        return new FactoryCompatibilityMap(version, DtoFactory.getInstance().createDto(Factory.class));
    }

    public FactoryCompatibilityMap set(String key, String value) throws FactoryUrlException {
        versionSupporter.set(factory, key, value);
        return this;
    }

    public static Factory processFactory(Factory factory) {
        return null;
    }

    public Factory getFactory() {
        return factory;
    }
}
