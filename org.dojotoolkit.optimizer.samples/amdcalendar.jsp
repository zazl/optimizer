<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>
<%@ page import="org.dojotoolkit.optimizer.JSOptimizer" %>
<%@ page import="org.dojotoolkit.optimizer.servlet.JSURLGenerator" %>
<!DOCTYPE html>
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<style type="text/css">
		@import "dojo/resources/dojo.css";
		@import "dijit/themes/claro/claro.css";
	</style>
	<script type="text/javascript">
		require = {
            packages: [
                {
                    name: 'dojo',
                    location: 'dojo',
                    main:'lib/main-browser',
                    lib: '.'
                },
                {
                    name: 'dijit',
                    location: 'dijit',
                    main:'lib/main',
                    lib: '.'
                }
            ],
            paths: {
                require: 'requirejs/require'
            },
			ready: function () {
                require(['amdtest/Calendar']);
			},
			locale : "<%=request.getLocale().toString().toLowerCase().replace('_', '-')%>"
		};
	</script>
	<%
		boolean debug = (request.getParameter("debug") == null) ? false : Boolean.valueOf(request.getParameter("debug"));
		String url = null;
		if (debug) {
			url = request.getContextPath() +"/_javascript?debug=true";
		} else {
		    JSOptimizer jsOptimizer = (JSOptimizer)pageContext.getServletContext().getAttribute("org.dojotoolkit.optimizer.JSOptimizer");
		    if (jsOptimizer == null) {
		    	throw new JspException("A JSOptimizer  has not been loaded into the servlet context");
		    }
		   	JSURLGenerator urlGenerator = new JSURLGenerator(jsOptimizer, request.getLocale(), request.getContextPath());
		   	url = urlGenerator.generateURL("amdtest/Calendar");
		}
	%>
	<script type="text/javascript" src="<%=url%>"></script>

</head>
<body class="claro">
<div id="calendarNode"></div>
</body>
</html>