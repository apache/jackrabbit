/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.trace;

import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.Credentials;
import javax.jcr.version.Version;

import org.apache.commons.logging.Log;
import org.xml.sax.ContentHandler;

/**
 * TODO
 */
public class TraceLogger {
    
    private String klass;
    
    private TraceFingerprint fingerprint;
    
    private Log log;
    
    public TraceLogger(String klass, TraceFingerprint fingerprint, Log log) {
        this.klass = klass;
        this.fingerprint = fingerprint;
        this.log = log;
    }
    
    private void fingerprint(String signature) {
        fingerprint.addTrace(klass + "." + signature);
    }
    
    public void trace(String method) {
        fingerprint(method + "()");
        log.trace(method + "()");
    }

    public void trace(String method, boolean arg) {
        fingerprint(method + "(boolean)");
        log.trace(method + "(" + arg + ")");
    }

    public void trace(String method, String arg) {
        fingerprint(method + "(java.lang.String)");
        log.trace(method + "(" + arg + ")");
    }

    public void trace(String method, String arg1, String arg2) {
        fingerprint(method + "(java.lang.String,java.lang.String)");
        log.trace(method + "(" + arg1 + "," + arg2 + ")");
    }

    public void trace(String method, String arg1, int arg2) {
        fingerprint(method + "(java.lang.String,int)");
        log.trace(method + "(" + arg1 + "," + arg2 + ")");
    }

    public void trace(String method, String arg1, String arg2, String arg3) {
        fingerprint(method
                + "(java.lang.String,java.lang.String,java.lang.String)");
        log.trace(method + "(" + arg1 + "," + arg2 + "," + arg3 + ")");
    }

    public void trace(String method, String arg1, InputStream arg2) {
        fingerprint(method + "(java.lang.String,java.io.InputStream)");
        log.trace(method + "(" + arg1 + ",inputstream)");
    }

    public void trace(String method, String arg1, InputStream arg2, int arg3) {
        fingerprint(method + "(java.lang.String,java.io.InputStream,int)");
        log.trace(method + "(" + arg1 + ",inputstream," + arg3 + ")");
    }

    public void trace(String method,
            String arg1, String arg2, String arg3, boolean arg4) {
        fingerprint(method + "(java.lang.String,java.lang.String,"
                + "java.lang.String,boolean)");
        log.trace(method
                + "(" + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + ")");
    }

    public void trace(String method, Credentials arg) {
        fingerprint(method + "(javax.jcr.Credentials)");
        log.trace(method + "("  + arg + ")");
    }

    public void trace(String method, Credentials arg1, String arg2) {
        fingerprint(method + "(javax.jcr.Credentials,java.lang.String)");
        log.trace(method + "("  + arg1 + "," + arg2 + ")");
    }

    public void trace(String method, Version[] arg1, boolean arg2) {
        fingerprint(method + "(javax.jcr.version.Version[],boolean)");
        log.trace(method + "(versions," + arg2 + ")");
    }

    public void trace(String method, String arg1, ContentHandler arg2,
            boolean arg3, boolean arg4) {
        fingerprint(method + "(java.lang.String,org.xml.sax.ContentHandler,"
                + "boolean,boolean)");
        log.trace(method
                + "(" + arg1 + ",contenthandler," + arg3 + "," + arg4 + ")");
    }
    
    public void trace(String method, String arg1, OutputStream arg2,
            boolean arg3, boolean arg4) {
        fingerprint(method + "(java.lang.String,java.io.OutputStream,"
                + "boolean,boolean)");
        log.trace(method
                + "(" + arg1 + ",outputstream," + arg3 + "," + arg4 + ")");
    }

}
