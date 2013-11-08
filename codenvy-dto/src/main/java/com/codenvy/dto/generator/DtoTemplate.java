// Copyright 2012 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.codenvy.dto.generator;

import com.codenvy.dto.server.DtoFactoryVisitor;
import com.codenvy.dto.shared.ClientToServerDto;
import com.codenvy.dto.shared.RoutableDto;
import com.codenvy.dto.shared.RoutingType;
import com.codenvy.dto.shared.ServerToClientDto;
import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base template for the generated output file that contains all the DTOs.
 * <p/>
 * Note that we generate client and server DTOs in separate runs of the
 * generator.
 * <p/>
 * The directionality of the DTOs only affects whether or not we expose methods
 * to construct an instance of the DTO. We need both client and server versions
 * of all DTOs (irrespective of direction), but you aren't allowed to construct
 * a new {@link ServerToClientDto} on the client. And similarly, you aren't
 * allowed to construct a {@link ClientToServerDto} on the server.
 */
public class DtoTemplate {
    public static class MalformedDtoInterfaceException extends RuntimeException {
        public MalformedDtoInterfaceException(String msg) {
            super(msg);
        }
    }

    // We keep a whitelist of allowed non-DTO generic types.
    static final Set<Class<?>> jreWhitelist = new HashSet<Class<?>>(
            Arrays.asList(new Class<?>[]{String.class, Integer.class, Double.class, Float.class, Boolean.class}));

    private final List<DtoImpl> dtoInterfaces = new ArrayList<DtoImpl>();

    // contains mapping for already implemented DTO interfaces
    private final Map<Class<?>, Class<?>> implementedDtoInterfaces = new HashMap<Class<?>, Class<?>>();

    private final String packageName;

    private final String className;

    private final boolean isServerType;

    private final String apiHash;

    /**
     * @return whether or not the specified interface implements
     *         {@link ClientToServerDto}.
     */
    static boolean implementsClientToServerDto(Class<?> i) {
        return implementsInterface(i, ClientToServerDto.class);
    }

    /**
     * @return whether or not the specified interface implements
     *         {@link ServerToClientDto}.
     */
    static boolean implementsServerToClientDto(Class<?> i) {
        return implementsInterface(i, ServerToClientDto.class);
    }

    /**
     * Walks the superinterface hierarchy to determine if a Class implements some
     * target interface transitively.
     */
    static boolean implementsInterface(Class<?> i, Class<?> target) {
        if (i.equals(target)) {
            return true;
        }

        boolean rtn = false;
        Class<?>[] superInterfaces = i.getInterfaces();
        for (Class<?> superInterface : superInterfaces) {
            rtn = rtn || implementsInterface(superInterface, target);
        }
        return rtn;
    }

    /**
     * @return whether or not the specified interface implements
     *         {@link RoutableDto}.
     */
    private static boolean implementsRoutableDto(Class<?> i) {
        return implementsInterface(i, RoutableDto.class);
    }

    /**
     * Constructor.
     *
     * @param packageName
     *         The name of the package for the outer DTO class.
     * @param className
     *         The name of the outer DTO class.
     * @param isServerType
     *         Whether or not the DTO impls are client or server.
     */
    DtoTemplate(String packageName, String className, String apiHash, boolean isServerType) {
        this.packageName = packageName;
        this.className = className;
        this.apiHash = apiHash;
        this.isServerType = isServerType;
    }

    public void addImplementation(Class<?> dtoInterface, Class<?> impl) {
        implementedDtoInterfaces.put(dtoInterface, impl);
    }

    /** Some DTO interfaces may be already implemented in dependencies of current project. Try to reuse them. If this method returns
     * <code>null</code> it means interface is not implemented yet. */
    public Class<?> getDtoImplementation(Class<?> dtoInterface) {
        return implementedDtoInterfaces.get(dtoInterface);
    }

    /**
     * Adds an interface to the DtoTemplate for code generation.
     *
     * @param i
     */
    public void addInterface(Class<?> i) {
        getDtoInterfaces().add(createDtoImplTemplate(i));
    }

    /** @return the dtoInterfaces */
    public List<DtoImpl> getDtoInterfaces() {
        return dtoInterfaces;
    }

