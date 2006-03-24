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
package org.apache.jackrabbit.extension;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.extension.configuration.ItemConfiguration;
import org.apache.jackrabbit.extension.configuration.RepositoryConfiguration;

/**
 * The <code>ExtensionDescriptor</code> class implements a descriptor for an
 * extension defined in a repository node with mixin node type
 * <code>rep:extension</code>.
 * <p>
 * Two instances of this class are considered equal if they are the same
 * instance or if they are of the same extension type and if their extension
 * names are equal.
 * <p>
 * This class implements the <code>Comparable</code> interface defining an order
 * amongst two instances of this class according to the extension type
 * identification and the extension name. See {@link #compareTo(Object)}.
 *
 * @author Felix Meschberger
 *
 * @see org.apache.jackrabbit.extension.ExtensionType
 * @see org.apache.jackrabbit.extension.ExtensionManager
 */
public class ExtensionDescriptor implements Comparable {

    /** default log */
    private static final Log log = LogFactory.getLog(ExtensionDescriptor.class);

    /**
     * The name of the property containing the extension type identification
     * (value is "rep:id").
     * This is a mandatory property of an extension node.
     */
    public static final String PROP_REP_ID = "rep:id";

    /**
     * The name of the property containing the extension name (value is
     * "rep:name").
     * This is a mandatory property of an extension node.
     */
    public static final String PROP_REP_NAME = "rep:name";

    /**
     * The name of the property containing the fully qualified name of a class
     * implementing the extension (value is "rep:class").
     * This is an optional property of the extension node.
     */
    public static final String PROP_REP_CLASS = "rep:class";

    /**
     * The name of the multivalue property containing the class path providing
     * the extension class(es) (value is "rep:classpath").
     * This is an optional property of the extension node.
     */
    public static final String PROP_REP_CLASSPATH = "rep:classpath";

    /**
     * The name of the property containing the fully qualified name of a class
     * implementing the <code>org.apache.commons.configuration.Configuration</code>
     * interface (value is "rep:configurationClass").
     * This is an optional property of the extension node.
     */
    public static final String PROP_REP_CONFIGURATION_CLASS =
        "rep:configurationClass";

    /**
     * The name of the child node containing the configuration for this
     * extension (value is "rep:configuration").
     * This is an optional child node of the extension node.
     */
    public static final String NODE_REP_CONFIGURATION = "rep:configuration";

    /**
     * The {@link ExtensionType} to which this extension belongs.
     * @see #getExtensionType
     */
    private final ExtensionType type;

    /**
     * The <code>Node</code> from which this descriptor has been loaded.
     * @see #getNode()
     */
    private final Node node;

    /**
     * The extension type identification read from the {@link #PROP_REP_ID}
     * property of the node describing the extension.
     * @see #getId()
     */
    private final String id;

    /**
     * The extension name read from the {@link #PROP_REP_NAME} property of the
     * node describing the extension.
     * @see #getName()()
     */
    private final String name;

    /**
     * The fully qualified name of the class implementing the extension or
     * <code>null</code> if none is defined. The value of this field is read
     * from the {@link #PROP_REP_CLASS} property of the node describing the
     * extension.
     * @see #getClassName()
     */
    private final String className;

    /**
     * The classpath to configure on the extension type's class loader to load
     * and use this extension or <code>null</code> if none is defined. The value
     * of this field is read from the {@link #PROP_REP_CLASSPATH} property of
     * the node describing the extension.
     * @see #getClassPath()
     */
    private final String[] classPath;

    /**
     * The fully qualified name of the class implementing the Apache Jakarta
     * Commons <code>Configuration</code> interface or <code>null</code> if
     * none is defined. The value of this field is read from the
     * {@link #PROP_REP_CONFIGURATION_CLASS} property of the node describing the
     * extension.
     * @see #getConfigurationClassName()
     * @see #getConfiguration()
     * @see #getConfigurationNode()
     */
    private final String configurationClassName;

    /**
     * The absolute path of the {@link #node} from which this descriptor has
     * been loaded.
     * @see #getNodePath();
     */
    private String nodePath;

