package it.fadeout.rest.resources;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import it.fadeout.Wasdi;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.User;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.GraphParameter;
import wasdi.shared.parameters.MosaicParameter;
import wasdi.shared.parameters.MultiSubsetParameter;
import wasdi.shared.parameters.OperatorParameter;
import wasdi.shared.parameters.RegridParameter;
import wasdi.shared.parameters.SubsetParameter;
import wasdi.shared.parameters.settings.ISetting;
import wasdi.shared.parameters.settings.MosaicSetting;
import wasdi.shared.parameters.settings.MultiSubsetSetting;
import wasdi.shared.parameters.settings.RegridSetting;
import wasdi.shared.parameters.settings.SubsetSetting;
import wasdi.shared.utils.LauncherOperationsUtils;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.PrimitiveResult;

/**
 * Processing Resource.
 * Hosts the API for:
 * 	.execute WASDI embedded operations
 * 		.mosaic
 * 		.subset
 * 		.multisubset
 * 		.regrid
 * 	.route the execution of a generic process in the right computing node
 * @author p.campanella
 *
 */
@Path("/processing")
public class ProcessingResources {
	
	/**
	 * Servlet Config to access web.xml file
	 */
    @Context
    ServletConfig m_oServletConfig;
    
    /**
     * Trigger mosaic operation
     * 
     * @param sSessionId User session id
     * @param sDestinationProductName output file name
     * @param sWorkspaceId Workspace
     * @param sParentId Proc Id of the parent
     * @param oSetting Mosaic Settings View Model
     * @return Primitive result with the http status of the operation
     * @throws IOException
     */
    @POST
    @Path("mosaic")
    @Produces({"application/xml", "application/json", "text/xml"})
    public PrimitiveResult mosaic(@HeaderParam("x-session-token") String sSessionId,
                                  @QueryParam("name") String sDestinationProductName,
                                  @QueryParam("workspace") String sWorkspaceId,
                                  @QueryParam("parent") String sParentId, MosaicSetting oSetting) throws IOException {
        Utils.debugLog("ProcessingResources.Mosaic( Destination: " + sDestinationProductName + ", Ws:" + sWorkspaceId + ", ... )");
        return callExecuteSNAPOperation(sSessionId, "", sDestinationProductName, sWorkspaceId, oSetting, LauncherOperations.MOSAIC, sParentId);
    }
    
    /**
     * Trigger a regrid operation 
     * @param sSessionId User Session
     * @param sDestinationProductName output file name
     * @param sWorkspaceId Workspace
     * @param sParentId Proc Id of the parent
     * @param oSetting Regrid Setting View Model
     * @return Primitive result with the http status of the operation
     * @throws IOException
     */
    @POST
    @Path("regrid")
    @Produces({"application/xml", "application/json", "text/xml"})
    public PrimitiveResult regrid(@HeaderParam("x-session-token") String sSessionId,
                                  @QueryParam("name") String sDestinationProductName,
                                  @QueryParam("workspace") String sWorkspaceId,
                                  @QueryParam("parent") String sParentId, RegridSetting oSetting) throws IOException {
        Utils.debugLog("ProcessingResources.Regrid( Dest: " + sDestinationProductName + ", Ws: " + sWorkspaceId + ", ... )");
        return callExecuteSNAPOperation(sSessionId, "", sDestinationProductName, sWorkspaceId, oSetting, LauncherOperations.REGRID, sParentId);
    }
    
    /**
     * Trigger a subset operation. This operation should be avoided, use multisubset instead
     * 
     * @param sSessionId User Session
     * @param sDestinationProductName output file name
     * @param sWorkspaceId Workspace
     * @param sParentId Proc Id of the parent
     * @param oSetting Subset Setting View Model
     * @return Primitive result with the http status of the operation
     * 
     * @throws IOException
     */
    @POST
    @Path("subset")
    @Produces({"application/xml", "application/json", "text/xml"})
    public PrimitiveResult subset(@HeaderParam("x-session-token") String sSessionId,
                                  @QueryParam("source") String sSourceProductName,
                                  @QueryParam("name") String sDestinationProductName,
                                  @QueryParam("workspace") String sWorkspaceId,
                                  @QueryParam("parent") String sParentId, SubsetSetting oSetting) throws IOException {
        Utils.debugLog("ProcessingResources.Subset( Source: " + sSourceProductName + ", Dest:" + sDestinationProductName + ", Ws:" + sWorkspaceId + ", ... )");
        return callExecuteSNAPOperation(sSessionId, sSourceProductName, sDestinationProductName, sWorkspaceId, oSetting, LauncherOperations.SUBSET, sParentId);
    }

