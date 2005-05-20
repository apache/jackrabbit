<%@taglib uri="/taglib/jcr" prefix="jcr" %>
<%@taglib uri="/taglib/c" prefix="c" %>
<h1>Out</h1>

<jcr:session>
<table cellpadding="10" border="1" > 
	<tr>
		<TH>Node</TH>
		<TH>Property</TH>
		<TH>Node: Template engine output</TH>
		<TH>Property: Template engine output</TH>
	</tr>
	<jcr:nodes node="/TestB" var="node" traverserDepth="0">
		<jcr:properties node="${node}" var="property">
			<tr>
				<TD><c:out value="${property.path}"/></TD>
				<TD><c:out value="${property.name}"/></TD>
				<TD>
					<jcr:out item="${node}" />
				</TD>
				<TD>
					<jcr:out item="${node}" property="jcr:primaryType" />
				</TD>
			</tr>
		</jcr:properties>
	</jcr:nodes>
</table>		
</jcr:session>