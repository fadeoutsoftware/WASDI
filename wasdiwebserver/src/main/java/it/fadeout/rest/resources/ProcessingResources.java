package it.fadeout.rest.resources;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.rcp.imgfilter.model.Filter;
import org.esa.snap.rcp.imgfilter.model.StandardFilters;
import org.glassfish.jersey.media.multipart.FormDataParam;

import it.fadeout.Wasdi;
import wasdi.shared.LauncherOperations;
import wasdi.shared.SnapOperatorFactory;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.User;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.parameters.ApplyOrbitParameter;
import wasdi.shared.parameters.ApplyOrbitSetting;
import wasdi.shared.parameters.CalibratorParameter;
import wasdi.shared.parameters.CalibratorSetting;
import wasdi.shared.parameters.GraphParameter;
import wasdi.shared.parameters.GraphSetting;
import wasdi.shared.parameters.ISetting;
import wasdi.shared.parameters.MultilookingParameter;
import wasdi.shared.parameters.MultilookingSetting;
import wasdi.shared.parameters.NDVIParameter;
import wasdi.shared.parameters.NDVISetting;
import wasdi.shared.parameters.OperatorParameter;
import wasdi.shared.parameters.RangeDopplerGeocodingParameter;
import wasdi.shared.parameters.RangeDopplerGeocodingSetting;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.FilterBandViewModel;
import wasdi.shared.viewmodels.SnapOperatorParameterViewModel;

@Path("/processing")
public class ProcessingResources {
	
	@Context
	ServletConfig m_oServletConfig;

