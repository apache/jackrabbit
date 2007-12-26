<%--
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
