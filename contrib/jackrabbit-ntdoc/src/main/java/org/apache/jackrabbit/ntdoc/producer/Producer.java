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
package org.apache.jackrabbit.ntdoc.producer;

import java.io.*;

import org.apache.jackrabbit.ntdoc.model.*;
import org.apache.jackrabbit.ntdoc.reporter.*;

/**
 * This class defines the abstract doc producer.
 */
public abstract class Producer
        extends ReporterDelegator {
    /**
     * Title.
     */
    private String title;

    /**
     * Output directory.
     */
    private File outputDir;

    /**
     * Node types.
     */
    private NodeTypeSet nodeTypes;

    /**
     * Return the title.
     */
    public String getTitle() {
        return this.title;
    }

    /**
     * Set the title.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Return the output dir.
     */
    public File getOutputDir() {
        return this.outputDir;
    }

    /**
     * Set the output directory.
     */
    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;
    }

    /**
     * Return the node types.
     */
    public NodeTypeSet getNodeTypes() {
        return this.nodeTypes;
    }

    /**
     * Set the node types.
     */
    public void setNodeTypes(NodeTypeSet nodeTypes) {
        this.nodeTypes = nodeTypes;
    }

    /**
     * Open a relative file.
     */
    protected File createFile(String name) {
        File file = new File(this.outputDir, name);
        file.getParentFile().mkdirs();
        return file;
    }

    /**
     * Open a relative file.
     */
    protected FileWriter createFileWriter(String name)
            throws IOException {
        return new FileWriter(createFile(name));
    }

    /**
     * Produce the documentation.
     */
    public abstract void produce()
            throws IOException;
}
