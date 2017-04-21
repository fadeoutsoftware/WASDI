package it.fadeout.rest.resources;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.esa.s1tbx.sar.gpf.orbits.ApplyOrbitFileOp;
import org.esa.snap.core.gpf.annotations.Parameter;

import it.fadeout.Wasdi;
import wasdi.shared.LauncherOperations;
import wasdi.shared.SnapOperatorFactory;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.User;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.parameters.ApplyOrbitParameter;
import wasdi.shared.parameters.ApplyOrbitSetting;
import wasdi.shared.parameters.CalibratorParameter;
import wasdi.shared.parameters.CalibratorSetting;
import wasdi.shared.parameters.DownloadFileParameter;
import wasdi.shared.parameters.ISetting;
import wasdi.shared.parameters.MultilookingParameter;
import wasdi.shared.parameters.MultilookingSetting;
import wasdi.shared.parameters.NDVIParameter;
import wasdi.shared.parameters.NDVISetting;
import wasdi.shared.parameters.OperatorParameter;
import wasdi.shared.parameters.PublishParameters;
import wasdi.shared.parameters.RangeDopplerGeocodingParameter;
import wasdi.shared.parameters.RangeDopplerGeocodingSetting;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.SnapOperatorParameterViewModel;

@Path("/processing")
public class SnapOperationsResources {
	
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
	@Produces({"application/xml", "application/json", "text/xml"})
	public SnapOperatorParameterViewModel[] OperatorParameters(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sOperation") String sOperation) throws IOException
	{
		ArrayList<SnapOperatorParameterViewModel> oChoices = new ArrayList<SnapOperatorParameterViewModel>();
		
		Class oOperatorClass = SnapOperatorFactory.getOperatorClass(sOperation);
		
		Field[] aoOperatorFields = oOperatorClass.getDeclaredFields();
    	for (Field oOperatorField : aoOperatorFields) {
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
	
	
	
	
	private String AcceptedUserAndSession(String sSessionId)
	{
		//Check user
		if (Utils.isNullOrEmpty(sSessionId)) return null;
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser==null) return null;
		if (Utils.isNullOrEmpty(oUser.getUserId())) return null;
		
		return oUser.getUserId();
	}
	
	private Response ExecuteOperation(String sSessionId, String sSourceProductName, String sDestinationProductName, String sWorkspaceId, ISetting oSetting, String sOperator)
	{
		
		String sUserId = AcceptedUserAndSession(sSessionId);
		
		if (Utils.isNullOrEmpty(sUserId)) return Response.status(401).build();
		
		try {
			//Update process list
			String sProcessId = "";
			try
			{
				ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
				ProcessWorkspace oProcess = new ProcessWorkspace();
				oProcess.setOperationDate(Wasdi.GetFormatDate(new Date()));
				oProcess.setOperationType(sOperator);
				oProcess.setProductName(sSourceProductName);
				oProcess.setWorkspaceId(sWorkspaceId);
				oProcess.setUserId(sUserId);
				sProcessId = oRepository.InsertProcessWorkspace(oProcess);
			}
			catch(Exception oEx){
				System.out.println("SnapOperations.ExecuteOperation: Error updating process list " + oEx.getMessage());
				oEx.printStackTrace();
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
			}

			String sPath = m_oServletConfig.getInitParameter("SerializationPath") + Wasdi.GetSerializationFileName();

			
			OperatorParameter oParameter = GetParameter(sOperator); 
			oParameter.setSourceProductName(sSourceProductName);
			oParameter.setDestinationProductName(sDestinationProductName);
			oParameter.setWorkspace(sWorkspaceId);
			oParameter.setUserId(sUserId);
			oParameter.setExchange(sWorkspaceId);
			oParameter.setProcessObjId(sProcessId);
			if (oSetting != null)
				oParameter.setSettings(oSetting);	
			
			SerializationUtils.serializeObjectToXML(sPath, oParameter);

			String sLauncherPath = m_oServletConfig.getInitParameter("LauncherPath");
			String sJavaExe = m_oServletConfig.getInitParameter("JavaExe");

			String sShellExString = sJavaExe + " -jar " + sLauncherPath +" -operation " + sOperator + " -parameter " + sPath;

			System.out.println("DownloadResource.Download: shell exec " + sShellExString);

			Process oProc = Runtime.getRuntime().exec(sShellExString);


		} catch (IOException e) {
			e.printStackTrace();
			return Response.serverError().build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.serverError().build();
		}
		
		return Response.ok().build();
	}

	
	private OperatorParameter GetParameter(String sOperation)
	{
		switch (sOperation) {
		case LauncherOperations.APPLYORBIT:
			return new ApplyOrbitParameter();
		case LauncherOperations.CALIBRATE:
			return new CalibratorParameter();
		case LauncherOperations.MULTILOOKING:
			return new MultilookingParameter();
		case LauncherOperations.TERRAIN:
			return new RangeDopplerGeocodingParameter();
		case LauncherOperations.NDVI:
			return new NDVIParameter();
			
		default:
			return null;
			
		}
	}
	
}
