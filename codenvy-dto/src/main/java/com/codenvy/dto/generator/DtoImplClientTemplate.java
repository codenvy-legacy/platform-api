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

import com.codenvy.dto.server.JsonSerializable;
import com.codenvy.dto.shared.*;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Primitives;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/** Generates the source code for a generated Client DTO impl. */
public class DtoImplClientTemplate extends DtoImpl {
    private static final String ROUTABLE_DTO_IMPL =
            RoutableDto.class.getPackage().getName().replace("dto.shared", "dto.client") + ".RoutableDtoClientImpl";

    DtoImplClientTemplate(DtoTemplate template, int routingType, Class<?> superInterface) {
        super(template, routingType, superInterface);
    }

    @Override
    String serialize() {
        StringBuilder builder = new StringBuilder();

        Class<?> dtoInterface = getDtoInterface();
        List<Method> methods = getDtoMethods();

        emitPreamble(dtoInterface, builder);

        // Enumerate the getters and emit field names and getters + setters.
        emitFields(methods, builder);
        emitMethods(methods, builder);
        List<Method> getters = getDtoGetters(getDtoInterface());
        // "builder" method, it is method that set field and return "this" instance
        emitWithMethods(getters, getDtoInterface().getCanonicalName(), builder);

        emitEqualsAndHashCode(getters, builder);
        emitSerializer(getters, builder);
        emitDeserializer(getters, builder);
        emitDeserializerShortcut(getters, builder);
        emitCopyConstructor(getters, builder);
        builder.append("  }\n\n");
        return builder.toString();
    }

    private void emitDefaultRoutingTypeConstructor(StringBuilder builder) {
        builder.append("    private ");
        builder.append(getImplClassName());
        builder.append("() {");
        builder.append("\n      super(");
        builder.append(getRoutingType());
        builder.append(");\n    ");
        builder.append("}\n\n");
    }

    private void emitEqualsAndHashCode(List<Method> getters, StringBuilder builder) {
        builder.append("    @Override\n");
        builder.append("    public boolean equals(Object o) {\n");
        builder.append("      if (!(o instanceof ").append(getImplClassName()).append(")) {\n");
        builder.append("        return false;\n");
        builder.append("      }\n");
        builder.append("      ").append(getImplClassName()).append(" other = (").append(getImplClassName()).append(") o;\n");
        for (Method method : getters) {
            String fieldName = getFieldName(method.getName());
            Class<?> returnType = method.getReturnType();
            if (returnType.isPrimitive()) {
                builder.append("      if (this.").append(fieldName).append(" != other.").append(fieldName).append(") {\n");
                builder.append("        return false;\n");
                builder.append("      }\n");
            } else {
                builder.append("      if (this.").append(fieldName).append(" != null) {\n");
                builder.append("        if (!this.").append(fieldName).append(".equals(other.").append(fieldName).append(")) {\n");
                builder.append("          return false;\n");
                builder.append("        }\n");
                builder.append("      } else {\n");
                builder.append("        if (other.").append(fieldName).append(" != null) {\n");
                builder.append("          return false;\n");
                builder.append("        }\n");
                builder.append("      }\n");
            }
        }
        builder.append("      return true;\n");
        builder.append("    }\n\n");

        // this isn't the greatest hash function in the world, but it meets the requirement that for any
        // two objects A and B, A.equals(B) only if A.hashCode() == B.hashCode()
        builder.append("    @Override\n");
        builder.append("    public int hashCode() {\n");
        builder.append("      int hash = 7;\n");
        for (Method method : getters) {
            Class<?> type = method.getReturnType();

            String fieldName = getFieldName(method.getName());
            if (type.isPrimitive()) {
                Class<?> wrappedType = Primitives.wrap(type);
                builder.append("      hash = hash * 31 + ").append(wrappedType.getName()).append(".valueOf(").append(fieldName)
                       .append(").hashCode();\n");
            } else {
                builder.append("      hash = hash * 31 + (").append(fieldName).append(" != null ? ").append(fieldName).append(
                        ".hashCode() : 0);\n");
            }
        }
        builder.append("      return hash;\n");
        builder.append("    }\n\n");
    }

    private void emitFactoryMethod(StringBuilder builder) {
        builder.append("    public static ");
        builder.append(getImplClassName());
        builder.append(" make() {");
        builder.append("\n        return new ");
        builder.append(getImplClassName());
        builder.append("();\n    }\n\n");
    }

    private void emitFields(List<Method> methods, StringBuilder builder) {
        for (Method method : methods) {
            if (!ignoreMethod(method)) {
                String fieldName = getFieldName(method.getName());
                builder.append("    ");
                builder.append(getFieldTypeAndAssignment(method, fieldName));
            }
        }
    }

