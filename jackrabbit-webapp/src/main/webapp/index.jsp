<%@ page import="org.apache.jackrabbit.j2ee.JCRWebdavServerServlet,
		 org.apache.jackrabbit.j2ee.RepositoryAccessServlet,
		 org.apache.jackrabbit.j2ee.SimpleWebdavServlet,
		 java.net.URI,
                 javax.jcr.Repository"%><%
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
%>
<html>
<head>
<title>Welcome to Apache Jackrabbit - WebDAV Server</title>
<link rel="shortcut icon" href="<%= request.getContextPath() %>/images/favicon.ico" type="image/vnd.microsoft.icon">
<style type="text/css" media="all">
      @import url("<%= request.getContextPath() %>/css/default.css");
</style>
</head>
<body>
<%
    Repository rep;
    try {
        rep = RepositoryAccessServlet.getRepository(pageContext.getServletContext());
    } catch (Throwable e) {
        %>Error while accessing the repository: <font color="red"><%= e.getMessage() %></font><br><%
        %>Check the configuration or use the <a href="admin/">easy setup</a> wizard.<%
        return;
    }

%>
<div id="bodyColumn">
<a href="http://jackrabbit.apache.org"><img src="<%= request.getContextPath() %>/images/jackrabbitlogo.gif" alt="" /></a><br>
<h2>Jackrabbit WebDAV Server</h2>
<p>
Welcome to the Jackrabbit WebDAV Server.
It currently provides 2 WebDAV servlets that allow distinct views to the JCR 
repository:

<ol>
<li><a href="#simple">Standard WebDAV Server</a></li>
<li><a href="#remoting">JCR WebDAV Server</a></li>
</ol>
</p>
<p>
See also the Jackrabbit <a href="http://jackrabbit.apache.org/doc/components/jcr-server.html">Jcr-Server</a> 
component for further information.</p>

<a name="simple"></a>
<h3>Standard WebDAV Server</h3><br>

The default WebDAV server (aka: Simple Server) represents a DAV1,2 and DeltaV compliant WebDAV 
server implementation. It covers a filebase view to the JCR repository, suitable 
for everybody looking for standard WebDAV functionality.

<p>
Enter the following URL to your WebDAV client:
</p>
<p>
<%
URI uri = new URI(request.getRequestURL().toString());
String href = uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort() + request.getContextPath() + SimpleWebdavServlet.getPathPrefix(pageContext.getServletContext()) + "/default/";
%>
<a href="<%= href %>"><%= href %></a>

</p>
<p>
Note, that <i>/default</i> is an assumption regarding the name of your default workspace such as 
configured in the <i>repository.xml</i>.
</p>

<br><h5>Links</h5>
<ul>
    <li><a href="<%= request.getContextPath() %><%= SimpleWebdavServlet.getPathPrefix(pageContext.getServletContext()) %>/default/">Browser View</a></li>
    <li><a href="<%= request.getContextPath() %>/search.jsp">Search</a> for files in the default workspace.</li>
    <li>Nothing to search for?<br><a href="<%= request.getContextPath() %>/populate.jsp">Populate</a> the default workspace with content.</li>
</ul>

<br><h5>Supported WebDAV functionality</h5>
<ul>
<li><a href="http://www.ietf.org/rfc/rfc2518.txt">RFC 2518 (WebDAV 1,2)</a></li>
<li><a href="http://www.ietf.org/rfc/rfc3253.txt">RFC 3253 (DeltaV)</a></li>
</ul> 

<br><h5>Configuration</h5>
<ul>
    <li>Context path: <%= request.getContextPath() %></li>
    <li>Resource path prefix: <%= SimpleWebdavServlet.getPathPrefix(pageContext.getServletContext()) %></li>
    <li>Workspace name: see <i>/WEB-INF/repository/repository.xml</i><br>
        The default value = 'default'</li>
    <li>Additional servlet configuration: see <i>/WEB-INF/web.xml</i></li>
    <li>WebDAV specific resource configuration: see <i>/WEB-INF/config.xml</i></li>
</ul>



<a name="remoting"></a>

<h3>JCR WebDAV Server</h3><br>

Itembased WebDAV View to the JCR repository, mapping the functionality
provided by JSR 170 to the WebDAV protocol in order to allow remoting of JSR170 via
WebDAV. Some more details regarding remoting are available as initial
draft "<a href="http://www.day.com/jsr170/server/JCR_Webdav_Protocol.zip">JCR_Webdav_Protocol.zip</a>".

<p>
Enter one of the following URLs to your WebDAV client:

<ul>
<li>
<%
uri = new URI(request.getRequestURL().toString());
href = uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort() + request.getContextPath() + JCRWebdavServerServlet.getPathPrefix(pageContext.getServletContext());
%>
<a href="<%= href %>"><%= href %></a><br>
to expose all workspaces of your JCR repository
</li>
<li>
<%
String shref = href + "/default/jcr:root";
%>
<a href="<%= shref %>"><%= shref %></a><br>
to expose a single workspace (example with workspace named 'default') of your JCR repository.
</li>
</ul>
</p>
<p>
Note, that <i>/default</i> is an assumption regarding the name of your default workspace such as 
configured in the <i>repository.xml</i>.
</p>
<br><h5>Links</h5>
<ul>
    <li><a href="<%= href %>">Browser View</a></li>
</ul>


<br><h5>Supported WebDAV functionality</h5>
<p>
This implementation focuses on remoting rather than standard WebDAV functionality
or compatibility with existing WebDAV clients.
The following RFCs are used to implement the remoting functionality:
<ul>
    <li><a href="http://www.ietf.org/rfc/rfc2518.txt">RFC 2518 (WebDAV 1,2)</a></li>
    <li><a href="http://www.ietf.org/rfc/rfc3253.txt">RFC 3253 (DeltaV)</a></li>
    <li><a href="http://www.ietf.org/rfc/rfc3648.txt">RFC 3648 (Ordering)</a></li>
    <li><a href="http://greenbytes.de/tech/webdav/draft-reschke-webdav-search-latest.html">Internet Draft WebDAV Search</a>.</li>
</ul>
For the client counterpart of this WebDAV servlet please take a look at
the <a href="https://svn.apache.org/repos/asf/jackrabbit/sandbox/spi/spi2dav">Spi2Dav</a> sandbox project.
</p>

<br><h5>Configuration</h5>
<ul>
    <li>Context Path: <%= request.getContextPath() %></li>
    <li>Resource Path Prefix: <%= JCRWebdavServerServlet.getPathPrefix(pageContext.getServletContext()) %></li>
    <li>Workspace Name: <i>optional</i> (available workspaces are mapped as resources)</li>
    <li>Additional servlet configuration: see <i>/WEB-INF/web.xml</i></li>
</ul>
</div>
<div id="footer">
<em>Powered by <a href="<%= rep.getDescriptor(Repository.REP_VENDOR_URL_DESC) %>"><%= rep.getDescriptor(Repository.REP_NAME_DESC)%></a> version <%= rep.getDescriptor(Repository.REP_VERSION_DESC) %>.</em>
</div>
</body>
</html>
