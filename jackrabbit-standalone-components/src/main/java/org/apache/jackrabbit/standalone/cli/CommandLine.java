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
package org.apache.jackrabbit.standalone.cli;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.iterators.IteratorChain;

/**
 * Command Line
 */
public class CommandLine implements Comparable, Cloneable {
    /** Resource bundle */
    protected static ResourceBundle bundle = CommandHelper.getBundle();

    /** Name */
    private String name;

    /** Description */
    private String description;

    /** Commons chain command implementation */
    private String impl;

    /** alias */
    private Collection alias = new ArrayList();

    /** Options */
    private Map options = new TreeMap();

    /** flags */
    private Map flags = new TreeMap();

    /** arguments */
    private Map arguments = new TreeMap();

    /**
     * constructor
     */
    public CommandLine() {
        super();
    }

    /**
     * @return required arguments
     */
    public Collection getRequiredArguments() {
        Predicate p = new Predicate() {
            public boolean evaluate(Object o) {
                Argument arg = (Argument) o;
                return arg.isRequired();
            }
        };
        return CollectionUtils.select(this.arguments.values(), p);
    }

    /**
     * @return required options
     */
    public Collection getRequiredOptions() {
        Predicate p = new Predicate() {
            public boolean evaluate(Object o) {
                Option opt = (Option) o;
                return opt.isRequired();
            }
        };
        return CollectionUtils.select(this.options.values(), p);
    }

    /**
     * @return the description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the localized description.
     */
    public String getLocalizedDescription() {
        if (description == null) {
            return bundle.getString("cmd." + this.name);
        } else {
            return bundle.getString(this.description);
        }
    }

    /**
     * @param description
     *        The description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the flags
     */
    public Map getFlags() {
        return flags;
    }

    /**
     * @param flags
     *        The flags to set
     */
    public void setFlags(Map flags) {
        this.flags = flags;
    }

    /**
     * @return the impl
     */
    public String getImpl() {
        return impl;
    }

    /**
     * @param impl
     *        The impl to set
     */
    public void setImpl(String impl) {
        this.impl = impl;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *        the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the options
     */
    public Map getOptions() {
        return options;
    }

    /**
     * @param options
     *        the <code>Option</code>s to set
     */
    public void setOptions(Map options) {
        this.options = options;
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo(Object o) {
        CommandLine cl = (CommandLine) o;
        return name.compareTo(cl.name);
    }

    /**
     * @return all the parameters. i.e. args, options and flags.
     */
    public Iterator getAllParameters() {
        IteratorChain chain = new IteratorChain();
        chain.addIterator(getArguments().values().iterator());
        chain.addIterator(getOptions().values().iterator());
        chain.addIterator(getFlags().values().iterator());
        return chain;
    }

    /**
     * @return the required parameters. i.e. args, options and flags.
     */
    public Iterator getRequiredParameters() {
        IteratorChain chain = new IteratorChain();
        chain.addIterator(getRequiredArguments().iterator());
        chain.addIterator(getRequiredOptions().iterator());
        return chain;
    }

    /**
     * @param arg
     *        the <code>Argument</code> to add
     */
    public void addArgument(Argument arg) {
        if (arguments.containsKey(new Integer(arg.getPosition()))) {
            throw new IllegalArgumentException(
                "there's an argument in the position in command " + this.name);
        }
        // Put default values description and arg name i18n keys
        if (arg.getArgName() == null) {
            arg.setArgName("cmd." + this.getName() + "." + arg.getName());
        }
        if (arg.getDescription() == null) {
            arg.setDescription("cmd." + this.getName() + "." + arg.getName()
                    + ".desc");
        }
        this.arguments.put(new Integer(arg.getPosition()), arg);
    }

    /**
     * @param opt the <code>Option</code> to add
     */
    public void addOption(Option opt) {
        // Put default values description and arg name i18n keys
        if (opt.getArgName() == null) {
            opt.setArgName("cmd." + this.getName() + "." + opt.getName());
        }
        if (opt.getDescription() == null) {
            opt.setDescription("cmd." + this.getName() + "." + opt.getName()
                    + ".desc");
        }
        this.options.put(opt.getName(), opt);
    }

    /**
     * @param flag the <code>Flag</code> to add
     */
    public void addFlag(Flag flag) {
        // Put default values description and arg name i18n keys
        if (flag.getDescription() == null) {
            flag.setDescription("cmd." + this.getName() + "." + flag.getName()
                    + ".desc");
        }
        this.flags.put(flag.getName(), flag);
    }

    /**
     * @return the arguments
     */
    public Map getArguments() {
        return arguments;
    }

    /**
     * @param arguments
     *        the arguments to set
     */
    public void setArguments(Map arguments) {
        this.arguments = arguments;
    }

    /**
     * @return the alias
     */
    public Collection getAlias() {
        return alias;
    }

    /**
     * @param alias
     *        the alias to set
     */
    public void setAlias(Collection alias) {
        this.alias = alias;
    }

    /**
     * @param alias the alias to add
     */
    public void addAlias(String alias) {
        this.alias.add(alias);
    }

    /**
     * {@inheritDoc}
     */
    public Object clone() {
        CommandLine cl = new CommandLine();
        cl.alias = this.alias;
        // Arguments
        Iterator iter = this.arguments.values().iterator();
        while (iter.hasNext()) {
            Argument arg = (Argument) iter.next();
            cl.addArgument((Argument) arg.clone());
        }
        cl.description = this.description;
        // Flags
        iter = this.flags.values().iterator();
        while (iter.hasNext()) {
            Flag f = (Flag) iter.next();
            cl.addFlag((Flag) f.clone());
        }
        cl.impl = this.impl;
        cl.name = this.name;
        // Flags
        iter = this.options.values().iterator();
        while (iter.hasNext()) {
            Option o = (Option) iter.next();
            cl.addOption((Option) o.clone());
        }
        return cl;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "CommandLine-" + this.getName() + "(" + impl + ")";
    }
}
