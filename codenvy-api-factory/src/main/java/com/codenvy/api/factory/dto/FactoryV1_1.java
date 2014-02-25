package com.codenvy.api.factory.dto;

import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.dto.shared.DTO;

import java.util.List;

/**
 * @author Sergii Kabashniuk
 */
@DTO
public interface FactoryV1_1 extends FactoryV1_0 {
    String getId();

    void setId(String id);

    void setProjectattributes(ProjectAttributes projectattributes);

    ProjectAttributes getProjectattributes();

    /**
     * @return Codenow  button style: vertical, horisontal, dark, wite
     */

    String getStyle();

    void setStyle(String style);


    /**
     * @return Description of the factory.
     */
    String getDescription();

    void setDescription(String description);

    /**
     * @return Author's email provided as meta information.
     */
    String getContactmail();

    void setContactmail(String contactmail);

    /**
     * @return Author's as meta information.
     */
    String getAuthor();

    void setAuthor(String author);


    /**
     * @return path of the file to open in the project.
     */
    String getOpenfile();

    void setOpenfile(String openfile);


    /**
     * @return The orgid will be a field that we use to identify an organization that created the Factory
     * <p/>
     * This will allow us to group together many factories across a single organization.
     */
    String getOrgid();

    void setOrgid(String orgid);


    /**
     * @return The affiliateid will be an Affiliate Code that we issue to partners that will give them certain
     * <p/>
     * referral fees on any business we generate from affiliates.
     */
    String getAffiliateid();

    void setAffiliateid(String affiliateid);


    /**
     * @return Indicates should .git folder be removed after cloning (allow commit to origin repository)
     */
    boolean getVcsinfo();

    void setVcsinfo(boolean vcsinfo);


    /**
     * @return Allow to checkout to the latest commit in given branch
     */
    String getVcsbranch();

    void setVcsbranch(String vcsbranch);


    /**
     * @return Id of user that create factory, set by the server
     */
    String getUserid();

    void setUserid(String userid);

    /**
     * @return Creation time of factory, set by the server (in milliseconds, from Unix epoch, no timezone)
     */
    long getCreated();

    void setCreated(long created);

    /**
     * @return Allow to use text replacement in project files after clone
     */
    Variable getVariable();

    void setVariable(Variable variable);

    /**
     * @return The time when the factory becomes valid (in milliseconds, from Unix epoch, no timezone)
     */
    @Deprecated
    long getValidsince();

    @Deprecated
    void setValidsince(long validsince);

    /**
     * @return The time when the factory becomes invalid (in milliseconds, from Unix epoch, no timezone)
     */
    @Deprecated
    long getValiduntil();

    @Deprecated
    void setValiduntil(long validuntil);

    /**
     * @return welcome page configuration.
     */
    WelcomePage getWelcome();

    void setWelcome(WelcomePage welcome);

    List<Link> getLinks();

    void setLinks(List<Link> links);

    FactoryV1_1 withLinks(List<Link> links);
}
