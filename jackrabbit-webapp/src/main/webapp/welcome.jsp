<%@ page import="org.apache.jackrabbit.j2ee.JCRWebdavServerServlet,
                 org.apache.jackrabbit.j2ee.RepositoryAccessServlet,
                 org.apache.jackrabbit.j2ee.SimpleWebdavServlet,
                 java.net.URI,
                 javax.jcr.Repository"
%><%--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
--%><%

Repository rep;
try {
    rep = RepositoryAccessServlet.getRepository(pageContext.getServletContext());
} catch (Throwable e) {
    log("Error accesing the repository", e);
    %><jsp:forward page="bootstrap/missing.jsp"/><%
}

request.setAttribute("title", "Apache Jackrabbit JCR Server");
%><jsp:include page="header.jsp"/>
<p>
  Welcome to the Apache Jackrabbit JCR Server. This web application
  contains a JCR content repository and makes it available to clients
  through WebDAV and other means.
</p>
<p>
  The following two WebDAV views are provided for accessing the
  content in the JCR content repository.
</p>
<ul>
  <li><a href="webdav-simple.jsp">Standard WebDAV</a></li>
  <li><a href="webdav-jcr.jsp">JCR WebDAV</a></li>
</ul>
<p>
  Clients can also access the repository using the JCR API. Both local
  and remote access is supported.
</p>
<ul>
  <li><a href="remote.jsp">Remote repository access</a></li>
  <li><a href="local.jsp">Local repository access</a></li>
</ul>

<h3>About Apache Jackrabbit</h3>
<p>
  <a href="http://jackrabbit.apache.org/">Apache Jackrabbit</a> is a fully
  conforming implementation of the Content Repository for Java Technology API
  (JCR). A content repository is a hierarchical content store with support for
  structured and unstructured content, full text search, versioning,
  transactions, observation, and more. Typical applications that use content
  repositories include content management, document management, and records
  management systems.
</p>
<p>
  Version 1.0 of the JCR API was specified by the
  <a href="http://jcp.org/en/jsr/detail?id=170">Java Specification Request 170</a>
  (JSR 170) and work on the JCR version 2.0 has begun in
  <a href="http://jcp.org/en/jsr/detail?id=170">JSR 283</a>.
</p>
<p>
  Apache Jackrabbit is a project of the
  <a href="http://www.apache.org/">Apache Software Foundation</a>. 
</p>
<jsp:include page="footer.jsp"/>
