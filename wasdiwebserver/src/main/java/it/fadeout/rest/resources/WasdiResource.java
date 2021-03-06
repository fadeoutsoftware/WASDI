package it.fadeout.rest.resources;

import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.PrimitiveResult;

@Path("wasdi")
public class WasdiResource {

	@Context
	ServletConfig m_oServletConfig;

	@GET
	@Path("/hello")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public PrimitiveResult hello() {
		Utils.debugLog("WasdiResource.hello");
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setStringValue("Hello Wasdi!!");
		return oResult;
	}

	@GET
	@Path("/version")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public PrimitiveResult serverVersion() {
		Utils.debugLog("WasdiResource.serverVersion");
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setStringValue(m_oServletConfig.getInitParameter("ServerVersion"));
		return oResult;
	}
}
