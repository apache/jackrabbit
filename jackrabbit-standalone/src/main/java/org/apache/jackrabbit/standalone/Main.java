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
package org.apache.jackrabbit.standalone;

import java.io.File;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.webapp.WebAppContext;

/**
 *
 */
public class Main {

    /**
     * @param args
     */
    public static void main(String[] argv) throws Exception {
        File jackrabbit = new File("jackrabbit");
        jackrabbit.mkdirs();

        WebAppContext webapp = new WebAppContext();
        webapp.setContextPath("/");
        webapp.setWar("target/jackrabbit-standalone-SNAPSHOT-jar-with-dependencies.jar");
        webapp.setExtractWAR(false);
        webapp.setTempDirectory(new File(jackrabbit, "jetty"));

        Server server = new Server(8080);
        server.setHandler(webapp);
        server.start();
        server.join();
    }

}
