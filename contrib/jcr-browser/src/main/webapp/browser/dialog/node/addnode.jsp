<%@taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@taglib uri="http://jakarta.apache.org/taglib/string" prefix="str" %>
<%@taglib uri="http://jackrabbit.apache.org/jcr-taglib" prefix="jcr" %>
<% 
pageContext.setAttribute("path", request.getParameter("path")); 
%>
<jcr:session>
<jcr:set var="node" item="${path}"/>
<c:if test="${!node.node}">
	<jcr:set var="node" item="${node.parent}"/>
</c:if>
<div class="dialog">
<h3>Node - Add node</h3>
<hr height="1"/>
<form action="response.txt" id="dialogForm">
<table class="dialog">
<tr>
	<th>Parent</th>
	<td><c:out value="${node.path}"/></td>
</tr>
<tr>
	<th>Namespace</th>
	<td>
	<select name="namespace">
	<c:forEach var="prefix" items="${jcrsession.namespacePrefixes}">
<c:if test="${empty prefix}"><option selected="selected"><c:out value="${prefix}"/></option></c:if>
<c:if test="${!empty prefix}"><option><c:out value="${prefix}"/></option></c:if>		
	</c:forEach>
	</select>
	</td>
</tr>
<tr>
	<th>Primary node type</th>
	<td>
	<select name="nodetype">
	<c:forEach var="nt" items="${jcrsession.workspace.nodeTypeManager.primaryNodeTypes}">
<c:if test="${nt.name eq 'nt:unstructured'}"><option selected="selected"><c:out value="${nt.name}"/></option></c:if>
<c:if test="${nt.name ne 'nt:unstructured'}"><option><c:out value="${nt.name}"/></option></c:if>
	</c:forEach>
	</select>
	</td>
</tr>
<tr>
	<th>Name</th>
	<td><input type="text" name="name"/></td>
</tr>
<tr>
	<td colspan="2">
<hr height="1"/>
<input type="button" value="Submit" onClick="submitDialog();"/>
<input type="button" value="Close" onClick="hideDialog();"/>
	</td>
</tr>

</table>
</form>
</div>
</jcr:session>