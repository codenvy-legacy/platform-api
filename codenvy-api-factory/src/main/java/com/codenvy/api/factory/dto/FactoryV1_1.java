package com.codenvy.api.factory.dto;

import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.factory.Compatibility;
import com.codenvy.dto.shared.DTO;

import java.util.List;

import static com.codenvy.api.factory.Compatibility.Encoding.ENCODED;
import static com.codenvy.api.factory.Compatibility.Optionality.OPTIONAL;
import static com.codenvy.api.factory.Compatibility.Version.V1_1;
import static com.codenvy.api.factory.Compatibility.Version.V1_2;

/**
 * @author Sergii Kabashniuk
 */
@DTO
public interface FactoryV1_1 extends FactoryV1_0 {
    @Compatibility(optionality = OPTIONAL, encoding = ENCODED)
    String getId();

    void setId(String id);

    @Compatibility(optionality = OPTIONAL)
    void setProjectattributes(ProjectAttributes projectattributes);

    ProjectAttributes getProjectattributes();

    /**
     * @return Codenow  button style: vertical, horisontal, dark, wite
     */
    @Compatibility(optionality = OPTIONAL, encoding = ENCODED)
    String getStyle();

    void setStyle(String style);


    /**
     * @return Description of the factory.
     */
    @Compatibility(optionality = OPTIONAL, encoding = ENCODED)
    String getDescription();

    void setDescription(String description);

    /**
     * @return Author's email provided as meta information.
     */
    @Compatibility(optionality = OPTIONAL)
    String getContactmail();

    void setContactmail(String contactmail);

    /**
     * @return Author's as meta information.
     */
    @Compatibility(optionality = OPTIONAL)
    String getAuthor();

    void setAuthor(String author);


    /**
     * @return path of the file to open in the project.
     */
    @Compatibility(optionality = OPTIONAL)
    String getOpenfile();

    void setOpenfile(String openfile);


    /**
     * @return The orgid will be a field that we use to identify an organization that created the Factory
     * <p/>
     * This will allow us to group together many factories across a single organization.
     */
    @Compatibility(optionality = OPTIONAL)
    String getOrgid();

    void setOrgid(String orgid);


    /**
     * @return The affiliateid will be an Affiliate Code that we issue to partners that will give them certain
     * <p/>
     * referral fees on any business we generate from affiliates.
     */
    @Compatibility(optionality = OPTIONAL)
    String getAffiliateid();

    void setAffiliateid(String affiliateid);


    /**
     * @return Indicates should .git folder be removed after cloning (allow commit to origin repository)
     */
    @Compatibility(optionality = OPTIONAL)
    boolean getVcsinfo();

    void setVcsinfo(boolean vcsinfo);


    /**
     * @return Allow to checkout to the latest commit in given branch
     */
    @Compatibility(optionality = OPTIONAL)
    String getVcsbranch();

    void setVcsbranch(String vcsbranch);


    /**
     * @return Id of user that create factory, set by the server
     */
    // TODO
    String getUserid();

    void setUserid(String userid);

    /**
     * @return Creation time of factory, set by the server (in milliseconds, from Unix epoch, no timezone)
     */
    //TODO
    long getCreated();

    void setCreated(long created);

    /**
     * @return Allow to use text replacement in project files after clone
     */
    @Compatibility(optionality = OPTIONAL)
    Variable getVariable();

    void setVariable(Variable variable);

    /**
     * @return The time when the factory becomes valid (in milliseconds, from Unix epoch, no timezone)
     */
    @Deprecated
    @Compatibility(optionality = OPTIONAL, ignoredSince = V1_1, deprecatedSince = V1_2, trackedOnly = true)
    long getValidsince();

    @Deprecated
    void setValidsince(long validsince);

    /**
     * @return The time when the factory becomes invalid (in milliseconds, from Unix epoch, no timezone)
     */
    @Deprecated
    @Compatibility(optionality = OPTIONAL, ignoredSince = V1_1, deprecatedSince = V1_2, trackedOnly = true)
    long getValiduntil();

    @Deprecated
    void setValiduntil(long validuntil);

    /**
     * @return welcome page configuration.
     */
    @Compatibility(optionality = OPTIONAL, encoding = ENCODED, trackedOnly = true)
    WelcomePage getWelcome();

    void setWelcome(WelcomePage welcome);

    List<Link> getLinks();

    void setLinks(List<Link> links);

    FactoryV1_1 withLinks(List<Link> links);
}
