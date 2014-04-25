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
package com.codenvy.api.vfs.server.search;

import com.codenvy.api.vfs.server.LazyIterator;
import com.codenvy.api.vfs.server.MountPoint;
import com.codenvy.api.vfs.server.VirtualFile;
import com.codenvy.api.vfs.server.VirtualFileFilter;
import com.codenvy.api.vfs.server.exceptions.InvalidArgumentException;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemRuntimeException;
import com.codenvy.api.vfs.server.util.MediaTypeFilter;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.LinkedList;
import java.util.Set;

/**
 * Lucene based searcher.
 *
 * @author andrew00x
 */
public abstract class LuceneSearcher implements Searcher {
    private static final Logger LOG          = LoggerFactory.getLogger(LuceneSearcher.class);
    private static final int    RESULT_LIMIT = 1000;

    private final VirtualFileFilter filter;

    private IndexWriter   luceneIndexWriter;
    private Directory     luceneIndexDirectory;
    private IndexSearcher luceneIndexSearcher;
    private boolean       reopening;
    private boolean       closed;

    public LuceneSearcher(Set<String> indexedMediaTypes) {
        this(new MediaTypeFilter(indexedMediaTypes));
    }

    public LuceneSearcher(VirtualFileFilter filter) {
        this.filter = filter;
    }

    protected Analyzer makeAnalyzer() {
        return new SimpleAnalyzer();
    }

    protected abstract Directory makeDirectory() throws VirtualFileSystemException;

    /**
     * Init lucene index. Need call this method if index directory is clean. Scan all files in virtual filesystem and add to index.
     *
     * @param mountPoint
     *         MountPoint
     * @throws com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException
     *         if any virtual filesystem error
     */
    public synchronized void init(MountPoint mountPoint) throws VirtualFileSystemException {
        final long start = System.currentTimeMillis();
        try {
            luceneIndexDirectory = makeDirectory();
            luceneIndexWriter = new IndexWriter(luceneIndexDirectory, makeAnalyzer(), IndexWriter.MaxFieldLength.UNLIMITED);
            luceneIndexSearcher = new IndexSearcher(luceneIndexWriter.getReader());
            addTree(mountPoint.getRoot());
        } catch (IOException e) {
            throw new VirtualFileSystemException(e);
        }
        final long end = System.currentTimeMillis();
        LOG.debug("Index creation time: {} ms", (end - start));
    }

    public synchronized void close() {
        if (!closed) {
            closeQuietly(luceneIndexSearcher);
            closeQuietly(luceneIndexWriter);
            closeQuietly(luceneIndexDirectory);
            closed = true;
        }
    }

    /**
     * Get IndexSearcher. It is important to call method {@link #releaseLuceneSearcher(org.apache.lucene.search.IndexSearcher)}
     * to release obtained searcher.
     * <pre>
     *    Searcher searcher = ...
     *    IndexSearcher luceneSearcher = searcher.getLuceneSearcher();
     *    try {
     *       // use obtained lucene searcher
     *    } finally {
     *       searcher.releaseLuceneSearcher(searcher);
     *    }
     * </pre>
     *
     * @return IndexSearcher
     * @throws java.io.IOException
     *         if an i/o error occurs
     */
    public synchronized IndexSearcher getLuceneSearcher() throws IOException {
        maybeReopenIndexReader();
        luceneIndexSearcher.getIndexReader().incRef();
        return luceneIndexSearcher;
    }

    // MUST CALL UNDER LOCK
    private void maybeReopenIndexReader() throws IOException {
        while (reopening) {
            try {
                wait();
            } catch (InterruptedException e) {
                notify();
                throw new VirtualFileSystemRuntimeException(e);
            }
        }

        reopening = true;
        try {
            IndexReader reader = luceneIndexSearcher.getIndexReader();
            IndexReader newReader = luceneIndexSearcher.getIndexReader().reopen();
            if (newReader != reader) {
                luceneIndexSearcher = new IndexSearcher(newReader);
            }
        } finally {
            reopening = false;
            notifyAll();
        }
    }

    /**
     * Release IndexSearcher.
     *
     * @param luceneSearcher
     *         IndexSearcher
     * @throws java.io.IOException
     *         if an i/o error occurs
     * @see #getLuceneSearcher()
     */
    public synchronized void releaseLuceneSearcher(IndexSearcher luceneSearcher) throws IOException {
        luceneSearcher.getIndexReader().decRef();
    }

