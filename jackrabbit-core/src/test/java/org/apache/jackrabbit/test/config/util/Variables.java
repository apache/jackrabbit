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

package org.apache.jackrabbit.test.config.util;

import java.util.Enumeration;
import java.util.Properties;

import org.apache.jackrabbit.test.config.xml.ConfException;
import org.apache.jackrabbit.util.Text;

/**
 * <code>Variables</code> extends the standard {@link Properties} with
 * a {@link #replaceVariables(String)} method that effectively calss
 * {@link Text#replaceVariables(Properties, String, boolean)}.
 *
 */
public class Variables extends Properties {

	private static final long serialVersionUID = 3311868957558121444L;
	
	public String replaceVariables(String value) throws ConfException {
		try {
			return Text.replaceVariables(this, value, false);
		} catch (IllegalArgumentException e) {
			throw new ConfException(e.getMessage());
		}
	}

	/**
	 * Clones the given Properties and replaces variables in all values.
	 */
	public Properties replaceVariables(Properties properties) throws ConfException {
		Properties newProps = new Properties();
		for (Enumeration keys = properties.keys(); keys.hasMoreElements();) {
			String key = (String) keys.nextElement();
			newProps.setProperty(key, replaceVariables(properties.getProperty(key)));
		}
		return newProps;
	}
}
