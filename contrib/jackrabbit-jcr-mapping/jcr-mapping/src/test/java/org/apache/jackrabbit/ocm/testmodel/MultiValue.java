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
package org.apache.jackrabbit.ocm.testmodel;

import java.util.Collection;

/**
 * 
 * Simple object used to test multivalue properties
 * 
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Lombart
 *         Christophe </a>
 * @version $Id: Exp $
 */
public class MultiValue
{
	private String path;
	
	private String name;
	
	private Collection multiValues;

	private Collection nullMultiValues;

	
	
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * @return Returns the name.
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @param name The name to set.
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * @return Returns the multiValues.
	 */
	public Collection getMultiValues()
	{
		return multiValues;
	}

	/**
	 * @param multiValues
	 *            The multiValues to set.
	 */
	public void setMultiValues(Collection multiValues)
	{
		this.multiValues = multiValues;
	}

	/**
	 * @return Returns the nullMultiValues.
	 */
	public Collection getNullMultiValues()
	{
		return nullMultiValues;
	}

	/**
	 * @param nullMultiValues
	 *            The nullMultiValues to set.
	 */
	public void setNullMultiValues(Collection nullMultiValues)
	{
		this.nullMultiValues = nullMultiValues;
	}

}
