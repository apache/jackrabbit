<%--
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
--%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
          "http://www.w3.org/TR/html4/loose.dtd">
<html>
  <head>
    <title><%= request.getAttribute("title") %></title>
    <link rel="stylesheet"
          href="<%= request.getContextPath() %>/css/default.css"
          type="text/css"/>
    <link rel="shortcut icon"
          href="<%= request.getContextPath() %>/images/favicon.ico"
          type="image/vnd.microsoft.icon" />
  </head>
  <body>
    <div id="page">
      <div id="banner">
        <p id="jcr">
          <a href="<%= request.getContextPath() %>/">
            <img src="<%= request.getContextPath() %>/images/jlogo.gif"
                 alt="Apache Jackrabbit" height="100" width="336"/>
          </a>
        </p>
        <p id="asf">
          <a href="http://www.apache.org/">
            <img src="<%= request.getContextPath() %>/images/asf-logo.gif"
                 alt="Apache Software Foundation" height="100" width="387"/>
          </a>
        </p>
      </div>
      <div id="navigation">
        <ul>
          <li>Jackrabbit JCR Server
            <ul>
              <li><a href="<%= request.getContextPath() %>/">Welcome</a></li>
              <li><a href="<%= request.getContextPath() %>/webdav-simple.jsp">Standard WebDAV</a></li>
              <li><a href="<%= request.getContextPath() %>/webdav-jcr.jsp">JCR WebDAV</a></li>
              <li><a href="<%= request.getContextPath() %>/troubleshooting.jsp">Troubleshooting</a></li>
            </ul>
          </li>
          <li>Default workspace
            <ul>
              <li><a href="<%= request.getContextPath() %>/repository/default/">Browse</a></li>
              <li><a href="<%= request.getContextPath() %>/search.jsp">Search</a></li>
              <li><a href="<%= request.getContextPath() %>/populate.jsp">Populate</a></li>
            </ul>
          </li>
          <li>Apache Jackrabbit
            <ul>
              <li><a href="http://jackrabbit.apache.org/">Apache Jackrabbit</a></li>
              <li><a href="http://jackrabbit.apache.org/api/1.4/">Jackrabbit API</a></li>
              <li><a href="http://wiki.apache.org/jackrabbit/FrontPage">Jackrabbit Wiki</a></li>
            </ul>
          </li>
          <li>JCR
            <ul>
              <li><a href="http://jcp.org/en/jsr/detail?id=170">JSR 170</a></li>
              <li><a href="http://jcp.org/en/jsr/detail?id=283">JSR 283</a></li>
              <li><a href="http://www.day.com/maven/jsr170/javadocs/jcr-1.0/">JCR API</a></li>
            </ul>
          </li>
        </ul>
      </div>
      <div id="content">
        <h2><%= request.getAttribute("title") %></h2>
