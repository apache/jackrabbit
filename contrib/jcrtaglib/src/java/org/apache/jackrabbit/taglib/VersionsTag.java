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
package org.apache.jackrabbit.taglib;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.servlet.jsp.JspException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Iterates over the versions of the given node
 * 
 * @author <a href="mailto:edgarpoce@gmail.com">Edgar Poce </a>
 */
public class VersionsTag extends NodesTag
{
    /** logger */
    private static Log log = LogFactory.getLog(VersionsTag.class);

    /** tag name */
    public static String TAG_NAME = "versions";

    /**
     * Override superclass getNode.
     * @return the baseVersion of the given Node
     */
    protected Node getNode() throws JspException, RepositoryException
    {
        Node node = super.getNode() ;
        Version version = node.getBaseVersion() ;
        return version ;
    }
}