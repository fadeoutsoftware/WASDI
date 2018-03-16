package it.fadeout.rest.resources;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.media.jai.JAI;
import javax.media.jai.OperationRegistry;
import javax.media.jai.RegistryElementDescriptor;
import javax.media.jai.operator.FormatDescriptor;
import javax.servlet.ServletConfig;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FilterBand;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.rcp.imgfilter.FilteredBandAction;
import org.esa.snap.rcp.imgfilter.model.Filter;
import org.esa.snap.rcp.imgfilter.model.StandardFilters;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import com.bc.ceres.jai.operator.PaintDescriptor;

import it.fadeout.Wasdi;
import it.fadeout.business.DownloadsThread;
import it.fadeout.business.ProcessingThread;
import wasdi.shared.LauncherOperations;
import wasdi.shared.SnapOperatorFactory;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.SnapWorkflow;
import wasdi.shared.business.User;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.SnapWorkflowRepository;
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
import wasdi.shared.utils.BandImageManager;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.BandImageViewModel;
import wasdi.shared.viewmodels.ProductMaskViewModel;
import wasdi.shared.viewmodels.SnapOperatorParameterViewModel;
import wasdi.shared.viewmodels.SnapWorkflowViewModel;

@Path("/processing")
public class ProcessingResources {
	
