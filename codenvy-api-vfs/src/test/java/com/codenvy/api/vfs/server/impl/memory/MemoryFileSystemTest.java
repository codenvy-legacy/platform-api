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
package com.codenvy.api.vfs.server.impl.memory;

import junit.framework.TestCase;

import com.codenvy.api.vfs.server.ContentStream;
import com.codenvy.api.vfs.server.URLHandlerFactorySetup;
import com.codenvy.api.vfs.server.VirtualFile;
import com.codenvy.api.vfs.server.VirtualFileSystemApplication;
import com.codenvy.api.vfs.server.VirtualFileSystemRegistry;
import com.codenvy.api.vfs.server.observation.EventListenerList;
import com.codenvy.api.vfs.shared.ItemType;
import com.codenvy.api.vfs.shared.dto.AccessControlEntry;
import com.codenvy.api.vfs.shared.dto.File;
import com.codenvy.api.vfs.shared.dto.Item;
import com.codenvy.api.vfs.shared.dto.ItemList;
import com.codenvy.api.vfs.shared.dto.Link;
import com.codenvy.api.vfs.shared.dto.Principal;
import com.codenvy.api.vfs.shared.dto.Project;
import com.codenvy.api.vfs.shared.dto.Property;
import com.codenvy.api.vfs.shared.dto.VirtualFileSystemInfo;
import com.codenvy.api.vfs.shared.dto.VirtualFileSystemInfo.BasicPermissions;
import com.codenvy.commons.env.EnvironmentContext;
import com.codenvy.dto.server.DtoFactory;

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
import org.everrest.core.tools.ByteArrayContainerResponseWriter;
import org.everrest.core.tools.DependencySupplierImpl;
import org.everrest.core.tools.ResourceLauncher;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** @author <a href="mailto:andrey.parfonov@exoplatform.com">Andrey Parfonov</a> */
public abstract class MemoryFileSystemTest extends TestCase {
    protected static EventListenerList eventListenerList;

    protected static VirtualFileSystemRegistry virtualFileSystemRegistry = new VirtualFileSystemRegistry();

    static {
        URLHandlerFactorySetup.setup(virtualFileSystemRegistry, eventListenerList);
    }

    protected MemoryFileSystemProvider fileSystemProvider;
    protected MemoryMountPoint         mountPoint;

    protected static final String MY_WORKSPACE_ID       = "my-ws";
    protected final        String BASE_URI              = "http://localhost/service";
    protected final        String SERVICE_URI           = BASE_URI + "/my-ws/vfs/v2/";
    protected final        String DEFAULT_CONTENT       = "__TEST__";
    protected final        byte[] DEFAULT_CONTENT_BYTES = DEFAULT_CONTENT.getBytes();

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected ResourceLauncher launcher;

    /** @see junit.framework.TestCase#setUp() */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        System.setProperty("org.exoplatform.mimetypes", "conf/mimetypes.properties");

        eventListenerList = new EventListenerList();

        fileSystemProvider = new MemoryFileSystemProvider(MY_WORKSPACE_ID);
        virtualFileSystemRegistry.registerProvider(MY_WORKSPACE_ID, fileSystemProvider);
        mountPoint = (MemoryMountPoint)fileSystemProvider.getMountPoint(true);
        DependencySupplierImpl dependencies = new DependencySupplierImpl();
        dependencies.addComponent(VirtualFileSystemRegistry.class, virtualFileSystemRegistry);
        dependencies.addComponent(EventListenerList.class, eventListenerList);
        ResourceBinder resources = new ResourceBinderImpl();
        ProviderBinder providers = new ApplicationProviderBinder();
        RequestHandler requestHandler = new RequestHandlerImpl(new RequestDispatcher(resources),
                                                               providers, dependencies, new EverrestConfiguration());
        ApplicationContextImpl.setCurrent(new ApplicationContextImpl(null, null, ProviderBinder.getInstance()));
        launcher = new ResourceLauncher(requestHandler);

        ApplicationPublisher deployer = new ApplicationPublisher(resources, providers);
        deployer.publish(new VirtualFileSystemApplication());

