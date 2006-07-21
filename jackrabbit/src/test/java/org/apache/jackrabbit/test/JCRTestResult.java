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
package org.apache.jackrabbit.test;

import junit.framework.TestResult;
import junit.framework.Test;
import junit.framework.AssertionFailedError;
import junit.framework.TestListener;
import junit.framework.TestCase;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Extends the standard JUnit TestResult class. This class ignores test errors
 * that originated in throwing a {@link NotExecutableException}.
 */
public class JCRTestResult extends TestResult {

    /** The original TestResult we delegate to */
    private final TestResult orig;

    /** The log writer of the test classes */
    private final LogPrintWriter log;

    /**
     * Set of Strings that identify the test methods that currently fails but
     * are recognized as known issues. Those will not be reported as errors.
     */
    private final Set knownIssues = new HashSet();

    /**
     * Creates a new JCRTestResult that delegates to <code>orig</code>.
     * @param orig the original TestResult this result wraps.
     * @param log the logger
     */
    public JCRTestResult(TestResult orig, LogPrintWriter log) {
        this.orig = orig;
        this.log = log;
        String propValue = System.getProperty("known.issues");
        if (propValue != null) {
            StringTokenizer tok = new StringTokenizer(propValue);
            while (tok.hasMoreTokens()) {
                knownIssues.add(tok.nextToken());
            }
        }
    }

    /**
     * Only add an error if <code>throwable</code> is not of type
     * {@link NotExecutableException} and the test case is not a known issue.
     * @param test the test.
     * @param throwable the exception thrown by the test.
     */
    public synchronized void addError(Test test, Throwable throwable) {
        if (throwable instanceof NotExecutableException) {
            log.println("Test case: " + test.toString() + " not executable: " + throwable.getMessage());
        } else if (isKnownIssue(test)) {
            log.println("Known issue: " + test + ": " + throwable.getMessage());
        } else {
            orig.addError(test, throwable);
        }
    }

    /**
     * Only adds a failure if <code>test</code> is not a known issue.
     * @param test the test case that failed.
     * @param assertionFailedError the assertion error.
     */
    public synchronized void addFailure(Test test,
                                        AssertionFailedError assertionFailedError) {
        if (isKnownIssue(test)) {
            log.println("Known issue: " + test + ": " + assertionFailedError.getMessage());
        } else {
            orig.addFailure(test, assertionFailedError);
        }
    }

    //-----------------------< default overwrites >-----------------------------

    public synchronized void addListener(TestListener testListener) {
        orig.addListener(testListener);
    }

    public synchronized void removeListener(TestListener testListener) {
        orig.removeListener(testListener);
    }

    public void endTest(Test test) {
        orig.endTest(test);
    }

    public synchronized int errorCount() {
        return orig.errorCount();
    }

    public synchronized Enumeration errors() {
        return orig.errors();
    }

    public synchronized int failureCount() {
        return orig.failureCount();
    }

    public synchronized Enumeration failures() {
        return orig.failures();
    }

    public synchronized int runCount() {
        return orig.runCount();
    }

    public synchronized boolean shouldStop() {
        return orig.shouldStop();
    }

    public void startTest(Test test) {
        orig.startTest(test);
    }

    public synchronized void stop() {
        orig.stop();
    }

    public synchronized boolean wasSuccessful() {
        return orig.wasSuccessful();
    }

    //------------------------------< internal >--------------------------------

    /**
     * Returns <code>true</code> if <code>test</code> is a known issue;
     * <code>false</code> otherwise.
     * @param test the test case to check.
     * @return <code>true</code> if <code>test</code> is a known issue.
     */
    private boolean isKnownIssue(Test test) {
        String testName = "-";
        if (test instanceof TestCase) {
            testName = test.getClass().getName() + "#" + ((TestCase) test).getName();
        }
        return knownIssues.contains(testName);
    }
}