	@Context
	ServletConfig m_oServletConfig;
		
//	@GET
//	@Path("test")
//	@Produces({"application/xml", "application/json", "text/xml"})
//	public Response test() throws IOException
//	{
//		return ExecuteOperation("ciao", "test", "test", "test", null, LauncherOperations.TERRAIN);
//	}
	
	
	@POST
	@Path("geometric/rangeDopplerTerrainCorrection")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response TerrainCorrection(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sSourceProductName") String sSourceProductName, @QueryParam("sDestinationProductName") String sDestinationProductName, @QueryParam("sWorkspaceId") String sWorkspaceId, RangeDopplerGeocodingSetting oSetting) throws IOException
	{
		Wasdi.DebugLog("ProcessingResources.TerrainCorrection");
		return ExecuteOperation(sSessionId, sSourceProductName, sDestinationProductName, sWorkspaceId, oSetting, LauncherOperations.TERRAIN);
	}
	
	@POST
	@Path("radar/applyOrbit")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response ApplyOrbit(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sSourceProductName") String sSourceProductName, @QueryParam("sDestinationProductName") String sDestinationProductName, @QueryParam("sWorkspaceId") String sWorkspaceId, ApplyOrbitSetting oSetting) throws IOException
	{	
		Wasdi.DebugLog("ProcessingResources.ApplyOrbit");
		return ExecuteOperation(sSessionId, sSourceProductName, sDestinationProductName, sWorkspaceId, oSetting, LauncherOperations.APPLYORBIT);
	}
	
	@POST
	@Path("radar/radiometricCalibration")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response Calibrate(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sSourceProductName") String sSourceProductName, @QueryParam("sDestinationProductName") String sDestinationProductName, @QueryParam("sWorkspaceId") String sWorkspaceId, CalibratorSetting oSetting) throws IOException
	{
		Wasdi.DebugLog("ProcessingResources.Calibrate");
		return ExecuteOperation(sSessionId, sSourceProductName, sDestinationProductName, sWorkspaceId, oSetting, LauncherOperations.CALIBRATE);
	}
	
	@POST
	@Path("radar/multilooking")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response Multilooking(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sSourceProductName") String sSourceProductName, @QueryParam("sDestinationProductName") String sDestinationProductName, @QueryParam("sWorkspaceId") String sWorkspaceId, MultilookingSetting oSetting) throws IOException
	{
		Wasdi.DebugLog("ProcessingResources.Multilooking");
		return ExecuteOperation(sSessionId, sSourceProductName, sDestinationProductName, sWorkspaceId, oSetting, LauncherOperations.MULTILOOKING);

	}
	
	@POST
	@Path("optical/ndvi")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response NDVI(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sSourceProductName") String sSourceProductName, @QueryParam("sDestinationProductName") String sDestinationProductName, @QueryParam("sWorkspaceId") String sWorkspaceId, NDVISetting oSetting) throws IOException
	{
		Wasdi.DebugLog("ProcessingResources.NDVI");
		return ExecuteOperation(sSessionId, sSourceProductName, sDestinationProductName, sWorkspaceId, oSetting, LauncherOperations.NDVI);
	}


	@GET
	@Path("parameters")
	@Produces({"application/json"})
	public SnapOperatorParameterViewModel[] OperatorParameters(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sOperation") String sOperation) throws IOException
	{
		Wasdi.DebugLog("ProcessingResources.OperatorParameters");
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
	
	
	/**
	 * Save a SNAP Workflow XML
	 * @param fileInputStream
	 * @param sSessionId
	 * @param workspace
	 * @param name
	 * @param description
	 * @return
	 * @throws Exception
	 */
	@POST
	@Path("/uploadgraph")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadGraph(@FormDataParam("file") InputStream fileInputStream, @HeaderParam("x-session-token") String sSessionId, 
			@QueryParam("workspace") String workspace, @QueryParam("name") String name, @QueryParam("description") String description) throws Exception {

		Wasdi.DebugLog("ProcessingResources.uploadGraph");
		
		try {
			if (Utils.isNullOrEmpty(sSessionId)) return Response.status(401).build();
			User oUser = Wasdi.GetUserFromSession(sSessionId);

			if (oUser==null) return Response.status(401).build();
			if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(401).build();

			String sUserId = oUser.getUserId();
			
			String sDownloadRootPath = m_oServletConfig.getInitParameter("DownloadRootPath");
			if (!sDownloadRootPath.endsWith("/")) sDownloadRootPath = sDownloadRootPath + "/";
			
			File oUserWorkflowPath = new File(sDownloadRootPath+sUserId+ "/workflows/");
			
			if (!oUserWorkflowPath.exists()) {
				oUserWorkflowPath.mkdirs();
			}
			
			String sWorkflowId =  UUID.randomUUID().toString();
			File humidityTifFile = new File(sDownloadRootPath+sUserId+ "/workflows/" + sWorkflowId + ".xml");
			
			Wasdi.DebugLog("ProcessingResources.uploadGraph: workflow file Path: " + humidityTifFile.getPath());
			
			//save uploaded file
			int read = 0;
			byte[] bytes = new byte[1024];
			OutputStream out = new FileOutputStream(humidityTifFile);
			while ((read = fileInputStream.read(bytes)) != -1) {
				out.write(bytes, 0, read);
			}
			out.flush();
			out.close();
			
			SnapWorkflow oWorkflow = new SnapWorkflow();
			oWorkflow.setName(name);
			oWorkflow.setDescription(description);
			oWorkflow.setFilePath(humidityTifFile.getPath());
			oWorkflow.setUserId(sUserId);
			oWorkflow.setWorkflowId(sWorkflowId);
			
			SnapWorkflowRepository oSnapWorkflowRepository = new SnapWorkflowRepository();
			oSnapWorkflowRepository.InsertSnapWorkflow(oWorkflow);			
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return Response.serverError().build();
		}

				
		return Response.ok().build();
	}
	
	/**
	 * Get workflow list by user id
	 * @param sSessionId
	 * @return
	 */
	@GET
	@Path("/getgraphsbyusr")
	public ArrayList<SnapWorkflowViewModel> getWorkflowsByUser(@HeaderParam("x-session-token") String sSessionId) {
		Wasdi.DebugLog("ProcessingResources.getWorkflowsByUser");
		
		if (Utils.isNullOrEmpty(sSessionId)) return null;
		User oUser = Wasdi.GetUserFromSession(sSessionId);

		if (oUser==null) return null;
		if (Utils.isNullOrEmpty(oUser.getUserId())) return null;

		String sUserId = oUser.getUserId();
		
		SnapWorkflowRepository oSnapWorkflowRepository = new SnapWorkflowRepository();
		ArrayList<SnapWorkflowViewModel> aoRetWorkflows = new ArrayList<>();
		
		List<SnapWorkflow> aoDbWorkflows = oSnapWorkflowRepository.GetSnapWorkflowByUser(sUserId);
		
		for (int i=0; i<aoDbWorkflows.size(); i++) {
			SnapWorkflowViewModel oVM = new SnapWorkflowViewModel();
			oVM.setName(aoDbWorkflows.get(i).getName());
			oVM.setDescription(aoDbWorkflows.get(i).getDescription());
			oVM.setWorkflowId(aoDbWorkflows.get(i).getWorkflowId());
			
			aoRetWorkflows.add(oVM);
		}
		
		return aoRetWorkflows;
	}
	
	/**
	 * Delete a workflow from id
	 * @param sSessionId
	 * @param sWorkflowId
	 * @return
	 */
	@GET
	@Path("/deletegraph")
	public Response deleteWorkflow(@HeaderParam("x-session-token") String sSessionId, @QueryParam("workflowId") String sWorkflowId) {
		Wasdi.DebugLog("ProcessingResources.deleteWorkflow");
		
		if (Utils.isNullOrEmpty(sSessionId)) return Response.status(Status.UNAUTHORIZED).build();
		User oUser = Wasdi.GetUserFromSession(sSessionId);

		if (oUser==null) return Response.status(Status.UNAUTHORIZED).build();
		if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(Status.UNAUTHORIZED).build();

		String sUserId = oUser.getUserId();
		
		SnapWorkflowRepository oSnapWorkflowRepository = new SnapWorkflowRepository();
		
		SnapWorkflow oWorkflow = oSnapWorkflowRepository.GetSnapWorkflow(sWorkflowId);
		 
		if (oWorkflow == null) return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		
		if (oWorkflow.getUserId().equals(sUserId) == false)  return Response.status(Status.UNAUTHORIZED).build();

		oSnapWorkflowRepository.DeleteSnapWorkflow(sWorkflowId);
	
		return Response.ok().build();
	}
	
	/**
	 * Executes a workflow from a file stream
	 * @param fileInputStream
	 * @param sessionId
	 * @param workspace
	 * @param sourceProductName
	 * @param destinationProdutName
	 * @return
	 * @throws Exception
	 */
	@POST
	@Path("/graph")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response executeGraph(@FormDataParam("file") InputStream fileInputStream, @HeaderParam("x-session-token") String sessionId, 
			@QueryParam("workspace") String workspace, @QueryParam("source") String sourceProductName, @QueryParam("destination") String destinationProdutName) throws Exception {

		Wasdi.DebugLog("ProcessingResources.ExecuteGraph");
		
		if (Utils.isNullOrEmpty(sessionId)) return Response.status(Status.UNAUTHORIZED).build();
		User oUser = Wasdi.GetUserFromSession(sessionId);

		if (oUser==null) return Response.status(Status.UNAUTHORIZED).build();
		if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(Status.UNAUTHORIZED).build();

		GraphSetting settings = new GraphSetting();
		String graphXml;
		graphXml = IOUtils.toString(fileInputStream, Charset.defaultCharset().name());
		settings.setGraphXml(graphXml);
		
		return ExecuteOperation(sessionId, sourceProductName, destinationProdutName, workspace, settings, LauncherOperations.GRAPH);
		
	}
	
	@POST
	@Path("/graph_file")
	public Response executeGraphFromFile(@HeaderParam("x-session-token") String sessionId, 
			@QueryParam("workspace") String workspace, @QueryParam("source") String sourceProductName, @QueryParam("destination") String destinationProdutName) throws Exception {

		Wasdi.DebugLog("ProcessingResources.executeGraphFromFile");

		if (Utils.isNullOrEmpty(sessionId)) return Response.status(Status.UNAUTHORIZED).build();
		User oUser = Wasdi.GetUserFromSession(sessionId);

		if (oUser==null) return Response.status(Status.UNAUTHORIZED).build();
		if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(Status.UNAUTHORIZED).build();
		
		GraphSetting settings = new GraphSetting();		
		String graphXml;
		
		FileInputStream fileInputStream = new FileInputStream("/usr/lib/wasdi/S1_GRD_preprocessing.xml");
		
		graphXml = IOUtils.toString(fileInputStream, Charset.defaultCharset().name());
		settings.setGraphXml(graphXml);
		
		return ExecuteOperation(sessionId, sourceProductName, destinationProdutName, workspace, settings, LauncherOperations.GRAPH);
	}
	
	/**
	 * Exectues a Workflow from workflow Id
	 * @param sessionId
	 * @param workspace
	 * @param sourceProductName
	 * @param destinationProdutName
	 * @param workflowId
	 * @return
	 * @throws Exception
	 */
	@GET
	@Path("/graph_id")
	public Response executeGraphFromWorkflowId(@HeaderParam("x-session-token") String sessionId, 
			@QueryParam("workspace") String workspace, @QueryParam("source") String sourceProductName, @QueryParam("destination") String destinationProdutName, @QueryParam("workflowId") String workflowId) throws Exception {

		Wasdi.DebugLog("ProcessingResources.executeGraphFromWorkflowId");
		
		if (Utils.isNullOrEmpty(sessionId)) return Response.status(Status.UNAUTHORIZED).build();
		User oUser = Wasdi.GetUserFromSession(sessionId);

		if (oUser==null) return Response.status(Status.UNAUTHORIZED).build();
		if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(Status.UNAUTHORIZED).build();

		String sUserId = oUser.getUserId();
		
		GraphSetting settings = new GraphSetting();		
		String graphXml;
		
		SnapWorkflowRepository oSnapWorkflowRepository = new SnapWorkflowRepository();
		SnapWorkflow oWF = oSnapWorkflowRepository.GetSnapWorkflow(workflowId);
		
		if (oWF == null) return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		if (oWF.getUserId().equals(sUserId)==false) return Response.status(Status.UNAUTHORIZED).build();
		
		FileInputStream fileInputStream = new FileInputStream(oWF.getFilePath());
		
		graphXml = IOUtils.toString(fileInputStream, Charset.defaultCharset().name());
		settings.setGraphXml(graphXml);
		
		return ExecuteOperation(sessionId, sourceProductName, destinationProdutName, workspace, settings, LauncherOperations.GRAPH);
	}

	@GET
	@Path("/standardfilters")
	@Produces({"application/json"})
	public Map<String, Filter[]> getStandardFilters(@HeaderParam("x-session-token") String sSessionId) {
		
		Wasdi.DebugLog("ProcessingResources.GetStandardFilters");
		
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
	
	@GET
	@Path("/productmasks")
	@Produces({"application/json"})
	public ArrayList<ProductMaskViewModel> getProductMasks(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("file") String productFile, @QueryParam("band") String bandName) throws Exception {
		
		Wasdi.DebugLog("ProcessingResources.getProductMasks");
		
		ArrayList<ProductMaskViewModel> masks = new ArrayList<ProductMaskViewModel>();
		
		Product product = ProductIO.readProduct(productFile);
		Band band = product.getBand(bandName);
		
		final ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
		for (int i = 0; i < maskGroup.getNodeCount(); i++) {
			final Mask mask = maskGroup.get(i);
			if (mask.getRasterWidth() == band.getRasterWidth() &&
				mask.getRasterHeight() == band.getRasterHeight()) {
				ProductMaskViewModel vm = new ProductMaskViewModel();
				vm.setName(mask.getName());
				vm.setDescription(mask.getDescription());
				vm.setMaskType(mask.getImageType().getName());
				vm.setColorRed(mask.getImageColor().getRed());
				vm.setColorGreen(mask.getImageColor().getGreen());
				vm.setColorBlue(mask.getImageColor().getBlue());
				masks.add(vm);
			}
		}

		return masks;
	}

	@POST
	@Path("/bandimage")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getBandImage(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("workspace") String workspace,
			BandImageViewModel model) throws IOException {
		
		Wasdi.DebugLog("ProcessingResources.getBandImage");

		// Check user session
		String userId = AcceptedUserAndSession(sSessionId);
		if (Utils.isNullOrEmpty(userId)) return Response.status(401).build();

		
		// Init the registry for JAI
		OperationRegistry operationRegistry = JAI.getDefaultInstance().getOperationRegistry();
		RegistryElementDescriptor oDescriptor = operationRegistry.getDescriptor("rendered", "Paint");
		
		if (oDescriptor==null) {
			System.out.println("getBandImage: REGISTER Descriptor!!!!");
			try {
				operationRegistry.registerServices(this.getClass().getClassLoader());
			} catch (Exception e) {
				e.printStackTrace();
			}
			oDescriptor = operationRegistry.getDescriptor("rendered", "Paint");
			
			
			IIORegistry.getDefaultInstance().registerApplicationClasspathSpis();
		}
		
		
		// Get Download File Path
        String downloadPath = m_oServletConfig.getInitParameter("DownloadRootPath");
        File productFile = new File(new File(new File(downloadPath, userId), workspace), model.getProductFileName());
		
//		File productFile = new File("/home/doy/tmp/wasdi/tmp/S2B_MSIL1C_20180117T102339_N0206_R065_T32TMQ_20180117T122826.zip");
        
        if (!productFile.exists()) {
        	System.out.println("ProcessingResource.getBandImage: FILE NOT FOUND: " + productFile.getAbsolutePath());
        	return Response.status(500).build();
        }
        
        Product oSNAPProduct = ProductIO.readProduct(productFile);
        
        if (oSNAPProduct == null) {
        	Wasdi.DebugLog("ProcessingResources.getBandImage: SNAP product is null, impossibile to read. Return");
        	return Response.status(500).build();
        } else {
        	Wasdi.DebugLog("ProcessingResources.getBandImage: product read");
        }
        
		BandImageManager manager = new BandImageManager(oSNAPProduct);
		
		RasterDataNode raster = null;
		
		if (model.getFilterVM() != null) {
			Filter filter = model.getFilterVM().getFilter();
			FilterBand filteredBand = manager.getFilterBand(model.getBandName(), filter, model.getFilterIterationCount());
			if (filteredBand == null) {
				Wasdi.DebugLog("ProcessingResource.getBandImage: CANNOT APPLY FILTER TO BAND " + model.getBandName());
	        	return Response.status(500).build();
			}
			raster = filteredBand;
		} else {
			raster = oSNAPProduct.getBand(model.getBandName());
		}
		
		if (model.getVp_x()<0||model.getVp_y()<0||model.getImg_w()<=0||model.getImg_h()<=0) {
			Wasdi.DebugLog("ProcessingResources.getBandImage: Invalid Parameters: VPX= " + model.getVp_x() +" VPY= "+ model.getVp_y() +" VPW= "+ model.getVp_w() +" VPH= "+ model.getVp_h() + " OUTW = " + model.getImg_w() + " OUTH = " +model.getImg_h() );
			return Response.status(500).build();
		} else {
			Wasdi.DebugLog("ProcessingResources.getBandImage: parameters OK");
		}
		
		Rectangle vp = new Rectangle(model.getVp_x(), model.getVp_y(), model.getVp_w(), model.getVp_h());
		Dimension imgSize = new Dimension(model.getImg_w(), model.getImg_h());
		
		List<ProductMaskViewModel> productMasksModels = model.getProductMasks();
		if (productMasksModels!=null) {
			for (ProductMaskViewModel maskModel : productMasksModels) {
				Mask mask = oSNAPProduct.getMaskGroup().get(maskModel.getName());
				if (mask == null) {
					Wasdi.DebugLog("ProcessingResources.getBandImage: cannot find mask by name: " + maskModel.getName());
				} else {
					//set the user specified color
					mask.setImageColor(new Color(maskModel.getColorRed(), maskModel.getColorGreen(), maskModel.getColorBlue()));
					mask.setImageTransparency(maskModel.getTransparency());
					raster.getOverlayMaskGroup().add(mask);
				}
			}
		}
		
		BufferedImage img;
		try {
			img = manager.buildImageWithMasks(raster, imgSize, vp);
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(500).build();
		}
		
		if (img == null) {
			Wasdi.DebugLog("ProcessingResource.getBandImage: img null");
			return Response.status(500).build();
		}
		
		Wasdi.DebugLog("ProcessingResource.getBandImage: Generated image for band " + model.getBandName() + " X= " + model.getVp_x() + " Y= " + model.getVp_y() + " W= " + model.getVp_w() + " H= "  + model.getVp_h());
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    ImageIO.write(img, "jpg", baos);
	    byte[] imageData = baos.toByteArray();
		
		return Response.ok(imageData).build();
	}
	
	

	@POST
	@Path("/assimilation")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public String Assimilation(
			@FormDataParam("humidity") InputStream humidityFile,
			@FormDataParam("humidity") FormDataContentDisposition humidityFileMetaData,
			@HeaderParam("x-session-token") String sessionId,
			@QueryParam("midapath") String midaPath) {
		
		Wasdi.DebugLog("ProcessingResource.Assimilation");

		User user = Wasdi.GetUserFromSession(sessionId);
		try {

			//check authentication
			if (user == null || Utils.isNullOrEmpty(user.getUserId())) {
				return null;				
			}
			
			//build and check paths
			File assimilationWD = new File(m_oServletConfig.getInitParameter("AssimilationWDPath"));
			if (!assimilationWD.isDirectory()) {				
				System.out.println("ProcessingResource.Assimilation: ERROR: Invalid directory: " + assimilationWD.getAbsolutePath());
				throw new InternalServerErrorException("invalid directory in assimilation settings");				
			}						
			File midaTifFile = new File(midaPath);
			if (!midaTifFile.canRead()) {
				System.out.println("ProcessingResource.Assimilation: ERROR: Invalid mida path: " + midaTifFile.getAbsolutePath());
				throw new InternalServerErrorException("invalid path in assimilation settings");
			}						
			File humidityTifFile = new File(assimilationWD, UUID.randomUUID().toString() + ".tif");
			File resultDir = new File(m_oServletConfig.getInitParameter("AssimilationResultPath"));
			File resultTifFile = new File(resultDir, UUID.randomUUID().toString() + ".tif");
			
			
			//save uploaded file			
			int read = 0;
			byte[] bytes = new byte[1024];
			OutputStream out = new FileOutputStream(humidityTifFile);
			while ((read = humidityFile.read(bytes)) != -1) {
				out.write(bytes, 0, read);
			}
			out.flush();
			out.close();

			//execute assimilation
			if (launchAssimilation(midaTifFile, humidityTifFile, resultTifFile)) {
				String url = "wasdidownloads/" + resultTifFile.getName();
				return url;				
			}
			
			throw new InternalServerErrorException("unable to execute assimilation");
			
		} catch (Exception e) {
			System.out.println("ProcessingResource.Assimilation: error launching assimilation " + e.getMessage());
			e.printStackTrace();
			throw new InternalServerErrorException("error launching assimilation: " + e.getMessage());
		}

	}

	
	@POST
	@Path("/upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadModel(@FormDataParam("file") InputStream fileInputStream,
			@FormDataParam("file") FormDataContentDisposition fileMetaData, @HeaderParam("x-session-token") String sSessionId, @QueryParam("sWorkspaceId") String sWorkspaceId) throws Exception
	{ 
		Wasdi.DebugLog("ProcessingResource.UploadModel");
		
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser==null) return Response.status(Status.UNAUTHORIZED).build();
		if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(Status.UNAUTHORIZED).build();

		String sDownloadRootPath = m_oServletConfig.getInitParameter("DownloadRootPath");
		if (!sDownloadRootPath.endsWith("/"))
			sDownloadRootPath += "/";

		String sDownloadPath = sDownloadRootPath + oUser.getUserId()+ "/" + sWorkspaceId + "/" + "CONTINUUM";

		if(!Files.exists(Paths.get(sDownloadPath)))
		{
			if (Files.createDirectories(Paths.get(sDownloadPath))== null)
			{
				System.out.println("ProcessingResource.uploadMapFile: Directory " + sDownloadPath + " not created");
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}

		}

		try
		{
			int read = 0;
			byte[] bytes = new byte[1024];

			OutputStream out = new FileOutputStream(new File(sDownloadPath + "/" + fileMetaData.getFileName()));
			while ((read = fileInputStream.read(bytes)) != -1) 
			{
				out.write(bytes, 0, read);
			}
			out.flush();
			out.close();

			/* TODO: Abilitare questa parte se si vuole far partire l'assimilazione dopo l'upload del file (E' ancora da testare) 
			 * 
			 * 
			String sAssimilationContinuumPath =   m_oServletConfig.getInitParameter("AssimilationContinuumPath");
			String sMulesmeEstimatePath =   m_oServletConfig.getInitParameter("MulesmeStimePath");
			if (!sAssimilationContinuumPath.endsWith("/"))
				sAssimilationContinuumPath += "/";
			if (!sMulesmeEstimatePath.endsWith("/"))
				sMulesmeEstimatePath += "/";


			//Continuum format SoilMoistureItaly_20160801230000
			//Get continuum date
			SimpleDateFormat oDateFormat = new SimpleDateFormat("yyyyMMdd");
			String sFileName = fileMetaData.getFileName();
			String sDate = sFileName.split("_")[1].substring(0, 7);  
			Calendar oContinuumDate = Calendar.getInstance();
			Calendar oMulesmeDate = Calendar.getInstance();
			oContinuumDate.setTime(oDateFormat.parse(sDate));
			oMulesmeDate.setTime(oDateFormat.parse(sDate));
			oMulesmeDate.add(Calendar.DATE, 1);  // number of days to add
			String sMulesmeDate = oDateFormat.format(oMulesmeDate); 

			//Search in catalog soil moisture map with date = continuum date + 1 day
			CatalogRepository oRepo = new CatalogRepository();
			Catalog oCatalog = oRepo.GetCatalogsByDate(sMulesmeDate);
			if (oCatalog != null)
			{
				//Copy file into Mulesme Stime path
				Files.move(Paths.get(oCatalog.getFilePath()), Paths.get(sMulesmeEstimatePath));
				//Copy file from CONTINUUM to assimilation path
				Files.move(Paths.get(sDownloadPath + "/" + fileMetaData.getFileName()), Paths.get(sAssimilationContinuumPath));
				//launch assimilation
				LaunchAssimilation();
			}
			 */



		} catch (IOException e) 
		{
			throw new WebApplicationException("CatalogResources.uploadModel: Error while uploading file. Please try again !!");
		}




		return Response.ok().build();
	}

	private boolean launchAssimilation(File midaTifFile, File humidityTifFile, File resultTifFile) {

		try {
			
			String cmd[] = new String[] {
					m_oServletConfig.getInitParameter("AssimilationScript"),
					midaTifFile.getAbsolutePath(),
					humidityTifFile.getAbsolutePath(),
					resultTifFile.getAbsolutePath()
			};
			
			System.out.println("ProcessingResource.LaunchAssimilation: shell exec " + Arrays.toString(cmd));

			Process proc = Runtime.getRuntime().exec(cmd);
			BufferedReader input = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line;
            while((line=input.readLine()) != null) {
            	System.out.println("ProcessingResource.LaunchAssimilation: Assimilation stdout: " + line);
            }
			if (proc.waitFor() != 0) return false;
		} catch (Exception oEx) {
			System.out.println("ProcessingResource.LaunchAssimilation: error during assimilation process " + oEx.getMessage());
			oEx.printStackTrace();
			return false;
		}

		return true;
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

			String sPath = m_oServletConfig.getInitParameter("SerializationPath") + oProcess.getProcessObjId();

			
			OperatorParameter oParameter = GetParameter(operation); 
			oParameter.setSourceProductName(sSourceProductName);
			oParameter.setDestinationProductName(sDestinationProductName);
			oParameter.setWorkspace(sWorkspaceId);
			oParameter.setUserId(sUserId);
			oParameter.setExchange(sWorkspaceId);
			oParameter.setProcessObjId(oProcess.getProcessObjId());
			if (oSetting != null) oParameter.setSettings(oSetting);	
			
			SerializationUtils.serializeObjectToXML(sPath, oParameter);

//			String sLauncherPath = m_oServletConfig.getInitParameter("LauncherPath");
//			String sJavaExe = m_oServletConfig.getInitParameter("JavaExe");
//
//			String sShellExString = sJavaExe + " -jar " + sLauncherPath +" -operation " + operation + " -parameter " + sPath;
//
//			System.out.println("SnapOperations.ExecuteOperation: shell exec " + sShellExString);
//
//			Process oProc = Runtime.getRuntime().exec(sShellExString);


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