    /** Emits a method to get a field. Getting a collection ensures that the collection is created. */
    private void emitGetter(Method method, String methodName, String fieldName, String returnType, StringBuilder builder) {
        builder.append("    @Override\n    public ");
        builder.append(returnType);
        builder.append(" ");
        builder.append(methodName);
        builder.append("() {\n");

        // Initialize the collection.
        Class<?> returnTypeClass = method.getReturnType();
        if (isList(returnTypeClass) || isMap(returnTypeClass)) {
            builder.append("      ");
            builder.append(getEnsureName(fieldName));
            builder.append("();\n");
        }

        builder.append("      return ");
        //emitReturn(method, fieldName, builder);
        builder.append(fieldName);
        builder.append(";\n    }\n\n");
    }

    private void emitMethods(List<Method> methods, StringBuilder builder) {
        for (Method method : methods) {
            if (!ignoreMethod(method)) {
                String fieldName = getFieldName(method.getName());
                if (fieldName == null) {
                    continue;
                }
                Class<?> returnTypeClass = method.getReturnType();
                String returnType = method.getGenericReturnType().toString().replace('$', '.').replace("class ", "").replace(
                        "interface ", "");

                // Getter.
                emitGetter(method, method.getName(), fieldName, returnType, builder);

                // Setter.
                emitSetter(method, fieldName, builder);

                // List-specific methods.
                if (isList(returnTypeClass)) {
                    emitListAdd(method, fieldName, builder);
                    emitClear(fieldName, builder);
                    emitEnsureCollection(method, fieldName, builder);
                } else if (isMap(returnTypeClass)) {
                    emitMapPut(method, fieldName, builder);
                    emitClear(fieldName, builder);
                    emitEnsureCollection(method, fieldName, builder);
                }
            }
        }
    }

    private void emitSerializer(List<Method> getters, StringBuilder builder) {
        builder.append("    public JSONObject toJsonObject() {\n");
        if (isCompactJson()) {
            builder.append("      JSONArray result = new JSONArray();\n");
            for (Method method : getters) {
                emitSerializeFieldForMethodCompact(method, builder);
            }
        } else {
            builder.append("      JSONObject result = new JSONObject();\n");
            for (Method method : getters) {
                emitSerializeFieldForMethod(method, builder);
            }
        }
        builder.append("      return result;\n");
        builder.append("    }\n");
        builder.append("\n");
        builder.append("    @Override\n");
        builder.append("    public String toJson() {\n");
        builder.append("      return toJsonObject().toString();\n");
        builder.append("    }\n");
        builder.append("\n");
        builder.append("    @Override\n");
        builder.append("    public String toString() {\n");
        builder.append("      return toJson();\n");
        builder.append("    }\n\n");
    }

    private void emitSerializeFieldForMethod(Method method, final StringBuilder builder) {
        final String fieldName = getFieldName(method.getName());
        final String fieldNameOut = fieldName + "Out";
        final String baseIndentation = "      ";

        builder.append("\n");
        List<Type> expandedTypes = expandType(method.getGenericReturnType());
        emitSerializerImpl(expandedTypes, 0, builder, fieldName, fieldNameOut, baseIndentation);
        builder.append("      result.put(\"");
        builder.append(fieldName);
        builder.append("\", ");
        builder.append(fieldNameOut);
        builder.append(");\n");
    }

    private void emitSerializeFieldForMethodCompact(Method method, StringBuilder builder) {
        if (method == null) {
            builder.append("      result.set(0, JSONNull.getInstance());\n");
            return;
        }
        final String fieldName = getFieldName(method.getName());
        final String fieldNameOut = fieldName + "Out";
        final String baseIndentation = "      ";

        builder.append("\n");
        List<Type> expandedTypes = expandType(method.getGenericReturnType());
        emitSerializerImpl(expandedTypes, 0, builder, fieldName, fieldNameOut, baseIndentation);
        if (isLastMethod(method)) {
            if (isList(getRawClass(expandedTypes.get(0)))) {
                builder.append("      if (").append(fieldNameOut).append(".size() != 0) {\n");
                builder.append("        result.set(result.size(), ").append(fieldNameOut).append(");\n");
                builder.append("      }\n");
                return;
            }
        }
        builder.append("      result.add(").append(fieldNameOut).append(");\n");
    }

