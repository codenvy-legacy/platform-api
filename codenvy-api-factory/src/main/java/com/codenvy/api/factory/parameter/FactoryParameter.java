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
package com.codenvy.api.factory.parameter;

import com.codenvy.api.factory.FactoryFormat;

import java.lang.annotation.*;

/**
 * Provide factory parameter compatibility options.
 *
 * @author Alexander Garagatyi
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FactoryParameter {
    public enum Obligation {
        MANDATORY, OPTIONAL
    }


    public enum Version {
        // NEVER must be the last defined constant
        V1_0, V1_1, V1_2, NEVER;

        public static Version fromString(String v) {
            if (null != v) {
                switch (v) {
                    case "1.0":
                        return V1_0;
                    case "1.1":
                        return V1_1;
                    case "1.2":
                        return V1_2;
                }
            }

            throw new IllegalArgumentException("Unknown version " + v + ".");
        }

        @Override
        public String toString() {
            return super.name().substring(1).replace('_', '.');
        }
    }

    public FactoryFormat format() default FactoryFormat.BOTH;

    public Obligation obligation();

    public boolean setByServer() default false;

    public boolean trackedOnly() default false;

    public String queryParameterName();

    public Version deprecatedSince() default Version.NEVER;

    public Version ignoredSince() default Version.NEVER;
}
