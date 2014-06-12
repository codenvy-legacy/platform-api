/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.vfs.server.impl.memory;

import com.codenvy.api.core.util.Pair;
import com.codenvy.api.vfs.server.VirtualFile;
import com.codenvy.api.vfs.server.search.LuceneSearcher;
import com.codenvy.api.vfs.shared.dto.Item;
import com.codenvy.api.vfs.shared.dto.ItemList;

import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.Version;
import org.everrest.core.impl.ContainerResponse;
import org.everrest.core.tools.ByteArrayContainerResponseWriter;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author andrew00x
 */
public class SearcherTest extends MemoryFileSystemTest {
    private Pair<String[], String>[] queryToResult;
    private VirtualFile              searchTestFolder;
    private String                   searchTestPath;
    private String                   file1;
    private String                   file2;
    private String                   file3;

    private LuceneSearcher searcher;

    @SuppressWarnings("unchecked")
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        searchTestFolder = mountPoint.getRoot().createFolder("SearcherTest");
        searcher = (LuceneSearcher)mountPoint.getSearcherProvider().getSearcher(mountPoint, true);

        VirtualFile searchTestFolder = this.searchTestFolder.createFolder("SearcherTest_Folder");
        searchTestPath = searchTestFolder.getPath();

        file1 = searchTestFolder.createFile("SearcherTest_File01", "text/xml", new ByteArrayInputStream("to be or not to be".getBytes()))
                                .getPath();

        file2 = searchTestFolder.createFile("SearcherTest_File02", "text/plain", new ByteArrayInputStream("to be or not to be".getBytes()))
                                .getPath();

        VirtualFile folder = searchTestFolder.createFolder("folder01");
        String folder1 = folder.getPath();
        file3 = folder.createFile("SearcherTest_File03", "text/plain", new ByteArrayInputStream("to be or not to be".getBytes())).getPath();

        queryToResult = new Pair[10];
        // text
        queryToResult[0] = new Pair<>(new String[]{file1, file2, file3}, "text=to%20be%20or%20not%20to%20be");
        queryToResult[1] = new Pair<>(new String[]{file1, file2, file3}, "text=to%20be%20or");
        // text + media type
        queryToResult[2] = new Pair<>(new String[]{file2, file3}, "text=to%20be%20or&mediaType=text/plain");
        queryToResult[3] = new Pair<>(new String[]{file1}, "text=to%20be%20or&mediaType=text/xml");
        // text + name
        queryToResult[4] = new Pair<>(new String[]{file2}, "text=to%20be%20or&name=*File02");
        queryToResult[5] = new Pair<>(new String[]{file1, file2, file3}, "text=to%20be%20or&name=SearcherTest*");
        // text + path
        queryToResult[6] = new Pair<>(new String[]{file3}, "text=to%20be%20or&path=" + folder1);
        queryToResult[7] = new Pair<>(new String[]{file1, file2, file3}, "text=to%20be%20or&path=" + searchTestPath);
        // name + media type
        queryToResult[8] = new Pair<>(new String[]{file2, file3}, "name=SearcherTest*&mediaType=text/plain");
        queryToResult[9] = new Pair<>(new String[]{file1}, "name=SearcherTest*&mediaType=text/xml");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void testSearch() throws Exception {
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String requestPath = SERVICE_URI + "search";
        Map<String, List<String>> h = new HashMap<>(1);
        h.put("Content-Type", Arrays.asList("application/x-www-form-urlencoded"));
        for (Pair<String[], String> pair : queryToResult) {
            ContainerResponse response = launcher.service("POST", requestPath, BASE_URI, h, pair.second.getBytes(), writer, null);
            //log.info(new String(writer.getBody()));
            assertEquals("Error: " + response.getEntity(), 200, response.getStatus());
            List<Item> result = ((ItemList)response.getEntity()).getItems();
            assertEquals(String.format(
                    "Expected %d but found %d for query %s", pair.first.length, result.size(), pair.second),
                         pair.first.length,
                         result.size());
            List<String> resultPaths = new ArrayList<>(result.size());
            for (Item item : result) {
                resultPaths.add(item.getPath());
            }
            List<String> copy = new ArrayList<>(resultPaths);
            copy.removeAll(Arrays.asList(pair.first));
            assertTrue(String.format("Expected result is %s but found %s", Arrays.toString(pair.first), resultPaths), copy.isEmpty());
            writer.reset();
        }
    }

    public void testDelete() throws Exception {
        IndexSearcher luceneSearcher = searcher.getLuceneSearcher();
        TopDocs topDocs = luceneSearcher.search(new TermQuery(new Term("path", file1)), 10);
        assertEquals(1, topDocs.totalHits);
        searcher.releaseLuceneSearcher(luceneSearcher);

        mountPoint.getVirtualFile(file1).delete(null);
        luceneSearcher = searcher.getLuceneSearcher();
        topDocs = luceneSearcher.search(new TermQuery(new Term("path", file1)), 10);
        assertEquals(0, topDocs.totalHits);
        searcher.releaseLuceneSearcher(luceneSearcher);
    }

