package com.codenvy.api.factory.dto;

import com.codenvy.api.factory.FactoryParameter;
import com.codenvy.dto.shared.DTO;

import static com.codenvy.api.factory.FactoryParameter.Obligation.OPTIONAL;

/**
 * @author Sergii Kabashniuk
 */
@DTO
public interface Restriction {

    /**
     * @return The time when the factory becomes valid (in milliseconds, from Unix epoch, no timezone)
     */
    @FactoryParameter(obligation = OPTIONAL, trackedOnly = true)
    long getValidsince();

    void setValidsince(long validsince);


    /**
     * @return The time when the factory becomes invalid (in milliseconds, from Unix epoch, no timezone)
     */
    @FactoryParameter(obligation = OPTIONAL, trackedOnly = true)
    long getValiduntil();

    void setValiduntil(long validuntil);

    /**
     * @return referer dns name
     */
    @FactoryParameter(obligation = OPTIONAL, trackedOnly = true)
    String getRefererhostname();

    void setRefererhostname(String refererhostname);

    /**
     * @return Indicates that factory is password protected. Set by server
     */
    @FactoryParameter(obligation = OPTIONAL, trackedOnly = true, setByServer = true)
    String getRestrictbypassword();

    void setRestrictbypassword(String restrictbypassword);


    /**
     * @return Password asked for factory activation. Not exposed in any case.
     */
    @FactoryParameter(obligation = OPTIONAL, trackedOnly = true)
    String getPassword();

    void setPassword(String password);

    /**
     * @return It is a number that indicates the maximum number of sessions this factory is allowed to have.
     */
    @FactoryParameter(obligation = OPTIONAL, trackedOnly = true)
    int getValidsessioncount();

    void setValidsessioncount(int validsessioncount);

}
