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
package org.apache.jackrabbit.server.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Item;

/**
 * <code>AbstractExportContext</code> covers methods common to most ExportContext
 * implementations.
 */
public abstract class AbstractExportContext implements ExportContext {

    private static Logger log = LoggerFactory.getLogger(AbstractExportContext.class);

    private final IOListener ioListener;
    private final Item exportRoot;
    private final boolean hasStream;

    protected boolean completed;

    public AbstractExportContext(
            Item exportRoot, boolean hasStream, IOListener ioListener) {
        this.exportRoot = exportRoot;
        this.hasStream = hasStream;
        this.ioListener = (ioListener != null) ? ioListener : new DefaultIOListener(log);
    }

    public IOListener getIOListener() {
        return ioListener;
    }

    public Item getExportRoot() {
        return exportRoot;
    }

    public boolean hasStream() {
        return hasStream;
    }

    public void informCompleted(boolean success) {
        completed = true;
    }

    public boolean isCompleted() {
        return completed;
    }

    protected void checkCompleted() {
        if (completed) {
            throw new IllegalStateException("ExportContext has already been finalized.");
        }
    }
}