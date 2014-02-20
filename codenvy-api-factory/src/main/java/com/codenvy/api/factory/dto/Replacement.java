package com.codenvy.api.factory.dto;

import com.codenvy.dto.shared.DTO;

/**
 * @author Sergii Kabashniuk
 */
@DTO
public interface Replacement {

    String getFind();

    void setFind(String find);

    String getReplace();

    void setReplace(String replace);

    String getReplacemode();

    void setReplacemode(String replacemode);

}