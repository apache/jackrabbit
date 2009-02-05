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
package org.apache.jackrabbit.server.remoting.davex;

import junit.framework.TestCase;

import java.util.Properties;

/**
 * <code>BatchReadConfigTest</code>...
 */
public class BatchReadConfigTest extends TestCase {

    public void testDefaultDepth() {
        BatchReadConfig cnf = new BatchReadConfig();

        assertEquals(BatchReadConfig.DEPTH_DEFAULT, cnf.getDefaultDepth());
        assertEquals(cnf.getDefaultDepth(), cnf.getDepth("nt:base"));

        cnf.setDefaultDepth(5);
        assertEquals(5, cnf.getDefaultDepth());
        assertEquals(cnf.getDefaultDepth(), cnf.getDepth("nt:base"));

        cnf.setDefaultDepth(BatchReadConfig.DEPTH_INFINITE);
        assertEquals(BatchReadConfig.DEPTH_INFINITE, cnf.getDefaultDepth());
        assertEquals(cnf.getDefaultDepth(), cnf.getDepth("nt:base"));

        try {
            cnf.setDefaultDepth(-12);
            fail("Invalid depth");
        } catch (IllegalArgumentException e) {
            //ok
        }
    }

    public void testDepth() {
        BatchReadConfig cnf = new BatchReadConfig();

        cnf.setDepth("nt:file", 15);
        assertEquals(15, cnf.getDepth("nt:file"));
        assertEquals(cnf.getDefaultDepth(), cnf.getDepth("nt:base"));

        cnf.setDepth("nt:file", BatchReadConfig.DEPTH_INFINITE);
        assertEquals(BatchReadConfig.DEPTH_INFINITE, cnf.getDepth("nt:file"));
        assertEquals(cnf.getDefaultDepth(), cnf.getDepth("nt:base"));

        try {
            cnf.setDepth("nt:file",-12);
            fail("Invalid depth");
        } catch (IllegalArgumentException e) {
            //ok
        }
    }

    public void testAdd() {
        Properties props = new Properties();
        props.setProperty("nt:file", "15");
        props.setProperty("default", "-1");

        BatchReadConfig cnf = new BatchReadConfig();
        cnf.add(props);

        assertEquals(15, cnf.getDepth("nt:file"));
        assertEquals(BatchReadConfig.DEPTH_INFINITE, cnf.getDefaultDepth());
        assertEquals(cnf.getDefaultDepth(), cnf.getDepth("nt:base"));

        cnf.setDefaultDepth(BatchReadConfig.DEPTH_DEFAULT);
        assertEquals(15, cnf.getDepth("nt:file"));
        assertEquals(BatchReadConfig.DEPTH_DEFAULT, cnf.getDefaultDepth());
        assertEquals(cnf.getDefaultDepth(), cnf.getDepth("nt:base"));
    }
}