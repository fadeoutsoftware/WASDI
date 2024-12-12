package it.fadeout.rest.resources;

import static wasdi.shared.utils.WasdiFileUtils.createDirectoryIfDoesNotExist;
import static wasdi.shared.utils.WasdiFileUtils.writeFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import it.fadeout.Wasdi;
import it.fadeout.rest.resources.largeFileDownload.FileStreamingOutput;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.SnapWorkflow;
import wasdi.shared.business.users.ResourceTypes;
import wasdi.shared.business.users.User;
import wasdi.shared.business.users.UserAccessRights;
import wasdi.shared.business.users.UserApplicationRole;
import wasdi.shared.business.users.UserResourcePermission;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.SnapWorkflowRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.data.UserResourcePermissionRepository;
import wasdi.shared.parameters.GraphParameter;
import wasdi.shared.parameters.OperatorParameter;
import wasdi.shared.parameters.settings.GraphSetting;
import wasdi.shared.utils.MailUtils;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.workflows.SnapWorkflowViewModel;
import wasdi.shared.viewmodels.workflows.WorkflowSharingViewModel;

/**
 * Worflows resource.
 * 
 * All the workflows are an ESA SNAP graph file, saved as XML.
 * 
 * Hosts API for
 * 	.Upload new workflows
 * 	.Update existing workflows
 * 	.Share workflows
 * 	.Download workflows
 * 	.Run workflows
 * 
 * @author p.campanella
 *
 */
@Path("workflows")
public class WorkflowsResource {
    
    /**
     * Upload and save a new SNAP Workflow using a XML file
     *
     * @param oFileInputStream Stream of the xml file
     * @param sSessionId User Session
     * @param sName Workflow Name
     * @param sDescription Workflow Description 
     * @return std http response
     */
    @POST
    @Path("/uploadfile")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadFile(@FormDataParam("file") InputStream oFileInputStream,
                                @HeaderParam("x-session-token") String sSessionId, @QueryParam("workspace") String sWorkspaceId,
                                @QueryParam("name") String sName, @QueryParam("description") String sDescription,
                                @QueryParam("public") Boolean bPublic) {

        WasdiLog.debugLog("WorkflowsResource.uploadFile( Ws: " + sWorkspaceId + ", Name: " + sName + ", Descr: " + sDescription + ", Public: " + bPublic + " )");

        try {
        	
            // Check authorization
            User oUser = Wasdi.getUserFromSession(sSessionId);

            if (oUser == null) {
            	WasdiLog.warnLog("WorkflowsResource.uploadFile: invalid session");
            	return Response.status(Status.UNAUTHORIZED).build();
            }        	
        	
            // Checks whether null file is passed
            if (oFileInputStream == null) {
            	WasdiLog.warnLog("WorkflowsResource.uploadFile: no file stream");
            	return Response.status(Status.BAD_REQUEST).build();
            }

            String sUserId = oUser.getUserId();

            // Get Workflows Path
            String sDirectoryPathname = PathsConfig.getWorkflowsPath();

            createDirectoryIfDoesNotExist(sDirectoryPathname);

            // Generate Workflow Id and file
            String sWorkflowId = UUID.randomUUID().toString();
            String sFilePathname = sDirectoryPathname + sWorkflowId + ".xml";
            File oWorkflowXmlFile = new File(sFilePathname);

            WasdiLog.debugLog("WorkflowsResource.uploadFile: workflow file Path: " + oWorkflowXmlFile.getPath());

            // save uploaded file
            writeFile(oFileInputStream, oWorkflowXmlFile);

            // Create Entity
            SnapWorkflow oWorkflow = new SnapWorkflow();
            oWorkflow.setName(sName);
            oWorkflow.setDescription(sDescription);
            oWorkflow.setFilePath(oWorkflowXmlFile.getPath());
            oWorkflow.setUserId(sUserId);
            oWorkflow.setWorkflowId(sWorkflowId);

            if (bPublic == null) oWorkflow.setIsPublic(false);
            else oWorkflow.setIsPublic(bPublic.booleanValue());

            if (Wasdi.getActualNode() != null) {
                oWorkflow.setNodeCode(Wasdi.getActualNode().getNodeCode());
                oWorkflow.setNodeUrl(Wasdi.getActualNode().getNodeBaseAddress());
            }
            
            boolean bResult = fillWorkflowIONodes(oWorkflowXmlFile, oWorkflow);
            
            if (!bResult)  {
            	WasdiLog.warnLog("WorkflowsResource.uploadFile: error finding IO Nodes");
            	return Response.serverError().build();
            }
            
            // Save the Workflow
            SnapWorkflowRepository oSnapWorkflowRepository = new SnapWorkflowRepository();
            oSnapWorkflowRepository.insertSnapWorkflow(oWorkflow);            

        } catch (Exception oEx) {
        	WasdiLog.errorLog("WorkflowsResource.uploadFile error: " + oEx);
        }

        return Response.ok().build();
    }
    
