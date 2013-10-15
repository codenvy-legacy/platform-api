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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Advanced factory format for factory 1.1. Contains additional information about factory. */
public class AdvancedFactoryUrl extends SimpleFactoryUrl {
    private String id;
    private String style;
    private String description;
    private String contactmail;
    private String author;
    private Set<Link> links = new LinkedHashSet<>();

    public AdvancedFactoryUrl() {
        super();
    }

    public AdvancedFactoryUrl(String version, String vcs, String vcsUrl, String commitId, String action, String openFile,
                              boolean vcsInfo, String orgid, String affiliateid, String vcsBranch, Map<String, String> projectAttributes) {
        super(version, vcs, vcsUrl, commitId, action, openFile, vcsInfo, orgid, affiliateid, vcsBranch, projectAttributes);
    }

    public AdvancedFactoryUrl(AdvancedFactoryUrl originFactory, Set<Link> links) {
        super(originFactory.getV(), originFactory.getVcs(), originFactory.getVcsurl(),
              originFactory.getCommitid(), originFactory.getAction(), originFactory.getOpenfile(), originFactory.getVcsinfo(),
              originFactory.getOrgid(), originFactory.getAffiliateid(), originFactory.getVcsbranch(), originFactory.getProjectattributes());

        id = originFactory.getId();
        style = originFactory.getStyle();
        description = originFactory.getDescription();
        contactmail = originFactory.getContactmail();
        author = originFactory.getAuthor();

        setLinks(links);
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContactmail() {
        return contactmail;
    }

    public void setContactmail(String contactMail) {
        this.contactmail = contactMail;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Set<Link> getLinks() {
        return Collections.unmodifiableSet(links);
    }

    public void setLinks(Set<Link> links) {
        this.links = links;
        if (links != null) {
            this.links = new LinkedHashSet<>(links);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AdvancedFactoryUrl)) return false;
        if (!super.equals(o)) return false;

        AdvancedFactoryUrl that = (AdvancedFactoryUrl)o;

        if (author != null ? !author.equals(that.author) : that.author != null) return false;
        if (contactmail != null ? !contactmail.equals(that.contactmail) : that.contactmail != null) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (links != null ? !links.equals(that.links) : that.links != null) return false;
        if (style != null ? !style.equals(that.style) : that.style != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (style != null ? style.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (contactmail != null ? contactmail.hashCode() : 0);
        result = 31 * result + (author != null ? author.hashCode() : 0);
        result = 31 * result + (links != null ? links.hashCode() : 0);
        return result;
    }
}
