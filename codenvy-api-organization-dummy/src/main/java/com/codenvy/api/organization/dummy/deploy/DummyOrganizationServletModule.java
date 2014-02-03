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
package com.codenvy.api.organization.dummy.deploy;

import com.codenvy.api.organization.dummy.filter.DummyRequestFilter;
import com.codenvy.inject.DynaModule;
import com.google.inject.servlet.ServletModule;
import org.everrest.guice.servlet.GuiceEverrestServlet;

@DynaModule
public class DummyOrganizationServletModule extends ServletModule {
    @Override
    protected void configureServlets() {
        filter("/*").through(DummyRequestFilter.class);
        serve("/*").with(GuiceEverrestServlet.class);
    }
}