    /**
     * Update a SNAP Workflow XML file
     *
     * @param fileInputStream Xml file containing the new version of the Snap Workflow
     * @param sSessionId      Session of the current user
     * @param sWorkflowId     Used to check if the workflow exists, and then, upload the file
     * @return std http response
     */
    @POST
    @Path("/updatefile")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response updateFile(@FormDataParam("file") InputStream fileInputStream,
                                    @HeaderParam("x-session-token") String sSessionId,
                                    @QueryParam("workflowid") String sWorkflowId) {
        WasdiLog.debugLog("WorkflowsResource.updateFile( InputStream, WorkflowId: " + sWorkflowId);

        try {
            // Check authorization
            User oUser = Wasdi.getUserFromSession(sSessionId);

            if (oUser == null) {
            	WasdiLog.warnLog("WorkflowsResource.updateFile: invalid session");
            	return Response.status(Status.UNAUTHORIZED).build();
            }
            
            if (!PermissionsUtils.canUserWriteWorkflow(oUser.getUserId(), sWorkflowId)) {
            	WasdiLog.warnLog("WorkflowsResource.updateFile: user cannot write the workflow");
            	return Response.status(Status.FORBIDDEN).build();            	
            }
            
            // Check that the workflow exists on db
            SnapWorkflowRepository oSnapWorkflowRepository = new SnapWorkflowRepository();
            SnapWorkflow oWorkflow = oSnapWorkflowRepository.getSnapWorkflow(sWorkflowId);
            
            if (oWorkflow == null) {
                WasdiLog.debugLog("WorkflowsResource.updateFile: error in workflowId " + sWorkflowId + " not found on DB");
                return Response.notModified("WorkflowId not found, please check parameters").build();
            }            

            // Get Download Path
            String sDirectoryPathname = PathsConfig.getWorkflowsPath();

            createDirectoryIfDoesNotExist(sDirectoryPathname);
            
            // original xml file
            File oWorkflowXmlFile = new File(PathsConfig.getWorkflowsPath() + sWorkflowId + ".xml");
            // new xml file
            File oWorkflowXmlFileTemp = new File(PathsConfig.getWorkflowsPath() + sWorkflowId + ".xml.temp");
            //if the new one is ok delete the old and rename the ".temp" file
            // save uploaded file in ".temp" format
            
            try {
                writeFile(fileInputStream, oWorkflowXmlFileTemp);
            } 
            catch (Exception oEx) {
            	WasdiLog.errorLog("WorkflowsResource.updateFile error writing file: " + oEx);
            } 

            oWorkflow.getInputNodeNames().clear();
            oWorkflow.getOutputNodeNames().clear();
            
            fillWorkflowIONodes(oWorkflowXmlFileTemp, oWorkflow);
            
            // checks that the graph field is valid by checking the nodes
            try (FileReader oFileReader = new FileReader(oWorkflowXmlFileTemp)) {
            	
                // Overwrite the old file
                Files.write(oWorkflowXmlFile.toPath(), Files.readAllBytes(oWorkflowXmlFileTemp.toPath()));
                // Delete the temp file
                Files.delete(oWorkflowXmlFileTemp.toPath());

                WasdiLog.debugLog("WorkflowsResource.updateFile: workflow files updated! workflowID" + oWorkflow.getWorkflowId());

                if (Wasdi.getActualNode() != null) {
                    oWorkflow.setNodeCode(Wasdi.getActualNode().getNodeCode());
                    oWorkflow.setNodeUrl(Wasdi.getActualNode().getNodeBaseAddress());
                }

                // Updates the location on the current server
                oWorkflow.setFilePath(oWorkflowXmlFile.getPath());
                oSnapWorkflowRepository.updateSnapWorkflow(oWorkflow);

            } 
            catch (Exception oEx) {
                if (oWorkflowXmlFileTemp.exists()) {
                	boolean bIsFileDeleted = oWorkflowXmlFileTemp.delete();
                	WasdiLog.debugLog("WorkflowsResource.updateFile. Result of the deletion of the xml temporary file: " + bIsFileDeleted);
                }
                WasdiLog.errorLog("WorkflowsResource.updateFile error: ", oEx);
                return Response.status(Status.NOT_MODIFIED).build();
            }
        } 
        catch (Exception oEx2) {
        	WasdiLog.errorLog("WorkflowsResource.updateFile error: " + oEx2);
        }
        return Response.ok().build();
    }    

