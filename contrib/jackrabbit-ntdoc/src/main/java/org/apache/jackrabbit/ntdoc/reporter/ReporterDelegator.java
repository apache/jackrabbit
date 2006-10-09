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

/**
 * This class implements the reporter delegator.
 */
public class ReporterDelegator
        implements Reporter {
    /**
     * Reporter.
     */
    private Reporter reporter;

    /**
     * Return the reporter.
     */
    public Reporter getReporter() {
        return this.reporter;
    }

    /**
     * Set the reporter.
     */
    public void setReporter(Reporter reporter) {
        this.reporter = reporter;
    }

    /**
     * Report info.
     */
    public void info(String msg) {
        if (this.reporter != null) {
            this.reporter.info(msg);
        }
    }

    /**
     * Report warning.
     */
    public void warning(String msg) {
        if (this.reporter != null) {
            this.reporter.warning(msg);
        }
    }

    /**
     * Report error.
     */
    public void error(String msg) {
        if (this.reporter != null) {
            this.reporter.error(msg);
        }
    }
}