    /**
     * Produces code to serialize the type with the given variable names.
     *
     * @param expandedTypes
     *         the type and its generic (and its generic (..))
     *         expanded into a list, @see {@link #expandType(java.lang.reflect.Type)}
     * @param depth
     *         the depth (in the generics) for this recursive call. This can
     *         be used to index into {@code expandedTypes}
     * @param builder
     * @param inVar
     *         the java type that will be the input for serialization
     * @param outVar
     *         the JsonElement subtype that will be the output for
     *         serialization
     * @param i
     *         indentation string
     */
    private void emitSerializerImpl(List<Type> expandedTypes, int depth, StringBuilder builder, String inVar, String outVar, String i) {

        Type type = expandedTypes.get(depth);
        String childInVar = inVar + "_";
        String childOutVar = outVar + "_";
        String entryVar = "entry" + depth;
        Class<?> rawClass = getRawClass(type);

        if (isList(rawClass)) {
            String childInTypeName = getImplName(expandedTypes.get(depth + 1), false);
            builder.append(i).append("JSONArray ").append(outVar).append(" = new JSONArray();\n");
            if (depth == 0) {
                builder.append(i).append(getEnsureName(inVar)).append("();\n");
            }
            builder.append(i).append("for (").append(childInTypeName).append(" ").append(childInVar).append(" : ").append(
                    inVar).append(") {\n");

        } else if (isMap(rawClass)) {
            String childInTypeName = getImplName(expandedTypes.get(depth + 1), false);
            builder.append(i).append("JSONObject ").append(outVar).append(" = new JSONObject();\n");
            if (depth == 0) {
                builder.append(i).append(getEnsureName(inVar)).append("();\n");
            }
            builder.append(i).append("for (java.util.Map.Entry<String, ").append(childInTypeName).append("> ").append(
                    entryVar).append(" : ").append(inVar).append(".entrySet()) {\n");
            builder.append(i).append("  ").append(childInTypeName).append(" ").append(childInVar).append(" = ").append(
                    entryVar).append(".getValue();\n");

        } else if (rawClass.isEnum()) {
            builder.append(i).append("JSONValue ").append(outVar).append(" = (").append(inVar).append(
                    " == null) ? JSONNull.getInstance() : new JSONString(").append(inVar).append(".name());\n");
        } else if (getEnclosingTemplate().isDtoInterface(rawClass)) {
            builder.append(i).append("JSONValue ").append(outVar).append(" = ").append(inVar).append(
                    " == null ? JSONNull.getInstance() : ((").append(getImplNameForDto((Class<?>)expandedTypes.get(depth))).append(")")
                   .append(inVar).append(").toJsonObject();\n");
        } else if (rawClass.equals(String.class)) {
            builder.append(i).append("JSONValue ").append(outVar).append(" = (").append(inVar).append(
                    " == null) ? JSONNull.getInstance() : new JSONString(").append(inVar).append(");\n");
        } else if (isNumber(rawClass)) {
            builder.append(i).append("JSONValue ").append(outVar).append(" = new JSONNumber(").append(inVar).append(");\n");
        } else if (isBoolean(rawClass)) {
            builder.append(i).append("JSONValue ").append(outVar).append(" = JSONBoolean.getInstance(").append(inVar).append(");\n");
        } else {
            final Class<?> dtoImplementation = getEnclosingTemplate().getDtoImplementation(rawClass);
            if (dtoImplementation != null) {
                builder.append(i).append("JSONValue ").append(outVar).append(" = ").append(inVar).append(
                        " == null ? JSONNull.getInstance() : ((").append(dtoImplementation.getCanonicalName()).append(")")
                       .append(inVar).append(").toJsonObject();\n");
            } else {
                throw new IllegalArgumentException("Unable to generate client implementation for DTO interface " +
                                                   getDtoInterface().getCanonicalName() + ". Type " + rawClass +
                                                   " is not allowed to use in DTO interface.");
            }
        }

        if (depth + 1 < expandedTypes.size()) {
            emitSerializerImpl(expandedTypes, depth + 1, builder, childInVar, childOutVar, i + "  ");
        }

        if (isList(rawClass)) {
            builder.append(i).append("  ").append(outVar).append(".set(").append(outVar).append(".size(), ").append(childOutVar).append(");\n");
            builder.append(i).append("}\n");

        } else if (isMap(rawClass)) {
            builder.append(i).append("  ").append(outVar).append(".put(").append(entryVar).append(".getKey(), ").append(
                    childOutVar).append(");\n");
            builder.append(i).append("}\n");
        }
    }

    /** Generates a static factory method that creates a new instance based on a JsonElement. */
    private void emitDeserializer(List<Method> getters, StringBuilder builder) {
        builder.append("    public static ").append(getImplClassName()).append(" fromJsonObject(JSONValue jsonValue) {\n");
        builder.append("      if (jsonValue == null || jsonValue.isNull() != null) {\n");
        builder.append("        return null;\n");
        builder.append("      }\n\n");
        builder.append("      ").append(getImplClassName()).append(" dto = new ").append(getImplClassName()).append(
                "();\n");
        if (isCompactJson()) {
            for (Method method : getters) {
                emitDeserializeFieldForMethodCompact(method, builder);
            }
        } else {
            builder.append("      JSONObject json = jsonValue.isObject();\n");
            for (Method method : getters) {
                emitDeserializeFieldForMethod(method, builder);
            }
        }
        builder.append("\n      return dto;\n");
        builder.append("    }\n\n");
    }

    private void emitDeserializerShortcut(List<Method> methods, StringBuilder builder) {
        builder.append("    public static ");
        builder.append(getImplClassName());
        builder.append(" fromJsonString(String jsonString) {\n");
        builder.append("      if (jsonString == null) {\n");
        builder.append("        return null;\n");
        builder.append("      }\n\n");
        builder.append("      return fromJsonObject(JSONParser.parseStrict(jsonString));\n");
        builder.append("    }\n\n");
    }

