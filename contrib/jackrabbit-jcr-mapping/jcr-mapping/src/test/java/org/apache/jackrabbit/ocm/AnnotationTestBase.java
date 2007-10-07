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
import org.apache.jackrabbit.ocm.testmodel.A;
import org.apache.jackrabbit.ocm.testmodel.Atomic;
import org.apache.jackrabbit.ocm.testmodel.B;
import org.apache.jackrabbit.ocm.testmodel.C;
import org.apache.jackrabbit.ocm.testmodel.D;
import org.apache.jackrabbit.ocm.testmodel.DFull;
import org.apache.jackrabbit.ocm.testmodel.Default;
import org.apache.jackrabbit.ocm.testmodel.E;
import org.apache.jackrabbit.ocm.testmodel.MultiValue;
import org.apache.jackrabbit.ocm.testmodel.Page;
import org.apache.jackrabbit.ocm.testmodel.Paragraph;
import org.apache.jackrabbit.ocm.testmodel.Residual;
import org.apache.jackrabbit.ocm.testmodel.Residual.ResidualNodes;
import org.apache.jackrabbit.ocm.testmodel.Residual.ResidualProperties;
import org.apache.jackrabbit.ocm.testmodel.auto.CmsObject;
import org.apache.jackrabbit.ocm.testmodel.auto.Content;
import org.apache.jackrabbit.ocm.testmodel.auto.Document;
import org.apache.jackrabbit.ocm.testmodel.auto.Folder;
import org.apache.jackrabbit.ocm.testmodel.auto.impl.CmsObjectImpl;
import org.apache.jackrabbit.ocm.testmodel.auto.impl.ContentImpl;
import org.apache.jackrabbit.ocm.testmodel.auto.impl.DocumentImpl;
import org.apache.jackrabbit.ocm.testmodel.auto.impl.FolderImpl;
import org.apache.jackrabbit.ocm.testmodel.collection.Element;
import org.apache.jackrabbit.ocm.testmodel.collection.Main;

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
		
		// Register content classes used by the unit tests
		classes.add(Atomic.class);
		classes.add(Default.class);
		classes.add(A.class);
		classes.add(B.class);
		classes.add(C.class);
		classes.add(D.class);
		classes.add(DFull.class);
		classes.add(E.class);
		classes.add(Page.class);
		classes.add(Paragraph.class);
		classes.add(Main.class);
		classes.add(Element.class);
		classes.add(MultiValue.class);
		

		classes.add(Residual.class); 
		classes.add(ResidualProperties.class);
		classes.add(ResidualNodes.class);
		Mapper mapper = new AnnotationMapperImpl(classes);
		ocm = new ObjectContentManagerImpl(session, mapper);
		
	}





}