    /**
     * The extension instance created for this descriptor by the
     * {@link #getExtension()} method or <code>null</code> if none has been
     * created yet.
     * @see #getExtension()
     */
    private Object extension;

    /**
     * The configuration object created for this descriptor by the
     * {@link #getConfiguration()} method or <code>null</code> if none has been
     * created yet.
     * @see #getConfiguration()
     */
    private Configuration configuration;

    /**
     * Creates an instance of this class loading the definition from the given
     * <code>extensionNode</code>.
     * <p>
     * This method does not check whether the node is of the correct type but
     * merely accesses the properties required to exist and tries to access
     * optional properties. If an error occurrs accessing the properties,
     * an <code>ExtensionException</code> is thrown with the cause set.
     *
     * @param type The {@link ExtensionType} having loaded this extension
     *      object.
     * @param extensionNode The <code>Node</code> containing the extension
     *      description.
     *
     * @throws ExtensionException If an error occurrs reading the extension
     *      description from the node.
     */
    /* package */ ExtensionDescriptor(ExtensionType type, Node extensionNode)
            throws ExtensionException {

        this.type = type;
        node = extensionNode;

        try {
            // required data
            id = getPropertyOrNull(extensionNode, PROP_REP_ID);
            name = getPropertyOrNull(extensionNode, PROP_REP_NAME);
            if (id == null || name == null) {
                throw new ExtensionException("Missing id or name property");
            }

            // optional class, classpath and configuration class
            className = getPropertyOrNull(extensionNode, PROP_REP_CLASS);
            classPath = getPropertiesOrNull(extensionNode, PROP_REP_CLASSPATH);
            configurationClassName =
               getPropertyOrNull(extensionNode, PROP_REP_CONFIGURATION_CLASS);
        } catch (RepositoryException re) {
            throw new ExtensionException("Cannot load extension", re);
        }
    }

    /**
     * Returns the {@link ExtensionType} which has loaded this extension.
     */
    private ExtensionType getExtensionType() {
        return type;
    }

    /**
     * Returns the <code>Node</code> from which this extension has been loaded.
     * Any modification to the node returned will only be active the next
     * time an instance of this class is created from the node.
     */
    public final Node getNode() {
        return node;
    }

    /**
     * Returns the absolute path of the <code>Node</code> from which this
     * extension has been loaded.
     */
    public final String getNodePath() {
        if (nodePath == null) {
            try {
                nodePath = getNode().getPath();
            } catch (RepositoryException re) {
                log.warn("Cannot get the path of the extension node", re);
                nodePath = getNode().toString();
            }
        }

        return nodePath;
    }

    /**
     * Returns the identification of the extension type implemented by this
     * extension.
     */
    public final String getId() {
        return id;
    }

    /**
     * Returns the name of this extension.
     */
    public final String getName() {
        return name;
    }

    /**
     * Returns the fully qualified name of the class implementing this extension
     * or <code>null</code> if none is configured in the extension descriptor
     * node.
     */
    public final String getClassName() {
        return className;
    }

    /**
     * Returns the extension class path or <code>null</code> if none has been
     * configured in the extension descriptor. Note that an empty array is
     * never returned by this method.
     */
    public final String[] getClassPath() {
        return classPath;
    }

    /**
     * Returns the fully qualified name of the extensions configuration class
     * or <code>null</code> if none is configured in the extension's node.
     * @see #getConfiguration()
     * @see #getConfigurationNode()
     */
    public final String getConfigurationClassName() {
        return configurationClassName;
    }

    //---------- Instantiation support ----------------------------------------

    /**
     * Returns the class loader to be used to load the extension object and the
     * configuration for the extension described by this descriptor.
     */
    public ClassLoader getExtensionLoader() {
        return getExtensionType().getClassLoader(this);
    }

