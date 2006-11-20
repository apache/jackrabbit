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
package org.apache.jackrabbit.browser.command;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.fileupload.FileItem;

/**
 * Puts commons-fileupload FileItem InputStream in a context variable
 */
public class FileItemToInputStream implements Command {

	/** file item key */
	private String fileItemKey = "fileItem";

	private String targetKey = "inputStream";

	public boolean execute(Context ctx) throws Exception {
		FileItem fileItem = (FileItem) ctx.get(fileItemKey);
		ctx.put(this.targetKey, fileItem.getInputStream());
		return false;
	}

	/**
	 * @return the fileItemKey
	 */
	public String getFileItemKey() {
		return fileItemKey;
	}

	/**
	 * @param fileItemKey
	 *            the fileItemKey to set
	 */
	public void setFileItemKey(String fileItemKey) {
		this.fileItemKey = fileItemKey;
	}

	/**
	 * @return the targetKey
	 */
	public String getTargetKey() {
		return targetKey;
	}

	/**
	 * @param targetKey
	 *            the targetKey to set
	 */
	public void setTargetKey(String targetKey) {
		this.targetKey = targetKey;
	}

}
