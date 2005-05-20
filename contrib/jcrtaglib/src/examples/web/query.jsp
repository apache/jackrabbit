<%@taglib uri="/taglib/jcr" prefix="jcr" %>
<%@taglib uri="/taglib/c" prefix="c" %>
<h1>Query tag</h1>

<%
 String q = "SELECT * FROM nt:base WHERE jcr:path LIKE '/TestA/%'" ;
%>

<jcr:session>

<h3>SQL: <%=q%> </h3>
<jcr:query stmt="<%= q %>" var="node" lang="sql">
	Node: <c:out value="${node.path}"/><br>
</jcr:query>

<h3>XPATH: //* </h3>
<jcr:query stmt="//*" var="node" lang="xpath">
	Node: <c:out value="${node.path}"/><br>
</jcr:query>

</jcr:session>

