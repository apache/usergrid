package org.usergrid.rest.management.metrics;

import com.sun.jersey.api.json.JSONWithPadding;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.usergrid.rest.AbstractContextResource;
import org.usergrid.rest.ApiResponse;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Context;

/**
 * @author zznate
 */
@Component("org.usergrid.rest.management.metrics.MetricsResource")
@Scope("prototype")
@Produces({ MediaType.APPLICATION_JSON })
public class MetricsResource extends AbstractContextResource {

  public MetricsResource() {

  }

  @GET
  @Path("all")
  public JSONWithPadding getDeveloperMetrics(@Context UriInfo ui) {

    ApiResponse response = new ApiResponse(ui);
    response.setAction("get developer metrics");

    return new JSONWithPadding(response);
  }

}
