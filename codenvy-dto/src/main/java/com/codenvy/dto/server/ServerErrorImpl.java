/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 * [2012] - [2013] Codenvy, S.A.
 * All Rights Reserved.
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
package com.codenvy.dto.server;

import com.codenvy.dto.shared.RoutableDto;
import com.codenvy.dto.shared.ServerError;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

/**
 * Server side implementation for ServerError.
 *
 * @author <a href="mailto:evidolob@codenvy.com">Evgen Vidolob</a>
 */
public class ServerErrorImpl extends RoutableDtoServerImpl implements ServerError, JsonSerializable {

    private static final Gson gson = new GsonBuilder().serializeNulls().create();

    protected ServerErrorImpl() {
        super(RoutableDto.SERVER_ERROR);
    }

    protected ServerErrorImpl(int type) {
        super(type);
    }

    public static ServerErrorImpl make() {
        return new ServerErrorImpl();
    }

    protected String details;

    private boolean _hasDetails;

    public boolean hasDetails() {
        return _hasDetails;
    }

    @Override
    public String getDetails() {
        return details;
    }

    public ServerErrorImpl setDetails(String v) {
        _hasDetails = true;
        details = v;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }
        if (!(o instanceof ServerErrorImpl)) {
            return false;
        }
        ServerErrorImpl other = (ServerErrorImpl)o;
        if (this._hasDetails != other._hasDetails) {
            return false;
        }
        if (this._hasDetails) {
            if (!this.details.equals(other.details)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = hash * 31 + (_hasDetails ? details.hashCode() : 0);
        return hash;
    }

    public JsonElement toJsonElement() {
        JsonObject result = new JsonObject();

        JsonElement detailsOut = (details == null) ? JsonNull.INSTANCE : new JsonPrimitive(details);
        result.add("details", detailsOut);
        result.add("_type", new JsonPrimitive(getType()));
        return result;
    }

    @Override
    public String toJson() {
        return gson.toJson(toJsonElement());
    }

    @Override
    public String toString() {
        return toJson();
    }

    public static ServerErrorImpl fromJsonElement(JsonElement jsonElem) {
        if (jsonElem == null || jsonElem.isJsonNull()) {
            return null;
        }

        ServerErrorImpl dto = new ServerErrorImpl();
        JsonObject json = jsonElem.getAsJsonObject();

        if (json.has("details")) {
            JsonElement detailsIn = json.get("details");
            String detailsOut = gson.fromJson(detailsIn, String.class);
            dto.setDetails(detailsOut);
        }

        return dto;
    }

    public static ServerErrorImpl fromJsonString(String jsonString) {
        if (jsonString == null) {
            return null;
        }

        return fromJsonElement(new JsonParser().parse(jsonString));
    }
}