<%@taglib uri="http://jackrabbit.apache.org/jcr-taglib" prefix="jcr" %>
<%@taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<% 
pageContext.setAttribute("path", request.getParameter("path")); 
pageContext.setAttribute("jcrsession",session.getAttribute("jcr.session"));
%>
<%@page import="javax.jcr.version.OnParentVersionAction"%>
<%@page import="javax.jcr.Node"%>
<%@page import="javax.jcr.PropertyType"%>
<jcr:set var="item" item="${path}"/>
<!-- Item definition -->
<table  width="100%" class="itemInfo" style="valign: top;">
  <tr>
    <td>Node type: <c:out value="${item.definition.name}"/></td>
    <td>Declaring node type: <c:out value="${item.definition.declaringNodeType.name}"/></td>
    <td>Autocreated: <c:out value="${item.definition.autoCreated}"/></td>
    <td>Mandatory: <c:out value="${item.definition.mandatory}"/></td>
  </tr>
  <tr>
    <td>OPV:
<c:set var="opv"><c:out value="${item.definition.onParentVersion}"/></c:set>
<%= OnParentVersionAction.nameFromValue(Integer.valueOf(pageContext.getAttribute("opv").toString()))%>
</td>
    <td>Protected: <c:out value="${item.definition.protected}"/></td>
    <td></td>
    <td></td>
  </tr>
<c:if test="${item.node}">
<% Node node = (Node) pageContext.getAttribute("item"); %>
  <tr>
    <td>Required primary types: <br/>
<c:forEach items="${item.definition.requiredPrimaryTypes}" var="type">
<c:out value="${type.name}"/><br/>
</c:forEach>
</td>
    <td>Default Primary type: <c:out value="${item.definition.defaultPrimaryType.name}"/></td>
    <td>Allows same name siblings: <%= node.getDefinition().allowsSameNameSiblings() %></td>
    <td></td>
  </tr>
</c:if>
<c:if test="${!item.node}">
  <tr>
<c:set var="requiredType"><c:out value="${item.definition.requiredType}"/></c:set>
    <td>Required type: <%= PropertyType.nameFromValue(Integer.valueOf(pageContext.getAttribute("requiredType").toString())) %></td>
    <td>Value constraints: <br/>
<c:forEach items="${item.definition.valueConstraints}" var="constraint">
<c:out value="${constraint}"/><br/>
</c:forEach>
	</td>
    <td>Default values: <br/>
<c:forEach items="${item.definition.defaultValues}" var="defaultValue">
<c:out value="${defaultValue}"/><br/>
</c:forEach>
	</td>
    <td>Multiple: <c:out value="${item.definition.multiple}"/></td>
  </tr>
</c:if>
</table>
