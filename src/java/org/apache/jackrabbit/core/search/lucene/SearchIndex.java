/*
 * Copyright 2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.search.lucene;

import EDU.oswego.cs.dl.util.concurrent.FIFOReadWriteLock;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.Query;

import java.io.IOException;

/**
 */
public class SearchIndex {

    private static final Logger log = Logger.getLogger(SearchIndex.class);

    /**
     * 512k default size
     */
    //private static final long DEFAULT_MERGE_SIZE = 512 * 1024 * 1024;

    //private long mergeSize = DEFAULT_MERGE_SIZE;

    private PersistentIndex persistentIndex;

    //private VolatileIndex volatileIndex;

    private final Analyzer analyzer;

    private final FIFOReadWriteLock readWriteLock = new FIFOReadWriteLock();

    public SearchIndex(FileSystem fs, Analyzer analyzer)
            throws IOException {
        //volatileIndex = new VolatileIndex(analyzer);
        boolean create;
        try {
            create = !fs.exists("segments");
            persistentIndex = new PersistentIndex(fs, create, analyzer);
            persistentIndex.setUseCompoundFile(true);
            this.analyzer = analyzer;
        } catch (FileSystemException e) {
            throw new IOException(e.getMessage());
        }
    }

    public void addDocument(Document doc) throws IOException {
        try {
            readWriteLock.writeLock().acquire();
        } catch (InterruptedException e) {
            // FIXME: ??? do logging, simply return?
            return;
        }

        try {
            persistentIndex.addDocument(doc);
        } finally {
            readWriteLock.writeLock().release();
        }

        /*
        volatileIndex.addDocument(doc);
        if (volatileIndex.size() > mergeSize) {
            persistentIndex.mergeIndex(volatileIndex);
            // create new volatile index
            volatileIndex = new VolatileIndex(analyzer);
        }
        */
    }

    public void removeDocument(Term idTerm) throws IOException {
        try {
            readWriteLock.writeLock().acquire();
        } catch (InterruptedException e) {
            // FIXME: ??? do logging, simply return?
            return;
        }

        try {
            persistentIndex.removeDocument(idTerm);
        } finally {
            readWriteLock.writeLock().release();
        }

        //volatileIndex.removeDocument(idTerm);
    }

    public void close() {
        /*
        try {
            persistentIndex.mergeIndex(volatileIndex);
        } catch (IOException e) {
            // FIXME do logging
        }
        volatileIndex.close();
        */
        log.info("Closing search index.");
        persistentIndex.close();
    }

    public Hits executeQuery(Query query,
                             String[] orderProps,
                             boolean[] orderSpecs) throws IOException {
        try {
            readWriteLock.readLock().acquire();
        } catch (InterruptedException e) {
            // FIXME: ??? do logging, simply return?
            return null;
        }

        /*
        SortField[] sortFields = new SortField[orderProps.length];
        for (int i = 0; i < orderProps.length; i++) {
            sortFields[i] = new SortField(orderProps[i], SortField.STRING, !ascending);
        }
*/
        Hits hits = null;
        try {
            hits = persistentIndex.getIndexSearcher().search(query
                    /*, new Sort(sortFields) */);
        } finally {
            readWriteLock.readLock().release();
        }

        return hits;
    }

    public Analyzer getAnalyzer() {
        return analyzer;
    }

    //--------------------------< properties >----------------------------------

    public void setUseCompoundFile(boolean b) {
        persistentIndex.setUseCompoundFile(b);
    }

    public void setMinMergeDocs(int minMergeDocs) {
        persistentIndex.setMinMergeDocs(minMergeDocs);
    }

    public void setMaxMergeDocs(int maxMergeDocs) {
        persistentIndex.setMaxMergeDocs(maxMergeDocs);
    }

    public void setMergeFactor(int mergeFactor) {
        persistentIndex.setMergeFactor(mergeFactor);
    }
}