    /**
     * Creates an instance of the extension class defined by this descriptor.
     * <p>
     * If the descriptor contains a classpath specification, the class loader of
     * the extension type to which the extension belongs, is configured with the
     * additional classpath.
     * <p>
     * The extension class must provide either of two constructors for it to be
     * instantiated by this method:
     * <ol>
     * <li>If a public constructor taking an instance of
     * {@link ExtensionDescriptor} is available, that constructor is used to
     * create the extension instance.</il>
     * <li>Otherwise if a public default constructor taking no paramaters at
     * all is available, that constructor is used to create the extension
     * instance. In this case it is the responsibility of the application to
     * provide the extension instance with more information if required.</li>
     * </ol>
     * <p>
     * If neither constructor is available in the class, this method fails with
     * an {@link ExtensionException}.
     * <p>
     * If the class provides a public method taking a single parameter of
     * type <code>ExtensionDescriptor</code>, that method is called with this
     * instance as the parameter value. This allows for parameterless default
     * constructors in the extension classes while still getting the extension
     * descriptor.
     * <p>
     * If no class has been defined for this extension, an
     * <code>IllegalArgumentException</code> is thrown.
     *
     * @return The instance created for this extension.
     *
     * @throws IllegalArgumentException if no extension class specification is
     *      available in this extension descriptor.
     * @throws ExtensionException if the extension class has no suitable
     *      constructor or if an error occurrs loading or instantiating the
     *      class.
     */
    public Object getExtension() throws ExtensionException {

        // immediately return the extension, if it is already defined
        if (extension != null) {
            return extension;
        }
        // otherwise, we have to instantiate

        // fail if there is no class name in the descriptor
        if (getClassName() == null) {
            throw new IllegalArgumentException("Descriptor has no class definition");
        }

        try {
            log.debug("Loading class " + getClassName());
            Class clazz = getExtensionLoader().loadClass(getClassName());
            Object extension = instantiate(clazz);
            setDescriptor(extension);
            return extension;

        } catch (Exception e) {
            throw new ExtensionException("Cannot instantiate extension " +
                getClassName(), e);
        }
    }

    /**
     * Returns the node containing the configuration of this extension. If the
     * extension's node has a child node <code>rep:configuration</code>, that
     * child node is returned, otherwise the extension's node is returned.
     *
     * @return The configuration node of this extension.
     */
    public Node getConfigurationNode() {
        Node node = getNode();

        try {
            if (node.hasNode(NODE_REP_CONFIGURATION)) {
                return node.getNode(NODE_REP_CONFIGURATION);
            }
        } catch (RepositoryException re) {
            log.warn("Cannot check or access configuration node " +
                NODE_REP_CONFIGURATION + ". Using extension node", re);
        }

        return node;
    }

    /**
     * Returns the <code>Configuration</code> object used to configure this
     * extension.
     * <p>
     * If the extension descriptor does not contain the fully qualified name of
     * a configuration class, this method returns an instance of the
     * {@link ItemConfiguration} class loaded from the extension's node.
     * <p>
     * Otherwise the named class is loaded through the extensions class loader
     * (see {@link #getExtensionLoader()}) and instantiated. A class to be used
     * like this must implement the <code>Configuration</code> interface and
     * provide a public default constructor. If any of the requirements is not
     * met by the configured class, this method throws an exception.
     * <p>
     * If the configured class implements the {@link RepositoryConfiguration}
     * interface, the configuration is configured with the extension's node
     * and loaded.
     * <p>
     * The main use of this method is for the extension class itself to
     * configure itself. Another use may be for an administrative application
     * to update configuration and optionally store it back.
     *
     * @return The <code>Configuration</code> object used to configured this
     *      extension.
     *
     * @throws ExtensionException If the configuration class has no public
     *      default constructor or if the configuration class is not an
     *      implementation of the <code>Configuration</code> interface or if an
     *      error occurrs loading or instantiating the configuration class.
     */
    public Configuration getConfiguration() throws ExtensionException {
        // immediately return the configuration, if it is already defined
        if (configuration != null) {
            return configuration;
        }
        // otherwise, we have to instantiate

        // use a default configuration if no specific class defined
        if (getConfigurationClassName() == null) {
            log.debug("No configurationClass setting, using ItemConfiguration");
            try {
                return new ItemConfiguration(getConfigurationNode());
            } catch (ConfigurationException ce) {
                throw new ExtensionException(
                    "Cannot load ItemConfiguration from " + getNodePath());
            }
        }

        try {
            log.debug("Loading class " + getConfigurationClassName());
            Class clazz =
                getExtensionLoader().loadClass(getConfigurationClassName());

            // create an instance using the one taking an extension descriptor
            // if available otherwise use the default constructor
            log.debug("Creating configuration object instance");
            Object configObject = clazz.newInstance();
            if (!(configObject instanceof Configuration)) {
                throw new ExtensionException("Configuration class " +
                    getClassName() +
                    " does not implement Configuration interface");
            }

            // load the repository configuration from the extension node
            if (configObject instanceof RepositoryConfiguration) {
                RepositoryConfiguration repoConfig =
                    (RepositoryConfiguration) configObject;
                repoConfig.setNode(getConfigurationNode());
                repoConfig.load();
            }

            configuration = (Configuration) configObject;
            return configuration;

        } catch (Exception e) {
            throw new ExtensionException("Cannot instantiate extension " +
                getClassName(), e);
        }
    }

