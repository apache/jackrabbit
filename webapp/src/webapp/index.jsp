<%@ page import="org.apache.jackrabbit.server.simple.WebdavServlet,
		 javax.jcr.Repository,
		 org.apache.jackrabbit.client.RepositoryAccessServlet"%><%
%><html>
<head>
<title>Jackrabbit Examples</title>
</head>
<body>
<ul>
<li><a href="<%= request.getContextPath() %><%= WebdavServlet.getPathPrefix() %>/">Repository Browser</a></li>
<%--
<li><a href="<%= request.getContextPath() %>/example.jsp">Repository Servlet Example</a></li>
--%>
</ul>
<%
    Repository rep = RepositoryAccessServlet.getRepository();

%><hr size="1"><em>Powered by <a href="<%= rep.getDescriptor(Repository.REP_VENDOR_URL_DESC) %>"><%= rep.getDescriptor(Repository.REP_NAME_DESC)%></a> version <%= rep.getDescriptor(Repository.REP_VERSION_DESC) %>.</em>
</body>
</html>