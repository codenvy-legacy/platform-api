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

import java.util.LinkedHashSet;
import java.util.Set;

/** Class holds Factory url parameters values */
public class FactoryUrl {
    private String version;
    private String vcs;
    private String vcsUrl;
    private String commitId;
    private Set<Link> links = new LinkedHashSet<>();

    public FactoryUrl() {
    }

    public FactoryUrl(String version, String vcs, String vcsUrl, String commitId) {
        this.version = version;
        this.vcs = vcs;
        this.vcsUrl = vcsUrl;
        this.commitId = commitId;
    }

    public FactoryUrl(String version, String vcs, String vcsUrl, String commitId, Set<Link> links) {
        this.version = version;
        this.vcs = vcs;
        this.vcsUrl = vcsUrl;
        this.commitId = commitId;

        setLinks(links);
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

    public Set<Link> getLinks() {
        return links;
    }

    public void setLinks(Set<Link> links) {
        this.links = links;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FactoryUrl)) return false;

        FactoryUrl that = (FactoryUrl)o;

        if (commitId != null ? !commitId.equals(that.commitId) : that.commitId != null) return false;
        if (links != null ? !links.equals(that.links) : that.links != null) return false;
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
        result = 31 * result + (links != null ? links.hashCode() : 0);
        return result;
    }
}
