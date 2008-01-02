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
<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
          "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Error: JCR API not found</title>
<link rel="shortcut icon" href="<%= request.getContextPath() %>/images/favicon.ico" type="image/vnd.microsoft.icon">
<style type="text/css" media="all">
      @import url("<%= request.getContextPath() %>/css/default.css");
</style>
</head>
<body>
<div id="bodyColumn">
<a href="http://jackrabbit.apache.org"><img src="<%= request.getContextPath() %>/images/jackrabbitlogo.gif" alt="" /></a><br>
<h1>Error: JCR API not found</h1>
<p>
The <code>javax.jcr.Repository</code> interface from the JCR API could not
be loaded. To resolve this issue, you need to make the jcr-1.0.jar available
in the shared classpath of the servlet container.
</p>
</div>
</body>
</html>
