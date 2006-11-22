<%@taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@taglib uri="http://jakarta.apache.org/taglib/string" prefix="str" %>
<%@taglib uri="http://jackrabbit.apache.org/jcr-taglib" prefix="jcr" %>
<%@page import="javax.jcr.Session"%>
<%
pageContext.setAttribute("jcrsession",session.getAttribute("jcr.session"));
%>
<div class="dialog">
<h3>Session - Attributes</h3>
<hr height="1"/>
<table class="dialog">
<c:forEach var="name" items="${jcrsession.attributeNames}">
<tr>
<td><c:out value="${name}"/></td>
<td><%= ((Session) pageContext.getAttribute("jcrsession")).getAttribute(pageContext.getAttribute("name").toString())%></td>
</tr>
</c:forEach>
</table>
<hr height="1"/>
<input type="button" value="Close" onClick="hideDialog();"/>
</div>
