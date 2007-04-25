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
package org.apache.portals.graffito.jcr.spring.test.components.impl;

import java.util.Collection;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.portals.graffito.jcr.query.Filter;
import org.apache.portals.graffito.jcr.query.Query;
import org.apache.portals.graffito.jcr.query.QueryManager;
import org.apache.portals.graffito.jcr.spring.JcrMappingTemplate;
import org.apache.portals.graffito.jcr.spring.test.components.ArticleService;
import org.apache.portals.graffito.jcr.spring.test.components.NewsService;
import org.apache.portals.graffito.jcr.spring.test.model.Article;
import org.apache.portals.graffito.jcr.spring.test.model.News;

/**
 * Default implementation for {@link org.apache.portals.graffito.jcr.spring.test.components.ArticleService}
 * 
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Lombart Christophe </a>
 *
 */
public class ArticleServiceImpl implements ArticleService {
    private static final Log log = LogFactory.getLog(ArticleServiceImpl.class);

    private JcrMappingTemplate jcrMappingtemplate;
    private NewsService newsService;

    
    public void createArticle(Article article)
	{
    	
        jcrMappingtemplate.insert(article);
        jcrMappingtemplate.save();
        
        
        News news = new News();
        news.setContent("A new article has been created by " + article.getAuthor());
        news.setCreationDate(new Date());
        news.setPath("/news-" + System.currentTimeMillis());
        newsService.createNews(news);
	}
    
	public Collection getArticles()
	{

		QueryManager queryManager = jcrMappingtemplate.createQueryManager();
	    Filter filter = queryManager.createFilter(Article.class);
	    
	    Query query = queryManager.createQuery(filter);
		return jcrMappingtemplate.getObjects(query);
	}


	public NewsService getNewsService()
	{
		return newsService;
	}

	public void setNewsService(NewsService newsService)
	{
		this.newsService = newsService;
	}

	/**
     * @return Returns the template.
     */
    public JcrMappingTemplate getJcrMappingTemplate() {
        return jcrMappingtemplate;
    }

    /**
     * @param template The template to set.
     */
    public void setJcrMappingTemplate(JcrMappingTemplate template) {
        this.jcrMappingtemplate = template;
    }
    
    
    
}