    public void testDelete2() throws Exception {
        IndexSearcher luceneSearcher = searcher.getLuceneSearcher();
        TopDocs topDocs = luceneSearcher.search(new PrefixQuery(new Term("path", searchTestPath)), 10);
        assertEquals(3, topDocs.totalHits);
        searcher.releaseLuceneSearcher(luceneSearcher);

        mountPoint.getVirtualFile(searchTestPath).delete(null);
        luceneSearcher = searcher.getLuceneSearcher();
        topDocs = luceneSearcher.search(new PrefixQuery(new Term("path", searchTestPath)), 10);
        assertEquals(0, topDocs.totalHits);
        searcher.releaseLuceneSearcher(luceneSearcher);
    }

    public void testAdd() throws Exception {
        IndexSearcher luceneSearcher = searcher.getLuceneSearcher();
        TopDocs topDocs = luceneSearcher.search(new PrefixQuery(new Term("path", searchTestPath)), 10);
        assertEquals(3, topDocs.totalHits);
        searcher.releaseLuceneSearcher(luceneSearcher);
        mountPoint.getVirtualFile(searchTestPath).createFile("new_file", "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT_BYTES));

        luceneSearcher = searcher.getLuceneSearcher();
        topDocs = luceneSearcher.search(new PrefixQuery(new Term("path", searchTestPath)), 10);
        assertEquals(4, topDocs.totalHits);
        searcher.releaseLuceneSearcher(luceneSearcher);
    }

    public void testUpdate() throws Exception {
        IndexSearcher luceneSearcher = searcher.getLuceneSearcher();
        TopDocs topDocs = luceneSearcher.search(
                new QueryParser(Version.LUCENE_29, "text", new SimpleAnalyzer()).parse("updated"), 10);
        assertEquals(0, topDocs.totalHits);
        searcher.releaseLuceneSearcher(luceneSearcher);
        mountPoint.getVirtualFile(file2).updateContent("text/plain", new ByteArrayInputStream("updated content".getBytes()), null);

        luceneSearcher = searcher.getLuceneSearcher();
        topDocs = luceneSearcher.search(new QueryParser(Version.LUCENE_29, "text", new SimpleAnalyzer()).parse("updated"), 10);
        assertEquals(1, topDocs.totalHits);
        searcher.releaseLuceneSearcher(luceneSearcher);
    }

    public void testMove() throws Exception {
        IndexSearcher luceneSearcher = searcher.getLuceneSearcher();
        String destination = searchTestFolder.createFolder("___destination").getPath();
        String expected = destination + '/' + "SearcherTest_File03";
        TopDocs topDocs = luceneSearcher.search(new PrefixQuery(new Term("path", expected)), 10);
        assertEquals(0, topDocs.totalHits);
        searcher.releaseLuceneSearcher(luceneSearcher);
        mountPoint.getVirtualFile(file3).moveTo(mountPoint.getVirtualFile(destination), null);

        luceneSearcher = searcher.getLuceneSearcher();
        topDocs = luceneSearcher.search(new PrefixQuery(new Term("path", expected)), 10);
        assertEquals(1, topDocs.totalHits);
        topDocs = luceneSearcher.search(new PrefixQuery(new Term("path", file3)), 10);
        assertEquals(0, topDocs.totalHits);
        searcher.releaseLuceneSearcher(luceneSearcher);
    }

    public void testCopy() throws Exception {
        IndexSearcher luceneSearcher = searcher.getLuceneSearcher();
        String destination = searchTestFolder.createFolder("___destination").getPath();
        String expected = destination + '/' + "SearcherTest_File03";
        TopDocs topDocs = luceneSearcher.search(new PrefixQuery(new Term("path", expected)), 10);
        assertEquals(0, topDocs.totalHits);
        searcher.releaseLuceneSearcher(luceneSearcher);
        mountPoint.getVirtualFile(file3).copyTo(mountPoint.getVirtualFile(destination));

        luceneSearcher = searcher.getLuceneSearcher();
        topDocs = luceneSearcher.search(new PrefixQuery(new Term("path", expected)), 10);
        assertEquals(1, topDocs.totalHits);
        topDocs = luceneSearcher.search(new PrefixQuery(new Term("path", file3)), 10);
        assertEquals(1, topDocs.totalHits);
        searcher.releaseLuceneSearcher(luceneSearcher);
    }

    public void testRename() throws Exception {
        String newName = "___renamed";
        IndexSearcher luceneSearcher = searcher.getLuceneSearcher();
        TopDocs topDocs = luceneSearcher.search(new PrefixQuery(new Term("path", file3)), 10);
        assertEquals(1, topDocs.totalHits);
        searcher.releaseLuceneSearcher(luceneSearcher);
        mountPoint.getVirtualFile(file2).rename(newName, null, null);

        luceneSearcher = searcher.getLuceneSearcher();
        topDocs = luceneSearcher.search(new PrefixQuery(new Term("path", searchTestPath + '/' + newName)), 10);
        assertEquals(1, topDocs.totalHits);
        topDocs = luceneSearcher.search(new PrefixQuery(new Term("path", file2)), 10);
        assertEquals(0, topDocs.totalHits);
        searcher.releaseLuceneSearcher(luceneSearcher);
    }
}
