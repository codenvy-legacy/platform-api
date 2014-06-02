package com.codenvy.api.factory.dto;

import com.codenvy.api.factory.parameter.FactoryParameter;
import com.codenvy.dto.shared.DTO;

import static com.codenvy.api.factory.parameter.FactoryParameter.Obligation.OPTIONAL;

/**
 * Security restriction for the factory.
 *
 * @author Sergii Kabashniuk
 */
@DTO
public interface Restriction {

    /**
     * @return The time when the factory becomes valid (in milliseconds, from Unix epoch, no timezone)
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "validsince", trackedOnly = true)
    long getValidsince();

    void setValidsince(long validsince);

    Restriction withValidsince(long validsince);

    /**
     * @return The time when the factory becomes invalid (in milliseconds, from Unix epoch, no timezone)
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "validuntil", trackedOnly = true)
    long getValiduntil();

    void setValiduntil(long validuntil);

    Restriction withValiduntil(long validuntil);

    /**
     * @return referer dns queryParameterName
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "refererhostname", trackedOnly = true)
    String getRefererhostname();

    void setRefererhostname(String refererhostname);

    Restriction withRefererhostname(String refererhostname);

    /**
     * @return Indicates that factory is password protected. Set by server
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "restrictbypassword", trackedOnly = true, setByServer = true)
    boolean getRestrictbypassword();

    void setRestrictbypassword(boolean restrictbypassword);

    Restriction withRestrictbypassword(boolean restrictbypassword);

    /**
     * @return Password asked for factory activation. Not exposed in any case.
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "password", trackedOnly = true)
    String getPassword();

    void setPassword(String password);

    Restriction withPassword(String password);

    /**
     * @return It is a number that indicates the maximum number of sessions this factory is allowed to have.
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "maxsessioncount", trackedOnly = true)
    long getMaxsessioncount();

    void setMaxsessioncount(long maxsessioncount);

    Restriction withMaxsessioncount(long maxsessioncount);
}
