/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
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
package org.apache.jackrabbit.chain.command;

import javax.jcr.Node;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.jackrabbit.chain.CtxHelper;

/**
 * Saves the current working node.
 */
public class SaveNode implements Command {
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.commons.chain.Command#execute(org.apache.commons.chain.Context)
	 */
	public boolean execute(Context ctx) throws Exception {
		Node node = CtxHelper.getCurrentNode(ctx);
		node.save() ;
		return false;
	}
}
