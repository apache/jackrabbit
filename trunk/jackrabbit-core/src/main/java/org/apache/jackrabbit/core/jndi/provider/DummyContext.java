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
package org.apache.jackrabbit.core.jndi.provider;

import javax.naming.Binding;
import javax.naming.CompositeName;
import javax.naming.CompoundName;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NotContextException;
import javax.naming.OperationNotSupportedException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

/**
 * <code>DummyContext</code> is a simple service provider that
 * implements a flat namespace in memory. It is intended to be used for
 * testing purposes only.
 */
class DummyContext extends Hashtable implements Context, Cloneable {

    private transient Hashtable environment;

    private static final NameParser nameParser = new FlatNameParser();

    /**
     * Constructs a new <code>DummyContext</code> instance
     */
    DummyContext() {
        this(null);
    }

    /**
     * Constructs a new <code>DummyContext</code> instance
     *
     * @param environment
     */
    DummyContext(Hashtable environment) {
        if (environment == null) {
            this.environment = new Hashtable();
        } else {
            this.environment = (Hashtable) environment.clone();
        }
    }

    protected String getComponentName(Name name) throws NamingException {
        if (name instanceof CompositeName) {
            if (name.size() > 1) {
                throw new InvalidNameException(name.toString() + " has more components than namespace can handle");
            }
            return name.get(0);
        } else {
            // compound name
            return name.toString();
        }
    }

    protected Object getBoundObject(String name) throws NamingException {
        Object obj = get(name);
        if (obj == null) {
            throw new NameNotFoundException();
        } else {
            return obj;
        }
    }

    public Object clone() {
        Object obj = super.clone();
        ((DummyContext) obj).environment = (Hashtable) environment.clone();
        return obj;
    }

    //--------------------------------------------------------------< Context >
    /**
     * {@inheritDoc}
     */
    public void bind(Name name, Object obj) throws NamingException {
        if (name.isEmpty()) {
            throw new InvalidNameException("empty name");
        }
        String n = getComponentName(name);
        if (containsKey(n)) {
            throw new NameAlreadyBoundException();
        }
        put(n, obj);
    }

