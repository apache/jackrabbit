/*
 * Copyright 2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
package org.apache.jackrabbit.core.config;

import org.apache.commons.collections.BeanMap;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * <code>AbstractConfig</code> is the superclass of
 * <code>RepositoryConfig</code> and <code>WorkspaceConfig</code>.
 */
abstract class AbstractConfig implements EntityResolver {
    private static Logger log = Logger.getLogger(AbstractConfig.class);

    public static final String CONFIG_DTD_RESOURCE_PATH =
            "org/apache/jackrabbit/core/config/config.dtd";

    protected static final String FILE_SYSTEM_ELEMENT = "FileSystem";
    protected static final String PATH_ATTRIB = "path";
    protected static final String PARAM_ELEMENT = "param";
    protected static final String CLASS_ATTRIB = "class";
    protected static final String NAME_ATTRIB = "name";
    protected static final String VALUE_ATTRIB = "value";

    protected final String configId;
    protected final Document config;

    /**
     * constructor
     *
     * @param is
     */
    protected AbstractConfig(InputSource is) throws RepositoryException {
        configId = is.getSystemId() == null ? "[???]" : is.getSystemId();
        try {
            SAXBuilder parser = new SAXBuilder();
            parser.setEntityResolver(this);
            config = parser.build(is);
        } catch (Exception e) {
            String msg = "error while parsing config file " + is.getSystemId();
            log.error(msg, e);
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * Creates a {@link org.apache.jackrabbit.core.fs.FileSystem} instance
     * based on the config <code>fsConfig</code>.
     *
     * @param fsConfig a {@link #FILE_SYSTEM_ELEMENT}.
     * @param variables values of variables to be replaced in config.
     * @return a {@link org.apache.jackrabbit.core.fs.FileSystem} instance.
     * @throws RepositoryException if an error occurs while creating the
     *  {@link org.apache.jackrabbit.core.fs.FileSystem}.
     */
    static FileSystem createFileSystem(Element fsConfig, Map variables)
            throws RepositoryException {
        FileSystem fs;
        String className = "";
        try {
            // create the file system object
            className = fsConfig.getAttributeValue(CLASS_ATTRIB);
            Class c = Class.forName(className);
            fs = (FileSystem) c.newInstance();

            // set the properties of the file system object from the
            // param elements in the config
            BeanMap bm = new BeanMap(fs);
            List paramList = fsConfig.getChildren(PARAM_ELEMENT);
            for (Iterator i = paramList.iterator(); i.hasNext();) {
                Element param = (Element) i.next();
                String paramName = param.getAttributeValue(NAME_ATTRIB);
                String paramValue = param.getAttributeValue(VALUE_ATTRIB);
                // replace variables in param value
                bm.put(paramName, replaceVars(paramValue, variables));
            }
            fs.init();
        } catch (Exception e) {
            String msg = "Cannot instantiate implementing class " + className;
            log.error(msg, e);
            throw new RepositoryException(msg, e);
        }
        return fs;
    }

    /**
     * Helper method that replaces in the given string any occurences of the keys
     * in the specified map with their associated values.
     *
     * @param s
     * @param vars
     * @return
     */
    protected static String replaceVars(String s, Map vars) {
        if (vars.size() == 0) {
            return s;
        }
        Iterator iter = vars.keySet().iterator();
        while (iter.hasNext()) {
            String varName = (String) iter.next();
            String varValue = (String) vars.get(varName);
            int pos;
            int lastPos = 0;
            StringBuffer sb = new StringBuffer(s.length());
            while ((pos = s.indexOf(varName, lastPos)) != -1) {
                sb.append(s.substring(lastPos, pos));
                sb.append(varValue);
                lastPos = pos + varName.length();
            }
            if (lastPos < s.length()) {
                sb.append(s.substring(lastPos));
            }
            s = sb.toString();
        }
        return s;
    }

    //------------------------------------------------------< EntityResolver >
    /**
     * @see EntityResolver#resolveEntity(String, String)
     */
    public abstract InputSource resolveEntity(String publicId, String systemId)
            throws SAXException, IOException;
}
