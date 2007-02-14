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
package org.apache.jackrabbit.commons;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;

import junit.framework.TestCase;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

/**
 * Simple {@link TestCase} base class for mock-testing the abstract
 * JCR implementation classes in this package.
 */
public class MockCase extends TestCase implements MethodInterceptor {

    /**
     * Mock test state. Set to <code>true</code> when the mock methods
     * are being recorded, and to <code>false</code> when the methods are
     * being played back.
     */
    private boolean recording;

    /**
     * The recorded mock call sequence. List of {@link MethodCall} objects.
     */
    private LinkedList calls;

    /**
     * The abstract base class being tested.
     */
    private Class base;

    /**
     * Creates a mocked proxy object for the given abstract base class and
     * starts recording calls to the object.
     *
     * @param base the abstract base class being tested
     * @return mocked proxy object
     */
    protected Object record(Class base) {
        this.recording = true;
        this.calls = new LinkedList();
        this.base = base;
        return Enhancer.create(base, base.getInterfaces(), this);
    }

    /**
     * Switches the test state to replaying and verifying the recorded
     * call sequence.
     */
    protected void replay() {
        recording = false;
    }

    /**
     * Verifies that all recorded method calls were executed during
     * the verification phase.
     */
    protected void verify() {
        assertTrue(calls.isEmpty());
    }

    //---------------------------------------------------< MethodInterceptor >

    /**
     * Intercepts a method call to the mocked proxy object. Passes the
     * call through if the abstract base class being tested implements the
     * method, and records or verifies the method call otherwise.
     *
     * @param object the object on which the method is being called
     * @param method the method being called
     * @param args method arguments
     * @param proxy proxy for re-invoking the called method
     * @return method return value
     * @throws Throwable if an error occurs
     */
    public Object intercept(
            Object object, Method method, Object[] args, MethodProxy proxy)
            throws Throwable {
        try {
            base.getDeclaredMethod(method.getName(), method.getParameterTypes());
            return proxy.invokeSuper(object, args);
        } catch (NoSuchMethodException e) {
            if (recording) {
                calls.addLast(new MethodCall(method, args));
            } else {
                assertFalse(calls.isEmpty());
                MethodCall call = (MethodCall) calls.removeFirst();
                call.assertCall(method, args);
            }
            return null;
        }
    }

    //----------------------------------------------------------< MethodCall >

    /**
     * Record of a method call.
     */
    private static class MethodCall {

        /**
         * The method that was called.
         */
        private final Method method;

        /**
         * The arguments that were passed to the method.
         */
        private final Object[] args;

        /**
         * Creates a new method call record.
         *
         * @param method the method that was called
         * @param args the arguments that were passed to the method
         */
        public MethodCall(Method method, Object[] args) {
            this.method = method;
            this.args = args;
        }

        /**
         * Verifies that the given method is the same as was recorded.
         *
         * @param method the method that was called
         * @param args the arguments that were passed to the method
         */
        public void assertCall(Method method, Object[] args) {
            assertEquals(this.method, method);
            assertTrue(Arrays.equals(this.args, args));
        }

    }

}
