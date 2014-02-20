package com.codenvy.api.factory.dto;

import com.codenvy.dto.shared.DTO;

/**
 * @author Sergii Kabashniuk
 */
@DTO
public interface Restriction {

    /**
     * @return The time when the factory becomes valid (in milliseconds, from Unix epoch, no timezone)
     */
    long getValidsince();

    void setValidsince(long validsince);


    /**
     * @return The time when the factory becomes invalid (in milliseconds, from Unix epoch, no timezone)
     */
    long getValiduntil();

    void setValiduntil(long validuntil);

    /**
     * @return referer dns name
     */
    String getRefererhostname();

    void setRefererhostname(String refererhostname);

    /**
     * @return Indicates that factory is password protected. Set by server
     */
    String getRestrictbypassword();

    void setRestrictbypassword(String restrictbypassword);


    /**
     * @return Password asked for factory activation. Not exposed in any case.
     */
    String getPassword();

    void setPassword(String password);

    /**
     * @return It is a number that indicates the maximum number of sessions this factory is allowed to have.
     */
    int getValidsessioncount();

    void setValidsessioncount(int validsessioncount);

}