        // RUNTIME VARIABLES
        ConversationState user =
                new ConversationState(new Identity("john", new HashSet<MembershipEntry>(), new HashSet<>(Arrays.asList("developer"))));
        ConversationState.setCurrent(user);
        EnvironmentContext env = EnvironmentContext.getCurrent();
        env.setVariable(EnvironmentContext.WORKSPACE_ID, MY_WORKSPACE_ID);
        env.setVariable(EnvironmentContext.WORKSPACE_NAME, MY_WORKSPACE_ID);
    }

    /** @see junit.framework.TestCase#tearDown() */
    protected void tearDown() throws Exception {
        virtualFileSystemRegistry.unregisterProvider(MY_WORKSPACE_ID);
        super.tearDown();
    }

    // --------------------------------------------

    protected Property createProperty(String name, String value) {
        final Property property = DtoFactory.getInstance().createDto(Property.class);
        property.setName(name);
        if (value != null) {
            List<String> values = new ArrayList<>(1);
            values.add(value);
            property.setValue(values);
        }
        return property;
    }

    protected Principal createPrincipal(String name, Principal.Type type) {
        final Principal principal = DtoFactory.getInstance().createDto(Principal.class);
        principal.setName(name);
        principal.setType(type);
        return principal;
    }

    protected List<AccessControlEntry> createAcl(Map<Principal, Set<BasicPermissions>> permissions) {
        final List<AccessControlEntry> acl = new ArrayList<>(permissions.size());
        for (Map.Entry<Principal, Set<BasicPermissions>> e : permissions.entrySet()) {
            final Set<BasicPermissions> basicPermissions = e.getValue();
            final Principal principal = e.getKey();
            final List<String> plainPermissions = new ArrayList<>(basicPermissions.size());
            for (BasicPermissions permission : e.getValue()) {
                plainPermissions.add(permission.value());
            }
            final Principal copyPrincipal = DtoFactory.getInstance().createDto(Principal.class);
            copyPrincipal.setName(principal.getName());
            copyPrincipal.setType(principal.getType());
            final AccessControlEntry ace = DtoFactory.getInstance().createDto(AccessControlEntry.class);
            ace.setPrincipal(copyPrincipal);
            ace.setPermissions(plainPermissions);
            acl.add(ace);
        }
        return acl;
    }

    protected Item getItem(String id) throws Exception {
        String path = SERVICE_URI + "item/" + id;
        ContainerResponse response = launcher.service("GET", path, BASE_URI, null, null, null, null);
        if (response.getStatus() == 200) {
            return (Item)response.getEntity();
        }
        if (response.getStatus() == 404) {
            return null;
        }
        fail("Unable get " + id + ".\nStatus: " + response.getStatus() + "\nMessage: " + response.getEntity());
        //
        return null;
    }

    protected void checkPage(String url, String httpMethod, Method m, List<Object> expected) throws Exception {
        checkPage(url, httpMethod, null, null, m, expected);
    }

    protected void checkPage(String url, String httpMethod, Map<String, List<String>> headers, byte[] body, Method m,
                             List<Object> expected) throws Exception {
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        ContainerResponse response = launcher.service(httpMethod, url, BASE_URI, headers, body, writer, null);
        assertEquals(200, response.getStatus());
        @SuppressWarnings("unchecked")
        List<Item> items = ((ItemList)response.getEntity()).getItems();
        List<Object> all = new ArrayList<>(expected.size());
        for (Item i : items) {
            validateLinks(i);
            all.add(m.invoke(i));
        }
        assertEquals(all, expected);
    }

    protected void checkFileContext(String expectedBody, String expectedMediaType, VirtualFile file) throws Exception {
        assertEquals(expectedMediaType, file.getMediaType());
        ContentStream contentStream = file.getContent();
        InputStream in = contentStream.getStream();
        byte[] b = new byte[(int)contentStream.getLength()];
        int r;
        int offset = 0;
        while ((r = in.read(b, offset, b.length - offset)) > 0) {
            offset += r;
        }
        assertEquals(expectedBody, new String(b));
    }

    protected void validateLinks(Item item) throws Exception {
        Map<String, Link> links = item.getLinks();

        if (links.size() == 0) {
            fail("Links not found. ");
        }

        Link link = links.get(Link.REL_SELF);
        assertNotNull("'" + Link.REL_SELF + "' link not found. ", link);
        assertEquals(MediaType.APPLICATION_JSON, link.getType());
        assertEquals(Link.REL_SELF, link.getRel());
        assertEquals(UriBuilder.fromPath(SERVICE_URI).path("item").path(item.getId()).build().toString(), link.getHref());

        link = links.get(Link.REL_PARENT);
        if (item.getParentId() == null) {
            assertNull("'" + Link.REL_PARENT + "' link not allowed for root folder. ", link);
        } else {
            assertNotNull("'" + Link.REL_PARENT + "' link not found. ", link);
            assertEquals(MediaType.APPLICATION_JSON, link.getType());
            assertEquals(Link.REL_PARENT, link.getRel());
            assertEquals(UriBuilder.fromPath(SERVICE_URI).path("item").path(item.getParentId()).build().toString(),
                         link.getHref());
        }

        link = links.get(Link.REL_ACL);
        assertNotNull("'" + Link.REL_ACL + "' link not found. ", link);
        assertEquals(MediaType.APPLICATION_JSON, link.getType());
        assertEquals(Link.REL_ACL, link.getRel());
        assertEquals(UriBuilder.fromPath(SERVICE_URI).path("acl").path(item.getId()).build().toString(), link.getHref());

        link = links.get(Link.REL_DELETE);
        if (item.getParentId() == null) {
            assertNull("'" + Link.REL_DELETE + "' link not allowed for root folder. ", link);
        } else {
            assertNotNull("'" + Link.REL_DELETE + "' link not found. ", link);
            assertEquals(null, link.getType());
            assertEquals(Link.REL_DELETE, link.getRel());
            if (item.getItemType() == ItemType.FILE && ((File)item).isLocked()) {
                assertEquals(
                        UriBuilder.fromPath(SERVICE_URI).path("delete").path(item.getId())
                                  .queryParam("lockToken", "[lockToken]").build().toString(), link.getHref());
            } else {
                assertEquals(UriBuilder.fromPath(SERVICE_URI).path("delete").path(item.getId()).build().toString(),
                             link.getHref());
            }
        }

        link = links.get(Link.REL_COPY);
        if (item.getParentId() == null) {
            assertNull("'" + Link.REL_COPY + "' link not allowed for root folder. ", link);
        } else {
            assertNotNull("'" + Link.REL_COPY + "' link not found. ", link);
            assertEquals(MediaType.APPLICATION_JSON, link.getType());
            assertEquals(Link.REL_COPY, link.getRel());
            assertEquals(
                    UriBuilder.fromPath(SERVICE_URI).path("copy").path(item.getId()).queryParam("parentId", "[parentId]")
                              .build().toString(), link.getHref());
        }

        link = links.get(Link.REL_MOVE);
        if (item.getParentId() == null) {
            assertNull("'" + Link.REL_MOVE + "' link not allowed for root folder. ", link);
        } else {
            assertNotNull("'" + Link.REL_MOVE + "' link not found. ", link);
            assertEquals(MediaType.APPLICATION_JSON, link.getType());
            assertEquals(Link.REL_MOVE, link.getRel());
            if (item.getItemType() == ItemType.FILE && ((File)item).isLocked()) {
                assertEquals(
                        UriBuilder.fromPath(SERVICE_URI).path("move").path(item.getId()).queryParam("parentId", "[parentId]")
                                  .queryParam("lockToken", "[lockToken]").build().toString(), link.getHref());
            } else {
                assertEquals(
                        UriBuilder.fromPath(SERVICE_URI).path("move").path(item.getId()).queryParam("parentId", "[parentId]")
                                  .build().toString(), link.getHref());
            }
        }

        ItemType type = item.getItemType();
        if (type == ItemType.FILE) {
            File file = (File)item;

            link = links.get(Link.REL_CONTENT);
            assertNotNull("'" + Link.REL_CONTENT + "' link not found. ", link);
            assertEquals(file.getMimeType(), link.getType());
            assertEquals(Link.REL_CONTENT, link.getRel());
            assertEquals(UriBuilder.fromPath(SERVICE_URI).path("content").path(file.getId()).build().toString(),
                         link.getHref());

            link = links.get(Link.REL_DOWNLOAD_FILE);
            assertNotNull("'" + Link.REL_DOWNLOAD_FILE + "' link not found. ", link);
            assertEquals(file.getMimeType(), link.getType());
            assertEquals(Link.REL_DOWNLOAD_FILE, link.getRel());
            assertEquals(UriBuilder.fromPath(SERVICE_URI).path("downloadfile").path(file.getId()).build().toString(),
                         link.getHref());

            link = links.get(Link.REL_CONTENT_BY_PATH);
            assertNotNull("'" + Link.REL_CONTENT_BY_PATH + "' link not found. ", link);
            assertEquals(file.getMimeType(), link.getType());
            assertEquals(Link.REL_CONTENT_BY_PATH, link.getRel());
            assertEquals(UriBuilder.fromPath(SERVICE_URI).path("contentbypath").path(file.getPath().substring(1)).build()
                                   .toString(), link.getHref());

            link = links.get(Link.REL_CURRENT_VERSION);
            assertNotNull("'" + Link.REL_CURRENT_VERSION + "' link not found. ", link);
            assertEquals(MediaType.APPLICATION_JSON, link.getType());
            assertEquals(Link.REL_CURRENT_VERSION, link.getRel());
            // Versioning not supported. Have only one version of file so id of file is always id of current version.
            String expectedCurrentVersionId = file.getId();
            assertEquals(UriBuilder.fromPath(SERVICE_URI).path("item").path(expectedCurrentVersionId).build().toString(),
                         link.getHref());

            link = links.get(Link.REL_VERSION_HISTORY);
            assertNotNull("'" + Link.REL_VERSION_HISTORY + "' link not found. ", link);
            assertEquals(MediaType.APPLICATION_JSON, link.getType());
            assertEquals(Link.REL_VERSION_HISTORY, link.getRel());
            assertEquals(UriBuilder.fromPath(SERVICE_URI).path("version-history").path(file.getId()).build().toString(),
                         link.getHref());

            link = links.get(Link.REL_LOCK);
            if (file.isLocked()) {
                assertNull("'" + Link.REL_LOCK + "' link not allowed for locked files. ", link);
                link = links.get(Link.REL_UNLOCK);
                assertEquals(null, link.getType());
                assertEquals(Link.REL_UNLOCK, link.getRel());
                assertEquals(
                        UriBuilder.fromPath(SERVICE_URI).path("unlock").path(file.getId())
                                  .queryParam("lockToken", "[lockToken]").build().toString(), link.getHref());
            } else {
                assertNotNull("'" + Link.REL_LOCK + "' link not found. ", link);
                assertEquals(MediaType.APPLICATION_JSON, link.getType());
                assertEquals(Link.REL_LOCK, link.getRel());
                assertEquals(UriBuilder.fromPath(SERVICE_URI).path("lock").path(file.getId())
                                       .queryParam("timeout", "[timeout]").build().toString(), link.getHref());
                link = links.get(Link.REL_UNLOCK);
                assertNull("'" + Link.REL_UNLOCK + "' link not allowed for unlocked files. ", link);
            }
        } else {
            link = links.get(Link.REL_CHILDREN);
            assertNotNull("'" + Link.REL_CHILDREN + "' link not found. ", link);
            assertEquals(MediaType.APPLICATION_JSON, link.getType());
            assertEquals(Link.REL_CHILDREN, link.getRel());
            assertEquals(UriBuilder.fromPath(SERVICE_URI).path("children").path(item.getId()).build().toString(),
                         link.getHref());

            link = links.get(Link.REL_CREATE_FOLDER);
            assertNotNull("'" + Link.REL_CREATE_FOLDER + "' link not found. ", link);
            assertEquals(MediaType.APPLICATION_JSON, link.getType());
            assertEquals(Link.REL_CREATE_FOLDER, link.getRel());
            assertEquals(UriBuilder.fromPath(SERVICE_URI).path("folder").path(item.getId()).queryParam("name", "[name]")
                                   .build().toString(), link.getHref());

            link = links.get(Link.REL_CREATE_FILE);
            assertNotNull("'" + Link.REL_CREATE_FILE + "' link not found. ", link);
            assertEquals(MediaType.APPLICATION_JSON, link.getType());
            assertEquals(Link.REL_CREATE_FILE, link.getRel());
            assertEquals(UriBuilder.fromPath(SERVICE_URI).path("file").path(item.getId()).queryParam("name", "[name]")
                                   .build().toString(), link.getHref());

            link = links.get(Link.REL_UPLOAD_FILE);
            assertNotNull("'" + Link.REL_UPLOAD_FILE + "' link not found. ", link);
            assertEquals(MediaType.TEXT_HTML, link.getType());
            assertEquals(Link.REL_UPLOAD_FILE, link.getRel());
            assertEquals(UriBuilder.fromPath(SERVICE_URI).path("uploadfile").path(item.getId()).build().toString(),
                         link.getHref());

            link = links.get(Link.REL_CREATE_PROJECT);
            if (item instanceof Project) {
                assertNull("'" + Link.REL_CREATE_PROJECT + "' link not allowed for project. ", link);
            } else {
                assertNotNull("'" + Link.REL_CREATE_PROJECT + "' link not found. ", link);
                assertEquals(MediaType.APPLICATION_JSON, link.getType());
                assertEquals(Link.REL_CREATE_PROJECT, link.getRel());
                assertEquals(
                        UriBuilder.fromPath(SERVICE_URI).path("project").path(item.getId()).queryParam("name", "[name]")
                                  .queryParam("type", "[type]").build().toString(), link.getHref());
            }

            link = links.get(Link.REL_EXPORT);
            assertNotNull("'" + Link.REL_EXPORT + "' link not found. ", link);
            assertEquals("application/zip", link.getType());
            assertEquals(Link.REL_EXPORT, link.getRel());
            assertEquals(UriBuilder.fromPath(SERVICE_URI).path("export").path(item.getId()).build().toString(),
                         link.getHref());

            link = links.get(Link.REL_IMPORT);
            assertNotNull("'" + Link.REL_IMPORT + "' link not found. ", link);
            assertEquals("application/zip", link.getType());
            assertEquals(Link.REL_IMPORT, link.getRel());
            assertEquals(UriBuilder.fromPath(SERVICE_URI).path("import").path(item.getId()).build().toString(),
                         link.getHref());

            link = links.get(Link.REL_DOWNLOAD_ZIP);
            assertNotNull("'" + Link.REL_DOWNLOAD_ZIP + "' link not found. ", link);
            assertEquals("application/zip", link.getType());
            assertEquals(Link.REL_DOWNLOAD_ZIP, link.getRel());
            assertEquals(UriBuilder.fromPath(SERVICE_URI).path("downloadzip").path(item.getId()).build().toString(),
                         link.getHref());

            link = links.get(Link.REL_UPLOAD_ZIP);
            assertNotNull("'" + Link.REL_UPLOAD_ZIP + "' link not found. ", link);
            assertEquals(MediaType.TEXT_HTML, link.getType());
            assertEquals(Link.REL_UPLOAD_ZIP, link.getRel());
            assertEquals(UriBuilder.fromPath(SERVICE_URI).path("uploadzip").path(item.getId()).build().toString(),
                         link.getHref());
        }
    }

    protected void validateUrlTemplates(VirtualFileSystemInfo info) throws Exception {
        Map<String, Link> templates = info.getUrlTemplates();
        //log.info(">>>>>>>>>\n" + templates);

        Link template = templates.get(Link.REL_ITEM);
        assertNotNull("'" + Link.REL_ITEM + "' template not found. ", template);
        assertEquals(MediaType.APPLICATION_JSON, template.getType());
        assertEquals(Link.REL_ITEM, template.getRel());
        assertEquals(UriBuilder.fromPath(SERVICE_URI).path("item").path("[id]").build().toString(), template.getHref());

        template = templates.get(Link.REL_ITEM_BY_PATH);
        assertNotNull("'" + Link.REL_ITEM_BY_PATH + "' template not found. ", template);
        assertEquals(MediaType.APPLICATION_JSON, template.getType());
        assertEquals(Link.REL_ITEM_BY_PATH, template.getRel());
        assertEquals(UriBuilder.fromPath(SERVICE_URI).path("itembypath").path("[path]").build().toString(),
                     template.getHref());

        template = templates.get(Link.REL_COPY);
        assertNotNull("'" + Link.REL_COPY + "' template not found. ", template);
        assertEquals(MediaType.APPLICATION_JSON, template.getType());
        assertEquals(Link.REL_COPY, template.getRel());
        assertEquals(UriBuilder.fromPath(SERVICE_URI).path("copy").path("[id]").queryParam("parentId", "[parentId]")
                               .build().toString(), template.getHref());

        template = templates.get(Link.REL_MOVE);
        assertNotNull("'" + Link.REL_MOVE + "' template not found. ", template);
        assertEquals(MediaType.APPLICATION_JSON, template.getType());
        assertEquals(Link.REL_MOVE, template.getRel());
        assertEquals(UriBuilder.fromPath(SERVICE_URI).path("move").path("[id]").queryParam("parentId", "[parentId]")
                               .queryParam("lockToken", "[lockToken]").build().toString(), template.getHref());

        template = templates.get(Link.REL_CREATE_FILE);
        assertNotNull("'" + Link.REL_CREATE_FILE + "' template not found. ", template);
        assertEquals(MediaType.APPLICATION_JSON, template.getType());
        assertEquals(Link.REL_CREATE_FILE, template.getRel());
        assertEquals(UriBuilder.fromPath(SERVICE_URI).path("file").path("[parentId]").queryParam("name", "[name]")
                               .build().toString(), template.getHref());

        template = templates.get(Link.REL_CREATE_FOLDER);
        assertNotNull("'" + Link.REL_CREATE_FOLDER + "' template not found. ", template);
        assertEquals(MediaType.APPLICATION_JSON, template.getType());
        assertEquals(Link.REL_CREATE_FOLDER, template.getRel());
        assertEquals(UriBuilder.fromPath(SERVICE_URI).path("folder").path("[parentId]").queryParam("name", "[name]")
                               .build().toString(), template.getHref());

        template = templates.get(Link.REL_CREATE_PROJECT);
        assertNotNull("'" + Link.REL_CREATE_PROJECT + "' template not found. ", template);
        assertEquals(MediaType.APPLICATION_JSON, template.getType());
        assertEquals(Link.REL_CREATE_PROJECT, template.getRel());
        assertEquals(UriBuilder.fromPath(SERVICE_URI).path("project").path("[parentId]").queryParam("name", "[name]")
                               .queryParam("type", "[type]").build().toString(), template.getHref());

        template = templates.get(Link.REL_LOCK);
        assertNotNull("'" + Link.REL_LOCK + "' template not found. ", template);
        assertEquals(MediaType.APPLICATION_JSON, template.getType());
        assertEquals(Link.REL_LOCK, template.getRel());
        assertEquals(UriBuilder.fromPath(SERVICE_URI).path("lock").path("[id]")
                               .queryParam("timeout", "[timeout]").build().toString(), template.getHref());

        template = templates.get(Link.REL_UNLOCK);
        assertNotNull("'" + Link.REL_UNLOCK + "' template not found. ", template);
        assertEquals(null, template.getType());
        assertEquals(Link.REL_UNLOCK, template.getRel());
        assertEquals(UriBuilder.fromPath(SERVICE_URI).path("unlock").path("[id]").queryParam("lockToken", "[lockToken]")
                               .build().toString(), template.getHref());

        template = templates.get(Link.REL_SEARCH);
        assertNotNull("'" + Link.REL_SEARCH + "' template not found. ", template);
        assertEquals(MediaType.APPLICATION_JSON, template.getType());
        assertEquals(Link.REL_SEARCH, template.getRel());
        assertEquals(
                UriBuilder.fromPath(SERVICE_URI).path("search").queryParam("statement", "[statement]")
                          .queryParam("maxItems", "[maxItems]").queryParam("skipCount", "[skipCount]").build().toString(),
                template.getHref());

        template = templates.get(Link.REL_SEARCH_FORM);
        assertNotNull("'" + Link.REL_SEARCH_FORM + "' template not found. ", template);
        assertEquals(MediaType.APPLICATION_JSON, template.getType());
        assertEquals(Link.REL_SEARCH_FORM, template.getRel());
        assertEquals(
                UriBuilder.fromPath(SERVICE_URI).path("search").queryParam("maxItems", "[maxItems]")
                          .queryParam("skipCount", "[skipCount]").queryParam("propertyFilter", "[propertyFilter]").build().toString(),
                template.getHref());
    }

    static class Pair<A, B> {
        final A a;
        final B b;

        Pair(A a, B b) {
            this.a = a;
            this.b = b;
        }
    }
}
