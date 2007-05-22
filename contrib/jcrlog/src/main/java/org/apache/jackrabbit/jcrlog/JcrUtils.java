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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.jcr.Credentials;
import javax.jcr.ItemVisitor;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.observation.EventListener;

import org.apache.jackrabbit.api.JackrabbitNodeTypeManager;
import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;

/**
 * A collection of utility methods for the JCR API.
 * This class is mainly used when replaying a log file.
 *
 * @author Thomas Mueller
 *
 */
public class JcrUtils {

    static final String CLASSNAME = JcrUtils.class.getName();

    /**
     * Register a node type.
     * This is a utility method used while playing back a log file.
     */
    public static void registerNodeTypes(NodeTypeManager manager,
            Collection coll) throws InvalidNodeTypeDefException,
            RepositoryException {
        if (manager instanceof NodeTypeManagerImpl) {
            NodeTypeManagerImpl mgr = (NodeTypeManagerImpl) manager;
            mgr.getNodeTypeRegistry().registerNodeTypes(coll);
        } else if (manager instanceof LogObject) {
            LogObject proxy = (LogObject) manager;
            NodeTypeManagerImpl mgr = (NodeTypeManagerImpl)proxy.getObject();
            mgr.getNodeTypeRegistry().registerNodeTypes(coll);
            try {
                String s = StringUtils.serializeToString(coll);
                proxy.log(
                        "registerNodeTypes(StringUtils.deserializeFromString(\""
                                + s + "\"))", null);
            } catch (Throwable t) {
                proxy.log("Can't serialize collection: " + coll, t);
            }
        }
    }

    /**
     * Cast a NodeTypeManager to a JackrabbitNodeTypeManager.
     * This is a utility method used while playing back a log file.
     *
     * @param ntMgr
     * @return the casted object
     */
    public static JackrabbitNodeTypeManager cast(NodeTypeManager ntMgr) {
        return (JackrabbitNodeTypeManager)ntMgr;
    }

    /**
     * Cast a NodeTypeManager to a JackrabbitNodeTypeManager.
     * This is a utility method used while playing back a log file.
     */
    public static JackrabbitWorkspace cast(Workspace ws) {
        return (JackrabbitWorkspace)ws;
    }

    /**
     * Call a method via reflection.
     * This is a utility method used while playing back a log file.
     *
     * @param obj the object
     * @param methodName the method to call
     * @param params the parameter list
     * @return the value returned
     * @throws Exception
     */
    public static Object callViaReflection(Object obj, String methodName,
            Object[] params) throws Exception {
        Method[] methods = obj.getClass().getMethods();
        Method method = null;
        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];
            if (!Modifier.isPublic(m.getModifiers())) {
                continue;
            }
            if (m.getParameterTypes().length == params.length) {
                if (method != null) {
                    throw new Exception(
                            "More than one method with this number of parameters: "
                                    + obj.getClass().getName() + " "
                                    + methodName + " " + params.length);
                }
                method = m;
            }
        }
        return method.invoke(obj, params);
    }

    /**
     * Create a new dummy event listener.
     */
    public static EventListener createDoNothingEventListener() {
        return new DoNothing();
    }

    /**
     * Create a new dummy item visitor.
     */
    public static ItemVisitor createDoNothingItemVisitor() {
        return new DoNothing();
    }

    /**
     * Create a calendar from a string.
     * Timezones are not supported.
     */
    public static Calendar getCalendar(String s) {
        return StringUtils.parseCalendar(s);
    }

    /**
     * Create an input stream from a string.
     * This is a utility method used while playing back a log file.
     */
    public static InputStream getInputStream(String data) {
        StringTokenizer tokenizer = new StringTokenizer(data, ": ");
        Properties prop = new Properties();
        while (tokenizer.hasMoreTokens()) {
            prop.put(tokenizer.nextToken(), tokenizer.nextToken());
        }
        String d = prop.getProperty("data");
        if (d != null) {
            return new ByteArrayInputStream(StringUtils.convertStringToBytes(d));
        }
        String s = prop.getProperty("size");
        if (s != null) {
            int size = Integer.parseInt(s);
            byte[] buff = new byte[size];
            String a = prop.getProperty("adler32");
            if (a != null) {
                Arrays.fill(buff, (byte)'-');
                byte[] adler32 = a.getBytes();
                System.arraycopy(adler32, 0, buff, 0, Math.min(adler32.length, buff.length));
            }
            return new ByteArrayInputStream(buff);
        }
        return new ByteArrayInputStream(data.getBytes());
    }

    /**
     * Create an output stream from a string.
     * This is a utility method used while playing back a log file.
     */
    public static OutputStream getOutputStream() {
        return new ByteArrayOutputStream();
    }

    /**
     * Get a value from an array.
     * This is a utility method used while playing back a log file.
     */
    public static Value getValue(Value[] array, int id) {
        return array[id];
    }

    /**
     * Create a credentials object.
     * This is a utility method used while playing back a log file.
     */
    public static Credentials createSimpleCredentials(String username) {
        return new SimpleCredentials(username, "".toCharArray());
    }

    /**
     * Deserialize a Java object from a String.
     * This is a utility method used while playing back a log file.
     */
    public static Object deserializeFromString(String s) throws IOException, ClassNotFoundException {
        byte[] bytes = StringUtils.convertStringToBytes(s);
        return deserialize(bytes);
    }

    /**
     * Print something to System.out. This can be used for debugging.
     */
    public static void debug(Object o) {
        System.out.println(o == null ? "null" : o.toString());
    }

    private static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        Object obj = is.readObject();
        return obj;
    }

}
