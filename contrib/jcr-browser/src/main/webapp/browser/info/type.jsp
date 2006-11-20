<%@taglib uri="http://jackrabbit.apache.org/jcr-taglib" prefix="jcr" %>
<%@taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<% 
pageContext.setAttribute("path", request.getParameter("path")); 
pageContext.setAttribute("jcrsession",session.getAttribute("jcr.session"));
%>
<%@page import="javax.jcr.Node"%>
<jcr:set var="item" item="${path}"/>
<table  width="100%" class="itemInfo" style="valign: top;">
<c:if test="${item.node}">
<% Node node = (Node) pageContext.getAttribute("item"); %>
  <tr>
    <td>Primary node type: <c:out value="${item.primaryNodeType.name}"/></td>
    <td>Has Orderable Child Nodes: <%= node.getPrimaryNodeType().hasOrderableChildNodes() %></td>
    <td>Row 1: Col 2</td>
    <td>Row 1: Col 2</td>
  </tr>
</c:if>
<c:if test="${!item.node}">
  <tr>
    <td>Row 1: Col 1</td>
    <td>Row 1: Col 2</td>
  </tr>
</c:if>
</table>
