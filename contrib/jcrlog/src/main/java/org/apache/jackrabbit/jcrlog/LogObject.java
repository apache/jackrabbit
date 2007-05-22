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
package org.apache.jackrabbit.jcrlog;

import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.jcr.Item;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RangeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.lock.Lock;
import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.apache.jackrabbit.api.JackrabbitNodeTypeManager;
import org.apache.jackrabbit.api.JackrabbitWorkspace;

/**
 * Logging is implemented using java.lang.reflect.Proxy. For each JCR 'object'
 * there is a LogObject. The main 'invocation handler' is always called when a
 * public method on the JCR object is called.
 *
 * @author Thomas Mueller
 */

public class LogObject implements InvocationHandler {

    private final static String CLASS_NAME = LogObject.class.getName();

    /**
     * The package name of this class.
     * It is not hardcoded because this would break refactoring.
     */
    public final static String MY_PACKAGE_DOT = CLASS_NAME.substring(0, CLASS_NAME.lastIndexOf('.') + 1);

    private volatile boolean processing;

    private final static long SLOW_METHOD_MS = 100;

    private long start;

    private String name;

    private static int nextArrayId;

    private static final ArrayList INTERFACES = new ArrayList();

    private static final HashSet PREFIXES = new HashSet();

    private static final HashMap INTERFACE_MAP = new HashMap();

    static final InterfaceDef INTERFACE_DEF_REPOSITORY, INTERFACE_DEF_SESSION;

    static {
        addInterface("e", Event.class);
        addInterface("ei", EventIterator.class);
        addInterface("el", EventListener.class);
        addInterface("eli", EventListenerIterator.class);
        addInterface("im", Item.class);
        addInterface("imd", ItemDefinition.class);
        addInterface("lc", Lock.class);
        addInterface("nsr", NamespaceRegistry.class);
        addInterface("n", Node.class);
        addInterface("nd", NodeDefinition.class);
        addInterface("ni", NodeIterator.class);
        addInterface("nt", NodeType.class);
        addInterface("nti", NodeTypeIterator.class);
        addInterface("jntm", JackrabbitNodeTypeManager.class);
        addInterface("ntm", NodeTypeManager.class);
        addInterface("om", ObservationManager.class);
        addInterface("p", Property.class);
        addInterface("pd", PropertyDefinition.class);
        addInterface("pi", PropertyIterator.class);
        addInterface("q", Query.class);
        addInterface("qm", QueryManager.class);
        addInterface("qr", QueryResult.class);
        addInterface("rgi", RangeIterator.class);
        INTERFACE_DEF_REPOSITORY = addInterface("rp", Repository.class);
        addInterface("rw", Row.class);
        addInterface("ri", RowIterator.class);
        INTERFACE_DEF_SESSION = addInterface("s", Session.class);
        addInterface("v", Value.class);
        addInterface("vf", ValueFactory.class);
        addInterface("vs", Version.class);
        addInterface("vsh", VersionHistory.class);
        addInterface("vsi", VersionIterator.class);
        addInterface("jws", JackrabbitWorkspace.class);
        addInterface("ws", Workspace.class);
    }

    static InterfaceDef addInterface(String prefix, Class clazz) {
        if (PREFIXES.contains(prefix)) {
            throw new Error("Internal error: duplicate interface prefix "
                    + prefix);
        }
        PREFIXES.add(prefix);
        InterfaceDef idef = new InterfaceDef();
        idef.prefix = prefix;
        String name = clazz.getName();
        name = name.substring(name.lastIndexOf('.') + 1);
        idef.className = name;
        idef.interfaceClass = clazz;
        idef.type = INTERFACES.size();
        INTERFACES.add(idef);
        INTERFACE_MAP.put(clazz, idef);
        return idef;
    }

    private Log log;

    private int assignType;

    private Object obj;

    Object getObject() {
        return obj;
    }

    String getObjectName() {
        return name;
    }