    private void emitDeserializeFieldForMethod(Method method, StringBuilder builder) {
        final String fieldName = getFieldName(method.getName());
        final String fieldNameIn = fieldName + "In";
        final String fieldNameOut = fieldName + "Out";
        final String baseIndentation = "        ";

        builder.append("\n");
        builder.append("      if (json.containsKey(\"").append(fieldName).append("\")) {\n");
        List<Type> expandedTypes = expandType(method.getGenericReturnType());
        builder.append("        JSONValue ").append(fieldNameIn).append(" = json.get(\"").append(fieldName).append(
                "\");\n");
        emitDeserializerImpl(expandedTypes, 0, builder, fieldNameIn, fieldNameOut, baseIndentation);
        builder.append("        dto.").append(getSetterName(fieldName)).append("(").append(fieldNameOut).append(");\n");
        builder.append("      }\n");
    }

    private void emitDeserializeFieldForMethodCompact(Method method, final StringBuilder builder) {
        final String fieldName = getFieldName(method.getName());
        final String fieldNameIn = fieldName + "In";
        final String fieldNameOut = fieldName + "Out";
        final String baseIndentation = "        ";
        SerializationIndex serializationIndex = Preconditions.checkNotNull(
                method.getAnnotation(SerializationIndex.class));
        int index = serializationIndex.value() - 1;

        builder.append("\n");
        builder.append("      if (").append(index).append(" < json.size()) {\n");
        List<Type> expandedTypes = expandType(method.getGenericReturnType());
        builder.append("        JSONValue ").append(fieldNameIn).append(" = json.get(").append(index).append(");\n");
        emitDeserializerImpl(expandedTypes, 0, builder, fieldNameIn, fieldNameOut, baseIndentation);
        builder.append("        dto.").append(getSetterName(fieldName)).append("(").append(fieldNameOut).append(");\n");
        builder.append("      }\n");
    }

    /**
     * Produces code to deserialize the type with the given variable names.
     *
     * @param expandedTypes
     *         the type and its generic (and its generic (..))
     *         expanded into a list, @see {@link #expandType(java.lang.reflect.Type)}
     * @param depth
     *         the depth (in the generics) for this recursive call. This can
     *         be used to index into {@code expandedTypes}
     * @param builder
     * @param inVar
     *         the java type that will be the input for serialization
     * @param outVar
     *         the JsonElement subtype that will be the output for
     *         serialization
     * @param i
     *         indentation string
     */
    private void emitDeserializerImpl(List<Type> expandedTypes, int depth, StringBuilder builder, String inVar,
                                      String outVar, String i) {

        Type type = expandedTypes.get(depth);
        String childInVar = inVar + "_";
        String childInVarIterator = childInVar + "_iterator";
        String childOutVar = outVar + "_";
        Class<?> rawClass = getRawClass(type);

        if (isList(rawClass)) {
            builder.append(i).append(getImplName(type, false)).append(" ").append(outVar).append(" = null;\n");
            builder.append(i).append("if (").append(inVar).append(" != null && ").append(inVar).append(".isNull() == null) {\n");
            builder.append(i).append("  ").append(outVar).append(" = new ").append(getImplName(type, true)).append("();\n");
            builder.append(i).append("  for (int ").append(childInVarIterator).append(" = 0; ").append(childInVarIterator).append(" < ").append(inVar).append(".isArray().size(); ").append(childInVarIterator).append("++) {\n");
            builder.append(i).append("    JSONValue ").append(childInVar).append(" = ").append(inVar).append(".isArray().get(").append(childInVarIterator).append(");\n");

            emitDeserializerImpl(expandedTypes, depth + 1, builder, childInVar, childOutVar, i + "    ");

            builder.append(i).append("    ").append(outVar).append(".add(").append(childOutVar).append(");\n");
            builder.append(i).append("  }\n");
            builder.append(i).append("}\n");
        } else if (isMap(rawClass)) {
            // TODO: Handle type
            String entryVar = "key" + depth;
            String entriesVar = "keySet" + depth;
            builder.append(i).append(getImplName(type, false)).append(" ").append(outVar).append(" = null;\n");
            builder.append(i).append("if (").append(inVar).append(" != null && ").append(inVar).append(".isNull() == null) {\n");
            builder.append(i).append("  ").append(outVar).append(" = new ").append(getImplName(type, true)).append("();\n");
            builder.append(i).append("  java.util.Set<String> ").append(entriesVar).append(
                    " = ").append(inVar).append(".isObject().keySet();\n");
            builder.append(i).append("  for (String ").append(entryVar).append(" : ").append(entriesVar)
                   .append(") {\n");
            builder.append(i).append("    JSONValue ").append(childInVar).append(" = ").append(inVar).
                    append(".isObject().get(").append(entryVar).append(");\n");
            emitDeserializerImpl(expandedTypes, depth + 1, builder, childInVar, childOutVar, i + "    ");

            builder.append(i).append("    ").append(outVar).append(".put(").append(entryVar).append(", ").append(
                    childOutVar).append(");\n");
            builder.append(i).append("  }\n");
            builder.append(i).append("}\n");
        } else if (rawClass.isEnum()) {
            String primitiveName = rawClass.getCanonicalName();
            builder.append(i).append(primitiveName).append(" ").append(outVar).append(" = ").append(primitiveName).
                    append(".valueOf(").append(inVar).append(".isString().stringValue());\n");
        } else if (getEnclosingTemplate().isDtoInterface(rawClass)) {
            String className = getImplName(rawClass, false);
            builder.append(i).append(className).append(" ").append(outVar).append(" = ").append(getImplNameForDto(rawClass))
                   .append(".fromJsonObject(").append(inVar).append(");\n");
        } else if (rawClass.equals(String.class)) {
            String primitiveName = rawClass.getSimpleName();
            builder.append(i).append(primitiveName).append(" ").append(outVar).append(" = ").append(inVar).append(".isString() != null ? ").append(inVar).append(
                    ".isString().stringValue() : null;\n");
        } else if (isNumber(rawClass)) {
            String primitiveName = rawClass.getSimpleName();
            String typeCast = rawClass.equals(double.class) || rawClass.equals(Double.class) ? "" : "(" + getPrimitiveName(rawClass) + ")";
            builder.append(i).append(primitiveName).append(" ").append(outVar).append(" = ").append(typeCast).append(inVar)
                   .append(".isNumber().doubleValue();\n");
        } else if (isBoolean(rawClass)) {
            String primitiveName = rawClass.getSimpleName();
            builder.append(i).append(primitiveName).append(" ").append(outVar).append(" = ").append(inVar).append(
                    ".isBoolean().booleanValue();\n");
        } else {
            final Class<?> dtoImplementation = getEnclosingTemplate().getDtoImplementation(rawClass);
            if (dtoImplementation != null) {
                String className = getImplName(rawClass, false);
                builder.append(i).append(className).append(" ").append(outVar).append(" = ")
                       .append(dtoImplementation.getCanonicalName()).append(".fromJsonObject(").append(inVar).append(");\n");
            } else {
                throw new IllegalArgumentException("Unable to generate client implementation for DTO interface " +
                                                   getDtoInterface().getCanonicalName() + ". Type " + rawClass +
                                                   " is not allowed to use in DTO interface.");
            }
        }
    }

