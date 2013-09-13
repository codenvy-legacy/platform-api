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
package com.codenvy.api.core.rest.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Description of service or its parameters.
 * <p/>
 * It may be applied to:
 * <ul>
 * <li>sub-classes of {@link com.codenvy.api.core.rest.Service Service}. In this case value of this annotation is copied to the field
 * {@link com.codenvy.api.core.rest.dto.ServiceDescriptor#description}</li>
 * <li>parameter of RESTful method annotated with {@link javax.ws.rs.QueryParam &#64;QueryParam}. In this case value of this annotation is
 * copied to the field {@link com.codenvy.api.core.rest.dto.ParameterDescriptor#description}</li>
 * <li>entity parameter (not annotated with JAX-RS annotations) of RESTful method. Entity parameters are described in section 3.3.2.1 of
 * JAX-RS specification 1.0. In this case value of this annotation is copied to the filed of {@link
 * com.codenvy.api.core.rest.dto.RequestBodyDescriptor#description}</li>
 * </ul>
 * <p/>
 * For example: There is EchoService. Let's see on the values of Description annotations. Here we have two: at class and at method's
 * parameter.
 * <pre>
 * &#064Path("echo")
 * &#064Description("echo service")
 * public class EchoService extends Service {
 *
 *     &#064GenerateLink(rel = "message")
 *     &#064GET
 *     &#064Path("say")
 *     &#064Produces("plain/text")
 *     public String echo1(&#064Required &#064Description("echo message") &#064QueryParam("message") String message) {
 *         return message;
 *     }
 * }
 * </pre>
 * <p/>
 * Request to URL '${base_uri}/echo' gets next output:
 * <p/>
 * <pre>
 * {
 *   "description":"echo service",
 *   "version":"1.0",
 *   "href":"${base_uri}/echo",
 *   "links":[
 *     {
 *       "href":"${base_uri}/echo/say",
 *       "produces":"plain/text",
 *       "rel":"message",
 *       "method":"GET",
 *       "parameters":[
 *         {
 *           "name":"message",
 *           "type":"String",
 *           "required":true,
 *           "description":"echo message"
 *         }
 *       ]
 *     }
 *   ]
 * }
 * </pre>
 * See two descriptions in JSON output.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @see com.codenvy.api.core.rest.dto.ServiceDescriptor
 * @see com.codenvy.api.core.rest.dto.ParameterDescriptor
 * @see com.codenvy.api.core.rest.dto.RequestBodyDescriptor
 */
@Target({ElementType.TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Description {
    /** @return the description */
    String value();
}