    protected RepositoryException logAndConvert(Throwable t) {
        logReturn();
        if (t instanceof RepositoryException) {
            RepositoryException r = (RepositoryException) t;
            logException("RepositoryException", r);
            return r;
        } else if (t instanceof RuntimeException) {
            RuntimeException r = (RuntimeException) t;
            logException("RuntimeException", r);
            throw r;
        } else if (t instanceof Error) {
            Error r = (Error) t;
            logException("Error", r);
            throw r;
        } else {
            logException("Internal", t);
            throw new Error("Internal exception: " + t.toString());
        }
    }

    protected RuntimeException logRuntimeException(Throwable t) {
        logReturn();
        if (t instanceof RuntimeException) {
            RuntimeException r = (RuntimeException) t;
            logException("RuntimeException", r);
            return r;
        } else if (t instanceof Error) {
            Error r = (Error) t;
            logException("Error", r);
            throw r;
        } else {
            logException("LoggerException", t);
            throw new Error("Internal exception: " + t.toString());
        }
    }

    protected int logStart(int assignType, boolean array, String method,
            String parameters) {
        this.start = System.currentTimeMillis();
        this.assignType = assignType;
        int nextId = -1;
        String s = name + "." + method + "(" + parameters + ");";
        if (array && assignType >= 0) {
            nextId = nextArrayId++;
            InterfaceDef idef = getInterfaceDef(assignType);
            s = idef.className + "[] array" + nextId + " = " + s;
        } else if (assignType >= 0) {
            nextId = getNextId(assignType);
            s = getAssign(assignType, nextId, s);
        }
        logStartCall(s);
        return nextId;
    }

