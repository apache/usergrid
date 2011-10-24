<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
	<title>Activate User</title>
	<link rel="stylesheet" type="text/css" href="../../../css/styles.css" />
</head>
<body>

	<p>Your account with email address <c:out value="${it.user.email}"/> has been successfully activated.</p>

</body>
</html>