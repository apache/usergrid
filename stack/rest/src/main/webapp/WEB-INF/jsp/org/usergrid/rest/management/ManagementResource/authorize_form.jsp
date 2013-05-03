<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@ page import="org.usergrid.rest.AbstractContextResource"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
	<title>Sign In</title>
	<link rel="stylesheet" type="text/css" href="/css/styles.css" />
</head>
<body>

	<div class="dialog-area">
		<c:if test="${!empty it.errorMsg}"><div class="dialog-form-message">${it.errorMsg}</div></c:if>
		<form class="dialog-form" action="" method="post">
			<input type="hidden" name="response_type" value="${it.responseType}">
			<input type="hidden" name="client_id" value="${it.clientId}">
			<input type="hidden" name="redirect_uri" value="${it.redirectUri}">
			<input type="hidden" name="scope" value="${it.scope}">
			<input type="hidden" name="state" value="${it.state}">
			<fieldset>
				<p>
					<label for="username">Username</label>
				</p>
				<p>
					<input class="text_field" id="username" name="username" type="text" />
				</p>
				<p>
					<label for="password">Password</label>
				</p>
				<p>
					<input class="text_field" id="password" name="password" type="password" />
				</p>
				<p class="buttons">
					<button type="submit">Submit</button>
				</p>
			</fieldset>
		</form>
	</div>

</body>
</html>