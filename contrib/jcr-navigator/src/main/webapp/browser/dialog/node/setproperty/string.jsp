<%@taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@taglib uri="http://jakarta.apache.org/taglib/string" prefix="str" %>
<%@taglib uri="http://jackrabbit.apache.org/jcr-taglib" prefix="jcr" %>
<c:set var="type" scope="request">string</c:set>
<c:set var="editor" scope="request">
	<input type="hidden" name="type" value="String"/>
	<textarea name="value" value="" cols="30" rows="10"/></textarea>
</c:set>
<c:import url="setproperty.jsp"></c:import>