    /**
     * Trigger the execution of a multi subset operation.
     * 
     * @param sSessionId User Session
     * @param sDestinationProductName output file name
     * @param sWorkspaceId Workspace
     * @param sParentId Proc Id of the parent
     * @param oSetting Multi Subset Setting View Model
     * @return Primitive result with the http status of the operation
     *
     * @throws IOException
     */
    @POST
    @Path("multisubset")
    @Produces({"application/xml", "application/json", "text/xml"})
    public PrimitiveResult multiSubset(@HeaderParam("x-session-token") String sSessionId,
                                       @QueryParam("source") String sSourceProductName,
                                       @QueryParam("name") String sDestinationProductName,
                                       @QueryParam("workspace") String sWorkspaceId,
                                       @QueryParam("parent") String sParentId, MultiSubsetSetting oSetting) throws IOException {
        Utils.debugLog("ProcessingResources.MultiSubset( Source: " + sSourceProductName + ", Dest: " + sDestinationProductName + ", Ws:" + sWorkspaceId + ", ... )");
        return callExecuteSNAPOperation(sSessionId, sSourceProductName, sDestinationProductName, sWorkspaceId, oSetting, LauncherOperations.MULTISUBSET, sParentId);
    }

    /**
     * Trigger the execution in the launcher of a generic SNAP Operation
     *
     * @param sSessionId                User Session Id
     * @param sSourceProductName        Source Product Name
     * @param sDestinationProductName   Target Product Name
     * @param sWorkspaceId              Active Workspace
     * @param oSetting                  Generic SNAP Operation Settings
     * @param oOperation                Launcher Operation Type
     * @param sParentProcessWorkspaceId Id of the parent Process Workspace or null
     * @return Primitive result with boolValue = true 
     */
    private PrimitiveResult callExecuteSNAPOperation(String sSessionId, String sSourceProductName, String
            sDestinationProductName, String sWorkspaceId, ISetting oSetting, LauncherOperations oOperation, String
                                                     sParentProcessWorkspaceId) {

        Utils.debugLog("ProsessingResources.executeOperation (Source: " + sSourceProductName + ", Destination: "
                + sDestinationProductName + ", WS: " + sWorkspaceId + ", Parent: " + sParentProcessWorkspaceId + " )");
        PrimitiveResult oResult = new PrimitiveResult();
        String sProcessObjId = "";
        
        User oUser = Wasdi.getUserFromSession(sSessionId);
        
        // Is valid?
        if (oUser == null) {

            // Not authorized
            oResult.setIntValue(401);
            oResult.setBoolValue(false);

            return oResult;
        }

        try {
            // Update process list

            sProcessObjId = Utils.GetRandomName();

            // Create Operator instance
            OperatorParameter oParameter = getParameter(oOperation);

            if (oParameter == null) {
                Utils.debugLog("ProsessingResources.ExecuteOperation: impossible to create the parameter from the operation");
                oResult.setBoolValue(false);
                oResult.setIntValue(500);
                return oResult;
            }

            // Set common settings
            oParameter.setSourceProductName(sSourceProductName);
            oParameter.setDestinationProductName(sDestinationProductName);
            oParameter.setWorkspace(sWorkspaceId);
            oParameter.setUserId(oUser.getUserId());
            oParameter.setExchange(sWorkspaceId);
            oParameter.setProcessObjId(sProcessObjId);
            oParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sWorkspaceId));

            // Do we have settings?
            if (oSetting != null) oParameter.setSettings(oSetting);

            // Serialization Path
            String sPath = m_oServletConfig.getInitParameter("SerializationPath");

