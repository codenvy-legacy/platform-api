/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.core.factory;

import java.lang.annotation.*;

/**
 * Provide factory parameter compatibility options.
 *
 * @author Alexander Garagatyi
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FactoryParameter {
    enum Obligation {
        MANDATORY, OPTIONAL
    }

    enum FactoryFormat {
        ENCODED, NONENCODED, BOTH
    }

    enum Version {
        // NEVER must be the last defined constant
        V1_0, V1_1, V1_2, V2_0, NEVER;

        public static Version fromString(String v) {
            if (null != v) {
                switch (v) {
                    case "1.0":
                        return V1_0;
                    case "1.1":
                        return V1_1;
                    case "1.2":
                        return V1_2;
                    case "2.0":
                        return V2_0;
                }
            }

            throw new IllegalArgumentException("Unknown version " + v + ".");
        }

        @Override
        public String toString() {
            return super.name().substring(1).replace('_', '.');
        }
    }

    FactoryFormat format() default FactoryFormat.BOTH;

    Obligation obligation();

    boolean setByServer() default false;

    boolean trackedOnly() default false;

    String queryParameterName();

    Version deprecatedSince() default Version.NEVER;

    Version ignoredSince() default Version.NEVER;
}
