<%@taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@taglib uri="http://jakarta.apache.org/taglib/string" prefix="str" %>
<%@taglib uri="http://jackrabbit.apache.org/jcr-taglib" prefix="jcr" %>
<%@page import="javax.jcr.PropertyType"%>
<% 
pageContext.setAttribute("path", request.getParameter("path")); 
pageContext.setAttribute("jcrsession", session.getAttribute("jcr.session"));
%>
[<% int index = 0 ;%>
<%@page import="javax.jcr.Property"%>
<jcr:set var="parent" item="${path}"/>
<c:forEach var="node" items="${parent.nodes}">
{
Id:'<str:escape><c:out value="${node.path}"/></str:escape>',
Index:<%= index++ %>,
Node:'<str:escape><c:out value="${node.node}"/></str:escape>',
Name:'<a href="<c:url value="/repository/default"/><str:escape><c:out value="${node.path}"/></str:escape>" target="_new"><str:escape><c:out value="${node.name}"/></str:escape></a>',
Value:'-',
Type:'<str:escape><c:out value="${node.primaryNodeType.name}"/></str:escape>',
New:'<c:out value="${node.new}"/>',
Modified:'<c:out value="${node.modified}"/>'
},
</c:forEach>
<% int nodesIndex = index ;%>
<c:forEach var="prop" items="${parent.properties}">
<% 
Property prop = (Property) pageContext.getAttribute("prop") ;
if (nodesIndex!=index) {%>,<%}%>
{
	Id:'<str:escape><c:out value="${prop.path}"/></str:escape>',
	Index:<%= index++ %>,
	Node:'<str:escape><c:out value="${prop.node}"/></str:escape>',
	Name:'<str:escape><c:out value="${prop.name}"/></str:escape>',
<c:choose >
	<c:when test="${!prop.definition.multiple and prop.length <1000}">
		<c:set var="value"><jcr:out item="${prop}"/></c:set>
		Value:'<str:escape><c:out value="${value}" escapeXml="true"/></str:escape>',
	</c:when> 
	<c:otherwise>
		Value:'<font color="red">Preview unavailable. Value is multiple or longer than 1000.</font>',
	</c:otherwise> 
</c:choose>

	Type:'<str:escape><%= PropertyType.nameFromValue(prop.getType()) %></str:escape>',
	New:'<c:out value="${prop.new}"/>',
	Modified:'<c:out value="${prop.modified}"/>'
}
</c:forEach>
]
