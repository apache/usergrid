package org.usergrid.rest;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.rest.utils.CORSUtils;

public class SwaggerServlet extends HttpServlet {

	public static final Logger logger = LoggerFactory
			.getLogger(SwaggerServlet.class);

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		String path = request.getServletPath();

		logger.info("Swagger request: " + path);
		String destination = "/WEB-INF/jsp/swagger" + path + ".jsp";

		CORSUtils.allowAllOrigins(request, response);

		RequestDispatcher rd = getServletContext().getRequestDispatcher(
				destination);
		rd.forward(request, response);

	}

}