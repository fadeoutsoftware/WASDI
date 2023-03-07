package it.fadeout.rest.resources;

import static wasdi.shared.business.UserApplicationPermission.ADMIN_DASHBOARD;
import static wasdi.shared.utils.WasdiFileUtils.createDirectoryIfDoesNotExist;
import static wasdi.shared.utils.WasdiFileUtils.writeFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

import org.apache.commons.io.IOUtils;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphException;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.esa.snap.core.gpf.graph.Node;
import org.glassfish.jersey.media.multipart.FormDataParam;

import it.fadeout.Wasdi;
import it.fadeout.mercurius.business.Message;
import it.fadeout.mercurius.client.MercuriusAPI;
import it.fadeout.rest.resources.largeFileDownload.FileStreamingOutput;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.SnapWorkflow;
import wasdi.shared.business.User;
import wasdi.shared.business.UserApplicationRole;
import wasdi.shared.business.UserResourcePermission;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.SnapWorkflowRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.data.UserResourcePermissionRepository;
import wasdi.shared.parameters.GraphParameter;
import wasdi.shared.parameters.OperatorParameter;
import wasdi.shared.parameters.settings.GraphSetting;
import wasdi.shared.utils.Utils;
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
     * @param fileInputStream Stream of the xml file
     * @param sSessionId User Session
     * @param sName Workflow Name
     * @param sDescription Workflow Description 
     * @return std http response
     */
    @POST
    @Path("/uploadfile")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadFile(@FormDataParam("file") InputStream fileInputStream,
                                @HeaderParam("x-session-token") String sSessionId, @QueryParam("workspace") String sWorkspaceId,
                                @QueryParam("name") String sName, @QueryParam("description") String sDescription,
                                @QueryParam("public") Boolean bPublic) {

        WasdiLog.debugLog("WorkflowsResource.uploadFile( Ws: " + sWorkspaceId + ", Name: " + sName + ", Descr: " + sDescription + ", Public: " + bPublic + " )");

        try {
            // Check authorization
        	
            User oUser = Wasdi.getUserFromSession(sSessionId);
            // Checks whether null file is passed
            if (fileInputStream == null) return Response.status(400).build();

            if (oUser == null) return Response.status(401).build();
            

            String sUserId = oUser.getUserId();

            // Get Download Path
            String sDownloadRootPath = Wasdi.getDownloadPath();

            String sDirectoryPathname = sDownloadRootPath + "workflows/";

            createDirectoryIfDoesNotExist(sDirectoryPathname);

            // Generate Workflow Id and file
            String sWorkflowId = UUID.randomUUID().toString();
            String sFilePathname = sDirectoryPathname + sWorkflowId + ".xml";
            File oWorkflowXmlFile = new File(sFilePathname);

            WasdiLog.debugLog("WorkflowsResource.uploadFile: workflow file Path: " + oWorkflowXmlFile.getPath());

            // save uploaded file
            writeFile(fileInputStream, oWorkflowXmlFile);

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

            try (FileReader oFileReader = new FileReader(oWorkflowXmlFile)) {
                // Read the graph
                Graph oGraph;
                try {
                    oGraph = GraphIO.read(oFileReader);

                    // Take the nodes
                    Node[] aoNodes = oGraph.getNodes();

                    for (int iNodes = 0; iNodes < aoNodes.length; iNodes++) {
                        Node oNode = aoNodes[iNodes];
                        // Search Read and Write nodes
                        if (oNode.getOperatorName().equals("Read")) {
                            oWorkflow.getInputNodeNames().add(oNode.getId());
                        } else if (oNode.getOperatorName().equals("Write")) {
                            oWorkflow.getOutputNodeNames().add(oNode.getId());
                        }
                    }
                } catch (GraphException oE) {
                    WasdiLog.debugLog("WorkflowsResource.uploadFile: malformed workflow file");
                    // Close the file reader
                    oFileReader.close();
                    // Delete the file fom the server
                    Files.delete(oWorkflowXmlFile.toPath());
                    return Response.serverError().build();
                }
                // Save the Workflow
                SnapWorkflowRepository oSnapWorkflowRepository = new SnapWorkflowRepository();
                oSnapWorkflowRepository.insertSnapWorkflow(oWorkflow);

            } catch (Exception oEx) {
                WasdiLog.debugLog("WorkflowsResource.uploadFile: " + oEx);
                return Response.serverError().build();
            }


        } catch (Exception oEx2) {
        	WasdiLog.debugLog("WorkflowsResource.uploadFile: " + oEx2);
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

            if (oUser == null) return Response.status(401).build();
            
            

            // Get Download Path
            String sDownloadRootPath = Wasdi.getDownloadPath();

            WasdiLog.debugLog("WorkflowsResource.updateFile: download path " + sDownloadRootPath);

            String sDirectoryPathname = sDownloadRootPath + "workflows/";

            createDirectoryIfDoesNotExist(sDirectoryPathname);

            // Check that the workflow exists on db
            SnapWorkflowRepository oSnapWorkflowRepository = new SnapWorkflowRepository();
            SnapWorkflow oWorkflow = oSnapWorkflowRepository.getSnapWorkflow(sWorkflowId);
            
            if (oWorkflow == null) {
                WasdiLog.debugLog("WorkflowsResource.updateFile: error in workflowId " + sWorkflowId + " not found on DB");
                return Response.notModified("WorkflowId not found, please check parameters").build();
            }
            
            // Checks that user can modify the workflow
            UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
            
            if (!oUser.getUserId().equals(oWorkflow.getUserId()) && !oUserResourcePermissionRepository.isWorkflowSharedWithUser(oUser.getUserId(), oWorkflow.getWorkflowId())) {
                WasdiLog.debugLog("WorkflowsResource.updateFile: User " + oUser.getUserId() + " doesn't have rights on workflow " + oWorkflow.getName());
                return Response.status(Status.UNAUTHORIZED).build();
            }
            
            // original xml file
            File oWorkflowXmlFile = new File(sDownloadRootPath + "workflows/" + sWorkflowId + ".xml");
            // new xml file
            File oWorkflowXmlFileTemp = new File(sDownloadRootPath + "workflows/" + sWorkflowId + ".xml.temp");
            //if the new one is ok delete the old and rename the ".temp" file
            // save uploaded file in ".temp" format
            
            try {
                writeFile(fileInputStream, oWorkflowXmlFileTemp);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            oWorkflow.getInputNodeNames().clear();
            oWorkflow.getOutputNodeNames().clear();
            // checks that the graph field is valid by checking the nodes
            try (FileReader oFileReader = new FileReader(oWorkflowXmlFileTemp)) {
                // Read the graph
                Graph oGraph;
                try {
                    oGraph = GraphIO.read(oFileReader);

                    // Take the nodes
                    Node[] aoNodes = oGraph.getNodes();

                    for (int iNodes = 0; iNodes < aoNodes.length; iNodes++) {
                        Node oNode = aoNodes[iNodes];
                        // Search Read and Write nodes
                        if (oNode.getOperatorName().equals("Read")) {
                            oWorkflow.getInputNodeNames().add(oNode.getId());
                        } else if (oNode.getOperatorName().equals("Write")) {
                            oWorkflow.getOutputNodeNames().add(oNode.getId());
                        }
                    }
                    // Close the file reader
                    oFileReader.close();
                } catch (GraphException oE) {
                    // Close the file reader
                    oFileReader.close();
                    WasdiLog.debugLog("WorkflowsResource.updateFile: malformed workflow file");
                    // Leave the original file unchanged and delete the temp
                    Files.delete(oWorkflowXmlFileTemp.toPath());
                    return Response.status(Status.NOT_MODIFIED).build();
                }
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

            } catch (Exception oEx) {
                if (oWorkflowXmlFileTemp.exists()) oWorkflowXmlFileTemp.delete();
                WasdiLog.debugLog("WorkflowsResource.updateFile: " + oEx);
                return Response.status(Status.NOT_MODIFIED).build();
            }
        } 
        catch (Exception oEx2) {
        	WasdiLog.debugLog("WorkflowsResource.updateFile: " + oEx2);
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
    public Response getXML(
            @HeaderParam("x-session-token") String sSessionId,
            @QueryParam("workflowId") String sWorkflowId) {

        WasdiLog.debugLog("WorkflowsResource.getXML( Workspace Id : " + sWorkflowId + ");");
        String sXml = "";
        
        try {
            // Check authorization
            if (Utils.isNullOrEmpty(sSessionId)) {
                WasdiLog.debugLog("WorkflowsResource.getXML( Workspace Id : " + sWorkflowId + ");");
                return Response.status(Status.UNAUTHORIZED).build();
            }
            User oUser = Wasdi.getUserFromSession(sSessionId);
            // Checks whether null file is passed

            if (oUser == null) return Response.status(Status.UNAUTHORIZED).build();
            if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(Status.UNAUTHORIZED).build();

            // Check that the workflow exists on db
            SnapWorkflowRepository oSnapWorkflowRepository = new SnapWorkflowRepository();
            SnapWorkflow oWorkflow = oSnapWorkflowRepository.getSnapWorkflow(sWorkflowId);
            if (oWorkflow == null) {
                WasdiLog.debugLog("WorkflowsResource.getXML: error in workflowId " + sWorkflowId + " not found on DB");
                return Response.notModified("WorkflowId not found").build();
            }

            // Get Download Path
            String sDownloadRootPath = Wasdi.getDownloadPath();

            File oWorkflowsFile = new File(sDownloadRootPath + "workflows/" + sWorkflowId + ".xml");

            if (!oWorkflowsFile.exists()) {
                WasdiLog.debugLog("WorkflowsResource.getXML( Workflow Id : " + sWorkflowId + ") Error, workflow file not found;");
                return Response.status(Status.NOT_FOUND).build();
            }
            // Check is the owner or a the sharing
            sXml = new String(Files.readAllBytes(oWorkflowsFile.toPath()));
            return Response.ok(sXml, MediaType.APPLICATION_XML).build();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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
        WasdiLog.debugLog("WorkflowsResource.updateXML: workflowId " + sWorkflowId + " invoke WorkflowsResource.updateGraph");
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
        if (Utils.isNullOrEmpty(sSessionId)) {
            WasdiLog.debugLog("WorkflowsResource.updateParams( InputStream, Session: " + sSessionId + ", Ws: " + sWorkflowId + " ): invalid session");
            return Response.status(401).build();
        }
        User oUser = Wasdi.getUserFromSession(sSessionId);

        if (oUser == null) return Response.status(401).build();
        if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(401).build();

        SnapWorkflowRepository oSnapWorkflowRepository = new SnapWorkflowRepository();
        SnapWorkflow oSnapWorkflow = oSnapWorkflowRepository.getSnapWorkflow(sWorkflowId);
        if (oSnapWorkflow == null) {
            return Response.status(404).build();
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

        if (Utils.isNullOrEmpty(sSessionId)) {
            WasdiLog.debugLog("WorkflowsResource.getWorkflowsByUser: session null");
            return null;
        }
        User oUser = Wasdi.getUserFromSession(sSessionId);

        if (oUser == null) {
            WasdiLog.debugLog("WorkflowsResource.getWorkflowsByUser( " + sSessionId + " ): invalid session");
            return null;
        }

        if (Utils.isNullOrEmpty(oUser.getUserId())) {
            WasdiLog.debugLog("WorkflowsResource.getWorkflowsByUser: user id null");
            return null;
        }

        String sUserId = oUser.getUserId();


        SnapWorkflowRepository oSnapWorkflowRepository = new SnapWorkflowRepository();
        ArrayList<SnapWorkflowViewModel> aoRetWorkflows = new ArrayList<>();
        try {

            List<SnapWorkflow> aoDbWorkflows = oSnapWorkflowRepository.getSnapWorkflowPublicAndByUser(sUserId);

            for (SnapWorkflow oCurWF : aoDbWorkflows) {
                SnapWorkflowViewModel oVM = SnapWorkflowViewModel.getFromWorkflow(oCurWF);
                // check if it was shared, if so, set shared with me to true
                oVM.setSharedWithMe(false);
                aoRetWorkflows.add(oVM);
            }

            // find sharings by userId
            UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
            List<UserResourcePermission> aoWorkflowSharing = oUserResourcePermissionRepository.getWorkflowSharingsByUserId(sUserId);

            // For all the shared workflows
            for (UserResourcePermission oSharing : aoWorkflowSharing) {
                // Create the VM
                SnapWorkflow oSharedWithMe = oSnapWorkflowRepository.getSnapWorkflow(oSharing.getResourceId());
                SnapWorkflowViewModel oVM = SnapWorkflowViewModel.getFromWorkflow(oSharedWithMe);

                if (oVM.isPublic() == false) {
                    // This is shared and not public: add to return list
                    oVM.setSharedWithMe(true);
                    aoRetWorkflows.add(oVM);
                } else {
                    // This is shared but public, so this is already in our return list
                    for (SnapWorkflowViewModel oWorkFlow : aoRetWorkflows) {
                        // Find it and set shared flag = true
                        if (oSharedWithMe.getWorkflowId().equals(oWorkFlow.getWorkflowId())) {
                            oWorkFlow.setSharedWithMe(true);
                        }
                    }
                }
            }

        } catch (Exception oE) {
            WasdiLog.debugLog("WorkflowsResource.getWorkflowsByUser( " + sSessionId + " ): " + oE);
        }

        WasdiLog.debugLog("WorkflowsResource.getWorkflowsByUser: return " + aoRetWorkflows.size() + " workflows");

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
    public Response delete(@HeaderParam("x-session-token") String sSessionId, @QueryParam("workflowId") String
            sWorkflowId) {
        WasdiLog.debugLog("WorkflowsResource.delete( Workflow: " + sWorkflowId + " )");
        try {
            // Check User
            if (Utils.isNullOrEmpty(sSessionId)) return Response.status(Status.UNAUTHORIZED).build();
            User oUser = Wasdi.getUserFromSession(sSessionId);

            if (oUser == null) {
                WasdiLog.debugLog("WorkflowsResource.delete( Session: " + sSessionId + ", Workflow: " + sWorkflowId + " ): invalid session");
                return Response.status(Status.UNAUTHORIZED).build();
            }
            if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(Status.UNAUTHORIZED).build();

            String sUserId = oUser.getUserId();

            // Check if the workflow exists
            SnapWorkflowRepository oSnapWorkflowRepository = new SnapWorkflowRepository();
            SnapWorkflow oWorkflow = oSnapWorkflowRepository.getSnapWorkflow(sWorkflowId);

            if (oWorkflow == null) return Response.status(Status.INTERNAL_SERVER_ERROR).build();

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
            String sBasePath = Wasdi.getDownloadPath();
            sBasePath += "workflows/";
            String sWorkflowFilePath = sBasePath + oWorkflow.getWorkflowId() + ".xml";

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
            WasdiLog.debugLog("WorkflowsResource.delete( Session: " + sSessionId + ", Workflow: " + sWorkflowId + " ): " + oE);
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
                                         @QueryParam("workflowId") String sWorkflowId, @QueryParam("userId") String sUserId) {

        WasdiLog.debugLog("WorkflowsResource.shareWorkflow(  Workflow : " + sWorkflowId + ", User: " + sUserId + " )");

        //init repositories
        UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
        SnapWorkflowRepository oWorkflowRepository = new SnapWorkflowRepository();
        // Validate Session
        User oRequesterUser = Wasdi.getUserFromSession(sSessionId);
        PrimitiveResult oResult = new PrimitiveResult();
        oResult.setBoolValue(false);

        if (oRequesterUser == null) {
            WasdiLog.debugLog("WorkflowsResource.shareWorkflow( Session: " + sSessionId + ", Workflow: " + sWorkflowId + ", User: " + sUserId + " ): invalid session");
            oResult.setStringValue("Invalid session.");
            return oResult;
        }

        if (Utils.isNullOrEmpty(oRequesterUser.getUserId())) {
            oResult.setStringValue("Invalid user.");
            return oResult;
        }

        try {

            // Check if the processor exists and is of the user calling this API
            oWorkflowRepository = new SnapWorkflowRepository();
            SnapWorkflow oWorkflow = oWorkflowRepository.getSnapWorkflow(sWorkflowId);

            if (oWorkflow == null) {
                oResult.setStringValue("Invalid Workflow");
                return oResult;
            }
            if (oRequesterUser.getUserId().equals(sUserId)) {
                WasdiLog.debugLog("WorkflowsResource.ShareWorkflow: auto sharing not so smart");
                oResult.setStringValue("Impossible to autoshare.");
                return oResult;
            }
            /* This was ONLY THE OWNER CAN ADD SHARE
            if (!oWorkflow.getUserId().equals(oRequesterUser.getUserId())) {
                oResult.setStringValue("Unauthorized");
                return oResult;
            }*/

            // Check the destination user
            UserRepository oUserRepository = new UserRepository();
            User oDestinationUser = oUserRepository.getUser(sUserId);
            // Can't find destination user for the sharing
            if (oDestinationUser == null) {
                oResult.setStringValue("Can't find target user of the sharing");
                return oResult;
            }

            //if the requester is not the owner
            if (!oWorkflow.getUserId().equals(oRequesterUser.getUserId())) {

                // Is he trying to share with the owner?
                if (oWorkflow.getUserId().equals(sUserId)) {
                    oResult.setStringValue("Cannot Share with owner");
                    return oResult;
                }
                // the requester has the share?
                if (!oUserResourcePermissionRepository.isWorkflowSharedWithUser(oRequesterUser.getUserId(), sWorkflowId)
                		&& !UserApplicationRole.userHasRightsToAccessApplicationResource(oRequesterUser.getRole(), ADMIN_DASHBOARD)) {
                    oResult.setStringValue("Unauthorized");
                    return oResult;
                }

            }

            // Check if has been already shared
            if (oUserResourcePermissionRepository.isWorkflowSharedWithUser(sUserId, sWorkflowId)) {
                oResult.setStringValue("Already shared");
                return oResult;
            }

            // Create and insert the sharing
            UserResourcePermission oWorkflowSharing = new UserResourcePermission();
            oWorkflowSharing.setResourceType("workflow");
            Timestamp oTimestamp = new Timestamp(System.currentTimeMillis());
            oWorkflowSharing.setOwnerId(oWorkflow.getUserId());
            oWorkflowSharing.setUserId(sUserId);
            oWorkflowSharing.setResourceId(sWorkflowId);
            oWorkflowSharing.setCreatedBy(oRequesterUser.getUserId());
            oWorkflowSharing.setCreatedDate((double) oTimestamp.getTime());
            oWorkflowSharing.setPermissions("write");
            oUserResourcePermissionRepository.insertPermission(oWorkflowSharing);

            WasdiLog.debugLog("WorkflowsResource.shareWorkflow: Workflow" + sWorkflowId + " Shared from " + oRequesterUser.getUserId() + " to " + sUserId);

            try {
                String sMercuriusAPIAddress = WasdiConfig.Current.notifications.mercuriusAPIAddress;

                if (Utils.isNullOrEmpty(sMercuriusAPIAddress)) {
                    WasdiLog.debugLog("WorkflowsResource.shareWorkflow: sMercuriusAPIAddress is null");
                } else {
                    MercuriusAPI oAPI = new MercuriusAPI(sMercuriusAPIAddress);
                    Message oMessage = new Message();

                    String sTitle = "Workflow " + oWorkflow.getName() + " Shared";

                    oMessage.setTilte(sTitle);

                    String sSender = WasdiConfig.Current.notifications.sftpManagementMailSender;
                    if (sSender == null) {
                        sSender = "wasdi@wasdi.net";
                    }

                    oMessage.setSender(sSender);

                    String sMessage = "The user " + oRequesterUser.getUserId() + " shared with you the Workflow: " + oWorkflow.getName();

                    oMessage.setMessage(sMessage);

                    Integer iPositiveSucceded = 0;

                    iPositiveSucceded = oAPI.sendMailDirect(sUserId, oMessage);

                    WasdiLog.debugLog("WorkflowsResource.shareWorkflow: notification sent with result " + iPositiveSucceded);
                }

            } catch (Exception oEx) {
                WasdiLog.debugLog("WorkflowsResource.shareWorkflow: notification exception " + oEx.toString());
            }

        } catch (Exception oEx) {
            WasdiLog.debugLog("WorkflowsResource.shareWorkflow: " + oEx);

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
    public PrimitiveResult deleteUserSharingWorkflow(@HeaderParam("x-session-token") String
                                                             sSessionId, @QueryParam("workflowId") String sWorkflowId, @QueryParam("userId") String sUserId) {

        WasdiLog.debugLog("WorkflowsResource.deleteUserSharedWorkflow( ProcId: " + sWorkflowId + ", User:" + sUserId + " )");
        PrimitiveResult oResult = new PrimitiveResult();
        oResult.setBoolValue(false);
        try {
            // Validate Session
            User oOwnerUser = Wasdi.getUserFromSession(sSessionId);

            if (oOwnerUser == null) {
                WasdiLog.debugLog("WorkflowsResource.deleteUserSharedWorkflow( Session: " + sSessionId + ", ProcId: " + sWorkflowId + ", User:" + sUserId + " ): invalid session");
                oResult.setStringValue("Invalid session");
                return oResult;
            }

            if (Utils.isNullOrEmpty(oOwnerUser.getUserId())) {
                oResult.setStringValue("Invalid user.");
                return oResult;
            }

            if (Utils.isNullOrEmpty(sUserId)) {
                oResult.setStringValue("Invalid shared user.");
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
                WasdiLog.debugLog("WorkflowsResource.deleteUserSharedWorkflow: " + oEx);
                oResult.setStringValue("Error deleting processor sharing");
                return oResult;
            }

            oResult.setStringValue("Done");
            oResult.setBoolValue(true);
        } catch (Exception oE) {
            WasdiLog.debugLog("WorkflowsResource.deleteUserSharedWorkflow( Session: " + sSessionId + ", ProcId: " + sWorkflowId + ", User:" + sUserId + " ): " + oE);
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
        User oAskingUser = Wasdi.getUserFromSession(sSessionId);

        if (oAskingUser == null) {
            WasdiLog.debugLog("WorkflowsResource.getEnableUsersSharedWorkflow( Session: " + sSessionId + ", Workflow: " + sWorkflowId + "): invalid session");

            return oResult;
        }

        if (Utils.isNullOrEmpty(oAskingUser.getUserId())) {

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
            oUserResourcePermissionRepository.getWorkflowSharingsByWorkflowId(sWorkflowId).forEach(element -> {
                oResult.add(new WorkflowSharingViewModel(element));
            });
            return oResult;

        } catch (Exception oEx) {
            WasdiLog.debugLog("WorkflowsResource.getEnableUsersSharedWorkflow: " + oEx);
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

        if (Utils.isNullOrEmpty(sSessionId)) {
            oResult.setBoolValue(false);
            oResult.setIntValue(401);
            return oResult;
        }
        User oUser = Wasdi.getUserFromSession(sSessionId);
        if (oUser == null) {
            WasdiLog.debugLog("WorkflowsResource.run( Session: " + sSessionId + ", Ws: " + sWorkspaceId + ", ... ): invalid session");
            oResult.setBoolValue(false);
            oResult.setIntValue(401);
            return oResult;
        }

        if (Utils.isNullOrEmpty(oUser.getUserId())) {
            oResult.setBoolValue(false);
            oResult.setIntValue(401);
            return oResult;
        }

        try {
            String sUserId = oUser.getUserId();

            GraphSetting oGraphSettings = new GraphSetting();
            String sGraphXml;

            SnapWorkflowRepository oSnapWorkflowRepository = new SnapWorkflowRepository();
            SnapWorkflow oWF = oSnapWorkflowRepository.getSnapWorkflow(oSnapWorkflowViewModel.getWorkflowId());

            if (oWF == null) {
                oResult.setBoolValue(false);
                oResult.setIntValue(500);
                return oResult;
            }
            if (oWF.getUserId().equals(sUserId) == false && oWF.getIsPublic() == false) {

                UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();

                UserResourcePermission oWorkflowSharing = oUserResourcePermissionRepository.getWorkflowSharingByUserIdAndWorkflowId(sUserId, oSnapWorkflowViewModel.getWorkflowId());

                if (oWorkflowSharing == null) {

                    WasdiLog.debugLog("WorkflowsResource.run: Workflow now owned or shared, exit");

                    oResult.setBoolValue(false);
                    oResult.setIntValue(401);
                    return oResult;
                }
            }

            String sBasePath = Wasdi.getDownloadPath();
            String sWorkflowPath = sBasePath + "workflows/" + oWF.getWorkflowId() + ".xml";
            File oWorkflowFile = new File(sWorkflowPath);

            if (!oWorkflowFile.exists()) {
                WasdiLog.debugLog("WorkflowsResource.run: Workflow file not on this node. Try 	to download it");

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

                String sSourceProductName = "";
                String sDestinationProdutName = "";

                if (oSnapWorkflowViewModel.getInputFileNames().size() > 0) {
                    sSourceProductName = oSnapWorkflowViewModel.getInputFileNames().get(0);
                    sDestinationProdutName = Utils.getFileNameWithoutLastExtension(sSourceProductName) + "_" + sWorkFlowName + Utils.GetFileNameExtension(sSourceProductName); 
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

                    // Serialization Path
                    String sPath = WasdiConfig.Current.paths.serializationPath;

                    return Wasdi.runProcess(oUser.getUserId(), sSessionId, LauncherOperations.GRAPH.toString(), sSourceProductName, sPath, oParameter, sParentProcessWorkspaceId);

                } catch (IOException e) {
                    WasdiLog.debugLog("WorkflowsResource.run: " + e);
                    oResult.setBoolValue(false);
                    oResult.setIntValue(500);
                    return oResult;
                } catch (Exception e) {
                    WasdiLog.debugLog("WorkflowsResource.run: " + e);
                    oResult.setBoolValue(false);
                    oResult.setIntValue(500);
                    return oResult;
                }
            }
        } catch (Exception oEx) {
            WasdiLog.debugLog("WorkflowsResource.run: Error " + oEx.toString());
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
                WasdiLog.debugLog("WorkflowsResource.download( Session: " + sSessionId + ", WorkflowId: " + sWorkflowId + " ): invalid session");
                return Response.status(Status.UNAUTHORIZED).build();
            }

            SnapWorkflowRepository oSnapWorkflowRepository = new SnapWorkflowRepository();
            SnapWorkflow oSnapWorkflow = oSnapWorkflowRepository.getSnapWorkflow(sWorkflowId);

            // Take path
            String sDownloadRootPath = Wasdi.getDownloadPath();
            String sWorkflowXmlPath = sDownloadRootPath + "workflows/" + sWorkflowId + ".xml";

            File oFile = new File(sWorkflowXmlPath);

            ResponseBuilder oResponseBuilder = null;

            if (oSnapWorkflow == null) {
                WasdiLog.debugLog("WorkflowsResource.download( Session: " + sSessionId + ", WorkflowId: " + sWorkflowId + " ): Workflow Id not found on DB");
                oResponseBuilder = Response.noContent();
                return oResponseBuilder.build();
            }

            if (oFile.exists() == false) {
                WasdiLog.debugLog("WorkflowsResource.download: file does not exists " + oFile.getPath());
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
            WasdiLog.debugLog("WorkflowsResource.download: " + oEx);
        }

        return null;
    }    

}
