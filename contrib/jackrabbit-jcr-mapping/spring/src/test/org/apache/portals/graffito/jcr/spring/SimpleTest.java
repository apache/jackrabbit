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
package org.apache.portals.graffito.jcr.spring;

import java.util.Date;
import java.util.Iterator;

import org.apache.portals.graffito.jcr.spring.test.components.ArticleService;
import org.apache.portals.graffito.jcr.spring.test.components.NewsService;
import org.apache.portals.graffito.jcr.spring.test.model.Article;
import org.apache.portals.graffito.jcr.spring.test.model.News;
import org.springframework.beans.BeansException;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test Mapper
 *
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Christophe Lombart</a>
 */
public class SimpleTest extends TestCase
{

    
    /**
     * <p>Defines the test case name for junit.</p>
     * @param testName The test case name.
     */
    public SimpleTest(String testName)
    {
        super(testName);
    }

    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    /**
     * @see junit.framework.TestCase#tearDown()
     */
    public void tearDown() throws Exception
    {
        super.tearDown();
    }

    public static Test suite()
    {
        // All methods starting with "test" will be executed in the test suite.
        return new TestSuite(SimpleTest.class);
    }

    public void testComponents()    
    {
		try {
			System.out.println("Init Spring");
			AbstractApplicationContext context = new FileSystemXmlApplicationContext("src/spring-config/applicationContext-repository.xml");
			//ResourceLoader loader = new DefaultResourceLoader();

			System.out.println("Add article");
			ArticleService service = (ArticleService) context .getBean("org.apache.portals.graffito.jcr.spring.test.components.ArticleService");
			Article article = new Article();
			article.setPath("/article");
			article.setAuthor("Christophe");
			article.setContent("This is an interesting content");
			article.setCreationDate(new Date());
			article.setDescription("This is the article description");
			article.setTitle("Article Title");

			service.createArticle(article);

			System.out.println("Check News");
			NewsService newsService = (NewsService) context .getBean("org.apache.portals.graffito.jcr.spring.test.components.NewsService");
			Iterator news = newsService.getNews().iterator();
			while (news.hasNext())
			{
				News newsFound = (News) news.next();
				System.out.println("News found : " + newsFound.getContent());
			}
			context.close();
		} catch (Exception e) {
			
			e.printStackTrace();
		}
    	
    }
    
}