    /**
     * Returns the source code for a class that contains all the DTO impls for any
     * interfaces that were added via the {@link #addInterface(Class)} method.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        emitPreamble(builder);
        emitClientFrontendApiVersion(builder);
        emitDtos(builder);
        emitPostamble(builder);
        return builder.toString();
    }

    /**
     * Tests whether or not a given class is a part of our dto jar, and thus will
     * eventually have a generated Impl that is serializable (thus allowing it to
     * be a generic type).
     */
    boolean isDtoInterface(Class<?> potentialDto) {
        for (DtoImpl dto : getDtoInterfaces()) {
            if (dto.getDtoInterface().equals(potentialDto)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Will initialize the routing ID to be RoutableDto.INVALID_TYPE if it is not
     * routable. This is a small abuse of the intent of that value, but it allows
     * us to simply omit it from the routing type enumeration later.
     *
     * @param i
     *         the super interface type
     * @return a new DtoServerTemplate or a new DtoClientTemplate depending on
     *         isServerImpl.
     */
    private DtoImpl createDtoImplTemplate(Class<?> i) {
        int routingId = implementsRoutableDto(i) ? getRoutingId(i) : RoutableDto.NON_ROUTABLE_TYPE;
        return isServerType ? new DtoImplServerTemplate(this, routingId, i) : new DtoImplClientTemplate(this, routingId, i);
    }

    private void emitDtos(StringBuilder builder) {
        for (DtoImpl dto : getDtoInterfaces()) {
            builder.append(dto.serialize());
        }
    }

    private void emitPostamble(StringBuilder builder) {
        builder.append("\n}");
    }

    private void emitPreamble(StringBuilder builder) {
        builder.append("/*\n");
        builder.append(" * CODENVY CONFIDENTIAL\n");
        builder.append(" * __________________\n");
        builder.append(" *\n");
        builder.append(" * [2012] - [2013] Codenvy, S.A.\n");
        builder.append(" * All Rights Reserved.\n");
        builder.append(" *\n");
        builder.append(" * NOTICE:  All information contained herein is, and remains\n");
        builder.append(" * the property of Codenvy S.A. and its suppliers,\n");
        builder.append(" * if any.  The intellectual and technical concepts contained\n");
        builder.append(" * herein are proprietary to Codenvy S.A.\n");
        builder.append(" * and its suppliers and may be covered by U.S. and Foreign Patents,\n");
        builder.append(" * patents in process, and are protected by trade secret or copyright law.\n");
        builder.append(" * Dissemination of this information or reproduction of this material\n");
        builder.append(" * is strictly forbidden unless prior written permission is obtained\n");
        builder.append(" * from Codenvy S.A..\n");
        builder.append(" */\n\n\n");
        builder.append("// GENERATED SOURCE. DO NOT EDIT.\npackage ");
        builder.append(packageName);
        builder.append(";\n\n");
        if (isServerType) {
            builder.append("import com.codenvy.dto.server.JsonSerializable;\n");
            builder.append("\n");
            builder.append("import com.google.gson.Gson;\n");
            builder.append("import com.google.gson.GsonBuilder;\n");
            builder.append("import com.google.gson.JsonArray;\n");
            builder.append("import com.google.gson.JsonElement;\n");
            builder.append("import com.google.gson.JsonNull;\n");
            builder.append("import com.google.gson.JsonObject;\n");
            builder.append("import com.google.gson.JsonParser;\n");
            builder.append("import com.google.gson.JsonPrimitive;\n");
            builder.append("\n");
            builder.append("import java.util.List;\n");
            builder.append("import java.util.Map;\n");
        }
        if (!isServerType) {
            builder.append("import com.codenvy.ide.dto.ClientDtoFactoryVisitor;\n");
            builder.append("import com.codenvy.ide.dto.DtoFactoryVisitor;\n");
            builder.append("import com.codenvy.ide.dto.JsonSerializable;\n");
            builder.append("import com.google.gwt.json.client.*;\n");
            builder.append("import com.google.inject.Singleton;\n");
        }
        builder.append("\n\n@SuppressWarnings({\"unchecked\", \"cast\"})\n");
        if (!isServerType) {
            builder.append("@Singleton\n");
            builder.append("@DtoClientImpl\n");
        }
        // Note that we always use fully qualified path names when referencing Types
        // so we need not add any import statements for anything.
        builder.append("public class ");
        builder.append(className);
        if (isServerType) {
            builder.append(" implements ").append(DtoFactoryVisitor.class.getCanonicalName());
        }
        if (!isServerType) {
            builder.append(" implements ").append("DtoFactoryVisitor");
        }
        builder.append(" {\n\n");
        if (isServerType) {
            builder.append("  private static final Gson gson = new GsonBuilder().disableHtmlEscaping().serializeNulls().create();\n\n");
            builder.append("  @Override\n" +
                           "  public void accept(com.codenvy.dto.server.DtoFactory dtoFactory) {\n");
            for (DtoImpl dto : getDtoInterfaces()) {
                String dtoInterface = dto.getDtoInterface().getCanonicalName();
                builder.append("    dtoFactory.registerProvider(").append(dtoInterface).append(".class").append(", ")
                       .append("new com.codenvy.dto.server.DtoProvider<").append(dtoInterface).append(">() {\n");
                builder.append("        public Class<? extends ").append(dtoInterface).append("> getImplClass() {\n")
                       .append("            return ").append(dto.getImplClassName()).append(".class;\n");
                builder.append("        }\n\n");
                builder.append("        public ").append(dtoInterface).append(" newInstance() {\n")
                       .append("            return ").append(dto.getImplClassName()).append(".make();\n");
                builder.append("        }\n\n");
                builder.append("        public ").append(dtoInterface).append(" fromJson(String json) {\n")
                       .append("            return ").append(dto.getImplClassName()).append(".fromJsonString(json);\n");
                builder.append("        }\n\n");
                builder.append("        public ").append(dtoInterface).append(" clone(").append(dtoInterface).append(" origin) {\n")
                       .append("            return new ").append(dto.getImplClassName()).append("(origin);\n");
                builder.append("        }\n");
                builder.append("    });\n");
            }
            builder.append("  }\n\n");
        }
        if (!isServerType) {
            builder.append("  @Override\n" +
                           "  public void accept(com.codenvy.ide.dto.DtoFactory dtoFactory) {\n");
            for (DtoImpl dto : getDtoInterfaces()) {
                String dtoInterface = dto.getDtoInterface().getCanonicalName();
                builder.append("    dtoFactory.registerProvider(").append(dtoInterface).append(".class").append(", ")
                       .append("new com.codenvy.ide.dto.DtoProvider<").append(dtoInterface).append(">() {\n");
                builder.append("        public Class<? extends ").append(dtoInterface).append("> getImplClass() {\n")
                       .append("            return ").append(dto.getImplClassName()).append(".class;\n");
                builder.append("        }\n\n");
                builder.append("        public ").append(dtoInterface).append(" newInstance() {\n")
                       .append("            return ").append(dto.getImplClassName()).append(".make();\n");
                builder.append("        }\n\n");
                builder.append("        public ").append(dtoInterface).append(" fromJson(String json) {\n")
                       .append("            return ").append(dto.getImplClassName()).append(".fromJsonString(json);\n");
                builder.append("        }\n");
                builder.append("    });\n");
            }
            builder.append("  }\n\n");
        }
    }

    /**
     * Emits a static variable that is the hash of all the classnames, methodnames, and return types
     * to be used as a version hash between client and server.
     */
    private void emitClientFrontendApiVersion(StringBuilder builder) {
        builder.append("\n  public static final String CLIENT_SERVER_PROTOCOL_HASH = \"");
        builder.append(getApiHash());
        builder.append("\";\n\n");
    }

    private String getApiHash() {
        return apiHash;
    }

    /**
     * Extracts the {@link RoutingType} annotation to derive the stable
     * routing type.
     */
    private int getRoutingId(Class<?> i) {
        RoutingType routingTypeAnnotation = i.getAnnotation(RoutingType.class);

        Preconditions.checkNotNull(routingTypeAnnotation,
                                   "RoutingType annotation must be specified for all subclasses of RoutableDto. " + i.getName());

        return routingTypeAnnotation.type();
    }
}