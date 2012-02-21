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
package org.apache.jackrabbit.spi.commons.logging;

import javax.jcr.RepositoryException;

/**
 * Common base class for all log wrappers of SPI entities.
 */
public class AbstractLogger {

    /**
     * The {@link LogWriter} used by this instance for persisting log messages.
     */
    protected final LogWriter writer;

    /**
     * Create a new instance of this log wrapper which uses <code>writer
     * </code> for persisting log messages.
     * @param writer
     */
    public AbstractLogger(LogWriter writer) {
        super();
        this.writer = writer;
    }

    /**
     * Execute a <code>thunk</code> of a method which might throw a {@link RepositoryException}. The call
     * is logged to {@link #writer} right before it is actually performed and after it returns. Any
     * exception thrown by the call is logged before it is re-thrown.
     * @param thunk  thunk of the method to execute
     * @param methodName  the name of the method
     * @param args  the arguments passed to the method
     * @return  the value returned from executing the <code>thunk</code>
     * @throws RepositoryException  if executing the <code>thunk</code> throws an Exception the
     *   exception is re-thrown.
     */
    protected Object execute(Callable thunk, String methodName, Object[] args) throws RepositoryException {
        writer.enter(methodName, args);
        Object result = null;
        try {
            result = thunk.call();
            writer.leave(methodName, args, result);
            return result;
        }
        catch (RepositoryException e) {
            writer.error(methodName, args, e);
            throw e;
        }
        catch (RuntimeException e) {
            writer.error(methodName, args, e);
            throw e;
        }
    }

    /**
     * Execute a <code>thunk</code> of a method which does not throw any checked exception. The
     * call is logged to {@link #writer} right before it is actually performed and after it returns.
     * @param thunk  thunk of the method to execute
     * @param methodName  the name of the method
     * @param args  the arguments passed to the method
     * @return  the value returned from executing the <code>thunk</code>
     */
    protected Object execute(SafeCallable thunk, String methodName, Object[] args)  {
        writer.enter(methodName, args);
        Object result;
        try {
            result = thunk.call();
            writer.leave(methodName, args, result);
            return result;
        }
        catch (RuntimeException e) {
            writer.error(methodName, args, e);
            throw e;
        }
    }

    /**
     * Type of thunk used in {@link AbstractLogger#execute(Callable, String, Object[])}
     */
    protected interface Callable {
        public Object call() throws RepositoryException;
    }

    /**
     * Type of thunk used in {@link AbstractLogger#execute(SafeCallable, String, Object[])}
     */
    protected interface SafeCallable {
        public Object call();
    }

}
