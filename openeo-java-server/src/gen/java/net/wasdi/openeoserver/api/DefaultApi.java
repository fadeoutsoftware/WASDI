package net.wasdi.openeoserver.api;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import net.wasdi.openeoserver.viewmodels.Billing;
import net.wasdi.openeoserver.viewmodels.BillingPlan;
import net.wasdi.openeoserver.viewmodels.Capabilities;
import net.wasdi.openeoserver.viewmodels.Capabilities.ApiVersionEnum;
import net.wasdi.openeoserver.viewmodels.Endpoint;
import net.wasdi.openeoserver.viewmodels.Endpoint.MethodsEnum;
import net.wasdi.openeoserver.viewmodels.Error;
import net.wasdi.openeoserver.viewmodels.Link;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.log.WasdiLog;

@Path("")
public class DefaultApi  {

	public DefaultApi(@Context ServletConfig oServletContext) {
	}

	@javax.ws.rs.GET    
	@Produces({ "application/json" })
	public Response capabilities(@Context SecurityContext oSecurityContext) {
		
		Capabilities oCapabilities = new Capabilities();

		try {

			oCapabilities.setApiVersion(ApiVersionEnum.fromValue(WasdiConfig.Current.openEO.api_version));
			oCapabilities.setBackendVersion(WasdiConfig.Current.openEO.backend_version);
			oCapabilities.setBilling(getBilling());
			oCapabilities.setDescription(WasdiConfig.Current.openEO.description);
			oCapabilities.setEndpoints(getEndpoints());
			oCapabilities.setId(WasdiConfig.Current.openEO.id);
			oCapabilities.setLinks(getLinks());
			oCapabilities.setProduction(WasdiConfig.Current.openEO.production);
			oCapabilities.setStacVersion(WasdiConfig.Current.openEO.stac_version);
			oCapabilities.setTitle(WasdiConfig.Current.openEO.title);
			oCapabilities.setType(Capabilities.TypeEnum.fromValue(WasdiConfig.Current.openEO.type));
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("DefaultApi.method error: " , oEx);    		    		
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("DefaultApi.method", "InternalServerError", oEx.getMessage())).build();
		}

		return Response.ok().entity(oCapabilities).build();
	}
	
	protected List<Endpoint> getEndpoints() {
		ArrayList<Endpoint> aoEndPoints = new ArrayList<>();

		Endpoint oEndPoint = new Endpoint();
		oEndPoint.setPath("/collections");
		ArrayList<MethodsEnum> aoMethods = new ArrayList<>();
		aoMethods.add(MethodsEnum.GET);
		oEndPoint.setMethods(aoMethods);
		aoEndPoints.add(oEndPoint);

		oEndPoint = new Endpoint();
		oEndPoint.setPath("/collections/{collection_id}");
		aoMethods = new ArrayList<>();
		aoMethods.add(MethodsEnum.GET);
		oEndPoint.setMethods(aoMethods);
		aoEndPoints.add(oEndPoint);
		
		oEndPoint = new Endpoint();
		oEndPoint.setPath("/processes");
		aoMethods = new ArrayList<>();
		aoMethods.add(MethodsEnum.GET);
		oEndPoint.setMethods(aoMethods);
		aoEndPoints.add(oEndPoint);

		
		oEndPoint = new Endpoint();
		oEndPoint.setPath("/jobs");
		aoMethods = new ArrayList<>();
		aoMethods.add(MethodsEnum.GET);
		aoMethods.add(MethodsEnum.PUT);
		oEndPoint.setMethods(aoMethods);
		aoEndPoints.add(oEndPoint);

		
		oEndPoint = new Endpoint();
		oEndPoint.setPath("/jobs/{job_id}");
		aoMethods = new ArrayList<>();
		aoMethods.add(MethodsEnum.GET);
		aoMethods.add(MethodsEnum.DELETE);
		aoMethods.add(MethodsEnum.PATCH);
		oEndPoint.setMethods(aoMethods);
		aoEndPoints.add(oEndPoint);
		
		oEndPoint = new Endpoint();
		oEndPoint.setPath("/credentials/basic");
		aoMethods = new ArrayList<>();
		aoMethods.add(MethodsEnum.GET);
		oEndPoint.setMethods(aoMethods);
		aoEndPoints.add(oEndPoint);
		


		return aoEndPoints;
		
	}
	
	protected List<Link> getLinks() {
		ArrayList<Link> aoLinks = new ArrayList<>();
		
		
		try {
			Link oLink = new Link();
			oLink.setHref(new URI("https://www.wasdi.cloud"));
			oLink.setRel("about");
			oLink.setTitle("Wasdi Homepage");
			oLink.setType("text/html");
			aoLinks.add(oLink);
			
			oLink = new Link();
			oLink.setHref(new URI("https://wasdi.readthedocs.io/en/latest/Legal/EULA.html"));
			oLink.setRel("terms-of-service");
			oLink.setTitle("Terms of Service");
			oLink.setType("text/html");
			aoLinks.add(oLink);
			
			oLink = new Link();
			oLink.setHref(new URI("https://wasdi.readthedocs.io/en/latest/Legal/EULA.html"));
			oLink.setRel("privacy-policy");
			oLink.setTitle("Privacy Policy");
			oLink.setType("text/html");
			aoLinks.add(oLink);
			
			String sBaseAddress = WasdiConfig.Current.openEO.baseAddress;
			if (!sBaseAddress.endsWith("/")) sBaseAddress += "/";
			
			oLink = new Link();
			oLink.setHref(new URI(sBaseAddress + ".well-known/openeo"));
			oLink.setRel("version-history");
			oLink.setTitle("List of supported openEO versions");
			oLink.setType("application/json");
			aoLinks.add(oLink);
			
			oLink = new Link();
			oLink.setHref(new URI(sBaseAddress + "conformance"));
			oLink.setRel("conformance");
			oLink.setTitle("OGC Conformance Classes");
			oLink.setType("application/json");
			aoLinks.add(oLink);
			
			oLink = new Link();
			oLink.setHref(new URI(sBaseAddress + "collections"));
			oLink.setRel("data");
			oLink.setTitle("List of Datasets");
			oLink.setType("application/json");
			aoLinks.add(oLink);			
		} catch (Exception oEx) {
			WasdiLog.errorLog("DefaultApi.getLinks error: " , oEx);
		}
		
		
		return aoLinks;
	}
	
	protected Billing getBilling() {
		Billing oBilling = new Billing();
		try {
			ArrayList<BillingPlan> aoPlans = new ArrayList<>();
			oBilling.setPlans(aoPlans);
			
			oBilling.setCurrency("EURO");
			oBilling.setDefaultPlan("STANDARD");
			
			BillingPlan oBillingPlan = new BillingPlan();
			oBillingPlan.setName("PROFESSIONAL");
			oBillingPlan.setDescription("Professional WASDI Node");
			oBillingPlan.setPaid(true);			
			oBillingPlan.setUrl(new URI("https://www.wasdi.cloud"));
			
			oBilling.getPlans().add(oBillingPlan);
			
			oBillingPlan = new BillingPlan();
			oBillingPlan.setName("STANDARD");
			oBillingPlan.setDescription("Standard WASDI Node");
			oBillingPlan.setPaid(true);			
			oBillingPlan.setUrl(new URI("https://www.wasdi.cloud"));
			
			oBilling.getPlans().add(oBillingPlan);
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("DefaultApi.getLinks getBilling: " , oEx);
		}
		
		return oBilling;
	}
}
