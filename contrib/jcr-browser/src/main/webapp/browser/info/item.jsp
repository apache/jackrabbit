<%@taglib uri="http://jackrabbit.apache.org/jcr-taglib" prefix="jcr" %>
<%@taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<% 
pageContext.setAttribute("path", request.getParameter("path")); 
%>
<jcr:session>
<jcr:set var="node" item="${path}"/>
<table class="itemInfo">
 <tr><td width="150">path</td><td><c:out value="${node.path}"/></td></tr>
 <tr><td>New</td><td><c:out value="${node.new}"/></td></tr>
 <tr><td>Modified</td><td><c:out value="${node.modified}"/></td></tr>
 <tr><td>depth</td><td><c:out value="${node.depth}"/></td></tr>
<c:if test="${node.node}">
  <tr><td>corresponding paths</td><td></td></tr>
  <tr><td>index</td><td><c:out value="${node.index}"/></td></tr>
  <tr><td>child nodes</td><td><c:out value="${node.nodes.size}"/></td></tr>
  <tr><td>child properties</td><td><c:out value="${node.properties.size}"/></td></tr>
  <tr><td>primary item</td><td></td></tr>
  <tr><td>references</td><td><c:out value="${node.references.size}"/></td></tr>
  <tr><td>versionable</td><td></td></tr>
  <tr><td>is checked out</td><td></td></tr>
  <tr><td>base version</td><td></td></tr>
  <tr><td>lockable</td><td></td></tr>
  <tr><td>is locked</td><td></td></tr>
  <tr><td>holds lock</td><td></td></tr>
  <tr><td>locked node</td><td></td></tr>
  <tr><td>lock owner</td><td></td></tr>
  <tr><td>is deep</td><td></td></tr>
  <tr><td>token</td><td></td></tr>
  <tr><td>live</td><td></td></tr>
  <tr><td>scope</td><td></td></tr>
</c:if>
</table>
</jcr:session>