    private String getCaller() {
        Throwable t = new Throwable();
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            String className = e.getClassName();
            if (className.startsWith("$Proxy")) {
                continue;
            }
            if (className.startsWith(MY_PACKAGE_DOT)) {
                if (className.substring(MY_PACKAGE_DOT.length()).indexOf('.') < 0) {
                    // don't ignore sub-packages
                    continue;
                }
            }
            String s = className + "." + e.getMethodName() + "("
                    + e.getFileName() + ":" + e.getLineNumber() + ")";
            return s;
        }
        return "unknown caller";
    }

    protected void logStartCall(String s) {
        if (log.getLogCaller()) {
            log.write("//> " + getCaller(), null);
        }
        logJava(s);
        if (processing) {
            log.write("//Concurrent Access", null);
        }
    }

    private void logJava(String s) {
        log.write("/**/" + s, null);
    }

    protected String getAssign(int type, int id, String call) {
        InterfaceDef idef = getInterfaceDef(type);
        return idef.className + " " + idef.prefix + id + " = " + call;
    }

    private static InterfaceDef getInterfaceDef(int type) {
        return (InterfaceDef) INTERFACES.get(type);
    }

    protected void setLog(Log log, int type, int id) {
        this.log = log;
        InterfaceDef idef = getInterfaceDef(type);
        this.name = idef.prefix + id;
    }

    protected int getNextId(int type) {
        InterfaceDef idef = getInterfaceDef(type);
        return idef.nextId++;
    }

    protected void logReturn() {
        processing = false;
        long time = System.currentTimeMillis() - start;
        if (time > SLOW_METHOD_MS) {
            log.write("//time: " + time + " ms", null);
        }
    }

    private void logException(String title, Throwable e) {
        log.write("//" + title + ": " + e.toString(), e);
    }

    protected void logReturn(Class clazz, Object result) {
        logReturn();
        if (clazz == String.class && StringUtils.isUUID((String)result)) {
            log.write("//return " + StringUtils.quote(clazz, result) + "; // UUID", null);
        } else if (log.getLogReturn()) {
            log.write("//return " + StringUtils.quote(clazz, result) + ";", null);
        }
    }

    public String toString() {
        return name;
    }

    protected Object wrap(Object obj, boolean array, Class clazz,
            int assignType, int id) {
        if (array) {
            Object[] list = (Object[]) obj;
            for (int i = 0; i < list.length; i++) {
                if (list[i] != null) {
                    int nextId = getNextId(assignType);
                    InterfaceDef idef = getInterfaceDef(assignType);
                    logJava(getAssign(assignType, nextId,
                            JcrUtils.CLASSNAME + ".get"
                                    + idef.className + "(array" + id + ", " + i
                                    + ");"));
                    list[i] = wrapObject(list[i], clazz, nextId);
                }
            }
            return list;
        } else {
            return wrapObject(obj, clazz, id);
        }
    }

    private Object wrapObject(Object obj, Class clazz, int id) {
        LogObject handler = new LogObject();
        handler.setLog(log, assignType, id);
        handler.obj = obj;
        InterfaceDef idef = getInterfaceDef(assignType);

        Class proxyClass = idef.proxyClass;
        if (proxyClass == null) {
            proxyClass = Proxy.getProxyClass(clazz.getClassLoader(),
                    new Class[] { clazz });
            idef.proxyClass = proxyClass;
        }
        try {
            Object o2 = proxyClass.getConstructor(
                    new Class[] { InvocationHandler.class }).newInstance(
                    new Object[] { handler });
            return o2;
        } catch (Exception e) {
            logException("InvocationTargetException", e);
            throw new Error("Internal exception: " + e.toString());
        }
    }

    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        Object result;
        try {
            if (method.getName().equals("toString")) {
                return result = method.invoke(obj, args);
            }
            Class returnClass = method.getReturnType();
            int returnType = -1;
            InterfaceDef idef = (InterfaceDef) INTERFACE_MAP.get(returnClass);
            boolean array = false;
            if (idef != null) {
                returnType = idef.type;
            } else if (returnClass.isArray()) {
                array = true;
                returnClass = returnClass.getComponentType();
                idef = LogObject.getInterface(returnClass);
                if (idef != null) {
                    returnType = idef.type;
                }
            }
            wrapStreams(method.getParameterTypes(), args);
            int nextId = logStart(returnType, array, method.getName(),
                    StringUtils.quoteArgs(method.getParameterTypes(), args));
            result = method.invoke(obj, args);
            if (log.getCastToRealApi() && result != null && idef != null) {
                InterfaceDef idef2 = LogObject.getInterface(result.getClass());
                if (idef2 != null && idef2 != idef) {
                    // if the return value was of a more specific type
                    // (for example JackrabbitNodeTypeManager instead of just a NodeTypeManager), cast it
                    logJava(getAssign(idef2.type, nextId,
                            JcrUtils.CLASSNAME + ".cast("
                                    + idef.prefix + nextId + ");"));
                    returnType = idef2.type;
                    returnClass = idef2.interfaceClass;
                }
            }
            if (returnType >= 0) {
                result = wrap(result, array, returnClass, returnType, nextId);
            }
            logReturn(returnClass, result);
            return result;
        } catch (IllegalArgumentException e) {
            logException("IllegalArgumentException", e);
            throw new Error("Internal exception: " + e.toString());
        } catch (IllegalAccessException e) {
            logException("IllegalAccessException", e);
            throw new Error("Internal exception: " + e.toString());
        } catch (InvocationTargetException e) {
            Throwable t = e.getTargetException();
            throw t;
        }
    }

    private void wrapStreams(Class[] parameterTypes, Object[] args) {
        for (int i = 0; args != null && i < args.length; i++) {
            Object o = args[i];
            if (o == null || !(o instanceof InputStream)) {
                continue;
            }
            InputStream in = (InputStream) o;
            in = LogInputStream.wrapStream(in, log.getLogStream());
            args[i] = in;
        }
    }

    static InterfaceDef getInterface(Class clazz) {
        InterfaceDef idef = (InterfaceDef) INTERFACE_MAP.get(clazz);
        if (idef != null) {
            return idef;
        }
        Class[] interfaces = clazz.getInterfaces();
        InterfaceDef best = null;
        for (int i = 0; i < interfaces.length; i++) {
            idef = (InterfaceDef) INTERFACE_MAP.get(interfaces[i]);
            if (idef != null) {
                if (best == null || idef.className.length() > best.className.length()) {
                    best = idef;
                }
            }
        }
        return best;
    }

    void log(String string, Throwable t) {
        // TODO currently used for node type registration only
        log.write("// ### " + string, t);
    }

    /**
     * Internally used information about a wrapped interface.
     */
    static class InterfaceDef {

        String className;
        Class interfaceClass;
        String prefix;
        int nextId;
        Class proxyClass;
        int type;
    }

}
