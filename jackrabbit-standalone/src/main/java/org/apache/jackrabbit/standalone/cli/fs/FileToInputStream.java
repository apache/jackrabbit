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
package org.apache.jackrabbit.standalone.cli.fs;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Puts an java.io.InputStream in the context from an Fs path
 * 
 */
public class FileToInputStream implements Command {

	/** logger */
	private static Log log = LogFactory.getLog(FileToInputStream.class);

	// ---------------------------- < keys >

	/** file key */
	private String srcFsPathKey = "srcFsPath";

	/** target context key */
	private String destKey = "dest";

	/**
	 * {@inheritDoc}
	 */
	public boolean execute(Context ctx) throws Exception {
		String from = (String) ctx.get(this.srcFsPathKey);
		File file = new File(from);
		log.debug("putting " + file.getAbsolutePath() + " InputStream under "
				+ this.destKey);
		InputStream is = new BufferedInputStream(new FileInputStream(file));
		ctx.put(this.destKey, is);
		return false;
	}

	public String getDestKey() {
		return destKey;
	}

	public void setDestKey(String destKey) {
		this.destKey = destKey;
	}

	public String getSrcFsPathKey() {
		return srcFsPathKey;
	}

	public void setSrcFsPathKey(String srcFsPathKey) {
		this.srcFsPathKey = srcFsPathKey;
	}
}
