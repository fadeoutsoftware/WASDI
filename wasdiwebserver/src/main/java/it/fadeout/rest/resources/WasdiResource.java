package it.fadeout.rest.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.PrimitiveResult;

/**
 * Wasdi Resource.
 * 
 * Hosts API for:
 * 	.Keep Alive
 * 
 * @author p.campanella
 *
 */
@Path("wasdi")
public class WasdiResource {
	
	/**
	 * Hello API (is alive)
	 * @return
	 */
	@GET
	@Path("/hello")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public PrimitiveResult hello() {
		Utils.debugLog("WasdiResource.hello");
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setStringValue("Hello Wasdi!!");
		return oResult;
	}
}
