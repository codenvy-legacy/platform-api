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
    private String version;
    private String vcs;
    private String vcsUrl;
    private String commitId;

    // optional parameters
    private String action;
    private String openFile;
    private boolean keepVcsInfo = false;
    private Map<String, String> projectAttributes = Collections.emptyMap();

    public SimpleFactoryUrl() {
    }

    public SimpleFactoryUrl(String version, String vcs, String vcsUrl, String commitId, String action, String openFile, boolean keepVcsInfo,
                            Map<String, String> projectAttributes) {
        this.version = version;
        this.vcs = vcs;
        this.vcsUrl = vcsUrl;
        this.commitId = commitId;
        this.action = action;
        this.openFile = openFile;
        this.keepVcsInfo = keepVcsInfo;

        setProjectattributes(projectAttributes);
    }

    public void setV(String version) {
        this.version = version;
    }

    public void setVcs(String vcs) {
        this.vcs = vcs;
    }

    // Method mame should be lowercased to use correctly from json builder.
    public void setVcsurl(String vcsUrl) {
        this.vcsUrl = vcsUrl;
    }

    // Method mame should be lowercased to use correctly from json builder.
    public void setCommitid(String commitId) {
        this.commitId = commitId;
    }

    public void setAction(String action) {
        this.action = action;
    }

    // Method mame should be lowercased to use correctly from json builder.
    public void setOpenfile(String openFile) {
        this.openFile = openFile;
    }

    // Method mame should be lowercased to use correctly from json builder.
    public void setProjectattributes(Map<String, String> projectAttributes) {
        if (projectAttributes != null) {
            this.projectAttributes = new LinkedHashMap<>(projectAttributes);
        }
    }

    public String getVersion() {
        return version;
    }

    public String getVcs() {
        return vcs;
    }

    public String getVcsUrl() {
        return vcsUrl;
    }

    public String getCommitId() {
        return commitId;
    }

    public String getAction() {
        return action;
    }


    public String getOpenFile() {
        return openFile;
    }

    public boolean getKeepvcsinfo() {
        return keepVcsInfo;
    }

    public void setKeepvcsinfo(boolean keepVcsInfo) {
        this.keepVcsInfo = keepVcsInfo;
    }

    public Map<String, String> getProjectAttributes() {
        return Collections.unmodifiableMap(projectAttributes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SimpleFactoryUrl)) return false;

        SimpleFactoryUrl that = (SimpleFactoryUrl)o;

        if (keepVcsInfo != that.keepVcsInfo) return false;
        if (action != null ? !action.equals(that.action) : that.action != null) return false;
        if (commitId != null ? !commitId.equals(that.commitId) : that.commitId != null) return false;
        if (openFile != null ? !openFile.equals(that.openFile) : that.openFile != null) return false;
        if (projectAttributes != null ? !projectAttributes.equals(that.projectAttributes) : that.projectAttributes != null) return false;
        if (vcs != null ? !vcs.equals(that.vcs) : that.vcs != null) return false;
        if (vcsUrl != null ? !vcsUrl.equals(that.vcsUrl) : that.vcsUrl != null) return false;
        if (version != null ? !version.equals(that.version) : that.version != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = version != null ? version.hashCode() : 0;
        result = 31 * result + (vcs != null ? vcs.hashCode() : 0);
        result = 31 * result + (vcsUrl != null ? vcsUrl.hashCode() : 0);
        result = 31 * result + (commitId != null ? commitId.hashCode() : 0);
        result = 31 * result + (action != null ? action.hashCode() : 0);
        result = 31 * result + (openFile != null ? openFile.hashCode() : 0);
        result = 31 * result + (keepVcsInfo ? 1 : 0);
        result = 31 * result + (projectAttributes != null ? projectAttributes.hashCode() : 0);
        return result;
    }
}
