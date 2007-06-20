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
package org.apache.jackrabbit.taglib;

import javax.jcr.Node;
import javax.servlet.jsp.JspTagException;

import org.apache.jackrabbit.taglib.utils.JCRTagUtils;
import org.apache.log4j.Logger;

/**
 * Iterates over the versions of the given node
 * 
 * @author <a href="mailto:edgarpoce@gmail.com">Edgar Poce </a>
 */
public class VersionsTag extends NodesTag {
	/** logger */
	private static Logger log = Logger.getLogger(VersionsTag.class);

	/** tag name */
	public static String TAG_NAME = "versions";

	protected void prepare() throws JspTagException {
		try {
			Node node = super.getNode();
			this.nodes = node.getVersionHistory().getAllVersions();
		} catch (Exception e) {
			String msg = JCRTagUtils.getMessage(e);
			log.error(msg, e);
			throw new JspTagException(msg);
		}
	}

}