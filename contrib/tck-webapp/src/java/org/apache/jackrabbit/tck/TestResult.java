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

/**
 * The <code>TestResult</code> class is just a helper class to store test results.
 */
public class TestResult {

    /** Test succeeded */
    public static final int SUCCESS = 0;

    /** An error occured while testing */
    public static final int ERROR = 1;

    /** The test failed */
    public static final int FAILURE = 2;

    /** The test cannot be executed */
    public static final int NOT_EXECUTABLE = 4;

    /** The test */
    private Test test;

    /** Test status */
    private int status;

    /** Error message */
    private String errorMsg;

    /** Time that consumed while testing */
    private long testTime;

    public TestResult() {
        // do nothing
    }

    public long getTestTime() {
        return testTime;
    }

    public void setTestTime(long testTime) {
        this.testTime = testTime;
    }

    public Test getTest() {
        return test;
    }

    public void setTest(Test test) {
        this.test = test;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
}
