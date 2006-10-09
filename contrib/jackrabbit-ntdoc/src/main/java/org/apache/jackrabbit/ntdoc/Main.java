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

import java.util.*;
import org.apache.jackrabbit.ntdoc.reporter.*;

/**
 * This is the main entry point for ntdoc.
 */
public final class Main {
    /**
     * Print the usage.
     */
    private static void printUsage() {
        System.out.println("ntdoc -d <output> [-t <title>] <file(s)>");
    }

    /**
     * Set the parameters and return the producer if succesful.
     */
    private static NTDoc prepare(String[] args) {
        Map switches = new HashMap();
        List files = new LinkedList();

        String sw = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-")) {
                sw = args[i].substring(1);
            } else if (sw != null) {
                switches.put(sw, args[i]);
                sw = null;
            } else {
                files.add(args[i]);
            }
        }

        String[] tmp = new String[files.size()];
        return prepare(switches, (String[]) files.toArray(tmp));
    }

    /**
     * Set the parameters. Return true if it is valid, false otherwise.
     */
    private static NTDoc prepare(Map switches, String[] files) {
        String dir = (String) switches.get("d");
        String title = (String) switches.get("t");

        if ((dir == null) || (files.length == 0)) {
            return null;
        }

        NTDoc doc = new NTDoc();
        doc.setTitle(title);
        doc.setOutputDir(dir);
        doc.setInputFiles(files);
        doc.setReporter(new SimpleReporter(System.out));
        return doc;
    }

    /**
     * Execute ntdoc.
     */
    public static void main(String[] args) {
        NTDoc doc = prepare(args);
        if (doc != null) {
            doc.produce();
        } else {
            printUsage();
        }
    }
}
