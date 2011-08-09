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
package org.apache.jackrabbit.webdav.simple;

import junit.framework.TestCase;
import org.apache.jackrabbit.server.io.DefaultHandler;
import org.apache.jackrabbit.server.io.IOHandler;
import org.apache.jackrabbit.server.io.IOManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * <code>ResourceConfigTest</code>...
 */
public class ResourceConfigTest extends TestCase {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(ResourceConfigTest.class);

    public void testIOManagerConfig() throws Exception {
        InputStream in = new ByteArrayInputStream(CONFIG_1.getBytes("UTF-8"));

        ResourceConfig config = new ResourceConfig(null);
        config.parse(in);

        IOManager ioMgr = config.getIOManager();
        assertNotNull(ioMgr);
        assertEquals("org.apache.jackrabbit.server.io.IOManagerImpl", ioMgr.getClass().getName());

        IOHandler[] handlers = ioMgr.getIOHandlers();
        assertNotNull(handlers);
        assertEquals(1, handlers.length);
        assertEquals("org.apache.jackrabbit.server.io.DefaultHandler", handlers[0].getName());
    }

    public void testIOManagerConfigWithParam() throws Exception {
        InputStream in = new ByteArrayInputStream(CONFIG_2.getBytes("UTF-8"));

        ResourceConfig config = new ResourceConfig(null);
        config.parse(in);

        IOManager ioMgr = config.getIOManager();
        assertNotNull(ioMgr);
        assertEquals("org.apache.jackrabbit.server.io.IOManagerImpl", ioMgr.getClass().getName());

        IOHandler[] handlers = ioMgr.getIOHandlers();
        assertNotNull(handlers);
        assertEquals(1, handlers.length);
        assertEquals("org.apache.jackrabbit.server.io.DefaultHandler", handlers[0].getName());
        DefaultHandler dh = (DefaultHandler) handlers[0];
        assertEquals("nt:unstructured", dh.getCollectionNodeType());
        assertEquals("nt:unstructured", dh.getNodeType());
        assertEquals("nt:resource", dh.getContentNodeType());
    }


    private static final String CONFIG_1 = "<config>\n" +
            "    <!--\n" +
            "     Defines the IOManager implementation that is responsible for passing\n" +
            "     import/export request to the individual IO-handlers.\n" +
            "    -->\n" +
            "    <iomanager>\n" +
            "        <!-- class element defines the manager to be used. The specified class\n" +
            "             must implement the IOManager interface.\n" +
            "             Note, that the handlers are being added and called in the order\n" +
            "             they appear in the configuration.\n" +
            "        -->\n" +
            "        <class name=\"org.apache.jackrabbit.server.io.IOManagerImpl\" />\n" +
            "        <iohandler>\n" +
            "            <class name=\"org.apache.jackrabbit.server.io.DefaultHandler\" />\n" +
            "        </iohandler>\n" +
            "    </iomanager>" +
            "</config>";

    private static final String CONFIG_2 = "<config>\n" +
            "    <!--\n" +
            "     Defines the IOManager implementation that is responsible for passing\n" +
            "     import/export request to the individual IO-handlers.\n" +
            "    -->\n" +
            "    <iomanager>\n" +
            "        <!-- class element defines the manager to be used. The specified class\n" +
            "             must implement the IOManager interface.\n" +
            "             Note, that the handlers are being added and called in the order\n" +
            "             they appear in the configuration.\n" +
            "        -->\n" +
            "        <class name=\"org.apache.jackrabbit.server.io.IOManagerImpl\" />\n" +
            "        <iohandler>\n" +
            "            <class name=\"org.apache.jackrabbit.server.io.DefaultHandler\" />\n" +
            "            <param name=\"collectionNodetype\" value=\"nt:unstructured\"/>\n" +
            "            <param name=\"defaultNodetype\" value=\"nt:unstructured\"/>\n" +
            "            <param name=\"contentNodetype\" value=\"nt:resource\"/>\n" +         
            "        </iohandler>\n" +
            "    </iomanager>" +
            "</config>";
}