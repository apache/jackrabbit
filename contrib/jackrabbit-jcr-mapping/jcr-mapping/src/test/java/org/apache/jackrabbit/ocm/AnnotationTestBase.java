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
package org.apache.jackrabbit.ocm;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Repository;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.apache.jackrabbit.ocm.manager.impl.ObjectContentManagerImpl;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.AnnotationMapperImpl;
import org.apache.jackrabbit.ocm.repository.RepositoryUtil;
import org.apache.jackrabbit.ocm.testmodel.Atomic;
import org.apache.jackrabbit.ocm.testmodel.Default;
import org.apache.jackrabbit.ocm.testmodel.auto.CmsObject;
import org.apache.jackrabbit.ocm.testmodel.auto.Content;
import org.apache.jackrabbit.ocm.testmodel.auto.Document;
import org.apache.jackrabbit.ocm.testmodel.auto.Folder;
import org.apache.jackrabbit.ocm.testmodel.auto.impl.CmsObjectImpl;
import org.apache.jackrabbit.ocm.testmodel.auto.impl.ContentImpl;
import org.apache.jackrabbit.ocm.testmodel.auto.impl.DocumentImpl;
import org.apache.jackrabbit.ocm.testmodel.auto.impl.FolderImpl;

/**
 * Base class for testcases. Provides priviledged access to the jcr test
 * repository.
 * 
 * @author <a href="mailto:christophe.lombart@gmail.com">Christophe Lombart</a>
 * 
 * 
 */
public abstract class AnnotationTestBase extends AbstractTestBase
{


	/**
	 * <p>
	 * Defines the test case name for junit.
	 * </p>
	 * 
	 * @param testName
	 *            The test case name.
	 */
	public AnnotationTestBase(String testName)
	{
		super(testName);
	}

    
	protected void initObjectContentManager() throws UnsupportedRepositoryOperationException, javax.jcr.RepositoryException
	{
		Repository repository = RepositoryUtil.getRepository("repositoryTest");	
		session = RepositoryUtil.login(repository, "superuser", "superuser");
		List<Class> classes = new ArrayList<Class>();
		classes.add(Atomic.class);
		classes.add(Default.class);
		
		Mapper mapper = new AnnotationMapperImpl(classes);
		ocm = new ObjectContentManagerImpl(session, mapper);
		
	}





}