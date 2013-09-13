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
import com.codenvy.api.core.rest.dto.JsonDto;
import com.codenvy.api.core.rest.dto.Link;
import com.codenvy.api.core.rest.dto.ParameterDescriptor;
import com.codenvy.api.core.rest.dto.ParameterType;
import com.codenvy.api.core.rest.dto.ServiceDescriptor;

import org.everrest.core.RequestHandler;
import org.everrest.core.ResourceBinder;
import org.everrest.core.impl.ApplicationContextImpl;
import org.everrest.core.impl.ApplicationProviderBinder;
import org.everrest.core.impl.ApplicationPublisher;
import org.everrest.core.impl.ContainerResponse;
import org.everrest.core.impl.EverrestConfiguration;
import org.everrest.core.impl.ProviderBinder;
import org.everrest.core.impl.RequestDispatcher;
import org.everrest.core.impl.RequestHandlerImpl;
import org.everrest.core.impl.ResourceBinderImpl;
import org.everrest.core.tools.DependencySupplierImpl;
import org.everrest.core.tools.ResourceLauncher;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Application;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public class ServiceDescriptorTest {
    final String BASE_URI    = "http://localhost/service";
    final String SERVICE_URI = BASE_URI + "/test";

    @Description("test service")
    @Path("test")
    public static class EchoService extends Service {
        @GET
        @Path("my_method")
        @GenerateLink(rel = "echo")
        @Produces("text/plain")
        public String echo(@Description("some text") @Required @Valid({"a", "b"}) @QueryParam("text") String test) {
            return test;
        }
    }

    public static class Deployer extends Application {
        private final Set<Object>   singletons;
        private final Set<Class<?>> classes;

        public Deployer() {
            classes = new HashSet<>(1);
            classes.add(EchoService.class);
            singletons = Collections.emptySet();
        }

        @Override
        public Set<Class<?>> getClasses() {
            return classes;
        }

        @Override
        public Set<Object> getSingletons() {
            return singletons;
        }
    }

    ResourceLauncher launcher;

    @BeforeTest
    public void setUp() throws Exception {
        DependencySupplierImpl dependencies = new DependencySupplierImpl();
        ResourceBinder resources = new ResourceBinderImpl();
        ProviderBinder providers = new ApplicationProviderBinder();
        RequestHandler requestHandler = new RequestHandlerImpl(new RequestDispatcher(resources),
                                                               providers, dependencies, new EverrestConfiguration());
        ApplicationContextImpl.setCurrent(new ApplicationContextImpl(null, null, ProviderBinder.getInstance()));
        launcher = new ResourceLauncher(requestHandler);

        ApplicationPublisher deployer = new ApplicationPublisher(resources, providers);
        deployer.publish(new Deployer());
    }

    @Test
    public void testDescription() throws Exception {
        Assert.assertEquals(getDescriptor().getDescription(), "test service");
    }

    @Test
    public void testServiceLocation() throws Exception {
        Assert.assertEquals(getDescriptor().getHref(), SERVICE_URI);
    }

    @Test
    public void testLinkAvailable() throws Exception {
        Assert.assertEquals(getDescriptor().getLinks().size(), 1);
    }

    @Test
    public void testLinkInfo() throws Exception {
        Link link = getLink("echo");
        Assert.assertEquals(link.getMethod(), "GET");
        Assert.assertEquals(link.getHref(), SERVICE_URI + "/my_method");
        Assert.assertEquals(link.getProduces(), "text/plain");
    }

    @Test
    public void testLinkParameters() throws Exception {
        Link link = getLink("echo");
        List<ParameterDescriptor> parameters = link.getParameters();
        Assert.assertEquals(parameters.size(), 1);
        ParameterDescriptor parameterDescriptor = parameters.get(0);
        Assert.assertEquals(parameterDescriptor.getDescription(), "some text");
        Assert.assertEquals(parameterDescriptor.getName(), "text");
        Assert.assertEquals(parameterDescriptor.getType(), ParameterType.String);
        Assert.assertTrue(parameterDescriptor.isRequired());
        List<String> valid = parameterDescriptor.getValid();
        Assert.assertEquals(valid.size(), 2);
        Assert.assertTrue(valid.contains("a"));
        Assert.assertTrue(valid.contains("b"));
    }

    private Link getLink(String rel) throws Exception {
        List<Link> links = getDescriptor().getLinks();
        for (Link link : links) {
            if (link.getRel().equals(rel)) {
                return link;
            }
        }
        return null;
    }

    private ServiceDescriptor getDescriptor() throws Exception {
        String path = SERVICE_URI;
        ContainerResponse response = launcher.service("GET", path, BASE_URI, null, null, null, null);
        Assert.assertEquals(response.getStatus(), 200);
        JsonDto dto = (JsonDto)response.getEntity();
        Assert.assertNotNull(dto);
        return dto.cast();
    }
}
