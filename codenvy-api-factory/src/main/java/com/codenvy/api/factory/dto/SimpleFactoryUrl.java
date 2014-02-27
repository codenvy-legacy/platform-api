package com.codenvy.api.factory.dto;

import com.codenvy.dto.shared.DTO;

import java.util.List;
import java.util.Map;

/**
 * Holds factory url parameters values, used in factory v1.0
 */
@DTO
public interface SimpleFactoryUrl {
    void setV(String version);

    void setVcs(String vcs);

    void setVcsurl(String vcsUrl);

    void setCommitid(String commitId);

    void setAction(String action);

    void setOpenfile(String openFile);

    void setVcsinfo(boolean vcsInfo);

    void setOrgid(String orgid);

    void setAffiliateid(String affiliateid);

    void setVcsbranch(String vcsbranch);

    void setProjectattributes(Map<String, String> projectAttributes);

    String getV();

    String getVcs();

    String getVcsurl();

    String getCommitid();

    String getAction();

    String getOpenfile();

    boolean getVcsinfo();

    String getOrgid();

    String getAffiliateid();

    String getVcsbranch();

    Map<String, String> getProjectattributes();

    List<Variable> getVariables();

    void setVariables(List<Variable> variables);
}
