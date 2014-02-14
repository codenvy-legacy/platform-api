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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Holds factory url parameters values*/
public class Factory {
    // mandatory parameters
    private String v;
    private String vcs;
    private String vcsurl;
    private String commitid;
    @Deprecated
    private String pname;
    @Deprecated
    private String ptype;
    @Deprecated
    private String idcommit;
    // optional parameters
    private String action;
    private String openfile;
    private boolean vcsinfo = false;
    private String orgid;
    private String affiliateid;
    private String vcsbranch;
    private String style;
    private String description;
    private String contactmail;
    private String author;
    private String userid;
    private long validuntil = TimeUnit.DAYS.toMillis(3650) + System.currentTimeMillis(); //10 * 365 = 10 years
    private long validsince = System.currentTimeMillis();
    private long created    = System.currentTimeMillis();
    private WelcomePage       welcome;
    private ProjectAttributes projectattributes;
    private List<Variable> variables = Collections.emptyList();
    private Set<Link>      links     = new HashSet<Link>();


}
