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
package org.apache.jackrabbit.ntdoc;

import java.io.*;

import org.apache.jackrabbit.ntdoc.model.*;
import org.apache.jackrabbit.ntdoc.parser.*;
import org.apache.jackrabbit.ntdoc.producer.*;
import org.apache.jackrabbit.ntdoc.reporter.*;

/**
 * This is the main entry point for ntdoc.
 */
public final class NTDoc
        extends ReporterDelegator {
    /**
     * Default title.
     */
    private final static String DEFAULT_TITLE =
            "Generated Documentation";

    /**
     * Title.
     */
    private String title;

    /**
     * Output directory.
     */
    private File outputDir;

    /**
     * Input files.
     */
    private File[] inputFiles;

    /**
     * Return the title.
     */
    public String getTitle() {
        return this.title != null ? this.title : DEFAULT_TITLE;
    }

    /**
     * Set the title.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Return the output directory.
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
     * Set the output directory.
     */
    public void setOutputDir(String outputDir) {
        setOutputDir(new File(outputDir));
    }

    /**
     * Return the input files.
     */
    public File[] getInputFiles() {
        return this.inputFiles;
    }

    /**
     * Set the input files.
     */
    public void setInputFiles(File[] inputFiles) {
        this.inputFiles = inputFiles;
    }

    /**
     * Set the input files.
     */
    public void setInputFiles(String[] inputFiles) {
        File[] tmp = new File[inputFiles.length];
        for (int i = 0; i < inputFiles.length; i++) {
            tmp[i] = new File(inputFiles[i]);
        }

        setInputFiles(tmp);
    }

    /**
     * Parse node types.
     */
    private NodeTypeSet parseNodeTypes()
            throws IOException {
        NodeTypeSet nodeTypes = new NodeTypeSet();
        for (int i = 0; i < this.inputFiles.length; i++) {
            nodeTypes.addNodeTypes(parseNodeTypes(this.inputFiles[i]));
        }

        return nodeTypes;
    }

    /**
     * Parse node types.
     */
    private NodeTypeSet parseNodeTypes(File file)
            throws IOException {
        NodeTypeParser parser = NodeTypeParserFactory.newParser(file);
        parser.parse();
        return parser.getNodeTypes();
    }

    /**
     * Produce the documentation.
     */
    public boolean produce() {
        try {
            doProduce();
            return true;
        } catch (IOException e) {
            error(e.getMessage());
            return false;
        }
    }

    /**
     * Produce the documentation.
     */
    private void doProduce()
            throws IOException {
        NodeTypeSet nodeTypes = parseNodeTypes();
        info("Parsed " + nodeTypes.getSize() + " node type definitions");
        doProduce(nodeTypes);
    }

    /**
     * Produce the documentation.
     */
    private void doProduce(NodeTypeSet nodeTypes)
            throws IOException {
        Producer producer = new StandardProducer();
        producer.setReporter(getReporter());
        producer.setTitle(getTitle());
        producer.setOutputDir(getOutputDir());
        producer.setNodeTypes(nodeTypes);
        info("Producing node type documentation to '" + this.outputDir.getPath() + "'");
        producer.produce();
    }
}