    private void emitMockPreamble(Class<?> dtoInterface, StringBuilder builder) {
        builder.append("\n  public static class ");
        builder.append("Mock").append(getImplClassName());
        builder.append(" extends ");
        builder.append(getImplClassName());
        builder.append(" {\n");
        builder.append("    protected Mock");
        builder.append(getImplClassName());
        builder.append("() {}\n\n");

        emitFactoryMethod(builder);
    }

    private void emitPreamble(Class<?> dtoInterface, StringBuilder builder) {
        builder.append("  public static class ");
        builder.append(getImplClassName());

        Class<?> superType = getSuperInterface();
        if (superType != null && superType != JsonSerializable.class) {
            // We need to extend something.
            builder.append(" extends ");
            if (superType.equals(ServerToClientDto.class) || superType.equals(ClientToServerDto.class)) {
                // We special case RoutableDto's impl since it isn't generated.
                builder.append(ROUTABLE_DTO_IMPL);
            } else {
                builder.append(superType.getSimpleName()).append("Impl");
            }
        }
        builder.append(" implements ");
        builder.append(dtoInterface.getCanonicalName());
        builder.append(", JsonSerializable ");
        builder.append(" {\n\n");

        // If this guy is Routable, we make two constructors. One is a private
        // default constructor that hard codes the routing type, the other is a
        // protected constructor for any subclasses of this impl to pass up its
        // routing type.
        if (getRoutingType() != RoutableDto.NON_ROUTABLE_TYPE) {
            emitDefaultRoutingTypeConstructor(builder);
            emitProtectedConstructor(builder);
        }

        // If this DTO is allowed to be constructed on the server, we expose a
        // static factory method. A DTO is allowed to be constructed if it is a
        // ServerToClientDto, or if it is not a top level type (non-routable).
        if (DtoTemplate.implementsServerToClientDto(dtoInterface) || getRoutingType() == RoutableDto.NON_ROUTABLE_TYPE) {
            emitFactoryMethod(builder);
        }

        emitDefaultConstructor(builder);
    }

    private void emitDefaultConstructor(StringBuilder builder) {
        builder.append("    protected ");
        builder.append(getImplClassName());
        builder.append("() {\n");
        builder.append("    }\n\n");
    }

    private void emitProtectedConstructor(StringBuilder builder) {
        builder.append("    protected ");
        builder.append(getImplClassName());
        builder.append("(int type) {\n      super(type);\n");
        builder.append("    }\n\n");
    }

