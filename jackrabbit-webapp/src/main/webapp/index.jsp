<%@ page import="org.apache.jackrabbit.j2ee.JCRWebdavServerServlet,
		 org.apache.jackrabbit.j2ee.RepositoryAccessServlet,
		 org.apache.jackrabbit.j2ee.SimpleWebdavServlet,
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
%><html>
<head>
<title>Jackrabbit JCR-Server</title>
</head>
<body style="font-family:monospace">
<%
    Repository rep;
    try {
        rep = RepositoryAccessServlet.getRepository(pageContext.getServletContext());
    } catch (Throwable e) {
        %>Error while accessing the repository: <font color="red"><%= e.getMessage() %></font><br><%
        %>Check the configuration or use the <a href="admin/">easy setup</a> wizard.<%
        return;
    }

%><h1>JCR-Server</h1>
<h3>JCR-Server provides 2 views</h3><p/>
<ol>
    <li>
        <b>Filebased View</b> (SimpleWebdavServlet)<p/>
        Filebased ("Simple") WebDAV View to the JSR170 repository.<p/>
        Enter the following URL to your WebDAV client:<br>
        http://&lt;<i>host</i>&gt;:&lt;<i>port</i>&gt;<%= request.getContextPath() %><%= SimpleWebdavServlet.getPathPrefix(pageContext.getServletContext()) %>/&lt;<i>workspace name</i>&gt;/
        <p/>
        <ul>
            <li><a href="<%= request.getContextPath() %><%= SimpleWebdavServlet.getPathPrefix(pageContext.getServletContext()) %>/default/">Browser View</a></li>
            <li>Context Path: <%= request.getContextPath() %></li>
            <li>Resource Path Prefix: <%= SimpleWebdavServlet.getPathPrefix(pageContext.getServletContext()) %></li>
            <li>Workspace Name: see /WEB-INF/repository/repository.xml (Default = 'default')</li>
            <li>Source: /jcr-server/server/webdav/simple</li>
        </ul>
        <p/>
    </li>
    <li>
        <b>Item View</b> (JCRServerServlet)<p/>
        Itembased WebDAV View to the JSR170 repository, mapping the functionality
        provided by JSR170 to WebDAV, in order to allow remoting of JSR170 via
        WebDAV. Some more details regarding remoting are available as initial
        draft "<a href="http://www.day.com/jsr170/server/JCR_Webdav_Protocol.zip">JCR_Webdav_Protocol.zip</a>".
        In addition the implementation attempts to cover functionality of RFC 2518 and
        its extensions wherever possible, namely<br>
        <ul>
            <li><a href="http://www.ietf.org/rfc/rfc2518.txt">RFC 2518 (WebDAV 1,2)</a></li>
            <li><a href="http://www.ietf.org/rfc/rfc3253.txt">RFC 3253 (DeltaV)</a></li>
            <li><a href="http://www.ietf.org/rfc/rfc3648.txt">RFC 3648 (Ordering)</a></li>
            <li><a href="http://greenbytes.de/tech/webdav/draft-reschke-webdav-search-latest.html">Internet Draft WebDAV Search</a>.</li>
        </ul>
        <p/>
        Enter the following URL to your WebDAV client:<br>
        http://&lt;<i>host</i>&gt;:&lt;<i>port</i>&gt;<%= request.getContextPath() %><%= JCRWebdavServerServlet.getPathPrefix(pageContext.getServletContext()) %>/
        <p/>
        <ul>
            <li>Browser View: - Not Available - ("<%= request.getContextPath() %><%= JCRWebdavServerServlet.getPathPrefix(pageContext.getServletContext()) %>/")</li>
            <li>Context Path: <%= request.getContextPath() %></li>
            <li>Resource Path Prefix: <%= JCRWebdavServerServlet.getPathPrefix(pageContext.getServletContext()) %></li>
            <li>Workspace Name: - Not required - (available workspaces are mapped as resources)</li>
            <li>Source: /jcr-server/server/webdav/jcr</li>
        </ul>
    </li>
</ol>
<p/>
<hr size="1"><em>Powered by <a href="<%= rep.getDescriptor(Repository.REP_VENDOR_URL_DESC) %>"><%= rep.getDescriptor(Repository.REP_NAME_DESC)%></a> version <%= rep.getDescriptor(Repository.REP_VERSION_DESC) %>.</em>
</body>
</html>