	@POST
	@Path("geometric/rangeDopplerTerrainCorrection")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response TerrainCorrection(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sSourceProductName") String sSourceProductName, @QueryParam("sDestinationProductName") String sDestinationProductName, @QueryParam("sWorkspaceId") String sWorkspaceId, RangeDopplerGeocodingSetting oSetting) throws IOException
	{
		return ExecuteOperation(sSessionId, sSourceProductName, sDestinationProductName, sWorkspaceId, oSetting, LauncherOperations.TERRAIN);
	}
	
	@POST
	@Path("radar/applyOrbit")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response ApplyOrbit(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sSourceProductName") String sSourceProductName, @QueryParam("sDestinationProductName") String sDestinationProductName, @QueryParam("sWorkspaceId") String sWorkspaceId, ApplyOrbitSetting oSetting) throws IOException
	{	
		return ExecuteOperation(sSessionId, sSourceProductName, sDestinationProductName, sWorkspaceId, oSetting, LauncherOperations.APPLYORBIT);
	}
	
	@POST
	@Path("radar/radiometricCalibration")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response Calibrate(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sSourceProductName") String sSourceProductName, @QueryParam("sDestinationProductName") String sDestinationProductName, @QueryParam("sWorkspaceId") String sWorkspaceId, CalibratorSetting oSetting) throws IOException
	{
		return ExecuteOperation(sSessionId, sSourceProductName, sDestinationProductName, sWorkspaceId, oSetting, LauncherOperations.CALIBRATE);
	}
	
	@POST
	@Path("radar/multilooking")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response Multilooking(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sSourceProductName") String sSourceProductName, @QueryParam("sDestinationProductName") String sDestinationProductName, @QueryParam("sWorkspaceId") String sWorkspaceId, MultilookingSetting oSetting) throws IOException
	{
		
		return ExecuteOperation(sSessionId, sSourceProductName, sDestinationProductName, sWorkspaceId, oSetting, LauncherOperations.MULTILOOKING);

	}
	
	@POST
	@Path("optical/ndvi")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response NDVI(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sSourceProductName") String sSourceProductName, @QueryParam("sDestinationProductName") String sDestinationProductName, @QueryParam("sWorkspaceId") String sWorkspaceId, NDVISetting oSetting) throws IOException
	{
		return ExecuteOperation(sSessionId, sSourceProductName, sDestinationProductName, sWorkspaceId, oSetting, LauncherOperations.NDVI);
	}


	@GET
	@Path("parameters")
	@Produces({"application/json"})
	public SnapOperatorParameterViewModel[] OperatorParameters(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sOperation") String sOperation) throws IOException
	{
		ArrayList<SnapOperatorParameterViewModel> oChoices = new ArrayList<SnapOperatorParameterViewModel>();
		
		Class oOperatorClass = SnapOperatorFactory.getOperatorClass(sOperation);
		
		Field[] aoOperatorFields = oOperatorClass.getDeclaredFields();
    	for (Field oOperatorField : aoOperatorFields) {
    		
    		if (oOperatorField.getName().equals("mapProjection")) {
    			System.out.println("ciao");
    		}
    		
			Annotation[] aoAnnotations = oOperatorField.getAnnotations();			
			for (Annotation oAnnotation : aoAnnotations) {
				
				if (oAnnotation instanceof Parameter) {
					Parameter oAnnotationParameter = (Parameter) oAnnotation;					
					
					SnapOperatorParameterViewModel oParameter = new SnapOperatorParameterViewModel();					
					oParameter.setField(oOperatorField.getName());
					
				    oParameter.setAlias(oAnnotationParameter.alias());
				    oParameter.setItemAlias(oAnnotationParameter.itemAlias());
				    oParameter.setDefaultValue(oAnnotationParameter.defaultValue());
				    oParameter.setLabel(oAnnotationParameter.label());
				    oParameter.setUnit(oAnnotationParameter.unit());
				    oParameter.setDescription(oAnnotationParameter.description());
				    oParameter.setValueSet(oAnnotationParameter.valueSet());
				    oParameter.setInterval(oAnnotationParameter.interval());
				    oParameter.setCondition(oAnnotationParameter.condition());
				    oParameter.setPattern(oAnnotationParameter.pattern());
				    oParameter.setFormat(oAnnotationParameter.format());
				    oParameter.setNotNull(oAnnotationParameter.notNull());
				    oParameter.setNotEmpty(oAnnotationParameter.notEmpty());
					
					oChoices.add(oParameter);					
				}
			}
		}
		
		
		return oChoices.toArray(new SnapOperatorParameterViewModel[oChoices.size()]);
	}
	
	
	
	@POST
	@Path("/graph")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response executeGraph(@FormDataParam("file") InputStream fileInputStream, @HeaderParam("x-session-token") String sessionId, 
			@QueryParam("workspace") String workspace, @QueryParam("source") String sourceProductName, @QueryParam("destination") String destinationProdutName) throws Exception {

		GraphSetting settings = new GraphSetting();		
		String graphXml;
		graphXml = IOUtils.toString(fileInputStream, Charset.defaultCharset().name());
		settings.setGraphXml(graphXml);
		
		return ExecuteOperation(sessionId, sourceProductName, destinationProdutName, workspace, settings, LauncherOperations.GRAPH);
		
	}
	
	@GET
	@Path("/standardfilters")
	@Produces({"application/json"})
	public Map<String, Filter[]> getStandardFilters(@HeaderParam("x-session-token") String sSessionId) {
		Map<String, Filter[]> filtersMap = new HashMap<String, Filter[]>();
		filtersMap.put("Detect Lines", StandardFilters.LINE_DETECTION_FILTERS);
		filtersMap.put("Detect Gradients (Emboss)", StandardFilters.GRADIENT_DETECTION_FILTERS);
		filtersMap.put("Smooth and Blurr", StandardFilters.SMOOTHING_FILTERS);
		filtersMap.put("Sharpen", StandardFilters.SHARPENING_FILTERS);
		filtersMap.put("Enhance Discontinuities", StandardFilters.LAPLACIAN_FILTERS);
		filtersMap.put("Non-Linear Filters", StandardFilters.NON_LINEAR_FILTERS);
		filtersMap.put("Morphological Filters", StandardFilters.MORPHOLOGICAL_FILTERS);
		return filtersMap;
	}
	
	@POST
	@Path("/applyfilter")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response applyFilters(@HeaderParam("x-session-token") String sSessionId, FilterBandViewModel filterViewModel) throws IOException {
		
		Filter filter = filterViewModel.getFilter();
		
		return Response.status(200).build();
	}
	
	
	private String AcceptedUserAndSession(String sSessionId) {
		//Check user
		if (Utils.isNullOrEmpty(sSessionId)) return null;
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser==null) return null;
		if (Utils.isNullOrEmpty(oUser.getUserId())) return null;
		
		return oUser.getUserId();
	}
	
	private Response ExecuteOperation(String sSessionId, String sSourceProductName, String sDestinationProductName, String sWorkspaceId, ISetting oSetting, LauncherOperations operation) {
		
		String sUserId = AcceptedUserAndSession(sSessionId);
		
		if (Utils.isNullOrEmpty(sUserId)) return Response.status(401).build();
		
		try {
			//Update process list
			String sProcessId = "";
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
			ProcessWorkspace oProcess = new ProcessWorkspace();
			
			try
			{
				oProcess.setOperationDate(Wasdi.GetFormatDate(new Date()));
				oProcess.setOperationType(operation.name());
				oProcess.setProductName(sSourceProductName);
				oProcess.setWorkspaceId(sWorkspaceId);
				oProcess.setUserId(sUserId);
				oProcess.setProcessObjId(Utils.GetRandomName());
				oProcess.setStatus(ProcessStatus.CREATED.name());
				sProcessId = oRepository.InsertProcessWorkspace(oProcess);
			}
			catch(Exception oEx){
				System.out.println("SnapOperations.ExecuteOperation: Error updating process list " + oEx.getMessage());
				oEx.printStackTrace();
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
			}

			String sPath = m_oServletConfig.getInitParameter("SerializationPath") + Wasdi.GetSerializationFileName();

			
			OperatorParameter oParameter = GetParameter(operation); 
			oParameter.setSourceProductName(sSourceProductName);
			oParameter.setDestinationProductName(sDestinationProductName);
			oParameter.setWorkspace(sWorkspaceId);
			oParameter.setUserId(sUserId);
			oParameter.setExchange(sWorkspaceId);
			oParameter.setProcessObjId(oProcess.getProcessObjId());
			if (oSetting != null) oParameter.setSettings(oSetting);	
			
			SerializationUtils.serializeObjectToXML(sPath, oParameter);

			String sLauncherPath = m_oServletConfig.getInitParameter("LauncherPath");
			String sJavaExe = m_oServletConfig.getInitParameter("JavaExe");

			String sShellExString = sJavaExe + " -jar " + sLauncherPath +" -operation " + operation + " -parameter " + sPath;

			System.out.println("SnapOperations.ExecuteOperation: shell exec " + sShellExString);

			Process oProc = Runtime.getRuntime().exec(sShellExString);

//			//Update process
//			if (oProc != null)
//			{
//				int iPID = Wasdi.getPIDProcess(oProc);
//				if (!Utils.isNullOrEmpty(sProcessId))
//				{
//					oProcess.setPid(iPID);
//					System.out.println("SnapOperations.ExecuteOperation: Updating process pid: " + sProcessId);
//					if (!oRepository.UpdateProcess(oProcess)) {
//						System.out.println("SnapOperations.ExecuteOperation: pid not saved");
//					}
//					
//				}
//			}			

		} catch (IOException e) {
			e.printStackTrace();
			return Response.serverError().build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.serverError().build();
		}
		
		return Response.ok().build();
	}

	
	private OperatorParameter GetParameter(LauncherOperations op)
	{
		switch (op) {
		case APPLYORBIT:
			return new ApplyOrbitParameter();
		case CALIBRATE:
			return new CalibratorParameter();
		case MULTILOOKING:
			return new MultilookingParameter();
		case TERRAIN:
			return new RangeDopplerGeocodingParameter();
		case NDVI:
			return new NDVIParameter();
			
		case GRAPH:
			return new GraphParameter();
		default:
			return null;
			
		}
	}
	
	
}