    private void emitSetter(Method method, String fieldName, StringBuilder builder) {
        builder.append("    public ");
        builder.append("void");
        builder.append(" ");
        builder.append(getSetterName(fieldName));
        builder.append("(");
        builder.append(getFqParameterizedName(method.getGenericReturnType()));
        builder.append(" v) {\n");
        builder.append("      this.");
        builder.append(fieldName);
        builder.append(" = ");
        builder.append("v;\n    }\n\n");
    }

    private void emitWithMethods(List<Method> getters, String dtoInterfaceName, StringBuilder builder) {
        for (Method method : getters) {
            String fieldName = getFieldName(method.getName());
            emitWithMethod(method, dtoInterfaceName, fieldName, builder);
        }
    }

    private void emitWithMethod(Method method, String dtoInterfaceName, String fieldName, StringBuilder builder) {
        builder.append("    public ");
        builder.append(dtoInterfaceName);
        builder.append(" ");
        builder.append(getWithName(fieldName));
        builder.append("(");
        builder.append(getFqParameterizedName(method.getGenericReturnType()));
        builder.append(" v) {\n");
        builder.append("      this.");
        builder.append(fieldName);
        builder.append(" = ");
        builder.append("v;\n      return this;\n    }\n\n");
    }

    /**
     * Emits an add method to add to a list. If the list is null, it is created.
     *
     * @param method
     *         a method with a list return type
     */
    private void emitListAdd(Method method, String fieldName, StringBuilder builder) {
        builder.append("    public void ");
        builder.append(getListAdderName(fieldName));
        builder.append("(");
        builder.append(getTypeArgumentImplName((ParameterizedType)method.getGenericReturnType(), 0));
        builder.append(" v) {\n      ");
        builder.append(getEnsureName(fieldName));
        builder.append("();\n      ");
        builder.append(fieldName);
        builder.append(".add(v);\n");
        builder.append("    }\n\n");
    }

    /**
     * Emits a put method to put a value into a map. If the map is null, it is created.
     *
     * @param method
     *         a method with a map return value
     */
    private void emitMapPut(Method method, String fieldName, StringBuilder builder) {
        builder.append("    public void ");
        builder.append(getMapPutterName(fieldName));
        builder.append("(String k, ");
        builder.append(getTypeArgumentImplName((ParameterizedType)method.getGenericReturnType(), 1));
        builder.append(" v) {\n      ");
        builder.append(getEnsureName(fieldName));
        builder.append("();\n      ");
        builder.append(fieldName);
        builder.append(".put(k, v);\n");
        builder.append("    }\n\n");
    }

    /** Emits a method to clear a list or map. Clearing the collections ensures that the collection is created. */
    private void emitClear(String fieldName, StringBuilder builder) {
        builder.append("    public void ");
        builder.append(getClearName(fieldName));
        builder.append("() {\n      ");
        builder.append(getEnsureName(fieldName));
        builder.append("();\n      ");
        builder.append(fieldName);
        builder.append(".clear();\n");
        builder.append("    }\n\n");
    }

    private void emitCopyConstructor(List<Method> getters, StringBuilder builder) {
        String dtoInterface = getDtoInterface().getCanonicalName();
        String implClassName = getImplClassName();
        builder.append("    public ").append(implClassName).append("(").append(dtoInterface).append(" origin) {\n");
        for (Method method : getters) {
            emitDeepCopyForGetters(expandType(method.getGenericReturnType()), 0, builder, "origin", method, "      ");
        }
        builder.append("    }\n\n");
    }

    private List<Method> getDtoGetters(Class<?> dtoInterface) {
        if (!getEnclosingTemplate().isDtoInterface(dtoInterface)) {
            return Collections.emptyList();
        }
        Set<Class<?>> allInterfaces = new LinkedHashSet<Class<?>>();
        getAllInterfaces(dtoInterface, allInterfaces);
        List<Method> methodsToInclude = new ArrayList<Method>();
        for (Class<?> parent : allInterfaces) {
            if (getEnclosingTemplate().isDtoInterface(parent)) {
                for (Method m : parent.getDeclaredMethods()) {
                    if (isDtoGetter(m)) {
                        methodsToInclude.add(m);
                    }
                }
            }
        }
        return methodsToInclude;
    }

    private static void getAllInterfaces(Class<?> parent, Set<Class<?>> found) {
        found.add(parent);
        Class<?>[] interfaces = parent.getInterfaces();
        for (Class<?> i : interfaces) {
            if (found.add(i)) {
                getAllInterfaces(i, found);
            }
        }
    }

