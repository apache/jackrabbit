<%@taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@taglib uri="http://jakarta.apache.org/taglib/string" prefix="str" %>
<%@taglib uri="http://jackrabbit.apache.org/jcr-taglib" prefix="jcr" %>
<c:set var="type" scope="request">date</c:set>
<c:set var="editor" scope="request">
<div id="test" dojoType="datepicker"></div>
</c:set>
<c:import url="setproperty.jsp"></c:import>
