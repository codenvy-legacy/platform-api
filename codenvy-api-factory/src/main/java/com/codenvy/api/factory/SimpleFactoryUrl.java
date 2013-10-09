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
import java.util.LinkedHashMap;
import java.util.Map;

/** Holds factory url parameters values, used in factory v1.0 */
public class SimpleFactoryUrl {
    // mandatory parameters
    private String v;
    private String vcs;
    private String vcsurl;
    private String commitid;

    // optional parameters
    private String action;
    private String openfile;
    private boolean keepvcsinfo = false;
    private String orgid;
    private String affiliateid;
    private Map<String, String> projectattributes = Collections.emptyMap();

    public SimpleFactoryUrl() {
    }

    public SimpleFactoryUrl(String version, String vcs, String vcsUrl, String commitId, String action, String openFile, boolean keepVcsInfo,
                            String orgid, String affiliateid, Map<String, String> projectAttributes) {
        this.v = version;
        this.vcs = vcs;
        this.vcsurl = vcsUrl;
        this.commitid = commitId;
        this.action = action;
        this.openfile = openFile;
        this.keepvcsinfo = keepVcsInfo;
        this.orgid = orgid;
        this.affiliateid = affiliateid;

        setProjectattributes(projectAttributes);
    }

    public void setV(String version) {
        this.v = version;
    }

    public void setVcs(String vcs) {
        this.vcs = vcs;
    }

    public void setVcsurl(String vcsUrl) {
        this.vcsurl = vcsUrl;
    }

    public void setCommitid(String commitId) {
        this.commitid = commitId;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setOpenfile(String openFile) {
        this.openfile = openFile;
    }

    public void setKeepvcsinfo(boolean keepVcsInfo) {
        this.keepvcsinfo = keepVcsInfo;
    }

    public void setOrgid(String orgid) {
        this.orgid = orgid;
    }

    public void setAffiliateid(String affiliateid) {
        this.affiliateid = affiliateid;
    }

    // Method mame should be lowercased to use correctly from json builder.
    public void setProjectattributes(Map<String, String> projectAttributes) {
        if (projectAttributes != null) {
            this.projectattributes = new LinkedHashMap<>(projectAttributes);
        }
    }

    public String getV() {
        return v;
    }

    public String getVcs() {
        return vcs;
    }

    public String getVcsurl() {
        return vcsurl;
    }

    public String getCommitid() {
        return commitid;
    }

    public String getAction() {
        return action;
    }


    public String getOpenfile() {
        return openfile;
    }

    public boolean getKeepvcsinfo() {
        return keepvcsinfo;
    }

    public String getOrgid() {
        return orgid;
    }

    public String getAffiliateid() {
        return affiliateid;
    }

    public Map<String, String> getProjectattributes() {
        return Collections.unmodifiableMap(projectattributes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SimpleFactoryUrl)) return false;

        SimpleFactoryUrl that = (SimpleFactoryUrl)o;

        if (keepvcsinfo != that.keepvcsinfo) return false;
        if (action != null ? !action.equals(that.action) : that.action != null) return false;
        if (affiliateid != null ? !affiliateid.equals(that.affiliateid) : that.affiliateid != null) return false;
        if (commitid != null ? !commitid.equals(that.commitid) : that.commitid != null) return false;
        if (openfile != null ? !openfile.equals(that.openfile) : that.openfile != null) return false;
        if (orgid != null ? !orgid.equals(that.orgid) : that.orgid != null) return false;
        if (projectattributes != null ? !projectattributes.equals(that.projectattributes) : that.projectattributes != null) return false;
        if (vcs != null ? !vcs.equals(that.vcs) : that.vcs != null) return false;
        if (vcsurl != null ? !vcsurl.equals(that.vcsurl) : that.vcsurl != null) return false;
        if (v != null ? !v.equals(that.v) : that.v != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = v != null ? v.hashCode() : 0;
        result = 31 * result + (vcs != null ? vcs.hashCode() : 0);
        result = 31 * result + (vcsurl != null ? vcsurl.hashCode() : 0);
        result = 31 * result + (commitid != null ? commitid.hashCode() : 0);
        result = 31 * result + (action != null ? action.hashCode() : 0);
        result = 31 * result + (openfile != null ? openfile.hashCode() : 0);
        result = 31 * result + (keepvcsinfo ? 1 : 0);
        result = 31 * result + (orgid != null ? orgid.hashCode() : 0);
        result = 31 * result + (affiliateid != null ? affiliateid.hashCode() : 0);
        result = 31 * result + (projectattributes != null ? projectattributes.hashCode() : 0);
        return result;
    }
}
