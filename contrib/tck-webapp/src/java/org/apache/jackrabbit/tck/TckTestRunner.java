/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.tck;

import junit.framework.Test;
import junit.runner.BaseTestRunner;
import javax.servlet.jsp.JspWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.HashMap;
import java.text.MessageFormat;

import org.apache.jackrabbit.test.NotExecutableException;

/**
 * The <code>TckTestRunner</code> class implements the <code>TestListener</code> interface.
 */
public class TckTestRunner extends BaseTestRunner {
    /** Test state */
    int state;

    /** The test */
    Test test;

    /** the writer */
    Writer writer;

    /*** contains all results from all tests */
    Map results;

    /** test start time */
    long startTime;

    /** time that a test took */
    long testTime;

    /** Result of a test */
    TestResult result;

    /** String containing defined log output */
    private String logString;

    /** String containing defined interaction output */
    private String interactionString;

    /** current testclass */
    private String currentTestClass;

    /** new test identifier string */
    private String newTestString;

    /** test counter */
    private int tCount = 0;

    /**
     * The constructor inits the result map and sets the writer
     *
     * @param writer
     */
    public TckTestRunner(JspWriter writer) {
        this.writer = writer;
        results = new HashMap();
        currentTestClass = "";
    }

    /**
     * This method is called everytime a test is executed.
     * The result object is "reset". the state is "reset" to its default value.
     * The startTime is set.
     *
     * @param test The <code>Test</code> which will be executed
     */
    public synchronized void startTest(Test test) {
        result = new TestResult();
        state = TestResult.SUCCESS;
        startTime = System.currentTimeMillis();
        tCount++;
    }

    /**
     * The test could not be started. This should not happen...
     *
     * @param message error message
     */
    protected void runFailed(String message) {
        String msg = "RUN FAILED:" + message;
        write(msg, false);
    }

    /**
     * This method is called everytime a test is finished. it does not matter if the
     * test was successful or not. The <code>TestResult</code> is added to the results list.
     *
     * @param test the current <code>Test</code>
     */
    public synchronized void endTest(Test test) {
        testTime = System.currentTimeMillis() - startTime;
        result.setTest(test);
        result.setTestTime(testTime);
        result.setStatus(state);
        results.put(test.toString(), result);
        if (!currentTestClass.equals(test.getClass().getName())) {
            currentTestClass = test.getClass().getName();
            write(test.toString(), true);
        } else {
            write(test.toString(), false);
        }
    }

    /**
     * This method is called when a <code>Test</code> failed.
     * The "error" code is passed:
     * <li>- an error occured while testing
     * <li>- the test failed
     * And the <code>Throwable</code> object with the information why the test failed is passed as well.
     *
     * @param status "error" code
     * @param test current <code>Test</code>
     * @param t <code>Throwable</code> of error/failure
     */
    public void testFailed(int status, Test test, Throwable t) {
        if (t instanceof NotExecutableException) {
            state = TestResult.NOT_EXECUTABLE;
        } else {
            state = status;
        }
        result.setErrorMsg(t.toString());
    }

    /**
     * Writes test logging information to output
     *
     * @param msg
     */
    private void write(String msg, boolean newTestClass) {
        if (writer != null) {
            try {
                String html;
                if (logString!= null && !"".equals(logString)) {
                    html = MessageFormat.format(logString, new String[]{msg, TckHelper.getStatus(state)});
                    writer.write(html);
                }

                String color;
                switch (state) {
                    case TestResult.SUCCESS:
                        color = "pass";
                        break;
                    case TestResult.ERROR:
                    case TestResult.FAILURE:
                        color = "failure";
                        break;
                    case TestResult.NOT_EXECUTABLE:
                        color = "error";
                        break;
                    default:
                        color = "clear";
                }

                if (interactionString!= null && !"".equals(interactionString)) {
                    html = MessageFormat.format(interactionString, new String[]{msg, color, String.valueOf(testTime)});
                    if (newTestClass) {
                        html += newTestString;
                    }
                    writer.write(html);
                }
                writer.flush();
            } catch (IOException e) {
                // ignore
            }
        } else {
            System.out.println(msg);
        }
    }

    /**
     * Returns all results
     * @return all test results
     */
    public Map getResults() {
        return results;
    }

    public void setLogString(String logString) {
        this.logString = logString;
    }

    public void setInteractionString(String interactionString) {
        this.interactionString = interactionString;
    }

    public void setNewTestString(String newTestString) {
        this.newTestString = newTestString;
    }

    public void testStarted(String testName) {
    }

    public void testEnded(String testName) {
    }

    public int getNumberOfTests() {
        return tCount;
    }

    public void resetNumberOfTests() {
        tCount = 0;
    }
}

