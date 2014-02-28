package com.codenvy.api.factory.dto;

import com.codenvy.api.factory.Compatibility;
import com.codenvy.dto.shared.DTO;

import static com.codenvy.api.factory.Compatibility.Optionality.OPTIONAL;

/**
 * @author Sergii Kabashniuk
 */
@DTO
public interface Restriction {

    /**
     * @return The time when the factory becomes valid (in milliseconds, from Unix epoch, no timezone)
     */
    @Compatibility(optionality = OPTIONAL, trackedOnly = true)
    long getValidsince();

    void setValidsince(long validsince);


    /**
     * @return The time when the factory becomes invalid (in milliseconds, from Unix epoch, no timezone)
     */
    @Compatibility(optionality = OPTIONAL, trackedOnly = true)
    long getValiduntil();

    void setValiduntil(long validuntil);

    /**
     * @return referer dns name
     */
    @Compatibility(optionality = OPTIONAL, trackedOnly = true)
    String getRefererhostname();

    void setRefererhostname(String refererhostname);

    /**
     * @return Indicates that factory is password protected. Set by server
     */
    @Compatibility(optionality = OPTIONAL, trackedOnly = true)
    String getRestrictbypassword();

    void setRestrictbypassword(String restrictbypassword);


    /**
     * @return Password asked for factory activation. Not exposed in any case.
     */
    @Compatibility(optionality = OPTIONAL, trackedOnly = true)
    String getPassword();

    void setPassword(String password);

    /**
     * @return It is a number that indicates the maximum number of sessions this factory is allowed to have.
     */
    @Compatibility(optionality = OPTIONAL, trackedOnly = true)
    int getValidsessioncount();

    void setValidsessioncount(int validsessioncount);

}
