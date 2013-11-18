<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
	<title>Reset Password</title>
	<link rel="stylesheet" type="text/css" href="/css/styles.css" />
</head>
<body>

	<div class="dialog-area">
		<c:if test="${!empty it.errorMsg}"><div class="dialog-form-message">${it.errorMsg}</div></c:if>
		<form class="dialog-form" action="" method="post">
			<fieldset>
				<p>
					<label for="email">Please type your <strong>email
							address</strong> or <strong>username</strong> below.</label>
				</p>
				<p>
					<input class="text_field" id="email" name="email" type="text" />
				</p>
				<p id="human-proof"></p>
				${it.reCaptchaHtml}
				<p class="buttons">
					<button type="submit">Submit</button>
				</p>
			</fieldset>
		</form>
	</div>

</body>
</html>