    /**
     * Obtain and returns the workflow's xml as a String
     *
     * @param sSessionId  current user session
     * @param sWorkflowId Id of the workflow
     * @return
     */
    @GET
    @Path("/getxml")
    @Produces(MediaType.APPLICATION_XML)
    public Response getXML(@HeaderParam("x-session-token") String sSessionId, @QueryParam("workflowId") String sWorkflowId) {

        WasdiLog.debugLog("WorkflowsResource.getXML( Workflow Id : " + sWorkflowId + ");");
        String sXml = "";
        
        try {
            // Check authorization
            User oUser = Wasdi.getUserFromSession(sSessionId);

            if (oUser == null) {
            	WasdiLog.warnLog("WorkflowsResource.getXML: invalid session");
            	return Response.status(Status.UNAUTHORIZED).build();
            }
            
            if (!PermissionsUtils.canUserAccessWorkflow(oUser.getUserId(), sWorkflowId)) {
            	WasdiLog.warnLog("WorkflowsResource.getXML: user cannot access to the workflow");
            	return Response.status(Status.FORBIDDEN).build();            	
            }            

            // Check that the workflow exists on db
            SnapWorkflowRepository oSnapWorkflowRepository = new SnapWorkflowRepository();
            SnapWorkflow oWorkflow = oSnapWorkflowRepository.getSnapWorkflow(sWorkflowId);
            
            if (oWorkflow == null) {
                WasdiLog.warnLog("WorkflowsResource.getXML: error in workflowId " + sWorkflowId + " not found on DB");
                return Response.notModified("WorkflowId not found").build();
            }

            // Get Path
            File oWorkflowsFile = new File(PathsConfig.getWorkflowsPath() + sWorkflowId + ".xml");

            if (!oWorkflowsFile.exists()) {
                WasdiLog.warnLog("WorkflowsResource.getXML( Workflow Id : " + sWorkflowId + ") Error, workflow file not found;");
                return Response.status(Status.BAD_REQUEST).build();
            }
            // Check is the owner or a the sharing
            sXml = new String(Files.readAllBytes(oWorkflowsFile.toPath()));
            return Response.ok(sXml, MediaType.APPLICATION_XML).build();

        } 
        catch (Exception oEx) {
        	WasdiLog.errorLog("WorkflowsResource.getXML error: " + oEx);
        } 
        return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * Updates the content of a workflow receiving the new XML in the body
     * 
     * @param sSessionId User Session Id
     * @param sWorkflowId Workflow Id
     * @param sGraphXml String containing the graph XML
     * @return
     */
    @POST
    @Path("/updatexml")
    public Response updateXML(@HeaderParam("x-session-token") String sSessionId,
                                 @QueryParam("workflowId") String sWorkflowId,
                                 @FormDataParam("graphXml") String sGraphXml) {

        // convert string to file and invoke updateGraphFile
        WasdiLog.debugLog("WorkflowsResource.updateXML: workflowId " + sWorkflowId + " invoke WorkflowsResource.updateFile");
        return updateFile(new ByteArrayInputStream(sGraphXml.getBytes(Charset.forName("UTF-8"))), sSessionId, sWorkflowId);
    }




    /**
     * Updates the parameters of a Snap Workflow
     *
     * @param sSessionId   the current session Id
     * @param sWorkflowId  the workflowId
     * @param sName        The name of the workflow
     * @param sDescription The description
     * @param bPublic      Enabled if the workflow is publicly available to all WASDI users
     * @return std http response
     * @throws Exception
     */
    @POST
    @Path("/updateparams")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response updateParams(
            @HeaderParam("x-session-token") String sSessionId,
            @QueryParam("workflowid") String sWorkflowId,
            @QueryParam("name") String sName,
            @QueryParam("description") String sDescription,
            @QueryParam("public") Boolean bPublic

    ) {

        WasdiLog.debugLog("WorkflowsResource.updateParams( InputStream, Workflow: " + sName + ", WorkflowId: " + sWorkflowId);
        
        User oUser = Wasdi.getUserFromSession(sSessionId);
        
        if (oUser == null) {
        	WasdiLog.warnLog("WorkflowsResource.updateParams: invalid session");
        	return Response.status(Status.UNAUTHORIZED).build();
        }
        
        if (!PermissionsUtils.canUserWriteWorkflow(oUser.getUserId(), sWorkflowId)) {
        	WasdiLog.warnLog("WorkflowsResource.updateParams: user cannot write to the workflow");
        	return Response.status(Status.FORBIDDEN).build();            	
        }        

        SnapWorkflowRepository oSnapWorkflowRepository = new SnapWorkflowRepository();
        SnapWorkflow oSnapWorkflow = oSnapWorkflowRepository.getSnapWorkflow(sWorkflowId);
        
        if (oSnapWorkflow == null) {
            return Response.status(Status.BAD_REQUEST).build();
        }
        
        oSnapWorkflow.setName(sName);
        oSnapWorkflow.setDescription(sDescription);
        oSnapWorkflow.setIsPublic(bPublic);
        oSnapWorkflowRepository.updateSnapWorkflow(oSnapWorkflow);


        return Response.ok().build();
    }


    /**
     * Get workflow list by user id
     *
     * @param sSessionId User Session Id
     * @return List of Snap Workflow View Models
     */
    @GET
    @Path("/getbyuser")
    public ArrayList<SnapWorkflowViewModel> getWorkflowsByUser(@HeaderParam("x-session-token") String sSessionId) {
        WasdiLog.debugLog("WorkflowsResource.getWorkflowsByUser");
        
        User oUser = Wasdi.getUserFromSession(sSessionId);

        if (oUser == null) {
            WasdiLog.warnLog("WorkflowsResource.getWorkflowsByUser: invalid session");
            return null;
        }
        
        String sUserId = oUser.getUserId();

        SnapWorkflowRepository oSnapWorkflowRepository = new SnapWorkflowRepository();
        ArrayList<SnapWorkflowViewModel> aoRetWorkflows = new ArrayList<>();
        try {

            List<SnapWorkflow> aoDbWorkflows = oSnapWorkflowRepository.getSnapWorkflowPublicAndByUser(sUserId);

            for (SnapWorkflow oCurrentWorkflow : aoDbWorkflows) {
            	// Convert the Workflow in View Model
                SnapWorkflowViewModel oWorkflowViewModel = SnapWorkflowViewModel.getFromWorkflow(oCurrentWorkflow);
                
				// Are we the owner?
				if (oCurrentWorkflow.getUserId().equals(oUser.getUserId())) {
					// Yes: not shared, our own, not read only
					oWorkflowViewModel.setSharedWithMe(false);
					oWorkflowViewModel.setReadOnly(false);
				}
				else {
					// For now lets assume is read only
					oWorkflowViewModel.setReadOnly(true);
				}                
                
                aoRetWorkflows.add(oWorkflowViewModel);
            }

            // find sharings by userId
            UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
            List<UserResourcePermission> aoWorkflowSharing = oUserResourcePermissionRepository.getWorkflowSharingsByUserId(sUserId);

            // For all the shared workflows
            for (UserResourcePermission oSharing : aoWorkflowSharing) {
                // Create the VM
                SnapWorkflow oSharedWithMe = oSnapWorkflowRepository.getSnapWorkflow(oSharing.getResourceId());
                SnapWorkflowViewModel oWorkflowViewModel = SnapWorkflowViewModel.getFromWorkflow(oSharedWithMe);

                if (oWorkflowViewModel.isPublic() == false) {
                    // This is shared and not public: add to return list
                    oWorkflowViewModel.setSharedWithMe(true);
					// Keep if read only or not
                    oWorkflowViewModel.setReadOnly(!oSharing.canWrite());                    
                    aoRetWorkflows.add(oWorkflowViewModel);
                } else {
                    // This is shared but public, so this is already in our return list
                    for (SnapWorkflowViewModel oCurrentWorkFlowViewModel : aoRetWorkflows) {
                        // Find it and set shared flag = true
                        if (oSharedWithMe.getWorkflowId().equals(oCurrentWorkFlowViewModel.getWorkflowId())) {
                            oCurrentWorkFlowViewModel.setSharedWithMe(true);
                            oCurrentWorkFlowViewModel.setReadOnly(!oSharing.canWrite());
                        }
                    }
                }
            }

        } catch (Exception oE) {
            WasdiLog.errorLog("WorkflowsResource.getWorkflowsByUser error: " + oE);
        }

        return aoRetWorkflows;
    }

    /**
     * Delete a workflow from id
     * If invoked from an user that's not the owner the sharing is removed instead
     *
     * @param sSessionId  from the session id the user is extracted
     * @param sWorkflowId Id of the workflow to delete
     * @return std http response
     */
    @GET
    @Path("/delete")
    public Response delete(@HeaderParam("x-session-token") String sSessionId, @QueryParam("workflowId") String sWorkflowId) {
        WasdiLog.debugLog("WorkflowsResource.delete( Workflow: " + sWorkflowId + " )");
        try {
            // Check User
            User oUser = Wasdi.getUserFromSession(sSessionId);

            if (oUser == null) {
                WasdiLog.warnLog("WorkflowsResource.delete: invalid session");
                return Response.status(Status.UNAUTHORIZED).build();
            }
            
            if (!PermissionsUtils.canUserAccessWorkflow(oUser.getUserId(), sWorkflowId)) {
            	WasdiLog.warnLog("WorkflowsResource.delete: user cannot access to the workflow");
            	return Response.status(Status.FORBIDDEN).build();            	
            }            

            String sUserId = oUser.getUserId();

            // Check if the workflow exists
            SnapWorkflowRepository oSnapWorkflowRepository = new SnapWorkflowRepository();
            SnapWorkflow oWorkflow = oSnapWorkflowRepository.getSnapWorkflow(sWorkflowId);

            if (oWorkflow == null) {
            	WasdiLog.warnLog("WorkflowsResource.delete: workflow not found");
            	return Response.status(Status.BAD_REQUEST).build();
            }

            // check if the current user is the owner of the workflow
            if (oWorkflow.getUserId().equals(sUserId) == false) {

                // check if the current user has a sharing of the workflow
                UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();

                if (oUserResourcePermissionRepository.isWorkflowSharedWithUser(sUserId, sWorkflowId)) {
                    oUserResourcePermissionRepository.deletePermissionsByUserIdAndWorkflowId(sUserId, sWorkflowId);
                    WasdiLog.debugLog("WorkflowsResource.delete: Deleted sharing between user " + sUserId + " and workflow " + oWorkflow.getName() + " Workflow files kept in place");
                    return Response.ok().build();
                }
                // not the owner && no sharing with you. You have no power here !
                return Response.status(Status.FORBIDDEN).build();
            }
            
            // Get Download Path on the current WASDI instance
            String sWorkflowFilePath = PathsConfig.getWorkflowsPath() + oWorkflow.getWorkflowId() + ".xml";

            if (!Utils.isNullOrEmpty(sWorkflowFilePath)) {
                File oWorkflowFile = new File(sWorkflowFilePath);
                if (oWorkflowFile.exists()) {
                    if (!oWorkflowFile.delete()) {
                        WasdiLog.debugLog("WorkflowsResource.delete: Error deleting the workflow file " + oWorkflow.getFilePath());
                    }
                }
            } else {
                WasdiLog.debugLog("WorkflowsResource.delete: workflow file path is null or empty.");
            }

            // Delete sharings
            UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
            oUserResourcePermissionRepository.deletePermissionsByWorkflowId(sWorkflowId);

            // Delete the workflow
            oSnapWorkflowRepository.deleteSnapWorkflow(sWorkflowId);
        } catch (Exception oE) {
            WasdiLog.errorLog("WorkflowsResource.delete error: " + oE);
            return Response.serverError().build();
        }
        return Response.ok().build();
    }

    /**
     * Method to add a sharing of a selected Workflow
     *
     * @param sSessionId  current session Id
     * @param sWorkflowId The workflow to be shared
     * @param sUserId     The user that will receive the access to the Workflow through the sharing
     * @return Primitive result with boolean vale to true if the operation was done, false instead
     */
    @PUT
    @Path("share/add")
    @Produces({"application/xml", "application/json", "text/xml"})
    public PrimitiveResult shareWorkflow(@HeaderParam("x-session-token") String sSessionId,
                                         @QueryParam("workflowId") String sWorkflowId, @QueryParam("userId") String sUserId, @QueryParam("rights") String sRights) {

        WasdiLog.debugLog("WorkflowsResource.shareWorkflow(Workflow : " + sWorkflowId + ", User: " + sUserId + " )");

        //init repositories
        UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
        SnapWorkflowRepository oWorkflowRepository = new SnapWorkflowRepository();
        
        // Validate Session
        User oRequesterUser = Wasdi.getUserFromSession(sSessionId);
        PrimitiveResult oResult = new PrimitiveResult();
        oResult.setBoolValue(false);

        if (oRequesterUser == null) {
            WasdiLog.warnLog("WorkflowsResource.shareWorkflow: invalid session");
            oResult.setStringValue("Invalid session.");
            return oResult;
        }
        
		// Use Read By default
		if (!UserAccessRights.isValidAccessRight(sRights)) {
			sRights = UserAccessRights.READ.getAccessRight();
		}        
        
        if (!PermissionsUtils.canUserWriteWorkflow(oRequesterUser.getUserId(), sWorkflowId) && !UserApplicationRole.isAdmin(oRequesterUser)) {
            WasdiLog.warnLog("WorkflowsResource.shareWorkflow: user cannot write the  workflow");
            oResult.setStringValue("Invalid user.");
            return oResult;
        }

        try {

            // Check if the processor exists and is of the user calling this API
            SnapWorkflow oWorkflow = oWorkflowRepository.getSnapWorkflow(sWorkflowId);

            if (oWorkflow == null) {
            	WasdiLog.warnLog("WorkflowsResource.shareWorkflow: invalid workflow");
                oResult.setStringValue("Invalid Workflow");
                return oResult;
            }
            
            if (oRequesterUser.getUserId().equals(sUserId) && !UserApplicationRole.isAdmin(oRequesterUser)) {
                WasdiLog.warnLog("WorkflowsResource.ShareWorkflow: auto sharing not so smart");
                oResult.setStringValue("Impossible to autoshare.");
                return oResult;
            }

            // Check the destination user
            UserRepository oUserRepository = new UserRepository();
            User oDestinationUser = oUserRepository.getUser(sUserId);
            
            // Can't find destination user for the sharing
            if (oDestinationUser == null) {
            	WasdiLog.warnLog("WorkflowsResource.ShareWorkflow: invalid target user");
                oResult.setStringValue("Can't find target user of the sharing");
                return oResult;
            }

            // Check if has been already shared
            if (oUserResourcePermissionRepository.isWorkflowSharedWithUser(sUserId, sWorkflowId)) {
            	WasdiLog.warnLog("WorkflowsResource.ShareWorkflow: already shared");
                oResult.setStringValue("Already shared");
                return oResult;
            }

            // Create and insert the sharing
            UserResourcePermission oWorkflowSharing = new UserResourcePermission();
            oWorkflowSharing.setResourceType(ResourceTypes.WORKFLOW.getResourceType());
            Timestamp oTimestamp = new Timestamp(System.currentTimeMillis());
            oWorkflowSharing.setOwnerId(oWorkflow.getUserId());
            oWorkflowSharing.setUserId(sUserId);
            oWorkflowSharing.setResourceId(sWorkflowId);
            oWorkflowSharing.setCreatedBy(oRequesterUser.getUserId());
            oWorkflowSharing.setCreatedDate((double) oTimestamp.getTime());
            oWorkflowSharing.setPermissions(sRights);
            oUserResourcePermissionRepository.insertPermission(oWorkflowSharing);

            WasdiLog.debugLog("WorkflowsResource.shareWorkflow: Workflow " + sWorkflowId + " Shared from " + oRequesterUser.getUserId() + " to " + sUserId);

            try {

                String sTitle = "Workflow " + oWorkflow.getName() + " Shared";
                String sMessage = "The user " + oRequesterUser.getUserId() + " shared with you the Workflow: " + oWorkflow.getName();
                
                MailUtils.sendEmail(WasdiConfig.Current.notifications.sftpManagementMailSender, sUserId, sTitle, sMessage);
            } catch (Exception oEx) {
                WasdiLog.errorLog("WorkflowsResource.shareWorkflow: notification exception " + oEx.toString());
            }

        } catch (Exception oEx) {
            WasdiLog.errorLog("WorkflowsResource.shareWorkflow error: " + oEx);

            oResult.setStringValue("Error in save process");
            oResult.setBoolValue(false);

            return oResult;
        }

        oResult.setStringValue("Done");
        oResult.setBoolValue(true);

        return oResult;
    }

    /**
     * Removes a sharing of a workflow
     * 
     * @param sSessionId User Session Id 
     * @param sWorkflowId Workflow Id
     * @param sUserId User that has to be removed from the sharing list of WorkflowId
     * @return PrimitiveResult with boolValue = true and stringValue = done if ok, otherwise false and a text describing the error
     */
    @DELETE
    @Path("share/delete")
    @Produces({"application/xml", "application/json", "text/xml"})
    public PrimitiveResult deleteUserSharingWorkflow(@HeaderParam("x-session-token") String sSessionId, @QueryParam("workflowId") String sWorkflowId, @QueryParam("userId") String sUserId) {

        WasdiLog.debugLog("WorkflowsResource.deleteUserSharedWorkflow( ProcId: " + sWorkflowId + ", User:" + sUserId + " )");
        PrimitiveResult oResult = new PrimitiveResult();
        oResult.setBoolValue(false);
        try {
            // Validate Session
            User oOwnerUser = Wasdi.getUserFromSession(sSessionId);

            if (oOwnerUser == null) {
                WasdiLog.warnLog("WorkflowsResource.deleteUserSharedWorkflow: invalid session");
                oResult.setStringValue("Invalid session");
                return oResult;
            }

            if (Utils.isNullOrEmpty(sUserId)) {
            	WasdiLog.warnLog("WorkflowsResource.deleteUserSharedWorkflow: invalid target user");
                oResult.setStringValue("Invalid shared user.");
                return oResult;
            }
            
            if (!PermissionsUtils.canUserAccessWorkflow(oOwnerUser.getUserId(), sWorkflowId) &&
            		!UserApplicationRole.isAdmin(oOwnerUser)) {
                WasdiLog.warnLog("WorkflowsResource.deleteUserSharedWorkflow: user cannot access the workflow");
                oResult.setStringValue("Forbidden");
                return oResult;            	
            }

            try {
            	
                UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
                UserResourcePermission oWorkflowShare = oUserResourcePermissionRepository.getWorkflowSharingByUserIdAndWorkflowId(sUserId, sWorkflowId);

                if (oWorkflowShare != null) {
                    // if the user making the call is the one on the sharing OR
                    if (oWorkflowShare.getUserId().equals(oOwnerUser.getUserId()) ||
                            // if the user making the call is the owner of the workflow
                            oWorkflowShare.getOwnerId().equals(oOwnerUser.getUserId())) {
                        // Delete the sharing
                        oUserResourcePermissionRepository.deletePermissionsByUserIdAndWorkflowId(sUserId, sWorkflowId);
                    } else {
                        oResult.setStringValue("Unauthorized");
                        return oResult;
                    }
                } else {
                    oResult.setStringValue("Sharing not found");
                    return oResult;
                }
            } catch (Exception oEx) {
                WasdiLog.errorLog("WorkflowsResource.deleteUserSharedWorkflow error: " + oEx);
                oResult.setStringValue("Error deleting processor sharing");
                return oResult;
            }

            oResult.setStringValue("Done");
            oResult.setBoolValue(true);
        } 
        catch (Exception oE) {
            WasdiLog.errorLog("WorkflowsResource.deleteUserSharedWorkflow error: " + oE);
        }
        return oResult;
    }

    /**
     * Retrieves the active sharings given a workflow
     *
     * @param sSessionId User Session Id
     * @param sWorkflowId Workflows Id
     * @return list of Workflow Sharing View Models
     */
    @GET
    @Path("share/byworkflow")
    @Produces({"application/xml", "application/json", "text/xml"})
    public List<WorkflowSharingViewModel> getEnableUsersSharedWorkflow(@HeaderParam("x-session-token") String
                                                                               sSessionId, @QueryParam("workflowId") String sWorkflowId) {
        ArrayList<WorkflowSharingViewModel> oResult = new ArrayList<WorkflowSharingViewModel>();
        WasdiLog.debugLog("WorkflowsResource.getEnableUsersSharedWorkflow(  Workflow : " + sWorkflowId + " )");

        // Validate Session
        User oRequestingUser = Wasdi.getUserFromSession(sSessionId);

        if (oRequestingUser == null) {
            WasdiLog.warnLog("WorkflowsResource.getEnableUsersSharedWorkflow: invalid session");
            return oResult;
        }
        
        if (!PermissionsUtils.canUserAccessWorkflow(oRequestingUser.getUserId(), sWorkflowId)) {
            WasdiLog.warnLog("WorkflowsResource.getEnableUsersSharedWorkflow: user cannot access workflow");
            return oResult;        	
        }

        try {

            // Check if the processor exists and is of the user calling this API
            SnapWorkflowRepository oWorkflowRepository = new SnapWorkflowRepository();
            SnapWorkflow oValidateWorkflow = oWorkflowRepository.getSnapWorkflow(sWorkflowId);

            if (oValidateWorkflow == null) {
                // return
                WasdiLog.debugLog("WorkflowsResource.getEnableUsersSharedWorkflow: Workflow not found");
                // if something went wrong returns an empty set
                return oResult;
            }

            //Retrieve and returns the sharings
            UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
            oUserResourcePermissionRepository.getWorkflowSharingsByWorkflowId(sWorkflowId).forEach(oElement -> {
                oResult.add(new WorkflowSharingViewModel(oElement));
            });
            return oResult;

        } catch (Exception oEx) {
            WasdiLog.errorLog("WorkflowsResource.getEnableUsersSharedWorkflow error: " + oEx);
            return oResult;
        }


    }

    /**
     * Executes a Workflow from workflow Id
     *
     * @param sSessionId User Session Id
     * @param sWorkspaceId Actual Workspace Id
     * @param sParentProcessWorkspaceId Parent Process Id 
     * @param oSnapWorkflowViewModel Snap Workflow View Model with the info to run the workflow
     * @return Primitive result with boolValue = true and intValue = 200 if ok otherwise false and http error
     */
    @POST
    @Path("/run")
    public PrimitiveResult run(@HeaderParam("x-session-token") String sSessionId,
                                                      @QueryParam("workspace") String sWorkspaceId,
                                                      @QueryParam("parent") String sParentProcessWorkspaceId,
                                                      SnapWorkflowViewModel oSnapWorkflowViewModel) {

        PrimitiveResult oResult = new PrimitiveResult();
        WasdiLog.debugLog("WorkflowsResource.run( Ws: " + sWorkspaceId + ", ... )");
        
        User oUser = Wasdi.getUserFromSession(sSessionId);
        
        if (oUser == null) {
            WasdiLog.warnLog("WorkflowsResource.run: invalid session");
            oResult.setBoolValue(false);
            oResult.setIntValue(401);
            return oResult;
        }
        
        if (!PermissionsUtils.userHasValidSubscription(oUser)) {
            WasdiLog.warnLog("WorkflowsResource.run: user does not have a valid subscription");
            oResult.setBoolValue(false);
            oResult.setIntValue(401);
            return oResult;
        }

        try {

            GraphSetting oGraphSettings = new GraphSetting();
            String sGraphXml;

            SnapWorkflowRepository oSnapWorkflowRepository = new SnapWorkflowRepository();
            SnapWorkflow oWF = oSnapWorkflowRepository.getSnapWorkflow(oSnapWorkflowViewModel.getWorkflowId());

            if (oWF == null) {
            	WasdiLog.warnLog("WorkflowsResource.run: invalid workflow");
                oResult.setBoolValue(false);
                oResult.setIntValue(500);
                return oResult;
            }
            
            if (!PermissionsUtils.canUserAccessWorkflow(oUser.getUserId(), oWF.getWorkflowId())) {
                WasdiLog.warnLog("WorkflowsResource.run: user cannot access workflow");
                oResult.setBoolValue(false);
                oResult.setIntValue(401);
                return oResult;            	
            }
            
            if (!PermissionsUtils.canUserWriteWorkspace(oUser.getUserId(), sWorkspaceId)) {
                WasdiLog.warnLog("WorkflowsResource.run: user cannot write in the workspace");
                oResult.setBoolValue(false);
                oResult.setIntValue(401);
                return oResult;            	
            }            

            String sWorkflowPath = PathsConfig.getWorkflowsPath() + oWF.getWorkflowId() + ".xml";
            File oWorkflowFile = new File(sWorkflowPath);

            if (!oWorkflowFile.exists()) {
                WasdiLog.debugLog("WorkflowsResource.run: Workflow file not on this node. Try to download it");

                String sDownloadedWorkflowPath = Wasdi.downloadWorkflow(oWF.getNodeUrl(), oWF.getWorkflowId(), sSessionId);

                if (Utils.isNullOrEmpty(sDownloadedWorkflowPath)) {
                    WasdiLog.debugLog("Error downloading workflow. Return error");
                    oResult.setBoolValue(false);
                    oResult.setIntValue(500);
                    return oResult;
                }

                sWorkflowPath = sDownloadedWorkflowPath;
            }

            try (FileInputStream oFileInputStream = new FileInputStream(sWorkflowPath)) {
                String sWorkFlowName = oWF.getName().replace(' ', '_');

                sGraphXml = IOUtils.toString(oFileInputStream, Charset.defaultCharset().name());
                oGraphSettings.setGraphXml(sGraphXml);
                oGraphSettings.setWorkflowName(sWorkFlowName);

                oGraphSettings.setInputFileNames(oSnapWorkflowViewModel.getInputFileNames());
                oGraphSettings.setInputNodeNames(oSnapWorkflowViewModel.getInputNodeNames());
                oGraphSettings.setOutputFileNames(oSnapWorkflowViewModel.getOutputFileNames());
                oGraphSettings.setOutputNodeNames(oSnapWorkflowViewModel.getOutputNodeNames());
                
                if (oSnapWorkflowViewModel.getTemplateParams() != null) {
                	oGraphSettings.setTemplateParams(oSnapWorkflowViewModel.getTemplateParams());
                }

                String sSourceProductName = "";
                String sDestinationProdutName = "";

                if (oSnapWorkflowViewModel.getInputFileNames().size() > 0) {
                    sSourceProductName = oSnapWorkflowViewModel.getInputFileNames().get(0);
                    sDestinationProdutName = WasdiFileUtils.getFileNameWithoutLastExtension(sSourceProductName) + "_" + sWorkFlowName + WasdiFileUtils.getFileNameExtension(sSourceProductName); 
                }
                
                if (oGraphSettings.getOutputFileNames().size()>0) {
                	sDestinationProdutName = oSnapWorkflowViewModel.getOutputFileNames().get(0);
                }

                try {
                    String sProcessObjId = Utils.getRandomName();

                    // Create Operator instance
                    OperatorParameter oParameter = new GraphParameter();

                    // Set common settings
                    oParameter.setSourceProductName(sSourceProductName);
                    oParameter.setDestinationProductName(sDestinationProdutName);
                    oParameter.setWorkspace(sWorkspaceId);
                    oParameter.setUserId(oUser.getUserId());
                    oParameter.setExchange(sWorkspaceId);
                    oParameter.setProcessObjId(sProcessObjId);
                    oParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sWorkspaceId));
                    
                    // Set Graph Settings
                    oParameter.setSettings(oGraphSettings);

                    return Wasdi.runProcess(oUser.getUserId(), sSessionId, LauncherOperations.GRAPH.toString(), sSourceProductName, oParameter, sParentProcessWorkspaceId);

                } catch (IOException oEx) {
                    WasdiLog.errorLog("WorkflowsResource.run: " + oEx);
                    oResult.setBoolValue(false);
                    oResult.setIntValue(500);
                    return oResult;
                } catch (Exception oEx) {
                    WasdiLog.errorLog("WorkflowsResource.run: " + oEx);
                    oResult.setBoolValue(false);
                    oResult.setIntValue(500);
                    return oResult;
                }
            }
        } catch (Exception oEx) {
            WasdiLog.errorLog("WorkflowsResource.run: Error " + oEx.toString());
            oResult.setBoolValue(false);
            oResult.setIntValue(500);
            return oResult;
        }
    }

    /**
     * Download a file with the xml of the workflow
     * @param sSessionId User Session Id
     * @param sTokenSessionId User session that can be passed as a query param to be used by browser
     * @param sWorkflowId Workflow id to download 
     * @return stream of the file
     */
    @GET
    @Path("download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response download(@HeaderParam("x-session-token") String sSessionId,
                                      @QueryParam("token") String sTokenSessionId,
                                      @QueryParam("workflowId") String sWorkflowId) {

        WasdiLog.debugLog("WorkflowsResource.download( WorkflowId: " + sWorkflowId + " )");

        try {

            if (Utils.isNullOrEmpty(sSessionId) == false) {
                sTokenSessionId = sSessionId;
            }

            User oUser = Wasdi.getUserFromSession(sTokenSessionId);

            if (oUser == null) {
                WasdiLog.warnLog("WorkflowsResource.download: invalid session");
                return Response.status(Status.UNAUTHORIZED).build();
            }
            
            if (!PermissionsUtils.canUserAccessWorkflow(oUser.getUserId(), sWorkflowId)) {
                WasdiLog.warnLog("WorkflowsResource.download: user cannot access the workflow");
                return Response.status(Status.FORBIDDEN).build();            	
            }

            SnapWorkflowRepository oSnapWorkflowRepository = new SnapWorkflowRepository();
            SnapWorkflow oSnapWorkflow = oSnapWorkflowRepository.getSnapWorkflow(sWorkflowId);

            // Take path
            String sWorkflowXmlPath = PathsConfig.getWorkflowsPath() + sWorkflowId + ".xml";

            File oFile = new File(sWorkflowXmlPath);

            ResponseBuilder oResponseBuilder = null;

            if (oSnapWorkflow == null) {
                WasdiLog.warnLog("WorkflowsResource.download:  workflowId " + sWorkflowId + " not found on DB");
                oResponseBuilder = Response.noContent();
                return oResponseBuilder.build();
            }

            if (oFile.exists() == false) {
                WasdiLog.warnLog("WorkflowsResource.download: file does not exists " + oFile.getPath());
                oResponseBuilder = Response.serverError();
            } else {

                WasdiLog.debugLog("WorkflowsResource.download: returning file " + oFile.getPath());

                FileStreamingOutput oStream;
                oStream = new FileStreamingOutput(oFile);

                oResponseBuilder = Response.ok(oStream);
                oResponseBuilder.header("Content-Disposition", "attachment; filename=" + oSnapWorkflow.getName() + ".xml");
                oResponseBuilder.header("Content-Length", Long.toString(oFile.length()));
            }

            return oResponseBuilder.build();

        } catch (Exception oEx) {
            WasdiLog.errorLog("WorkflowsResource.download: " + oEx);
        }

        return null;
    }    

    /**
     * Fills the SnapWorkflow entity with input and output nodes
     * parsing the original SNAP XML searching for the corrisponding nodes
     * @param oFile XML File of the workflow
     * @param oSnapWorflow WASDI Entity
     * @return true if ok, false in case of problems
     */
    protected boolean fillWorkflowIONodes(File oFile, SnapWorkflow oSnapWorflow) {
    	
		DocumentBuilderFactory oDocBuildFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder oDocBuilder;
		
		try {
			
			oDocBuilder = oDocBuildFactory.newDocumentBuilder();
			Document oWorkflowXml = oDocBuilder.parse(oFile);
			oWorkflowXml.getDocumentElement().normalize();
			WasdiLog.debugLog("fillWorkflowIONodes.Root element: " + oWorkflowXml.getDocumentElement().getNodeName());
			
			// loop through each item
			NodeList aoItems = oWorkflowXml.getElementsByTagName("node");
			
			for (int iItem = 0; iItem < aoItems.getLength(); iItem++)
			{
				org.w3c.dom.Node oNode = aoItems.item(iItem);
				
				if (oNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
					Element oNodeElement = (Element) oNode;
					
					if (oNodeElement.hasAttribute("id")) {
						String sId = oNodeElement.getAttribute("id");
						NodeList aoOperatorItems = oNodeElement.getElementsByTagName("operator");
						
						if (aoOperatorItems == null) continue;
						if (aoOperatorItems.getLength()<=0) continue;
						
						String sOperator = aoOperatorItems.item(0).getTextContent();
						
						if (sOperator==null) continue;
						
						if (sOperator.equals("Write")) {
							oSnapWorflow.getOutputNodeNames().add(sId);
							WasdiLog.debugLog("fillWorkflowIONodes.fillWorkflowIONodes: Found Write Node with id = " + sId);
						}
						else if (sOperator.equals("Read")) {
							oSnapWorflow.getInputNodeNames().add(sId);
							WasdiLog.debugLog("fillWorkflowIONodes.fillWorkflowIONodes: Found Read Node with id = " + sId);
						}						
						
					}
				}
			}
			
			return true;

		} catch (Exception oEx) {
			WasdiLog.errorLog("WorkflowsResource.fillWorkflowIONodes: " + oEx);
			return false;
		}	
    }
}
