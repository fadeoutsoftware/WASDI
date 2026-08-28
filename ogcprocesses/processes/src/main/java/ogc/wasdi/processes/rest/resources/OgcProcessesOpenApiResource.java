package ogc.wasdi.processes.rest.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.integration.OpenApiContextLocator;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import io.swagger.v3.oas.models.OpenAPI;
import wasdi.shared.utils.log.WasdiLog;

@Path("/openapi.json")
public class OgcProcessesOpenApiResource {

	private static final String OPENAPI_CONTEXT_ID = "openapi.context.id.default";

	@GET
	@Produces({
			MediaType.APPLICATION_JSON,
			"application/vnd.oai.openapi+json;version=3.0",
			"application/vnd.oai.openapi+json",
			"application/openapi+json"
	})
	public Response getOpenApi() {
		try {
			OpenApiContext oContext = OpenApiContextLocator.getInstance().getOpenApiContext(OPENAPI_CONTEXT_ID);

			if (oContext == null) {
				return Response.status(Response.Status.NOT_FOUND).build();
			}

			OpenAPI oOpenApi = oContext.read();
			return Response.ok(oOpenApi).build();
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("OgcProcessesOpenApiResource.getOpenApi: error ", oEx);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error generating OpenAPI specification: " + oEx.getMessage())
					.build();
		}
	}
}