            return Wasdi.runProcess(oUser.getUserId(), sSessionId, oOperation.name(), sSourceProductName, sPath, oParameter, sParentProcessWorkspaceId);

        } catch (IOException e) {
            Utils.debugLog("ProsessingResources.ExecuteOperation: " + e);
            oResult.setBoolValue(false);
            oResult.setIntValue(500);
            return oResult;
        } catch (Exception e) {
            Utils.debugLog("ProsessingResources.ExecuteOperation: " + e);
            oResult.setBoolValue(false);
            oResult.setIntValue(500);
            return oResult;
        }
    }
    
    /**
     * Run process: this API takes in input a Parameter Object in the body and all the needed info as Query param to start a new Launcher process.
     * This is used by WASDI when routing run requests to different computing nodes.
     * This should be called only by the main node and not directly by client or libraries.
     * It calls the analogue static method in Wasdi that looks if this is the node where the involved workspace is.
     * If this is the node, the parameter is written in the disk and a New Process Workspace entity is created in the db
     * The main node should instead call this API on computing node that host the workspace in other case 
     *  
     * @param sSessionId User Session
     * @param sOperationType LauncherOperation value
     * @param sProductName Product Name as specified in the Parameter
     * @param sParentProcessWorkspaceId Proc id of the parent
     * @param sOperationSubType Operation sub type when available
     * @param sParameter XML representation of the Parameter of the operation
     * @return Primitive Result with the http codes
     * @throws IOException
     */
    @POST
    @Path("run")
    @Produces({"application/xml", "application/json", "text/xml"})
    public PrimitiveResult runProcess(@HeaderParam("x-session-token") String sSessionId,
                                      @QueryParam("operation") String sOperationType, @QueryParam("name") String
                                              sProductName, @QueryParam("parent") String sParentProcessWorkspaceId, @QueryParam("subtype") String
                                              sOperationSubType, String sParameter) throws IOException {
    	
    	// Make sure is "" and not null for safe programming
        if (Utils.isNullOrEmpty(sOperationSubType)) sOperationSubType = "";
        
        // Same for parent process Id
        if (Utils.isNullOrEmpty(sParentProcessWorkspaceId)) sParentProcessWorkspaceId = "";
        
        // Log intro
        Utils.debugLog("ProsessingResources.runProcess( Operation: " + sOperationType + ", OperationSubType: " + sOperationSubType + ", Product: " + sProductName + " Parent Id: " + sParentProcessWorkspaceId + ")");
        
        PrimitiveResult oResult = new PrimitiveResult();

        try {
        	
        	// Validate the Launcher Operation
            if (!LauncherOperationsUtils.isValidLauncherOperation(sOperationType)) {
                // Bad request
                oResult.setIntValue(400);
                oResult.setBoolValue(false);
                return oResult;
            }
            
            
            // Check the user
            User oUser = Wasdi.getUserFromSession(sSessionId);

            // Is valid?
            if (Utils.isNullOrEmpty(oUser.getUserId())) {

                // Not authorised
                oResult.setIntValue(401);
                oResult.setBoolValue(false);

                return oResult;
            }
            
            // Get an instance of the right parameter
            BaseParameter oParameter = BaseParameter.getParameterFromOperationType(sOperationType);

            if (oParameter == null) {
                // Error
                oResult.setIntValue(500);
                oResult.setBoolValue(false);

                return oResult;
            }
            
            // Deserialize the parameter received in the Body
            oParameter = (BaseParameter) SerializationUtils.deserializeStringXMLToObject(sParameter);
            
            String sPath = m_oServletConfig.getInitParameter("SerializationPath");
            
            // Make Wasdi handle this request: this should be in this node...
            return Wasdi.runProcess(oUser.getUserId(), sSessionId, sOperationType, sOperationSubType, sProductName, sPath, oParameter, sParentProcessWorkspaceId);
        } catch (Exception oE) {
            Utils.debugLog("ProcessingResources.runProcess: " + oE);
            oE.printStackTrace();
            oResult.setStringValue(oE.toString());
            oResult.setIntValue(500);
            oResult.setBoolValue(false);

            return oResult;
        }

    }


    /**
     * Get the parameter Object for a specific Launcher Operation
     * This supports only the operations that can be triggered by this API resource
     *
     * @param oOperation Type of operation
     * @return Operator Parameter for the input Type Operation 
     */
    private OperatorParameter getParameter(LauncherOperations oOperation) {
    	
        switch (oOperation) {
            case MOSAIC:
                return new MosaicParameter();
            case SUBSET:
                return new SubsetParameter();
            case MULTISUBSET:
                return new MultiSubsetParameter();
            case REGRID:
                return new RegridParameter();
            default:
                return null;

        }
    }
}
