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
package com.codenvy.api.factory;

import java.lang.annotation.*;

/**
 * Provide factory parameter compatibility options.
 *
 * @author Alexander Garagatyi
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Compatibility {
    public enum Optionality {
        MANDATORY, OPTIONAL
    }

    public enum Encoding {
        ENCODED, NONENCODED
    }

    public enum Version {
        NEVER, V1_0, V1_1, V1_2
    }

    public Encoding[] encoding() default {Encoding.ENCODED, Encoding.NONENCODED};

    public Optionality optionality();

    public boolean trackedOnly() default false;

    public Version deprecatedSince() default Version.NEVER;

    public Version ignoredSince() default Version.NEVER;
}
