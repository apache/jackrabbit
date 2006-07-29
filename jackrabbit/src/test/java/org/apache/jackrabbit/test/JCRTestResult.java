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
    private final Set knownIssues;

    /**
     * Set of Strings that identify the test methods that are listed as known
     * issues but whose test failures should still be reported.
     */
    private final Set knownIssuesOverride;

    /**
     * Creates a new JCRTestResult that delegates to <code>orig</code>.
     * @param orig the original TestResult this result wraps.
     * @param log the logger
     */
    public JCRTestResult(TestResult orig, LogPrintWriter log) {
        this.orig = orig;
        this.log = log;
        this.knownIssues = JCRTestResult.tokenize("known.issues");
        this.knownIssuesOverride = JCRTestResult.tokenize("known.issues.override");
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
     * Takes the named system property and returns the set of string tokens
     * in the property value. Returns an empty set if the named property does
     * not exist.
     *
     * @param name name of the system property
     * @return set of string tokens
     */
    private static Set tokenize(String name) {
        Set tokens = new HashSet();
        StringTokenizer tokenizer =
            new StringTokenizer(System.getProperty(name, ""));
        while (tokenizer.hasMoreTokens()) {
            tokens.add(tokenizer.nextToken());
        }
        return tokens;
    }

    /**
     * Checks if a variation of the name of the given test case is included
     * in the given set of token. The tested variations are:
     * <ul>
     *   <li>package name</li>
     *   <li>non-qualified class name</li>
     *   <li>fully qualified class name</li>
     *   <li>non-qualified method name</li>
     *   <li>class-qualified method name</li>
     *   <li>fully-qualified method name</li>
     * </ul>
     *
     * @param tokens set of string tokens
     * @param test test case
     * @return <code>true</code> if the test case name is included,
     *         <code>false</code> otherwise
     */
    private static boolean contains(Set tokens, TestCase test) {
        String className = test.getClass().getName();
        String methodName = test.getName();

        int i = className.lastIndexOf('.');
        if (i >= 0) {
            String packageName = className.substring(0, i);
            String shortName = className.substring(i + 1);
            return tokens.contains(packageName)
                || tokens.contains(shortName)
                || tokens.contains(className)
                || tokens.contains(methodName)
                || tokens.contains(shortName + "#" + methodName)
                || tokens.contains(className + "#" + methodName);
        } else {
            return tokens.contains(className)
                || tokens.contains(methodName)
                || tokens.contains(className + "#" + methodName);
        }
    }

    /**
     * Returns <code>true</code> if <code>test</code> is a known issue
     * whose test result should be ignored; <code>false</code> otherwise.
     *
     * @param test the test case to check.
     * @return <code>true</code> if <code>test</code> result should be ignored
     */
    private boolean isKnownIssue(Test test) {
        if (test instanceof TestCase) {
            return contains(knownIssues, (TestCase) test)
                && !contains(knownIssuesOverride, (TestCase) test);
        } else {
            return false;
        }
    }

}