    /**
     * {@inheritDoc}
     */
    public void bind(String name, Object obj) throws NamingException {
        bind(new CompositeName(name), obj);
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws NamingException {
    }

    /**
     * {@inheritDoc}
     */
    public Name composeName(Name name, Name prefix) throws NamingException {
        Name newName = (Name) prefix.clone();
        return newName.addAll(name);
    }

    /**
     * {@inheritDoc}
     */
    public String composeName(String name, String prefix) throws NamingException {
        return composeName(new CompositeName(name), new CompositeName(prefix)).toString();
    }

    /**
     * {@inheritDoc}
     */
    public Context createSubcontext(Name name) throws NamingException {
        throw new OperationNotSupportedException("subcontexts are not supported");
    }

    /**
     * {@inheritDoc}
     */
    public Context createSubcontext(String name) throws NamingException {
        return createSubcontext(new CompositeName(name));
    }

    /**
     * {@inheritDoc}
     */
    public void destroySubcontext(Name name) throws NamingException {
        throw new OperationNotSupportedException("subcontexts are not supported");
    }

    /**
     * {@inheritDoc}
     */
    public void destroySubcontext(String name) throws NamingException {
        destroySubcontext(new CompositeName(name));
    }

    /**
     * {@inheritDoc}
     */
    public Hashtable getEnvironment() throws NamingException {
        return (Hashtable) environment.clone();
    }

    /**
     * {@inheritDoc}
     */
    public String getNameInNamespace() throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**
     * {@inheritDoc}
     */
    public NameParser getNameParser(Name name) throws NamingException {
        return nameParser;
    }

    /**
     * {@inheritDoc}
     */
    public NameParser getNameParser(String name) throws NamingException {
        return nameParser;
    }

    /**
     * {@inheritDoc}
     */
    public NamingEnumeration list(Name name) throws NamingException {
        if (name.isEmpty()) {
            return new NamingEnum(this);
        }
        String n = getComponentName(name);
        Object obj = getBoundObject(n);
        if (obj instanceof Context) {
            return ((Context) obj).list("");
        } else {
            throw new NotContextException(name + " is not bound to a context");
        }
    }

    /**
     * {@inheritDoc}
     */
    public NamingEnumeration list(String name) throws NamingException {
        return list(new CompositeName(name));
    }

    /**
     * {@inheritDoc}
     */
    public NamingEnumeration listBindings(Name name) throws NamingException {
        if (name.isEmpty()) {
            return new BindingEnum(this);
        }
        String n = getComponentName(name);
        Object obj = getBoundObject(n);
        if (obj instanceof Context) {
            return ((Context) obj).listBindings("");
        } else {
            throw new NotContextException(name + " is not bound to a context");
        }
    }

    /**
     * {@inheritDoc}
     */
    public NamingEnumeration listBindings(String name) throws NamingException {
        return listBindings(new CompositeName(name));
    }

    /**
     * {@inheritDoc}
     */
    public Object lookup(Name name) throws NamingException {
        if (name.isEmpty()) {
            return clone();
        }
        String n = getComponentName(name);
        return getBoundObject(n);
    }

    /**
     * {@inheritDoc}
     */
    public Object lookup(String name) throws NamingException {
        return lookup(new CompositeName(name));
    }

    /**
     * {@inheritDoc}
     */
    public Object lookupLink(Name name) throws NamingException {
        // no special handling of links, delegate to lookup(Name)
        return lookup(name);
    }

    /**
     * {@inheritDoc}
     */
    public Object lookupLink(String name) throws NamingException {
        return lookupLink(new CompositeName(name));
    }

    /**
     * {@inheritDoc}
     */
    public void rebind(Name name, Object obj) throws NamingException {
        if (name.isEmpty()) {
            throw new InvalidNameException("empty name");
        }
        String n = getComponentName(name);
        put(n, obj);
    }

    /**
     * {@inheritDoc}
     */
    public void rebind(String name, Object obj) throws NamingException {
        rebind(new CompositeName(name), obj);
    }

    /**
     * {@inheritDoc}
     */
    public Object removeFromEnvironment(String propName) throws NamingException {
        return environment.remove(propName);
    }

    /**
     * {@inheritDoc}
     */
    public void rename(Name oldName, Name newName) throws NamingException {
        if (oldName.isEmpty() || newName.isEmpty()) {
            throw new InvalidNameException("empty name");
        } else {
            Object obj = lookup(oldName);
            bind(newName, obj);
            unbind(oldName);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void rename(String oldName, String newName) throws NamingException {
        rename(new CompositeName(oldName), new CompositeName(newName));
    }

    /**
     * {@inheritDoc}
     */
    public void unbind(Name name) throws NamingException {
        if (name.isEmpty()) {
            throw new InvalidNameException("empty name");
        }
        String n = getComponentName(name);
        remove(n);
    }

    /**
     * {@inheritDoc}
     */
    public void unbind(String name) throws NamingException {
        unbind(new CompositeName(name));
    }

    /**
     * {@inheritDoc}
     */
    public Object addToEnvironment(String propName, Object propVal) throws NamingException {
        return environment.put(propName, propVal);
    }

    //--------------------------------------------------------< inner classes >
    /**
     * <code>FlatNameParser</code> ...
     */
    static class FlatNameParser implements NameParser {

        private static final Properties syntax = new Properties();

        static {
            syntax.put("jndi.syntax.direction", "flat");
            syntax.put("jndi.syntax.ignorecase", "false");
        }

        /**
         * @see NameParser#parse(String)
         */
        public Name parse(String name) throws NamingException {
            return new CompoundName(name, syntax);
        }
    }

    /**
     * <code>NamingEnum</code> ...
     */
    class NamingEnum implements NamingEnumeration {
        protected Enumeration namesEnum;
        protected Hashtable bindings;

        NamingEnum(Hashtable bindings) {
            namesEnum = bindings.keys();
            this.bindings = bindings;
        }

        public boolean hasMoreElements() {
            return namesEnum.hasMoreElements();
        }

        public boolean hasMore() throws NamingException {
            return hasMoreElements();
        }

        public Object next() throws NamingException {
            return nextElement();
        }

        public Object nextElement() {
            String name = (String) namesEnum.nextElement();
            String className = bindings.get(name).getClass().getName();
            return new NameClassPair(name, className);
        }

        public void close() throws NamingException {
        }
    }

    /**
     * <code>BindingEnum</code> ...
     */
    class BindingEnum extends NamingEnum {

        BindingEnum(Hashtable bindings) {
            super(bindings);
        }

        public Object nextElement() {
            String name = (String) namesEnum.nextElement();
            return new Binding(name, bindings.get(name));
        }
    }
}
