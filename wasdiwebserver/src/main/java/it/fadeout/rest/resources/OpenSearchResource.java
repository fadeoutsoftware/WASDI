package it.fadeout.rest.resources;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.json.JSONObject;

import it.fadeout.opensearch.OpenSearchQuery;

@Path("/search")
public class OpenSearchResource {

	
	@GET
	@Path("/sentinel/result")
	@Produces({"application/xml", "application/json", "text/html"})
	public String SearchSentinel(@QueryParam("sQuery") String sQuery, @QueryParam("offset") String sOffset, @QueryParam("limit") String sLimit, @QueryParam("sortedby") String sSortedBy, @QueryParam("order") String sOrder )
	{
		try {
			ArrayList<String> asParams = new ArrayList<>();
			if (sOffset != null)
				asParams.add("offset=" + sOffset);
			if (sLimit != null)
				asParams.add("limit=" + sLimit);
			if (sSortedBy != null)
				asParams.add("sortedby=" + sSortedBy);
			if (sOrder != null)
				asParams.add("order=" + sOrder);
			//return OpenSearchQuery.ExecuteQuerySentinel(sQuery, asParams.toArray(new String[asParams.size()]));
			return OpenSearchQuery.ExecuteQuery(sQuery, asParams.toArray(new String[asParams.size()]));
		} catch (URISyntaxException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
		
	}
	
	@GET
	@Path("/sentinel/count")
	@Produces({"application/xml", "application/json", "text/html"})
	public String GetProductsCountSentinel(@QueryParam("sQuery") String sQuery)
	{
		try {
			System.out.println("OpenSearchResource.GetProductsCount: Query: " + sQuery);
			return OpenSearchQuery.ExecuteQueryCount(sQuery);
		} catch (URISyntaxException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return "";
		
	}
}
