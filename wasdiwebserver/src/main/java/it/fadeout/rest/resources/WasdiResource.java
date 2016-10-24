package it.fadeout.rest.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import wasdi.shared.viewmodels.PrimitiveResult;


@Path("wasdi")
public class WasdiResource {
	
	@GET
	@Path("/hello")
	@Produces({"application/xml", "application/json", "text/xml"})	
	public PrimitiveResult Hello()
	{
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setStringValue("Hello Wasdi!!");
		return oResult;
	}
	
}
