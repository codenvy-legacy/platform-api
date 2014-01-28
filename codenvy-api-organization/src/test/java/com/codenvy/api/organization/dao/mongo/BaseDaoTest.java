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
package com.codenvy.api.organization.dao.mongo;

import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;

import java.net.InetSocketAddress;

/**
 *
 */
public class BaseDaoTest {

    private static final String DB_NAME = "test1";

    protected DBCollection collection;
    protected MongoClient client;
    protected MongoServer server;
    protected DB db;

    public void setUp(String collectionName) throws Exception {
        server = new MongoServer(new MemoryBackend());

        // bind on a random local port
        InetSocketAddress serverAddress = server.bind();

        client = new MongoClient(new ServerAddress(serverAddress));
        db = client.getDB(DB_NAME);
        collection = db.getCollection(collectionName);

    }

    public void tearDown() throws Exception {
        client.close();
        server.shutdownNow();
    }

}
