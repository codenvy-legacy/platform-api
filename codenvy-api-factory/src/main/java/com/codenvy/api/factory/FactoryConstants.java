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
package com.codenvy.api.factory;

/**
 * Message constants for factory builder.
 */
public class FactoryConstants {
    public static final String INVALID_PARAMETER_MESSAGE =
            "Passed in an invalid parameter.  You either provided a non-valid parameter, or that parameter is not " +
            "accepted for this Factory version.  For more information, please visit " +
            "http://docs.codenvy.com/user/creating-factories/factory-parameter-reference/.";

    public static final String INVALID_VERSION_MESSAGE =
            "You have provided an inaccurate or deprecated Factory Version.  For more information, " +
            "please visit: http://docs.codenvy.com/user/creating-factories/factory-parameter-reference/.";

    public static final String UNPARSABLE_FACTORY_MESSAGE                     =
            "We cannot parse the provided factory. For more information, please visit: http://docs.codenvy" +
            ".com/user/creating-factories/factory-parameter-reference/";

    public static final String MISSING_MANDATORY_MESSAGE                      =
            "You are missing a mandatory parameter.  For more information, please visit: http://docs.codenvy" +
            ".com/user/creating-factories/factory-parameter-reference/.";

    public static final String ILLEGAL_HOSTNAME_MESSAGE =
            "This Factory has its access restricted by certain hostname. Your client does not match the specified " +
            "policy. Please contact the owner of this Factory for more information.";

    public static final String PARAMETRIZED_INVALID_TRACKED_PARAMETER_MESSAGE =
            "You have provided a Tracked Factory parameter %s, and you do not have a valid orgId.  You could have " +
            "provided the wrong code, your subscription has expired, or you do not have a valid subscription account." +
            "  Please contact info@codenvy.com with any questions.";

    public static final String PARAMETRIZED_INVALID_PARAMETER_MESSAGE         =
            "You have provided an invalid parameter %s for this version of Factory parameters %s.  For more " +
            "information, please visit: http://docs.codenvy.com/user/creating-factories/factory-parameter-reference/.";

    public static final String PARAMETRIZED_ILLEGAL_ORGID_PARAMETER_MESSAGE =
            "You do not have a valid orgID. You could have provided the wrong value, your subscription has expired, " +
            "or you do not have a valid subscription account. " +
            "Please contact info@codenvy.com with any questions. \n" +
            "orgID Submitted: %s";

    public static final String PARAMETRIZED_ENCODED_ONLY_PARAMETER_MESSAGE    =
            "You submitted a parameter that can only be submitted through an encoded Factory URL %s.  For more " +
            "information, please visit: http://docs.codenvy.com/user/creating-factories/factory-parameter-reference/.";

    public static final String PARAMETRIZED_ILLEGAL_TRACKED_PARAMETER_MESSAGE =
            "You do not have a valid orgID. Your Factory configuration has a parameter that can only be used with a " +
            "Tracked Factory subscription. You could have provided the wrong value, " +
            "your subscription has expired, or you do not have a valid subscription account. " +
            "Please contact info@codenvy.com with any questions. \n" +
            "orgID Submitted: %s \n" +
            "Invalid Parameter Name: %s";


    public static final String ILLEGAL_VALIDSINCE_MESSAGE =
            "This Factory is not yet valid due to time restrictions applied by its owner.  Please, " +
            "contact owner for more information.";

    public static final String ILLEGAL_VALIDUNTIL_MESSAGE =
            "This Factory has expired due to time restrictions applied by its owner.  Please, " +
            "contact owner for more information.";

    public static final String PARAMETRIZED_ILLEGAL_PARAMETER_VALUE_MESSAGE =
            "The parameter %s has a value submitted %s with a value that is unexpected. For more information, " +
            "please visit: http://docs.codenvy.com/user/creating-factories/factory-parameter-reference/.";
}