    //---------- Comparable interface -----------------------------------------

    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.

     * @param obj the Object to be compared, which must be an instance of this
     *      class.
     *
     * @return a negative integer, zero, or a positive integer as this
     *      descriptor is less than, equal to, or greater than the specified
     *      descriptor.
     *
     * @throws NullPointerException if <code>obj</code> is <code>null</code>.
     * @throws ClassCastException if <code>obj</code> is not an
     *      <code>ExtensionDescriptor</code>.
     */
    public int compareTo(Object obj) {
        // throws documented ClassCastException
        ExtensionDescriptor other = (ExtensionDescriptor) obj;

        // check the order amongst the id and return if not equal
        int idOrder = id.compareTo(other.id);
        if (idOrder != 0) {
            return idOrder;
        }

        // id's are the same, so return order amongst names
        return name.compareTo(other.name);
    }

    //---------- Object overwrite ---------------------------------------------

    /**
     * Returns a combined hash code of the {@link #getId() type identification}
     * and the {@link #getName() name} of this extension as this extension's
     * hash code.
     */
    public int hashCode() {
        return id.hashCode() + 17 * name.hashCode();
    }

    /**
     * Returns <code>true</code> if <code>obj</code> is the same as this or
     * if it is a <code>ExtensionDescriptor</code> whose type identification
     * and name equals the type identification and name of this extension.
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof ExtensionDescriptor) {
            ExtensionDescriptor other = (ExtensionDescriptor) obj;
            return id.equals(other.id) && name.equals(other.name);
        } else {
            return false;
        }
    }

    /**
     * Returns a string representation of this extension descriptor which
     * contains the extension type identification and the extension name.
     */
    public String toString() {
        return "Extension " + id + ":" + name;
    }

    //---------- innternal ----------------------------------------------------

    /**
     * Returns the value of the {@link #PROP_REP_NAME} property of the
     * <code>extensionNode</code> or <code>null</code> if no such property
     * exists.
     *
     * @param extensionNode The <code>Node</code> whose extension name property
     *      value is to bereturned.
     *
     * @throws RepositoryException if an error occurrs accessing the extension
     *      name property.
     */
    /* package */ static String getExtensionName(Node extensionNode)
            throws RepositoryException {
        return getPropertyOrNull(extensionNode, PROP_REP_NAME);
    }

    /**
     * Returns the string value of the named (single-value) property of the
     * node or <code>null</code> if the the property does not exists or its
     * value is empty.
     *
     * @param node The <code>Node</code> containing the named property.
     * @param property The name of the property to reutrn.
     *
     * @return The property's string value or <code>null</code> if the property
     *      does not exist or is empty.
     *
     * @throws RepositoryException If an error occurrs accesing the node or
     *      property.
     */
    private static String getPropertyOrNull(Node node, String property)
            throws RepositoryException {
        if (node.hasProperty(property)) {
            String value = node.getProperty(property).getString();
            return (value == null || value.length() == 0) ? null : value;
        }

        return null;
    }

