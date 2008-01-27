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
package org.apache.jackrabbit.ocm.version;

import java.util.Calendar;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.ocm.exception.VersionException;

/**
 *
 *
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Lombart Christophe </a>
 *
 */
public class Version
{

	private javax.jcr.version.Version version;

	public Version(javax.jcr.version.Version version)
	{
		this.version = version;
	}

	public Calendar getCreated()
	{
		try
		{
			return version.getCreated();
		}
		catch (RepositoryException e)
		{

			throw new VersionException("Error while retrieving the version creation date", e);
		}
	}

	public String getUuid()
	{
		try
		{
			return version.getUUID();
		}
		catch (RepositoryException e)
		{

			throw new VersionException("Error while retrieving the version UUID", e);
		}
	}

	public String getPath()
	{
		try
		{
			return version.getPath();
		}
		catch (RepositoryException e)
		{

			throw new VersionException("Error while retrieving the version path", e);
		}
	}

	public String getName()
	{
		try
		{
			return version.getName();
			
		}
		catch (RepositoryException e)
		{

			throw new VersionException("Error while retrieving the version path", e);
		}
	}	
	
}
