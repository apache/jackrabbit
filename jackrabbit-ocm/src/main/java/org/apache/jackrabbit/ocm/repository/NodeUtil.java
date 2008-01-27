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
package org.apache.jackrabbit.ocm.repository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.exception.JcrMappingException;
import org.apache.jackrabbit.ocm.exception.ObjectContentManagerException;

/**
* Utility class for managing JCR nodes.
*
*
* @author <a href="mailto:christophe.lombart@sword-technologies.com">Lombart Christophe </a>
* @version $Id: Exp $
*/
public class NodeUtil
{


    /** Item path separator */
    public static final String PATH_SEPARATOR = "/";

    private final static Log log = LogFactory.getLog(NodeUtil.class);


    /**
     * Check if a path is valid
     *
     * @param path The path to validate
     * @return true if the path is valid, else false
     */
    public static boolean isValidPath(String path)
    {
        if ((path == null) ||
            (path.equals(PATH_SEPARATOR)) ||
            (path.endsWith(PATH_SEPARATOR)) ||
            (! path.startsWith(PATH_SEPARATOR)) ||
            (path.equals("")))
        {
            return false;
        }
        return true;
    }

    /**
     * Get the parent path
     * @param path The path from wich the parent path has to be returned
     * @return The parent path
     *
     * @throws ObjectContentManagerException when the path is invalid
     */
    public static String getParentPath(String path) throws ObjectContentManagerException
    {
        String parentPath = "";

        if (!isValidPath(path))
        {
            throw new JcrMappingException("Invalid path : " + path);
        }

        String[] pathElements = path.split(PATH_SEPARATOR);

        // Firts path element should be = empty string because a uri always start with '/'
        // So, if len=2, means it is a root folder like '/foo'.
        // In this case the uri has not parent folder => return "/"
        if (pathElements.length == 2)
        {
            return PATH_SEPARATOR;
        }

        for(int i=0; i < pathElements.length -1; i++)
        {
            if (! pathElements[i].equals(""))
            {
               parentPath += PATH_SEPARATOR + pathElements[i];
            }
        }
        return parentPath;
    }

    /**
     * Get the node name
     * @param path  The path from which the node name has to be retrieved
     * @return The node name
     *
     * @throws ObjectContentManagerException when the path is invalid
     */
    public static String getNodeName(String path)  throws ObjectContentManagerException
    {

        String[] pathElements = path.split(PATH_SEPARATOR);

        if (! isValidPath(path))
        {
            throw new JcrMappingException("Invalid path : " + path);
        }
        return pathElements[pathElements.length-1];
    }


}