    @Override
    public String[] search(QueryExpression query) throws VirtualFileSystemException {
        final BooleanQuery luceneQuery = new BooleanQuery();
        final String name = query.getName();
        final String path = query.getPath();
        final String mediaType = query.getMediaType();
        final String text = query.getText();
        if (path != null) {
            luceneQuery.add(new PrefixQuery(new Term("path", path)), BooleanClause.Occur.MUST);
        }
        if (name != null) {
            luceneQuery.add(new WildcardQuery(new Term("name", name)), BooleanClause.Occur.MUST);
        }
        if (mediaType != null) {
            luceneQuery.add(new TermQuery(new Term("mediatype", mediaType)), BooleanClause.Occur.MUST);
        }
        if (text != null) {
            QueryParser qParser = new QueryParser(Version.LUCENE_29, "text", makeAnalyzer());
            try {
                luceneQuery.add(qParser.parse(text), BooleanClause.Occur.MUST);
            } catch (ParseException e) {
                throw new InvalidArgumentException(e.getMessage());
            }
        }
        IndexSearcher luceneSearcher = null;
        try {
            luceneSearcher = getLuceneSearcher();
            final TopDocs topDocs = luceneSearcher.search(luceneQuery, RESULT_LIMIT);
            if (topDocs.totalHits > RESULT_LIMIT) {
                throw new VirtualFileSystemException(
                        String.format("Too many (%d) matched results found. ", topDocs.totalHits));
            }
            final String[] result = new String[topDocs.scoreDocs.length];
            for (int i = 0, length = result.length; i < length; i++) {
                result[i] = luceneSearcher.doc(topDocs.scoreDocs[i].doc).getField("path").stringValue();
            }
            return result;
        } catch (IOException e) {
            throw new VirtualFileSystemException(e.getMessage(), e);
        } finally {
            if (luceneSearcher != null) {
                try {
                    releaseLuceneSearcher(luceneSearcher);
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public final void add(VirtualFile virtualFile) throws VirtualFileSystemException {
        if (filter.accept(virtualFile)) {
            doAdd(virtualFile);
        }
    }

    protected void doAdd(VirtualFile virtualFile) throws VirtualFileSystemException {
        if (virtualFile.isFolder()) {
            addTree(virtualFile);
        } else {
            addFile(virtualFile);
        }
    }

    protected void addTree(VirtualFile tree) throws VirtualFileSystemException {
        final LinkedList<VirtualFile> q = new LinkedList<>();
        q.add(tree);
        int indexedFiles = 0;
        while (!q.isEmpty()) {
            final VirtualFile folder = q.pop();
            if (folder.exists()) {
                LazyIterator<VirtualFile> children = folder.getChildren(VirtualFileFilter.ALL);
                while (children.hasNext()) {
                    final VirtualFile child = children.next();
                    if (child.isFolder()) {
                        q.push(child);
                    } else {
                        if (filter.accept(child)) {
                            addFile(child);
                            indexedFiles++;
                        }
                    }
                }
            }
        }
        LOG.debug("Indexed {} files from {}", indexedFiles, tree.getPath());
    }

    protected void addFile(VirtualFile virtualFile) throws VirtualFileSystemException {
        if (virtualFile.exists()) {
            Reader fContentReader = null;
            try {
                fContentReader = new BufferedReader(new InputStreamReader(virtualFile.getContent().getStream()));
                luceneIndexWriter.updateDocument(new Term("path", virtualFile.getPath()), createDocument(virtualFile, fContentReader));
            } catch (OutOfMemoryError oome) {
                close();
                throw oome;
            } catch (IOException e) {
                throw new VirtualFileSystemException(e.getMessage(), e);
            } finally {
                if (fContentReader != null) {
                    try {
                        fContentReader.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }
    }

    @Override
    public final void delete(String path) throws VirtualFileSystemException {
        doDelete(new Term("path", path));
    }

    protected void doDelete(Term deleteTerm) throws VirtualFileSystemException {
        try {
            luceneIndexWriter.deleteDocuments(new PrefixQuery(deleteTerm));
        } catch (OutOfMemoryError oome) {
            close();
            throw oome;
        } catch (IOException e) {
            throw new VirtualFileSystemException(e.getMessage(), e);
        }
    }

    @Override
    public final void update(VirtualFile virtualFile) throws VirtualFileSystemException {
        if (filter.accept(virtualFile)) {
            doUpdate(new Term("path", virtualFile.getPath()), virtualFile);
        }
    }

    protected void doUpdate(Term deleteTerm, VirtualFile virtualFile) throws VirtualFileSystemException {
        Reader fContentReader = null;
        try {
            fContentReader = new BufferedReader(new InputStreamReader(virtualFile.getContent().getStream()));
            luceneIndexWriter.updateDocument(deleteTerm, createDocument(virtualFile, fContentReader));
        } catch (OutOfMemoryError oome) {
            close();
            throw oome;
        } catch (IOException e) {
            throw new VirtualFileSystemException(e.getMessage(), e);
        } finally {
            if (fContentReader != null) {
                try {
                    fContentReader.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    protected Document createDocument(VirtualFile virtualFile, Reader inReader) throws VirtualFileSystemException {
        final Document doc = new Document();
        doc.add(new Field("path", virtualFile.getPath(), Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field("name", virtualFile.getName(), Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field("mediatype", getMediaType(virtualFile), Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field("text", inReader));
        return doc;
    }

    /** Get virtual file media type. Any additional parameters (e.g. 'charset') are removed. */
    private String getMediaType(VirtualFile virtualFile) throws VirtualFileSystemException {
        String mediaType = virtualFile.getMediaType();
        final int paramStartIndex = mediaType.indexOf(';');
        if (paramStartIndex != -1) {
            mediaType = mediaType.substring(0, paramStartIndex).trim();
        }
        return mediaType;
    }

    private void closeQuietly(IndexSearcher indexSearcher) {
        if (indexSearcher != null) {
            try {
                indexSearcher.getIndexReader().close();
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    private void closeQuietly(IndexWriter indexWriter) {
        if (indexWriter != null) {
            try {
                indexWriter.close();
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    private void closeQuietly(Directory directory) {
        if (directory != null) {
            try {
                directory.close();
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }
}
