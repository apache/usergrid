package org.usergrid.rest.security.shiro.filters;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.usergrid.security.shiro.PrincipalCredentialsToken;
import org.usergrid.security.shiro.utils.SubjectUtils;

import com.sun.jersey.spi.container.ContainerRequest;

@Component
public class ClientCredentialsSecurityFilter extends SecurityFilter {

	private static final Logger logger = LoggerFactory
			.getLogger(ClientCredentialsSecurityFilter.class);

	@Context
	protected HttpServletRequest httpServletRequest;

	public ClientCredentialsSecurityFilter() {
		logger.info("ClientCredentialsSecurityFilter is installed");
	}

	@Override
	public ContainerRequest filter(ContainerRequest request) {
		String clientId = httpServletRequest.getParameter("client_id");
		String clientSecret = httpServletRequest.getParameter("client_secret");

		if (isNotBlank(clientId) && isNotBlank(clientSecret)) {
			try {
				PrincipalCredentialsToken token = management
						.getPrincipalCredentialsTokenForClientCredentials(
								clientId, clientSecret);
				Subject subject = SubjectUtils.getSubject();
				subject.login(token);
			} catch (Exception e) {
			}
		}
		return request;
	}

}
