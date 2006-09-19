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
package org.apache.jackrabbit.command.ext;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.jackrabbit.command.CommandHelper;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.compact.CompactNodeTypeDefReader;

/**
 * Register a node type
 */
public class RegisterNodeType implements Command {

	private String cndStreamKey = "cndStream";

	private String cndName = "cndName";

	private String encodingKey = "encoding";

	/**
	 * {@inheritDoc}
	 */
	public boolean execute(Context ctx) throws Exception {
		InputStream is = (InputStream) ctx.get(cndStreamKey);
		Reader reader = null;
		if (ctx.containsKey(this.encodingKey)) {
			reader = new InputStreamReader(is, "utf-8");
		} else {
			reader = new InputStreamReader(is, (String) ctx
					.get(this.encodingKey));
		}

		String cndName = null;
		if (ctx.containsKey(this.cndName)) {
			cndName = (String) ctx.get(this.cndName);
		} else {
			cndName = "";
		}

		CompactNodeTypeDefReader cndReader = new CompactNodeTypeDefReader(
				reader, cndName);

		// Get the List of NodeTypeDef objects
		List ntdList = cndReader.getNodeTypeDefs();

		// Get the NodeTypeManager from the Workspace.
		// Note that it must be cast from the generic JCR NodeTypeManager to the
		// Jackrabbit-specific implementation.
		NodeTypeManagerImpl ntmgr = (NodeTypeManagerImpl) CommandHelper
				.getSession(ctx).getWorkspace().getNodeTypeManager();

		// Acquire the NodeTypeRegistry
		NodeTypeRegistry ntreg = ntmgr.getNodeTypeRegistry();

		// Loop through the prepared NodeTypeDefs
		for (Iterator i = ntdList.iterator(); i.hasNext();) {

			// Get the NodeTypeDef...
			NodeTypeDef ntd = (NodeTypeDef) i.next();

			// ...and register it
			ntreg.registerNodeType(ntd);
		}

		return false;
	}

	public String getCndName() {
		return cndName;
	}

	public void setCndName(String cndName) {
		this.cndName = cndName;
	}

	public String getCndStreamKey() {
		return cndStreamKey;
	}

	public void setCndStreamKey(String cndStreamKey) {
		this.cndStreamKey = cndStreamKey;
	}

	public String getEncodingKey() {
		return encodingKey;
	}

	public void setEncodingKey(String encodingKey) {
		this.encodingKey = encodingKey;
	}
}
