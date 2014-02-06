/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2014] Codenvy, S.A.
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
package com.codenvy.api.organization.dao.ldap;

import com.codenvy.api.core.util.CustomPortService;

import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.core.schema.SchemaPartition;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.ldif.extractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.ldif.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.loader.ldif.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.shared.ldap.schema.registries.SchemaLoader;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** @author andrew00x */
public class EmbeddedLdapServer {
    private DirectoryService service;

    private LdapServer server;
    private CustomPortService ports = new CustomPortService(8000, 10000);
    private String url;
    private int    port;

    public static EmbeddedLdapServer start(File serverDir) throws Exception {
        EmbeddedLdapServer embeddedLdapServer = new EmbeddedLdapServer(serverDir);
        embeddedLdapServer.server = new LdapServer();
        embeddedLdapServer.port = embeddedLdapServer.ports.acquire();
        embeddedLdapServer.server.setTransports(new TcpTransport(embeddedLdapServer.port));
        embeddedLdapServer.server.setDirectoryService(embeddedLdapServer.service);
        embeddedLdapServer.server.start();
        embeddedLdapServer.url = "ldap://localhost:" + embeddedLdapServer.port;
        return embeddedLdapServer;
    }

    public void stop() throws Exception {
        service.shutdown();
        server.stop();
        ports.release(port);
        service = null;
        server = null;
    }

    public int getPort() {
        return port;
    }

    public String getUrl() {
        return url;
    }

    private void initDirectoryService(File workDir) throws Exception {
        service = new DefaultDirectoryService();
        service.setWorkingDirectory(workDir);

        SchemaPartition schemaPartition = service.getSchemaService().getSchemaPartition();
        LdifPartition ldifPartition = new LdifPartition();
        String workDirectory = service.getWorkingDirectory().getPath();
        ldifPartition.setWorkingDirectory(workDirectory + "/schema");
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor(new File(workDirectory));
        extractor.extractOrCopy(true);
        schemaPartition.setWrappedPartition(ldifPartition);
        SchemaLoader schemaLoader = new LdifSchemaLoader(new File(workDirectory + "/schema"));
        SchemaManager schemaManager = new DefaultSchemaManager(schemaLoader);
        service.setSchemaManager(schemaManager);
        service.setShutdownHookEnabled(false);
        schemaManager.loadAllEnabled();
        schemaPartition.setSchemaManager(schemaManager);
        List<Throwable> errors = schemaManager.getErrors();
        if (!errors.isEmpty()) {
            throw new Exception("Schema load failed : " + errors);
        }

        Partition systemPartition = addPartition("system", ServerDNConstants.SYSTEM_DN);
        service.setSystemPartition(systemPartition);
        service.getChangeLog().setEnabled(false);
        service.setDenormalizeOpAttrsEnabled(true);
        Partition codenvyPartition = addPartition("codenvy", "dc=codenvy,dc=com");
        addIndex(codenvyPartition, "objectClass", "ou", "uid");
        service.startup();
        CoreSession adminSession = service.getAdminSession();
        if (!adminSession.exists(codenvyPartition.getSuffixDn())) {
            DN dnCodenvy = new DN("dc=codenvy,dc=com");
            ServerEntry codenvy = service.newEntry(dnCodenvy);
            codenvy.add("objectClass", "top", "domain", "extensibleObject");
            codenvy.add("dc", "codenvy");
            adminSession.add(codenvy);
        }
    }

    private Partition addPartition(String partitionId, String partitionDn) throws Exception {
        JdbmPartition partition = new JdbmPartition();
        partition.setId(partitionId);
        partition.setPartitionDir(new File(service.getWorkingDirectory(), partitionId));
        partition.setSuffix(partitionDn);
        service.addPartition(partition);
        return partition;
    }


    private void addIndex(Partition partition, String... attributes) {
        Set<Index<?, ServerEntry, Long>> indexedAttributes = new HashSet<>();
        for (String attribute : attributes) {
            indexedAttributes.add(new JdbmIndex<String, ServerEntry>(attribute));
        }
        ((JdbmPartition)partition).setIndexedAttributes(indexedAttributes);
    }

    private EmbeddedLdapServer(File workDir) throws Exception {
        initDirectoryService(workDir);
    }
}
