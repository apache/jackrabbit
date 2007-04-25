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
package org.apache.portals.graffito.jcr.testmodel;

import java.io.InputStream;
import java.util.Calendar;
/**
 * Java class used to map the jcr node type nt:resource
 * @author <a href="mailto:christophe.lombart@gmail.com">Lombart Christophe </a>
 *
 */
public class Resource
{

    private String mimeType;
    private String encoding;
    private InputStream data;
    private Calendar lastModified;
    
    public InputStream getData()
    {
        return data;
    }
    public void setData(InputStream data)
    {
        this.data = data;
    }
    public String getEncoding()
    {
        return encoding;
    }
    public void setEncoding(String encoding)
    {
        this.encoding = encoding;
    }
    public Calendar getLastModified()
    {
        return lastModified;
    }
    public void setLastModified(Calendar lastModified)
    {
        this.lastModified = lastModified;
    }
    public String getMimeType()
    {
        return mimeType;
    }
    public void setMimeType(String mimeType)
    {
        this.mimeType = mimeType;
    }
    
    
}
