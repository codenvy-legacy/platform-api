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
package com.codenvy.api.core.util;

import com.codenvy.commons.lang.ZipUtils;
import com.google.common.hash.Hashing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * This implementation of DownloadPlugin is able to download remote content as single zip archive. Implementation is able get only updates
 * from remote source. If directory {@code downloadTo} is not empty this plugin will traverse it and count md5sum for each file. Then
 * plugins makes POST request to {@code downloadUrl} and sends content in next format:
 * <pre>
 * &lt;md5sum&gt;&lt;space&gt;&lt;file path relative to download folder&gt;
 * ...
 * </pre>
 * Remote server respects such request and sends only updates files in single zip archive. This instance applies changes to local directory
 * {@code downloadTo}. If {@code downloadTo} is empty then this instance may send GET request to {@code downloadUrl} and unpacks content to
 * {@code downloadTo} directory.
 *
 * @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a>
 */
public class HttpDownloadPluginWithUpdates implements DownloadPlugin {
    private static final Logger LOG = LoggerFactory.getLogger(HttpDownloadPluginWithUpdates.class);

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE MMM d HH:mm:ss:SSS yyyy", Locale.US);

    @Override
    public void download(String downloadUrl, java.io.File downloadTo, Callback callback) {
        HttpURLConnection conn = null;
        try {
            long downloadDate = 0;
            final Path downloadDateFile = downloadTo.toPath().resolve("download_date");
            if (java.nio.file.Files.isReadable(downloadDateFile)) {
                try (BufferedReader reader = java.nio.file.Files.newBufferedReader(downloadDateFile, Charset.forName("UTF-8"))) {
                    try {
                        downloadDate = ((SimpleDateFormat)DATE_FORMAT.clone()).parse(reader.readLine()).getTime();
                    } catch (ParseException e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
            }
            FileFilter filter = null;
            if (downloadDate > 0) {
                filter = new ModificationDateFileFilter(downloadDate);
            }
            final LinkedList<java.io.File> q = new LinkedList<>();
            q.add(downloadTo);
            final long start = System.currentTimeMillis();
            final List<Pair<String, String>> md5sums = new LinkedList<>();
            while (!q.isEmpty()) {
                java.io.File current = q.pop();
                java.io.File[] list = filter == null ? current.listFiles() : current.listFiles(filter);
                if (list != null) {
                    for (java.io.File f : list) {
                        if (f.isDirectory()) {
                            q.push(f);
                        } else {
                            if (!"download_date".equals(f.getName())) {
                                md5sums.add(Pair.of(com.google.common.io.Files.hash(f, Hashing.md5()).toString(),
                                                    downloadTo.toPath().relativize(f.toPath()).toString()));
                            }
                        }
                    }
                }
            }
            final long end = System.currentTimeMillis();
            if (md5sums.size() > 0) {
                LOG.info("count md5sums of {} files, time: {}ms", md5sums.size(), (end - start)); // TODO: debug
            }
            conn = (HttpURLConnection)new URL(downloadUrl).openConnection();
            conn.setConnectTimeout(30 * 1000);
            conn.setConnectTimeout(30 * 1000);
            if (!md5sums.isEmpty()) {
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-type", "text/plain");
                conn.setDoOutput(true);
                try (OutputStream output = conn.getOutputStream();
                     Writer writer = new OutputStreamWriter(output)) {
                    for (Pair<String, String> pair : md5sums) {
                        writer.write(pair.first);
                        writer.write(' ');
                        writer.write(pair.second);
                        writer.write('\n');
                    }
                }
            }
            final int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new IOException(String.format("Invalid response status %d from remote server. ", responseCode));
            }
            try (InputStream in = conn.getInputStream()) {
                ZipUtils.unzip(in, downloadTo);
            }
            final String removeHeader = conn.getHeaderField("x-removed-paths");
            if (removeHeader != null) {
                for (String item : removeHeader.split(",")) {
                    java.io.File f = new java.io.File(downloadTo, item);
                    if (!f.delete()) {
                        if (f.exists()) {
                            throw new IOException(String.format("Can't delete %s", item));
                        }
                    }
                }
            }
            try (BufferedWriter writer = java.nio.file.Files.newBufferedWriter(downloadDateFile, Charset.forName("UTF-8"))) {
                writer.write(((SimpleDateFormat)DATE_FORMAT.clone()).format(new Date()));
            }
            callback.done(downloadTo);
        } catch (IOException e) {
            callback.error(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static class ModificationDateFileFilter implements FileFilter {
        long date;

        ModificationDateFileFilter(long date) {
            this.date = date;
        }

        @Override
        public boolean accept(java.io.File f) {
            return f.lastModified() <= date;
        }
    }
}
