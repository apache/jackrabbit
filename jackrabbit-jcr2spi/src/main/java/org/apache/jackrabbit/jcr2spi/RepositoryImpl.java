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
package org.apache.jackrabbit.jcr2spi;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.HashMap;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.NamespaceException;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;

import org.apache.jackrabbit.commons.AbstractRepository;
import org.apache.jackrabbit.jcr2spi.config.RepositoryConfig;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.XASessionInfo;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.spi.commons.value.ValueFormat;
import org.apache.jackrabbit.value.ValueFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>RepositoryImpl</code>...
 */
public class RepositoryImpl extends AbstractRepository implements Referenceable {

    private static Logger log = LoggerFactory.getLogger(RepositoryImpl.class);

    // configuration of the repository
    private final RepositoryConfig config;
    private final Map<String, Value[]> descriptors;
    private Reference reference = null;

    private RepositoryImpl(RepositoryConfig config) throws RepositoryException {
        this.config = config;

        // dummy value factory and dummy resolver as descriptors are not
        // expected to contain Name or Path values.
        ValueFactory vf = ValueFactoryImpl.getInstance(); 
        NamePathResolver resolver = new DefaultNamePathResolver(new NamespaceResolver() {
            public String getURI(String prefix) throws NamespaceException {
                return prefix;
            }
            public String getPrefix(String uri) throws NamespaceException {
                return uri;
            }
        });

        Map<String, QValue[]> descr = config.getRepositoryService().getRepositoryDescriptors();       
        descriptors = new HashMap<String, Value[]>(descr.size());
        for (String key : descr.keySet()) {
            QValue[] qvs = descr.get(key);
            Value[] vs = new Value[qvs.length];
            for (int i = 0; i < qvs.length; i++) {
                vs[i] = ValueFormat.getJCRValue(qvs[i], resolver, vf);
            }
            descriptors.put(key, vs);
        }
    }

    public static Repository create(RepositoryConfig config) throws RepositoryException {
        return new RepositoryImpl(config);
    }

    //---------------------------------------------------------< Repository >---
    /**
     * @see Repository#getDescriptorKeys()
     */
    public String[] getDescriptorKeys() {
        return descriptors.keySet().toArray(new String[descriptors.keySet().size()]);
    }

    /**
     * @see Repository#getDescriptor(String)
     */
    public String getDescriptor(String key) {
        Value v = getDescriptorValue(key);
        try {
            return (v == null) ? null : v.getString();
        } catch (RepositoryException e) {
            log.error("corrupt descriptor value: " + key, e);
            return null;
        }
    }

    /**
     * @see Repository#getDescriptorValue(String)
     */
    public Value getDescriptorValue(String key) {
        Value[] vs = getDescriptorValues(key);
        return (vs == null || vs.length != 1) ? null : vs[0];
    }

    /**
     * @see Repository#getDescriptorValues(String)
     */
    public Value[] getDescriptorValues(String key) {
        if (!descriptors.containsKey(key)) {
            return null;
        } else {
            return descriptors.get(key);

        }
    }

    /**
     * @see Repository#isSingleValueDescriptor(String)
     */
    public boolean isSingleValueDescriptor(String key) {
        Value[] vs = descriptors.get(key);
        return (vs != null && vs.length == 1);
    }