    private void emitDeepCopyForGetters(List<Type> expandedTypes, int depth, StringBuilder builder, String origin, Method getter,
                                        String i) {
        String getterName = getter.getName();
        String fieldName = getFieldName(getterName);
        String fieldNameIn = fieldName + "In";
        String fieldNameOut = fieldName + "Out";
        Type type = expandedTypes.get(depth);
        Class<?> rawClass = getRawClass(type);
        String rawTypeName = getImplName(type, false);

        if (isList(rawClass) || isMap(rawClass)) {
            builder.append(i).append(rawTypeName).append(" ").append(fieldNameIn).append(" = ").append(origin).append(".")
                   .append(getterName).append("();\n");
            builder.append(i).append("if (").append(fieldNameIn).append(" != null) {\n");
            builder.append(i).append("  ").append(rawTypeName).append(" ").append(fieldNameOut)
                   .append(" = new ").append(getImplName(type, true)).append("();\n");
            emitDeepCopyCollections(expandedTypes, depth, builder, fieldNameIn, fieldNameOut, i);
            builder.append(i).append("  ").append("this.").append(fieldName).append(" = ").append(fieldNameOut).append(";\n");
            builder.append(i).append("}\n");
        } else if (getEnclosingTemplate().isDtoInterface(rawClass)) {
            builder.append(i).append(rawTypeName).append(" ").append(fieldNameIn).append(" = ").append(origin).append(".")
                   .append(getterName).append("();\n");
            builder.append(i).append("this.").append(fieldName).append(" = ");
            emitCheckNullAndCopyDto(rawClass, fieldNameIn, builder);
            builder.append(";\n");
        } else {
            builder.append(i).append("this.").append(fieldName).append(" = ")
                   .append(origin).append(".").append(getterName).append("();\n");
        }
    }

    private void emitDeepCopyCollections(List<Type> expandedTypes, int depth, StringBuilder builder, String varIn, String varOut,
                                         String i) {
        Type type = expandedTypes.get(depth);
        String childVarIn = varIn + "_";
        String childVarOut = varOut + "_";
        String entryVar = "entry" + depth;
        Class<?> rawClass = getRawClass(type);
        Class<?> childRawType = getRawClass(expandedTypes.get(depth + 1));
        final String childTypeName = getImplName(expandedTypes.get(depth + 1), false);

        if (isList(rawClass)) {
            builder.append(i).append("  for (").append(childTypeName).append(" ").append(childVarIn)
                   .append(" : ").append(varIn).append(") {\n");
        } else if (isMap(rawClass)) {
            builder.append(i).append("  for (java.util.Map.Entry<String, ").append(childTypeName).append("> ").append(entryVar)
                   .append(" : ").append(varIn).append(".entrySet()) {\n");
            builder.append(i).append("    ").append(childTypeName).append(" ").append(childVarIn).append(" = ").append(
                    entryVar).append(".getValue();\n");
        }

        if (isList(childRawType) || isMap(childRawType)) {
            builder.append(i).append("    if (").append(childVarIn).append(" != null) {\n");
            builder.append(i).append("      ").append(childTypeName).append(" ").append(childVarOut)
                   .append(" = new ").append(getImplName(expandedTypes.get(depth + 1), true)).append("();\n");
            emitDeepCopyCollections(expandedTypes, depth + 1, builder, childVarIn, childVarOut, i + "    ");
            builder.append(i).append("      ").append(varOut);
            if (isList(rawClass)) {
                builder.append(".add(");
            } else {
                builder.append(".put(").append(entryVar).append(".getKey(), ");
            }
            builder.append(childVarOut);
            builder.append(");\n");
            builder.append(i).append("    ").append("}\n");
        } else {
            builder.append(i).append("      ").append(varOut);
            if (isList(rawClass)) {
                builder.append(".add(");
            } else {
                builder.append(".put(").append(entryVar).append(".getKey(), ");
            }
            if (getEnclosingTemplate().isDtoInterface(childRawType)) {
                emitCheckNullAndCopyDto(childRawType, childVarIn, builder);
            } else {
                builder.append(childVarIn);
            }
            builder.append(");\n");
        }
        builder.append(i).append("  }\n");
    }

    private void emitCheckNullAndCopyDto(Class<?> dto, String fieldName, StringBuilder builder) {
        String implName = dto.getSimpleName() + "Impl";
        builder.append(fieldName).append(" == null ? null : ")
               .append("new ").append(implName).append("(").append(fieldName).append(")");
    }

    /** Emit a method that ensures a collection is initialized. */
    private void emitEnsureCollection(Method method, String fieldName, StringBuilder builder) {
        builder.append("    void ");
        builder.append(getEnsureName(fieldName));
        builder.append("() {\n");
        builder.append("      if (");
        builder.append(fieldName);
        builder.append(" == null) {\n        ");
        builder.append(fieldName);
        builder.append(" = new ");
        builder.append(getImplName(method.getGenericReturnType(), true));
        builder.append("();\n");
        builder.append("      }\n");
        builder.append("    }\n");
    }

    /**
     * Appends a suitable type for the given type. For example, at minimum, this will replace DTO interfaces with their implementation
     * classes and JSON collections with corresponding Java types. If a suitable type cannot be determined, this will throw an exception.
     *
     * @param genericType
     *         the type as returned by e.g.
     *         method.getGenericReturnType()
     */
    private void appendType(Type genericType, final StringBuilder builder) {
        builder.append(getImplName(genericType, false));
    }

