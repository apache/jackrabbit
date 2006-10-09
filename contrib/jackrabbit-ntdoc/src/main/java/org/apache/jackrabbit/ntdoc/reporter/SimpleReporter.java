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
package org.apache.jackrabbit.ntdoc.reporter;

import java.io.*;

/**
 * This class implements the simple reporter.
 */
public final class SimpleReporter
        implements Reporter {
    /**
     * Print stream.
     */
    private final PrintStream out;

    /**
     * Construct the reporter.
     */
    public SimpleReporter(PrintStream out) {
        this.out = out;
    }

    /**
     * Write line.
     */
    private void report(String level, String msg) {
        if (this.out != null) {
            this.out.println("[" + level + "] " + msg);
        }
    }

    /**
     * Report info.
     */
    public void info(String msg) {
        report("INFO", msg);
    }

    /**
     * Report warning.
     */
    public void warning(String msg) {
        report("WARNING", msg);
    }

    /**
     * Report error.
     */
    public void error(String msg) {
        report("ERROR", msg);
    }
}
