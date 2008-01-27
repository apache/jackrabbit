/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.query.lucene;

import org.apache.jackrabbit.extractor.TextExtractor;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.Reader;
import java.io.InputStream;
import java.io.IOException;

import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;
import EDU.oswego.cs.dl.util.concurrent.Channel;
import EDU.oswego.cs.dl.util.concurrent.SynchronousChannel;
import EDU.oswego.cs.dl.util.concurrent.BoundedLinkedQueue;

/**
 * <code>PooledTextExtractor</code> implements a text extractor that extracts
 * the text using a pool of background threads.
 */
public class PooledTextExtractor implements TextExtractor {

    /**
     * The logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(PooledTextExtractor.class);

    /**
     * The actual text extractor.
     */
    private final TextExtractor extractor;

    /**
     * The pooled executor.
     */
    private final PooledExecutor executor;

    /**
     * The timeout for the {@link TextExtractorReader}.
     */
    private final long timout;

    /**
     * Returns a pooled text extractor based on <code>extractor</code>.
     *
     * @param extractor the actual text extractor.
     * @param poolSize  the pool size.
     * @param backLog   size of the back log queue.
     * @param timeout   the timeout in milliseconds until text extraction is put
     *                  into the indexing queue and the fulltext index for the
     *                  node is later updated when the text extractor finished
     *                  its work.
     */
    public PooledTextExtractor(TextExtractor extractor,
                               int poolSize,
                               int backLog,
                               long timeout) {
        this.extractor = extractor;
        this.timout = timeout;
        Channel c;
        if (backLog <= 0) {
            c = new SynchronousChannel();
        } else {
            c = new BoundedLinkedQueue(backLog);
        }
        this.executor = new PooledExecutor(c, poolSize);
        this.executor.setMinimumPoolSize(poolSize);
        this.executor.setBlockedExecutionHandler(
                new PooledExecutor.BlockedExecutionHandler() {
            public boolean blockedAction(Runnable command) {
                // execute with current thread and log message
                log.info("Extractor pool busy, running command with "
                        + "current thread: {}", command.toString());
                command.run();
                return true;
            }
        });
    }


    /**
     * {@inheritDoc}
     */
    public String[] getContentTypes() {
        return extractor.getContentTypes();
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This implementation returns an instance of {@link TextExtractorReader}.
     */
    public Reader extractText(InputStream stream,
                              String type,
                              String encoding) throws IOException {
        TextExtractorJob job = new TextExtractorJob(extractor, stream, type, encoding);
        return new TextExtractorReader(job, executor, timout);
    }

    /**
     * Shuts down this pooled text extractor. This methods stops all currently
     * running text extractor tasks and cleans up the pending queue (back log).
     */
    public void shutdown() {
        executor.shutdownNow();
        boolean interrupted;
        do {
            try {
                executor.awaitTerminationAfterShutdown();
                interrupted = false;
            } catch (InterruptedException e) {
                interrupted = true;
            }
        } while (interrupted);
        executor.drain();
    }
}
