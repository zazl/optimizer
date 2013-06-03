<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>
<%@ page import="org.dojotoolkit.optimizer.JSOptimizer" %>
<%@ page import="org.dojotoolkit.optimizer.servlet.JSURLGenerator" %>
<!DOCTYPE html>
<html>
<head>
    <title>AMD Calendar Example via JSP/Zazl Optimizer</title>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<style type="text/css">
		@import "dojo/resources/dojo.css";
		@import "dijit/themes/claro/claro.css";
	</style>
	<%
		JSOptimizer jsOptimizer = (JSOptimizer)pageContext.getServletContext().getAttribute("org.dojotoolkit.optimizer.JSOptimizer");
		if (jsOptimizer == null) {
			throw new JspException("A JSOptimizer  has not been loaded into the servlet context");
		}
		JSURLGenerator urlGenerator = new JSURLGenerator(jsOptimizer, request.getLocale(), request.getContextPath());
		String configString = "{\"packages\": [{\"name\": \"dojo\"},{\"name\": \"dijit\"},{\"name\": \"dojox\"}], \"config\": {\"amdtest/Calendar\": {\"myconfigval\": \"My Config Value\"}}}";
	%>
	<script type="text/javascript">
        var dojoConfig = {
            locale : "<%=request.getLocale().toString().toLowerCase().replace('_', '-')%>"
		};
        var zazlConfig = <%=configString%>;
	</script>
	<script type="text/javascript" src="<%=urlGenerator.generateURL("amdtest/Calendar", configString)%>"></script>
	<script type="text/javascript">
        require(["amdtest/Calendar"], 
        function(calendar) {
            console.log("done");
        });
	</script>
</head>
<body class="claro">
<div id="calendarNode"></div>
</body>
</html>