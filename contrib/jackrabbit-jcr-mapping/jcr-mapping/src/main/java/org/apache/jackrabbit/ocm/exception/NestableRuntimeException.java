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
package org.apache.jackrabbit.ocm.exception;


import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Nestable runtime exception. Able to wrap a cause exception in JDK previous to 1.4
 *
 * @author Spring framework
 */
public class NestableRuntimeException extends RuntimeException {

    /** Use serialVersionUID for interoperability. */
    private final static long serialVersionUID = -1939051127461985443L;

    /** Root cause of this nested exception */
    private Throwable cause;

    /**
     * No-arg constructor used by markup exceptions.
     */
    protected NestableRuntimeException() {
    }

    /**
     * Construct a <code>NestableRuntimeException</code> with the specified detail message.
     * @param msg the detail message
     */
    public NestableRuntimeException(String msg) {
        super(msg);
    }

    /**
     * Construct a <code>NestableRuntimeException</code> with the specified detail message
     * and nested exception.
     * @param msg the detail message
     * @param ex the nested exception
     */
    public NestableRuntimeException(String msg, Throwable ex) {
        super(msg);
        this.cause = ex;
    }

    /**
     * Construct a <code>NestableRuntimeException</code> with the specified
     * nested exception.
     *
     * @param ex the nested exception
     */
    public NestableRuntimeException(Throwable ex) {
        this.cause = ex;
    }

    /**
     * Return the nested cause, or <code>null</code> if none.
     */
    public Throwable getCause() {
        // Even if you cannot set the cause of this exception other than through
        // the constructor, we check for the cause being "this" here, as the cause
        // could still be set to "this" via reflection: for example, by a remoting
        // deserializer like Hessian's.
        return ((this.cause == this) ? null : this.cause);
    }

    /**
     * Return the detail message, including the message from the nested exception
     * if there is one.
     */
    public String getMessage() {
        if (getCause() == null) {
            return super.getMessage();
        } else {
            return super.getMessage() 
                + "; nested exception is " 
                + getCause().getClass().getName()
                + ": " + getCause().getMessage();
        }
    }

    /**
     * Print the composite message and the embedded stack trace to the specified stream.
     * @param ps the print stream
     */
    public void printStackTrace(PrintStream ps) {
        if (getCause() == null) {
            super.printStackTrace(ps);
        } else {
            ps.println(this);
            getCause().printStackTrace(ps);
        }
    }

    /**
     * Print the composite message and the embedded stack trace to the specified writer.
     * @param pw the print writer
     */
    public void printStackTrace(PrintWriter pw) {
        if (getCause() == null) {
            super.printStackTrace(pw);
        } else {
            pw.println(this);
            getCause().printStackTrace(pw);
        }
    }
}
