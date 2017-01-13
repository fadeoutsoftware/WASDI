package it.fadeout.rest.resources;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.json.JSONObject;

import it.fadeout.Wasdi;
import it.fadeout.opensearch.OpenSearchQuery;
import wasdi.shared.business.User;
import wasdi.shared.utils.Utils;

@Path("/search")
public class OpenSearchResource {

	
	@GET
	@Path("/sentinel/result")
	@Produces({"application/xml", "application/json", "text/html"})
	public String SearchSentinel(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sQuery") String sQuery, @QueryParam("offset") String sOffset, @QueryParam("limit") String sLimit, @QueryParam("sortedby") String sSortedBy, @QueryParam("order") String sOrder )
	{
		
		if (Utils.isNullOrEmpty(sSessionId)) return null;
		
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		
		if (oUser==null) return null;
		if (Utils.isNullOrEmpty(oUser.getUserId())) return null;
		
		try {
			HashMap<String, String> asParameterMap = new HashMap<>();
			ArrayList<String> asParams = new ArrayList<>();
			if (sOffset != null)
				asParameterMap.put("offset", sOffset);
			if (sLimit != null)
				asParameterMap.put("limit", sLimit);
			if (sSortedBy != null)
				asParameterMap.put("sortedby", sSortedBy);
			if (sOrder != null)
				asParameterMap.put("order", sOrder);
			//return OpenSearchQuery.ExecuteQuerySentinel(sQuery, asParams.toArray(new String[asParams.size()]));
			return OpenSearchQuery.ExecuteQuery(sQuery, asParameterMap);
		} catch (URISyntaxException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
		
	}
	
	@GET
	@Path("/sentinel/count")
	@Produces({"application/xml", "application/json", "text/html"})
	public String GetProductsCountSentinel(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sQuery") String sQuery)
	{
		
		if (Utils.isNullOrEmpty(sSessionId)) return null;
		
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		
		if (oUser==null) return null;
		if (Utils.isNullOrEmpty(oUser.getUserId())) return null;
		
		try {
			System.out.println("OpenSearchResource.GetProductsCount: Query: " + sQuery);
			return OpenSearchQuery.ExecuteQueryCount(sQuery);
		} catch (URISyntaxException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
		
	}
}