    /**
     * In most cases we simply echo the return type and field name, except for JsonArray<T>, which is special in the server impl case,
     * since it must be represented by a List<T> for Gson to correctly serialize/deserialize it.
     *
     * @param method
     *         The getter method.
     * @return String representation of what the field type should be, as well as
     *         the assignment (initial value) to said field type, if any.
     */
    private String getFieldTypeAndAssignment(Method method, String fieldName) {
        StringBuilder builder = new StringBuilder();
        builder.append("protected ");
        appendType(method.getGenericReturnType(), builder);
        builder.append(" ");
        builder.append(fieldName);
        builder.append(";\n");
        return builder.toString();
    }

    /**
     * Returns the fully-qualified type name using Java concrete implementation classes.
     * <p/>
     * For example, for JsonArray&lt;JsonStringMap&lt;Dto&gt;&gt;, this would return "ArrayList&lt;Map&lt;String, DtoImpl&gt;&gt;".
     */
    private String getImplName(Type type, boolean allowJreCollectionInterface) {
        Class<?> rawClass = getRawClass(type);
        String fqName = getFqParameterizedName(type);
        fqName = fqName.replaceAll(JsonArray.class.getCanonicalName(), ArrayList.class.getCanonicalName());
        fqName = fqName.replaceAll(JsonStringMap.class.getCanonicalName() + "<",
                                   HashMap.class.getCanonicalName() + "<String, ");

        if (allowJreCollectionInterface) {
            if (isList(rawClass)) {
                fqName = fqName.replaceFirst(List.class.getCanonicalName(), ArrayList.class.getCanonicalName());
            } else if (isMap(rawClass)) {
                fqName = fqName.replaceFirst(Map.class.getCanonicalName(), HashMap.class.getCanonicalName());
            }
        }

        return fqName;
    }

    /** Returns the fully-qualified type name including parameters. */
    private String getFqParameterizedName(Type type) {
        if (type instanceof Class<?>) {
            return ((Class<?>)type).getCanonicalName();
            //return getImplNameForDto((Class<?>)type);

        } else if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType)type;

            StringBuilder sb = new StringBuilder(getRawClass(pType).getCanonicalName());
            sb.append('<');
            for (int i = 0; i < pType.getActualTypeArguments().length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(getFqParameterizedName(pType.getActualTypeArguments()[i]));
            }
            sb.append('>');

            return sb.toString();

        } else {
            throw new IllegalArgumentException("We do not handle this type");
        }
    }

    /**
     * Returns the fully-qualified type name using Java concrete implementation classes of the first type argument for a parameterized
     * type.
     * If one is not specified, returns "Object".
     *
     * @param type
     *         the parameterized type
     * @return the first type argument
     */
    private String getTypeArgumentImplName(ParameterizedType type, int index) {
        Type[] typeArgs = type.getActualTypeArguments();
        if (typeArgs.length == 0) {
            return "Object";
        }
        return getImplName(typeArgs[index], false);
    }

    private String getImplNameForDto(Class<?> dtoInterface) {
        if (getEnclosingTemplate().isDtoInterface(dtoInterface)) {
            // This will eventually get a generated impl type.
            return dtoInterface.getSimpleName() + "Impl";
        }

        return dtoInterface.getCanonicalName();
    }

    /** Tests whether or not a given return type is a number primitive or its wrapper type. */
    private static boolean isNumber(Class<?> returnType) {
        final Class<?>[] numericTypes = { int.class, long.class, short.class, float.class, double.class, byte.class,
                                          Integer.class, Long.class, Short.class, Float.class, Double.class, Byte.class};

        for (Class<?> standardPrimitive : numericTypes) {
            if (returnType.equals(standardPrimitive)) {
                return true;
            }
        }

        return false;
    }

    /** Tests whether or not a given return type is a boolean primitive or its wrapper type. */
    private static boolean isBoolean(Class<?> returnType) {
        return returnType.equals(Boolean.class) || returnType.equals(boolean.class);
    }

    private static String getPrimitiveName(Class<?> returnType) {
        if (returnType.equals(Integer.class) || returnType.equals(int.class)) {
            return "int";
        } else if (returnType.equals(Long.class) || returnType.equals(long.class)) {
            return "long";
        } else if (returnType.equals(Short.class) || returnType.equals(short.class)) {
            return "short";
        } else if (returnType.equals(Float.class) || returnType.equals(float.class)) {
            return "float";
        } else if (returnType.equals(Double.class) || returnType.equals(double.class)) {
            return "double";
        } else if (returnType.equals(Byte.class) || returnType.equals(byte.class)) {
            return "byte";
        } else if (returnType.equals(Boolean.class) || returnType.equals(boolean.class)) {
            return "boolean";
        } else if (returnType.equals(Character.class) || returnType.equals(char.class)) {
            return "char";
        }

        throw new IllegalArgumentException("Unknown wrapper class type.");
    }
}