    /**
     * Returns the string values of the named (multi-valued) property of the
     * node or <code>null</code> if the the property does not exists or its
     * value is empty.
     *
     * @param node The <code>Node</code> containing the named property.
     * @param property The name of the property to reutrn.
     *
     * @return A string array containing the string representations of the
     *      property's values or <code>null</code> if the property does not
     *      exist or is empty.
     *
     * @throws RepositoryException If an error occurrs accesing the node or
     *      property.
     */
    private static String[] getPropertiesOrNull(Node node, String property)
            throws RepositoryException {

        if (node.hasProperty(property)) {
            Value[] clsPath = node.getProperty(property).getValues();
            if (clsPath != null && clsPath.length >= 0) {
                List pathList = new ArrayList();
                for (int i=0; i < clsPath.length; i++) {
                    String pathEntry = clsPath[i].getString().trim();

                    // ignore empty or existing path entry
                    if (pathEntry.length() == 0 ||
                            pathList.contains(pathEntry)) {
                        continue;
                    }

                    // new class path entry, add
                    pathList.add(pathEntry);
                }

                if (pathList.size() > 0) {
                    return (String[]) pathList.toArray(new String[pathList.size()]);
                }
            }
        }

        return null;
    }

    /**
     * Creates an instance of the given <code>clazz</code>. If the class has
     * a public constructor taking a single parameter of type
     * <code>ExtensionDescriptor</code> that constructor is used to create the
     * instance. Otherwise the public default constructor is used if available.
     * If none of both is available or if an error occurrs creating the instance
     * a <code>ExtensionException</code> is thrown.
     *
     * @param clazz The <code>Class</code> to instantiate.
     *
     * @return The instance created.
     *
     * @throws ExtensionException If an error occurrs instantiating the class.
     *      If instantiation failed due to an exception while calling the
     *      constructor, the causing exception is available as the cause of
     *      the exception.
     */
    private Object instantiate(Class clazz) throws ExtensionException {
        // find constructors (taking descriptor and default)
        Constructor defaultConstr = null;
        Constructor descrConstr = null;
        Constructor[] constructors = clazz.getConstructors();
        for (int i=0; i < constructors.length; i++) {
            Class parms[] = constructors[i].getParameterTypes();
            if (parms.length == 0) {
                defaultConstr = constructors[i];
            } else if (parms.length == 1 && parms[i].equals(getClass())) {
                descrConstr = constructors[i];
            }
        }

        try {
            // create an instance using the one taking an extension descriptor
            // if available otherwise use the default constructor
            if (descrConstr != null) {
                log.debug("Creating instance with descriptor " + this);
                return descrConstr.newInstance(new Object[]{ this });
            } else if (defaultConstr != null) {
                log.debug("Creating default instance without descriptor");
                return defaultConstr.newInstance(null);
            } else {
                throw new ExtensionException("No suitable constructor found " +
                        "to instantiate " +  getClassName());
            }
        } catch (InstantiationException ie) {
            throw new ExtensionException(
                "Cannot instantiate " + getClassName(), ie);
        } catch (IllegalAccessException iae) {
            throw new ExtensionException("Cannot access constructor of "
                + getClassName(), iae);
        } catch (InvocationTargetException ite) {
            throw new ExtensionException("Error while instantiating "
                + getClassName(), ite);
        }
    }

    /**
     * Calls a method taking a single parameter of type
     * <code>ExtensionDescriptor</code> to provide the extension descriptor to
     * the extension loaded.
     * <p>
     * If an error occurrs calling a method found, an WARN message is logged and
     * other methods according to the required signature are looked for. If no
     * suitable method can be found, an INFO method is logged and the extension
     * could not be provided with the extension descriptor.
     *
     * @param extension The extension to provide witch the extension descriptor.
     */
    private void setDescriptor(Object extension) {
        Method[] methods = extension.getClass().getMethods();
        for (int i=0; i < methods.length; i++) {
            Class[] parTypes = methods[i].getParameterTypes();
            if (parTypes.length == 1 && parTypes[0].equals(getClass())) {
                try {
                    methods[i].invoke(extension, new Object[]{ this });
                    return;
                } catch (Exception ite) {
                    log.warn("setDescriptor: Calling " +
                        extension.getClass().getName() + "." +
                        methods[i].getName() + " failed", ite);
                }
            }
        }

        log.info("setDescriptor: No setter method for ExtensionDescriptor " +
                "found in class " + extension.getClass().getName() +
                " of extension " + getId() + ":" + getName());
    }
}
