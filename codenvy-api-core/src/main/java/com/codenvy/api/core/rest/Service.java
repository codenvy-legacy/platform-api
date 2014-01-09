/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2013] Codenvy, S.A.
 *  All Rights Reserved.
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
package com.codenvy.api.core.rest;

import com.codenvy.api.core.rest.annotations.Description;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.core.rest.annotations.Required;
import com.codenvy.api.core.rest.annotations.Valid;
import com.codenvy.api.core.rest.shared.ParameterType;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.rest.shared.dto.LinkParameter;
import com.codenvy.api.core.rest.shared.dto.RequestBodyDescriptor;
import com.codenvy.api.core.rest.shared.dto.ServiceDescriptor;
import com.codenvy.dto.server.DtoFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

/**
 * Base class for all API services.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
public abstract class Service {
    @Context
    protected UriInfo uriInfo;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public final ServiceDescriptor getServiceDescriptor() {
        return generateServiceDescriptor(uriInfo, getClass());
    }

    public ServiceContext getServiceContext() {
        return new ServiceContextImpl(uriInfo.getBaseUriBuilder(), getClass());
    }

    //

    private static final Set<String> JAX_RS_ANNOTATIONS;

    static {
        List<String> tmp = new ArrayList<>(8);
        tmp.add(CookieParam.class.getName());
        tmp.add(Context.class.getName());
        tmp.add(HeaderParam.class.getName());
        tmp.add(MatrixParam.class.getName());
        tmp.add(PathParam.class.getName());
        tmp.add(QueryParam.class.getName());
        tmp.add(FormParam.class.getName());
        tmp.add("org.everrest.core.Property");
        JAX_RS_ANNOTATIONS = new HashSet<>(tmp);
    }

    private static ServiceDescriptor generateServiceDescriptor(UriInfo uriInfo, Class<? extends Service> service) {
        final List<Link> links = new ArrayList<>();
        for (Method method : service.getMethods()) {
            final GenerateLink generateLink = method.getAnnotation(GenerateLink.class);
            if (generateLink != null) {
                links.add(generateLinkForMethod(uriInfo, generateLink.rel(), method));
            }
        }
        final Description description = service.getAnnotation(Description.class);
        final ServiceDescriptor dto = DtoFactory.getInstance().createDto(ServiceDescriptor.class)
                                                .withHref(uriInfo.getRequestUri().toString())
                                                .withLinks(links)
                                                .withVersion(Constants.API_VERSION);
        if (description != null) {
            dto.setDescription(description.value());
        }
        return dto;
    }

    private static Link generateLinkForMethod(UriInfo uriInfo, String linkRel, Method method, Object... pathParameters) {
        String httpMethod = null;
        if (getAnnotation(method, GET.class) != null) {
            httpMethod = "GET";
        } else if (getAnnotation(method, POST.class) != null) {
            httpMethod = "POST";
        } else if (getAnnotation(method, PUT.class) != null) {
            httpMethod = "PUT";
        } else if (getAnnotation(method, DELETE.class) != null) {
            httpMethod = "DELETE";
        }

        if (httpMethod == null) {
            throw new IllegalArgumentException(
                    String.format("Method '%s' has not any HTTP method annotation and may not be used to produce link.", method.getName()));
        }

        final Consumes consumes = getAnnotation(method, Consumes.class);
        final Produces produces = getAnnotation(method, Produces.class);

        final UriBuilder baseUriBuilder = uriInfo.getBaseUriBuilder();
        final LinkedList<String> matchedURIs = new LinkedList<>(uriInfo.getMatchedURIs());
        // Get path to the root resource.
        if (uriInfo.getMatchedResources().size() < matchedURIs.size()) {
            matchedURIs.remove();
        }

        while (!matchedURIs.isEmpty()) {
            baseUriBuilder.path(matchedURIs.pollLast());
        }

        final Path path = method.getAnnotation(Path.class);
        if (path != null) {
            baseUriBuilder.path(path.value());
        }

        final Link link = DtoFactory.getInstance().createDto(Link.class)
                                    .withRel(linkRel)
                                    .withHref(baseUriBuilder.build(pathParameters).toString())
                                    .withMethod(httpMethod);
        if (consumes != null) {
            link.setConsumes(consumes.value()[0]);
        }
        if (produces != null) {
            link.setProduces(produces.value()[0]);
        }

        Class<?>[] parameterClasses = method.getParameterTypes();
        if (parameterClasses.length > 0) {
            Annotation[][] annotations = method.getParameterAnnotations();
            for (int i = 0; i < parameterClasses.length; i++) {
                if (annotations[i].length > 0) {
                    boolean isBodyParameter = false;
                    QueryParam queryParam = null;
                    Description description = null;
                    Required required = null;
                    Valid valid = null;
                    DefaultValue defaultValue = null;
                    for (int j = 0; j < annotations[i].length; j++) {
                        Annotation annotation = annotations[i][j];
                        isBodyParameter |= !JAX_RS_ANNOTATIONS.contains(annotation.annotationType().getName());
                        Class<?> annotationType = annotation.annotationType();
                        if (annotationType == QueryParam.class) {
                            queryParam = (QueryParam)annotation;
                        } else if (annotationType == Description.class) {
                            description = (Description)annotation;
                        } else if (annotationType == Required.class) {
                            required = (Required)annotation;
                        } else if (annotationType == Valid.class) {
                            valid = (Valid)annotation;
                        } else if (annotationType == DefaultValue.class) {
                            defaultValue = (DefaultValue)annotation;
                        }
                    }
                    if (queryParam != null) {
                        LinkParameter parameter = DtoFactory.getInstance().createDto(LinkParameter.class)
                                                            .withName(queryParam.value())
                                                            .withRequired(required != null)
                                                            .withType(getParameterType(parameterClasses[i]));
                        if (defaultValue != null) {
                            parameter.setDefaultValue(defaultValue.value());
                        }
                        if (description != null) {
                            parameter.setDescription(description.value());
                        }
                        if (valid != null) {
                            parameter.setValid(Arrays.asList(valid.value()));
                        }
                        link.getParameters().add(parameter);
                    } else if (isBodyParameter) {
                        if (description != null) {
                            link.setRequestBody(DtoFactory.getInstance().createDto(RequestBodyDescriptor.class)
                                                          .withDescription(description.value()));
                        }
                    }
                }
            }
        }
        return link;
    }

    private static <T extends Annotation> T getAnnotation(Method method, Class<T> annotationClass) {
        T annotation = method.getAnnotation(annotationClass);
        if (annotation == null) {
            for (Class<?> c = method.getDeclaringClass().getSuperclass();
                 annotation == null && c != null && c != Object.class;
                 c = c.getSuperclass()) {
                Method inherited = null;
                try {
                    inherited = c.getMethod(method.getName(), method.getParameterTypes());
                } catch (NoSuchMethodException ignored) {
                }
                if (inherited != null) {
                    annotation = inherited.getAnnotation(annotationClass);
                }
            }
        }
        return annotation;
    }

    private static ParameterType getParameterType(Class<?> clazz) {
        if (clazz == String.class) {
            return ParameterType.String;
        }
        // restriction for collections which allowed for QueryParam annotation
        if (clazz == List.class || clazz == Set.class || clazz == SortedSet.class) {
            return ParameterType.Array;
        }
        if (clazz == Boolean.class || clazz == boolean.class) {
            return ParameterType.Boolean;
        }
        if (clazz == short.class || clazz == int.class || clazz == long.class || clazz == float.class || clazz == double.class ||
            clazz == Short.class || clazz == Integer.class || clazz == Long.class || clazz == Float.class || clazz == Double.class) {
            return ParameterType.Number;
        }
        return ParameterType.Object;
    }

    private static class ServiceContextImpl implements ServiceContext {
        final UriBuilder uriBuilder;
        final Class      serviceClass;

        ServiceContextImpl(UriBuilder uriBuilder, Class serviceClass) {
            this.uriBuilder = uriBuilder;
            this.serviceClass = serviceClass;
        }

        @Override
        public UriBuilder getServiceUriBuilder() {
            return uriBuilder.clone().path(serviceClass);
        }

        @Override
        public UriBuilder getBaseUriBuilder() {
            return uriBuilder.clone();
        }
    }
}
