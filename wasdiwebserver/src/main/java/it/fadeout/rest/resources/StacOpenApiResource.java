package it.fadeout.rest.resources;

import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.integration.OpenApiContextLocator;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.log.WasdiLog;

/**
 * OpenAPI document scoped to the STAC API root.
 */
@Path("/stac/openapi.json")
public class StacOpenApiResource {

	private static final String OPENAPI_CONTEXT_ID = "openapi.context.id.default";
	private static final String STAC_PATH_PREFIX = "/stac";

	/**
	 * Returns the STAC-only OpenAPI document with paths relative to /stac.
	 */
	@GET
	@Produces({
			MediaType.APPLICATION_JSON,
			"application/vnd.oai.openapi+json;version=3.0",
			"application/vnd.oai.openapi+json",
			"application/openapi+json"
	})
	public Response getStacOpenApi(@Context HttpHeaders oHeaders) {
		try {
			OpenApiContext oContext = OpenApiContextLocator.getInstance().getOpenApiContext(OPENAPI_CONTEXT_ID);

			if (oContext == null || oContext.read() == null) {
				return Response.status(Response.Status.NOT_FOUND).build();
			}

			OpenAPI oFullOpenApi = oContext.read();
			OpenAPI oStacOpenApi = new OpenAPI();
			oStacOpenApi.setOpenapi(oFullOpenApi.getOpenapi());
			oStacOpenApi.setInfo(new Info().title("WASDI STAC API").version("1.0.0").description("WASDI STAC Catalog API"));

			String sBaseUrl = WasdiConfig.Current.baseUrl;
			if (sBaseUrl.endsWith("/")) sBaseUrl = sBaseUrl.substring(0, sBaseUrl.length() - 1);

			Server oStacServer = new Server();
			oStacServer.setUrl(sBaseUrl + STAC_PATH_PREFIX);
			oStacServer.setDescription("WASDI STAC Server URL");
			oStacOpenApi.setServers(java.util.Collections.singletonList(oStacServer));

			Paths oStacPaths = new Paths();
			if (oFullOpenApi.getPaths() != null) {
				for (Map.Entry<String, PathItem> oPathEntry : oFullOpenApi.getPaths().entrySet()) {
					String sPath = oPathEntry.getKey();

					if (STAC_PATH_PREFIX.equals(sPath)) {
						oStacPaths.addPathItem("/", oPathEntry.getValue());
					}
					else if (sPath.startsWith(STAC_PATH_PREFIX + "/") && !sPath.equals("/stac/openapi.json")) {
						oStacPaths.addPathItem(sPath.substring(STAC_PATH_PREFIX.length()), oPathEntry.getValue());
					}
				}
			}
			oStacOpenApi.setPaths(oStacPaths);
			oStacOpenApi.setComponents(oFullOpenApi.getComponents());
			oStacOpenApi.setTags(oFullOpenApi.getTags());

			String sOpenApiJson = Json.mapper().writeValueAsString(oStacOpenApi);
			sOpenApiJson = sOpenApiJson.replace("\"style\":\"FORM\"", "\"style\":\"form\"");

			String sResponseMediaType = MediaType.APPLICATION_JSON;
			if (oHeaders != null && oHeaders.getRequestHeader(HttpHeaders.ACCEPT) != null
					&& oHeaders.getRequestHeader(HttpHeaders.ACCEPT).stream().anyMatch(sAccept -> sAccept.contains("application/vnd.oai.openapi+json;version=3.0"))) {
				sResponseMediaType = "application/vnd.oai.openapi+json;version=3.0";
			}

			return Response.ok(sOpenApiJson, sResponseMediaType).build();
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("StacOpenApiResource.getStacOpenApi: error ", oEx);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}
	}
}