    /**
     * @see Repository#login(javax.jcr.Credentials, String)
     */
    public Session login(Credentials credentials, String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException {
        SessionInfo info = config.getRepositoryService().obtain(credentials, workspaceName);
        try {
            if (info instanceof XASessionInfo) {
                return new XASessionImpl((XASessionInfo) info, this, config);
            } else {
                return new SessionImpl(info, this, config);
            }
        } catch (RepositoryException ex) {
            config.getRepositoryService().dispose(info);
            throw ex;
        }
    }

    //------------------------------------------------------< Referenceable >---
    /**
     * @see Referenceable#getReference()
     */
    public Reference getReference() throws NamingException {
        if (config instanceof Referenceable) {
            Referenceable confref = (Referenceable)config;
            if (reference == null) {
                reference = new Reference(RepositoryImpl.class.getName(), RepositoryImpl.Factory.class.getName(), null);
                // carry over all addresses from referenceable config
                for (Enumeration<RefAddr> en = confref.getReference().getAll(); en.hasMoreElements(); ) {
                    reference.add(en.nextElement());
                }

                // also add the information required by factory class
                reference.add(new StringRefAddr(Factory.RCF, confref.getReference().getFactoryClassName()));
                reference.add(new StringRefAddr(Factory.RCC, config.getClass().getName()));
            }

            return reference;
        }
        else {
            throw new javax.naming.OperationNotSupportedException("Contained RepositoryConfig needs to implement javax.naming.Referenceable");
        }
    }

    /**
     * Implementation of {@link ObjectFactory} for repository instances.
     * <p>
     * Works by creating a {@link Reference} to a {@link RepositoryConfig}
     * instance based on the information obtained from the {@link RepositoryImpl}'s
     * {@link Reference}.
     * <p>
     * Address Types:
     * <dl>
     *  <dt>{@link #RCF}
     *  <dd>Class name for {@link ObjectFactory} creating instances of {@link RepositoryConfig}</dd>
     *  <dt>{@link #RCC}
     *  <dd>Class name for {@link RepositoryConfig} instances</dd>
     * </dl>
     * <p>
     * All other types are copied over verbatim to the new {@link Reference}.
     * <p>
     * A sample JNDI configuration inside a servlet container's <code>server.xml</code>:
     * <pre>
     *   &lt;Resource
     *         name="jcr/repositoryname"
     *         auth="Container"
     *         type="org.apache.jackrabbit.jcr2spi.RepositoryImpl"
     *         factory="org.apache.jackrabbit.jcr2spi.RepositoryImpl$Factory"
     *         org.apache.jackrabbit.jcr2spi.RepositoryImpl.factory="<em>class name of {@link ObjectFactory} for {@link RepositoryConfig} instances</em>"
     *         org.apache.jackrabbit.jcr2spi.RepositoryImpl.class="<em>class name of {@link RepositoryConfig} implementation class</em>"
     *         <em>...additional properties passed to the {@link ObjectFactory}...</em>
     *   /&gt;
     * </pre>
     */
    public static class Factory implements ObjectFactory {

        public static final String RCF = RepositoryImpl.class.getName() + ".factory";
        public static final String RCC = RepositoryImpl.class.getName() + ".class";

        public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?,?> environment) throws Exception {

            Object res = null;
            if (obj instanceof Reference) {
                Reference ref = (Reference)obj;
                String classname = ref.getClassName();

                if (RepositoryImpl.class.getName().equals(classname)) {

                    RefAddr rfac = ref.get(RCF);
                    if (rfac == null || !(rfac instanceof StringRefAddr)) {
                        throw new Exception("Address type " + RCF + " missing or of wrong class: " + rfac);
                    }
                    String configFactoryClassName = (String)((StringRefAddr)rfac).getContent();

                    RefAddr rclas = ref.get(RCC);
                    if (rclas == null || !(rclas instanceof StringRefAddr)) {
                        throw new Exception("Address type " + RCC + " missing or of wrong class: " + rclas);
                    }
                    String repositoryConfigClassName = (String)((StringRefAddr)rclas).getContent();

                    Object rof = Class.forName(configFactoryClassName).newInstance();

                    if (! (rof instanceof ObjectFactory)) {
                        throw new Exception(rof + " must implement ObjectFactory");
                    }

                    ObjectFactory of = (ObjectFactory)rof;
                    Reference newref = new Reference(repositoryConfigClassName,
                        configFactoryClassName, null);

                    // carry over all arguments except our own
                    for (Enumeration<RefAddr> en = ref.getAll(); en.hasMoreElements(); ){
                        RefAddr ra = en.nextElement();
                        String type = ra.getType();
                        if (! RCF.equals(type) && ! RCC.equals(type)) {
                            newref.add(ra);
                        }
                    }

                    Object config = of.getObjectInstance(newref, name, nameCtx, environment);
                    if (! (config instanceof RepositoryConfig)) {
                        throw new Exception(config + " must implement RepositoryConfig");
                    }
                    return RepositoryImpl.create((RepositoryConfig)config);
                }
                else {
                    throw new Exception("Unexpected class: " + classname);
                }
            }
            return res;
        }
    }

}