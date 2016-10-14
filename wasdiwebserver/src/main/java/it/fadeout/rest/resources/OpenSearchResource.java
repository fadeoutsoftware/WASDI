package it.fadeout.rest.resources;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import it.fadeout.opensearch.OpenSearchQuery;

@Path("/search")
public class OpenSearchResource {

	
	@GET
	@Path("/result/{sQuery}")
	@Produces({"application/xml", "application/json", "text/xml"})
	public String Search(@PathParam("sQuery") String sQuery)
	{
		try {
			return OpenSearchQuery.ExecuteQuery(sQuery);
		} catch (URISyntaxException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return "";
		
	}
}
