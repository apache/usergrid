<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@ page import="net.tanesha.recaptcha.ReCaptcha"%>
<%@ page import="net.tanesha.recaptcha.ReCaptchaFactory"%>
<%@ page import="org.apache.usergrid.rest.AbstractContextResource"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<!--
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
	<title>Reset Password</title>
	<link rel="stylesheet" type="text/css" href="/css/styles.css" />
</head>
<body>

	<div class="dialog-area">
		<c:if test="${!empty it.errorMsg}"><div class="dialog-form-message">${fn:escapeXml(it.errorMsg)}</div></c:if>
		<form class="dialog-form" action="" method="post">
			<input type="hidden" name="token" value="${fn:escapeXml(it.token)}">
			<fieldset>
				<p>
					<label for="password1">Please enter your new password for <c:out value="${it.user.email}" escapeXml="true"/>.</label>
				</p>
				<p>
					<input class="text_field" id="password1" name="password1" type="password" />
				</p>
				<p>
					<label for="password2">Enter your new password again to confirm.</label>
				</p>
				<p>
					<input class="text_field" id="password2" name="password2" type="password" />
				</p>
				<p class="buttons">
					<button type="submit">Submit</button>
				</p>
			</fieldset>
		</form>
	</div>

</body>
</html>
