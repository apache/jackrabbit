<%@taglib uri="/taglib/jcr" prefix="jcr" %>
<%@taglib uri="/taglib/c" prefix="c" %>
<h1>Properties</h1>

<jcr:session>
	Session: <c:out value="${session.userId}"/><br>
<table cellpadding="10" border="1" > 
	<tr>
		<TH>Path</TH>
		<TH>Name</TH>
		<TH>Type</TH>
		<TH>Value</TH>
	</tr>
	<jcr:nodes node="/TestB" var="node">
		<jcr:properties node="${node}" var="property" sortExp="item.name">
			<tr>
				<TD><c:out value="${property.path}"/></TD>
				<TD><c:out value="${property.name}"/></TD>
				<TD>
				<c:if test="${!empty property.value}">
					<c:out value="${property.value.type}"/>
				</c:if>
				&nbsp;
				</TD>
				<TD>
				<c:if test="${!empty property.value}">
					<c:out value="${property.value.string}"/>
				</c:if>
				&nbsp;
				</TD>
			</tr>
		</jcr:properties>
	</jcr:nodes>
</table>	
</jcr:session>
