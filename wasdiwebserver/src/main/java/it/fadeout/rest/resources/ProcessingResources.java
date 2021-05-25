package it.fadeout.rest.resources;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.media.jai.JAI;
import javax.media.jai.OperationRegistry;
import javax.media.jai.RegistryElementDescriptor;
import javax.servlet.ServletConfig;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FilterBand;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphException;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.esa.snap.core.gpf.graph.Node;
import org.esa.snap.core.jexp.impl.Tokenizer;
import org.esa.snap.rcp.imgfilter.model.Filter;
import org.esa.snap.rcp.imgfilter.model.StandardFilters;
import org.glassfish.jersey.media.multipart.FormDataParam;

import com.bc.ceres.binding.PropertyContainer;

import it.fadeout.Wasdi;
import it.fadeout.mercurius.business.Message;
import it.fadeout.mercurius.client.MercuriusAPI;
import it.fadeout.rest.resources.largeFileDownload.FileStreamingOutput;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.SnapWorkflow;
import wasdi.shared.business.User;
import wasdi.shared.business.WorkflowSharing;
import wasdi.shared.business.WpsProvider;
import wasdi.shared.data.SnapWorkflowRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.data.WorkflowSharingRepository;
import wasdi.shared.data.WpsProvidersRepository;
import wasdi.shared.launcherOperations.LauncherOperationsUtils;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.GraphParameter;
import wasdi.shared.parameters.GraphSetting;
import wasdi.shared.parameters.ISetting;
import wasdi.shared.parameters.MosaicParameter;
import wasdi.shared.parameters.MosaicSetting;
import wasdi.shared.parameters.MultiSubsetParameter;
import wasdi.shared.parameters.MultiSubsetSetting;
import wasdi.shared.parameters.OperatorParameter;
import wasdi.shared.parameters.RegridParameter;
import wasdi.shared.parameters.RegridSetting;
import wasdi.shared.parameters.SubsetParameter;
import wasdi.shared.parameters.SubsetSetting;
import wasdi.shared.utils.BandImageManager;
import wasdi.shared.utils.CredentialPolicy;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.BandImageViewModel;
import wasdi.shared.viewmodels.ColorManipulationViewModel;
import wasdi.shared.viewmodels.MaskViewModel;
import wasdi.shared.viewmodels.MathMaskViewModel;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.ProductMaskViewModel;
import wasdi.shared.viewmodels.RangeMaskViewModel;
import wasdi.shared.viewmodels.SnapWorkflowViewModel;
import wasdi.shared.viewmodels.WorkflowSharingViewModel;
import wasdi.shared.viewmodels.WpsViewModel;

@Path("/processing")
public class ProcessingResources {

    @Context
    ServletConfig m_oServletConfig;

    CredentialPolicy m_oCredentialPolicy = new CredentialPolicy();

    @POST
    @Path("geometric/mosaic")
    @Produces({"application/xml", "application/json", "text/xml"})
    public PrimitiveResult mosaic(@HeaderParam("x-session-token") String sSessionId,
                                  @QueryParam("sDestinationProductName") String sDestinationProductName,
                                  @QueryParam("sWorkspaceId") String sWorkspaceId,
                                  @QueryParam("parent") String sParentId, MosaicSetting oSetting) throws IOException {
        Utils.debugLog("ProcessingResources.Mosaic( Destination: " + sDestinationProductName + ", Ws:" + sWorkspaceId + ", ... )");
        return executeOperation(sSessionId, "", sDestinationProductName, sWorkspaceId, oSetting, LauncherOperations.MOSAIC, sParentId);
    }

    @POST
    @Path("geometric/regrid")
    @Produces({"application/xml", "application/json", "text/xml"})
    public PrimitiveResult regrid(@HeaderParam("x-session-token") String sSessionId,
                                  @QueryParam("sDestinationProductName") String sDestinationProductName,
                                  @QueryParam("sWorkspaceId") String sWorkspaceId,
                                  @QueryParam("parent") String sParentId, RegridSetting oSetting) throws IOException {
        Utils.debugLog("ProcessingResources.Regrid( Dest: " + sDestinationProductName + ", Ws: " + sWorkspaceId + ", ... )");
        return executeOperation(sSessionId, "", sDestinationProductName, sWorkspaceId, oSetting, LauncherOperations.REGRID, sParentId);
    }

    @POST
    @Path("geometric/subset")
    @Produces({"application/xml", "application/json", "text/xml"})
    public PrimitiveResult subset(@HeaderParam("x-session-token") String sSessionId,
                                  @QueryParam("sSourceProductName") String sSourceProductName,
                                  @QueryParam("sDestinationProductName") String sDestinationProductName,
                                  @QueryParam("sWorkspaceId") String sWorkspaceId,
                                  @QueryParam("parent") String sParentId, SubsetSetting oSetting) throws IOException {
        Utils.debugLog("ProcessingResources.Subset( Source: " + sSourceProductName + ", Dest:" + sDestinationProductName + ", Ws:" + sWorkspaceId + ", ... )");
        return executeOperation(sSessionId, sSourceProductName, sDestinationProductName, sWorkspaceId, oSetting, LauncherOperations.SUBSET, sParentId);
    }

    @POST
    @Path("geometric/multisubset")
    @Produces({"application/xml", "application/json", "text/xml"})
    public PrimitiveResult multiSubset(@HeaderParam("x-session-token") String sSessionId,
                                       @QueryParam("sSourceProductName") String sSourceProductName,
                                       @QueryParam("sDestinationProductName") String sDestinationProductName,
                                       @QueryParam("sWorkspaceId") String sWorkspaceId,
                                       @QueryParam("parent") String sParentId, MultiSubsetSetting oSetting) throws IOException {
        Utils.debugLog("ProcessingResources.MultiSubset( Source: " + sSourceProductName + ", Dest: " + sDestinationProductName + ", Ws:" + sWorkspaceId + ", ... )");
        return executeOperation(sSessionId, sSourceProductName, sDestinationProductName, sWorkspaceId, oSetting, LauncherOperations.MULTISUBSET, sParentId);
    }


    /**
     * Upload and save a new SNAP Workflow XML
     *
     * @param fileInputStream
     * @param sSessionId
     * @param sName
     * @param sDescription
     * @return
     * @throws Exception
     */
    @POST
    @Path("/uploadgraph")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadGraph(@FormDataParam("file") InputStream fileInputStream,
                                @HeaderParam("x-session-token") String sSessionId, @QueryParam("workspace") String sWorkspace,
                                @QueryParam("name") String sName, @QueryParam("description") String sDescription,
                                @QueryParam("public") Boolean bPublic) {

        Utils.debugLog("ProcessingResources.uploadGraph( Ws: " + sWorkspace + ", Name: " + sName + ", Descr: " + sDescription + ", Public: " + bPublic + " )");

        try {
            // Check authorization
            if (Utils.isNullOrEmpty(sSessionId)) {
                Utils.debugLog("ProcessingResources.uploadGraph( InputStream, Session: " + sSessionId + ", Ws: " + sWorkspace + ", Name: " + sName + ", Descr: " + sDescription + ", Public: " + bPublic + " ): invalid session");
                return Response.status(401).build();
            }
            User oUser = Wasdi.getUserFromSession(sSessionId);
            // Checks whether null file is passed
            if (fileInputStream == null) return Response.status(400).build();

            if (oUser == null) return Response.status(401).build();
            if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(401).build();

            String sUserId = oUser.getUserId();

            // Get Download Path
            String sDownloadRootPath = Wasdi.getDownloadPath(m_oServletConfig);

            File oWorkflowsPath = new File(sDownloadRootPath + "workflows/");

            if (!oWorkflowsPath.exists()) {
                oWorkflowsPath.mkdirs();
            }

            // Generate Workflow Id and file
            String sWorkflowId = UUID.randomUUID().toString();
            File oWorkflowXmlFile = new File(sDownloadRootPath + "workflows/" + sWorkflowId + ".xml");

            Utils.debugLog("ProcessingResources.uploadGraph: workflow file Path: " + oWorkflowXmlFile.getPath());

            // save uploaded file
            int iRead = 0;
            byte[] ayBytes = new byte[1024];

            try (OutputStream oOutStream = new FileOutputStream(oWorkflowXmlFile)) {
                while ((iRead = fileInputStream.read(ayBytes)) != -1) {
                    oOutStream.write(ayBytes, 0, iRead);
                }
                oOutStream.flush();
            }

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
                    Utils.debugLog("ProcessingResources.uploadGraph: malformed workflow file");
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
                Utils.debugLog("ProcessingResources.uploadGraph: " + oEx);
                return Response.serverError().build();
            }


        } catch (Exception oe) {
        }

        return Response.ok().build();
    }


    /**
     * Update a SNAP Workflow XML file
     *
     * @param fileInputStream Xml file containing the new version of the Snap Workflow
     * @param sSessionId      Session of the current user
     * @param sWorkflowId     Used to check if the workflow exists, and then, upload the file
     * @return
     */
    @POST
    @Path("/updategraphfile")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response updateGraphfile(@FormDataParam("file") InputStream fileInputStream,
                                    @HeaderParam("x-session-token") String sSessionId,
                                    @QueryParam("workflowid") String sWorkflowId) {
        Utils.debugLog("ProcessingResources.updateGraphFile( InputStream, WorkflowId: " + sWorkflowId);

        try {
            // Check authorization
            if (Utils.isNullOrEmpty(sSessionId)) {
                Utils.debugLog("ProcessingResources.updateGraph( InputStream, Session: " + sSessionId + ", Ws: " + sWorkflowId + " ): invalid session");
                return Response.status(401).build();
            }
            User oUser = Wasdi.getUserFromSession(sSessionId);

            if (oUser == null) return Response.status(401).build();
            if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(401).build();

            // Get Download Path
            String sDownloadRootPath = Wasdi.getDownloadPath(m_oServletConfig);

            Utils.debugLog("ProcessingResources.updateGraph: download path " + sDownloadRootPath);

            File oWorkflowsPath = new File(sDownloadRootPath + "workflows/");

            if (!oWorkflowsPath.exists()) {
                oWorkflowsPath.mkdirs();
            }

            // Check that the workflow exists on db
            SnapWorkflowRepository oSnapWorkflowRepository = new SnapWorkflowRepository();
            SnapWorkflow oWorkflow = oSnapWorkflowRepository.getSnapWorkflow(sWorkflowId);
            if (oWorkflow == null) {
                Utils.debugLog("ProcessingResources.updateGraph: error in workflowId " + sWorkflowId + " not found on DB");
                return Response.notModified("WorkflowId not found, please check parameters").build();
            }
            // original xml file
            File oWorkflowXmlFile = new File(sDownloadRootPath + "workflows/" + sWorkflowId + ".xml");
            // new xml file
            File oWorkflowXmlFileTemp = new File(sDownloadRootPath + "workflows/" + sWorkflowId + ".xml.temp");
            //if the new one is ok delete the old and rename the ".temp" file
            // save uploaded file in ".temp" format
            int iRead = 0;
            // flush the temp
            byte[] ayBytes = new byte[1024];
            try (OutputStream oOutStream = new FileOutputStream(oWorkflowXmlFileTemp)) {
                while ((iRead = fileInputStream.read(ayBytes)) != -1) { // While EOF
                    oOutStream.write(ayBytes, 0, iRead);
                }
                oOutStream.flush();
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
                    Utils.debugLog("ProcessingResources.uploadGraph: malformed workflow file");
                    // Leave the original file unchanged and delete the temp
                    Files.delete(oWorkflowXmlFileTemp.toPath());
                    return Response.serverError().build();
                }
                // Overwrite the old file
                Files.write(oWorkflowXmlFile.toPath(), Files.readAllBytes(oWorkflowXmlFileTemp.toPath()));
                // Delete the temp file
                Files.delete(oWorkflowXmlFileTemp.toPath());

                Utils.debugLog("ProcessingResources.uploadGraph: workflow files updated! workflowID" + oWorkflow.getWorkflowId());

                if (Wasdi.getActualNode() != null) {
                    oWorkflow.setNodeCode(Wasdi.getActualNode().getNodeCode());
                    oWorkflow.setNodeUrl(Wasdi.getActualNode().getNodeBaseAddress());
                }

                // Updates the location on the current server
                oWorkflow.setFilePath(oWorkflowXmlFile.getPath());
                oSnapWorkflowRepository.updateSnapWorkflow(oWorkflow);

            } catch (Exception oEx) {
                Utils.debugLog("ProcessingResources.updateGraph: " + oEx);
                return Response.serverError().build();
            }


        } catch (Exception oe) {
        }
        return Response.ok().build();
    }

    /**
     * Updates the parameters of a Snap Workflow
     *
     * @param sSessionId   the current session Id
     * @param sWorkflowId  the workflowId
     * @param sName        The name of the workflow
     * @param sDescription The description
     * @param bPublic      Enabled if the workflow is publicly available to all WASDI users
     * @return
     * @throws Exception
     */
    // TODO implement method
    @POST
    @Path("/updategraphparameters")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response updateGraphparameters(
            @HeaderParam("x-session-token") String sSessionId,
            @QueryParam("workflowid") String sWorkflowId,
            @QueryParam("name") String sName,
            @QueryParam("description") String sDescription,
            @QueryParam("public") Boolean bPublic

    ) {

        Utils.debugLog("ProcessingResources.updateGraphParameters( InputStream, Workflow: " + sName + ", WorkflowId: " + sWorkflowId);
        if (Utils.isNullOrEmpty(sSessionId)) {
            Utils.debugLog("ProcessingResources.updateGraph( InputStream, Session: " + sSessionId + ", Ws: " + sWorkflowId + " ): invalid session");
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
     * @param sSessionId
     * @return
     */
    @GET
    @Path("/getgraphsbyusr")
    public ArrayList<SnapWorkflowViewModel> getWorkflowsByUser(@HeaderParam("x-session-token") String sSessionId) {
        Utils.debugLog("ProcessingResources.getWorkflowsByUser");

        if (Utils.isNullOrEmpty(sSessionId)) {
            Utils.debugLog("ProcessingResources.getWorkflowsByUser: session null");
            return null;
        }
        User oUser = Wasdi.getUserFromSession(sSessionId);

        if (oUser == null) {
            Utils.debugLog("ProcessingResources.getWorkflowsByUser( " + sSessionId + " ): invalid session");
            return null;
        }

        if (Utils.isNullOrEmpty(oUser.getUserId())) {
            Utils.debugLog("ProcessingResources.getWorkflowsByUser: user id null");
            return null;
        }

        String sUserId = oUser.getUserId();

        // find sharings by userId
        WorkflowSharingRepository oWorkflowSharingRepository = new WorkflowSharingRepository();
        List<WorkflowSharing> aoWorkflowSharing = oWorkflowSharingRepository.getWorkflowSharingByUser(sUserId);
        // create a support list with workflows id
        HashSet<String> aoUniqueIds = new HashSet();
        for (WorkflowSharing wfs : aoWorkflowSharing) {
            aoUniqueIds.add(wfs.getWorkflowId());
        }

        SnapWorkflowRepository oSnapWorkflowRepository = new SnapWorkflowRepository();
        ArrayList<SnapWorkflowViewModel> aoRetWorkflows = new ArrayList<>();
        try {

            List<SnapWorkflow> aoDbWorkflows = oSnapWorkflowRepository.getSnapWorkflowPublicAndByUser(sUserId);

            for (SnapWorkflow oCurWF : aoDbWorkflows) {
                SnapWorkflowViewModel oVM = new SnapWorkflowViewModel();
                oVM.setName(oCurWF.getName());
                oVM.setDescription(oCurWF.getDescription());
                oVM.setWorkflowId(oCurWF.getWorkflowId());
                oVM.setOutputNodeNames(oCurWF.getOutputNodeNames());
                oVM.setInputNodeNames(oCurWF.getInputNodeNames());
                oVM.setPublic(oCurWF.getIsPublic());
                oVM.setUserId(oCurWF.getUserId());
                oVM.setNodeUrl(oCurWF.getNodeUrl());

                // check if it was shared, if so, set shared with me to true
                oVM.setSharedWithMe(aoUniqueIds.contains(oCurWF.getWorkflowId()));
                aoRetWorkflows.add(oVM);

            }
        } catch (Exception oE) {
            Utils.debugLog("ProcessingResources.getWorkflowsByUser( " + sSessionId + " ): " + oE);
        }

        Utils.debugLog("ProcessingResources.getWorkflowsByUser: return " + aoRetWorkflows.size() + " workflows");

        return aoRetWorkflows;
    }

    /**
     * Delete a workflow from id
     * If invoked from an user that's not the owner the sharing is removed instead
     *
     * @param sSessionId  from the session id the user is extracted
     * @param sWorkflowId Unique identified of the workflow to delete
     * @return
     */
    @GET
    @Path("/deletegraph")
    public Response deleteGraph(@HeaderParam("x-session-token") String sSessionId, @QueryParam("workflowId") String
            sWorkflowId) {
        Utils.debugLog("ProcessingResources.deleteWorkflow( Workflow: " + sWorkflowId + " )");
        try {
            // Check User
            if (Utils.isNullOrEmpty(sSessionId)) return Response.status(Status.UNAUTHORIZED).build();
            User oUser = Wasdi.getUserFromSession(sSessionId);

            if (oUser == null) {
                Utils.debugLog("ProcessingResources.deleteWorkflow( Session: " + sSessionId + ", Workflow: " + sWorkflowId + " ): invalid session");
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
                WorkflowSharingRepository oWorkflowSharingRepository = new WorkflowSharingRepository();
                
                if (oWorkflowSharingRepository.isSharedWithUser(sUserId, sWorkflowId)) {
                    oWorkflowSharingRepository.deleteByUserIdWorkflowId(sUserId, sWorkflowId);
                    Utils.debugLog("ProcessingResource.deleteWorkflow: Deleted sharing between user " + sUserId + " and workflow " + oWorkflow.getName() + " Workflow files kept in place");
                    return Response.ok().build();
                }
                // not the owner && no sharing with you. You have no power here !
                return Response.status(Status.FORBIDDEN).build();
            }
            // Get Download Path on the current WASDI instance
            String sBasePath = Wasdi.getDownloadPath(m_oServletConfig);
            sBasePath += "workflows/";
            String sWorkflowFilePath = sBasePath + oWorkflow.getWorkflowId() + ".xml";

            if (!Utils.isNullOrEmpty(sWorkflowFilePath)) {
                File oWorkflowFile = new File(sWorkflowFilePath);
                if (oWorkflowFile.exists()) {
                    if (!oWorkflowFile.delete()) {
                        Utils.debugLog("ProcessingResource.deleteWorkflow: Error deleting the workflow file " + oWorkflow.getFilePath());
                    }
                }
            } else {
                Utils.debugLog("ProcessingResource.deleteWorkflow: workflow file path is null or empty.");
            }
            
            // Delete sharings
            WorkflowSharingRepository oWorkflowSharingRepository = new WorkflowSharingRepository();
            oWorkflowSharingRepository.deleteByWorkflowId(sWorkflowId);
                        
            // Delete the workflow
            oSnapWorkflowRepository.deleteSnapWorkflow(sWorkflowId);
        } catch (Exception oE) {
            Utils.debugLog("ProcessingResources.deleteWorkflow( Session: " + sSessionId + ", Workflow: " + sWorkflowId + " ): " + oE);
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

        Utils.debugLog("ProcessingResource.shareWorkflow(  Workflow : " + sWorkflowId + ", User: " + sUserId + " )");

        //init repositories
        WorkflowSharingRepository oWorkflowSharingRepository = new WorkflowSharingRepository();
        SnapWorkflowRepository oWorkflowRepository = new SnapWorkflowRepository();
        // Validate Session
        User oRequesterUser = Wasdi.getUserFromSession(sSessionId);
        PrimitiveResult oResult = new PrimitiveResult();
        oResult.setBoolValue(false);

        if (oRequesterUser == null) {
            Utils.debugLog("ProcessingResource.shareWorkflow( Session: " + sSessionId + ", Workflow: " + sWorkflowId + ", User: " + sUserId + " ): invalid session");
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
                Utils.debugLog("ProcessingResource.ShareWorkflow: auto sharing not so smart");
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
                if (!oWorkflowSharingRepository.isSharedWithUser(oRequesterUser.getUserId(),sWorkflowId)){
                    oResult.setStringValue("Unauthorized");
                    return oResult;
                }

            }



            // Check if has been already shared
            if (oWorkflowSharingRepository.isSharedWithUser(sUserId, sWorkflowId)) {
                oResult.setStringValue("Already shared");
                return oResult;
            }

            // Create and insert the sharing
            WorkflowSharing oWorkflowSharing = new WorkflowSharing();
            Timestamp oTimestamp = new Timestamp(System.currentTimeMillis());
            oWorkflowSharing.setOwnerId(oRequesterUser.getUserId());
            oWorkflowSharing.setUserId(sUserId);
            oWorkflowSharing.setWorkflowId(sWorkflowId);
            oWorkflowSharing.setShareDate((double) oTimestamp.getTime());
            oWorkflowSharingRepository.insertWorkflowSharing(oWorkflowSharing);

            Utils.debugLog("ProcessingResource.shareWorkflow: Workflow" + sWorkflowId + " Shared from " + oRequesterUser.getUserId() + " to " + sUserId);

            try {
                String sMercuriusAPIAddress = m_oServletConfig.getInitParameter("mercuriusAPIAddress");

                if (Utils.isNullOrEmpty(sMercuriusAPIAddress)) {
                    Utils.debugLog("ProcessingResource.shareWorkflow: sMercuriusAPIAddress is null");
                } else {
                    MercuriusAPI oAPI = new MercuriusAPI(sMercuriusAPIAddress);
                    Message oMessage = new Message();

                    String sTitle = "Workflow " + oWorkflow.getName() + " Shared";

                    oMessage.setTilte(sTitle);

                    String sSender = m_oServletConfig.getInitParameter("sftpManagementMailSenser");
                    if (sSender == null) {
                        sSender = "wasdi@wasdi.net";
                    }

                    oMessage.setSender(sSender);

                    String sMessage = "The user " + oRequesterUser.getUserId() + " shared with you the Workflow: " + oWorkflow.getName();

                    oMessage.setMessage(sMessage);

                    Integer iPositiveSucceded = 0;

                    iPositiveSucceded = oAPI.sendMailDirect(sUserId, oMessage);

                    Utils.debugLog("Processing.shareWorkflow: notification sent with result " + iPositiveSucceded);
                }

            } catch (Exception oEx) {
                Utils.debugLog("Processing.shareWorkflow: notification exception " + oEx.toString());
            }

        } catch (Exception oEx) {
            Utils.debugLog("Processing.shareWorkflow: " + oEx);

            oResult.setStringValue("Error in save process");
            oResult.setBoolValue(false);

            return oResult;
        }

        oResult.setStringValue("Done");
        oResult.setBoolValue(true);

        return oResult;
    }

    @DELETE
    @Path("share/delete")
    @Produces({"application/xml", "application/json", "text/xml"})
    public PrimitiveResult deleteUserSharingWorkflow(@HeaderParam("x-session-token") String
                                                             sSessionId, @QueryParam("workflowId") String sWorkflowId, @QueryParam("userId") String sUserId) {

        Utils.debugLog("ProcessorsResource.deleteUserSharedWorkflow( ProcId: " + sWorkflowId + ", User:" + sUserId + " )");
        PrimitiveResult oResult = new PrimitiveResult();
        oResult.setBoolValue(false);
        try {
            // Validate Session
            User oOwnerUser = Wasdi.getUserFromSession(sSessionId);

            if (oOwnerUser == null) {
                Utils.debugLog("WorkflowsResource.deleteUserSharedWorkflow( Session: " + sSessionId + ", ProcId: " + sWorkflowId + ", User:" + sUserId + " ): invalid session");
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
                WorkflowSharingRepository oWorkflowSharingRepository = new WorkflowSharingRepository();

                WorkflowSharing oWorkflowShare = oWorkflowSharingRepository.getWorkflowSharingByUserIdWorkflowId(sUserId, sWorkflowId);

                if (oWorkflowShare != null) {
                    // if the user making the call is the one on the sharing OR
                    if (oWorkflowShare.getUserId().equals(oOwnerUser.getUserId()) ||
                            // if the user making the call is the owner of the workflow
                            oWorkflowShare.getOwnerId().equals(oOwnerUser.getUserId())) {
                        // Delete the sharing
                        oWorkflowSharingRepository.deleteByUserIdWorkflowId(sUserId, sWorkflowId);
                    } else {
                        oResult.setStringValue("Unauthorized");
                        return oResult;
                    }
                } else {
                    oResult.setStringValue("Sharing not found");
                    return oResult;
                }
            } catch (Exception oEx) {
                Utils.debugLog("WorkflowsResource.deleteUserSharedWorkflow: " + oEx);
                oResult.setStringValue("Error deleting processor sharing");
                return oResult;
            }

            oResult.setStringValue("Done");
            oResult.setBoolValue(true);
        } catch (Exception oE) {
            Utils.debugLog("WorkflowsResource.deleteUserSharedWorkflow( Session: " + sSessionId + ", ProcId: " + sWorkflowId + ", User:" + sUserId + " ): " + oE);
        }
        return oResult;
    }

    /**
     * Retrieves the active sharings given a workflow
     *
     * @param sSessionId
     * @param sWorkflowId
     * @return
     */
    @GET
    @Path("share/byworkflow")
    @Produces({"application/xml", "application/json", "text/xml"})
    public List<WorkflowSharingViewModel> getEnableUsersSharedWorkflow(@HeaderParam("x-session-token") String
                                                                               sSessionId, @QueryParam("workflowId") String sWorkflowId) {
        ArrayList<WorkflowSharingViewModel> oResult = new ArrayList<WorkflowSharingViewModel>();
        Utils.debugLog("ProcessingResource.getEnableUsersSharedWorkflow(  Workflow : " + sWorkflowId + " )");

        // Validate Session
        User oAskingUser = Wasdi.getUserFromSession(sSessionId);

        if (oAskingUser == null) {
            Utils.debugLog("ProcessingResource.shareProcessor( Session: " + sSessionId + ", Workflow: " + sWorkflowId + "): invalid session");

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
                Utils.debugLog("Processing.getEnableUsersSharedWorkflow: Workflow not found");
                // if something went wrong returns an empty set
                return oResult;
            }

            //Retrieve and returns the sharings
            WorkflowSharingRepository oWorkflowSharingRepository = new WorkflowSharingRepository();
            oWorkflowSharingRepository.getWorkflowSharingByWorkflow(sWorkflowId).forEach(element -> {
                oResult.add(new WorkflowSharingViewModel(element));
            });
            return oResult;

        } catch (Exception oEx) {
            Utils.debugLog("Processing.getEnableUsersSharedWorkflow: " + oEx);
            return oResult;
        }


    }

    /**
     * Executes a workflow from a file stream
     *
     * @param fileInputStream
     * @param sSessionId
     * @param sWorkspace
     * @param sSourceProductName
     * @param sDestinationProdutName
     * @return
     * @throws Exception
     */
    @POST
    @Path("/graph")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public PrimitiveResult executeGraph(@FormDataParam("file") InputStream fileInputStream,
                                        @HeaderParam("x-session-token") String sSessionId, @QueryParam("workspace") String sWorkspace,
                                        @QueryParam("source") String sSourceProductName, @QueryParam("destination") String
                                                sDestinationProdutName, @QueryParam("parent") String sParentProcessWorkspaceId)
            throws Exception {

        Utils.debugLog("ProcessingResources.ExecuteGraph( Ws: " + sWorkspace + ", Source: " + sSourceProductName + ", Dest: " + sDestinationProdutName + " )");
        PrimitiveResult oResult = new PrimitiveResult();

        if (Utils.isNullOrEmpty(sSessionId)) {
            oResult.setBoolValue(false);
            oResult.setIntValue(401);
            return oResult;
        }

        User oUser = Wasdi.getUserFromSession(sSessionId);

        if (oUser == null) {
            Utils.debugLog("ProcessingResources.ExecuteGraph( InputStream, " + sSessionId + ", Ws: " + sWorkspace + ", Source: " + sSourceProductName + ", Dest: " + sDestinationProdutName + " ): invalid session");
            oResult.setBoolValue(false);
            oResult.setIntValue(401);
            return oResult;
        }

        if (Utils.isNullOrEmpty(oUser.getUserId())) {
            oResult.setBoolValue(false);
            oResult.setIntValue(401);
            return oResult;
        }

        GraphSetting oSettings = new GraphSetting();
        String sGraphXml;
        sGraphXml = IOUtils.toString(fileInputStream, Charset.defaultCharset().name());
        oSettings.setGraphXml(sGraphXml);

        return executeOperation(sSessionId, sSourceProductName, sDestinationProdutName, sWorkspace, oSettings, LauncherOperations.GRAPH, sParentProcessWorkspaceId);

    }


    /**
     * Exectues a Workflow from workflow Id
     *
     * @param sSessionId
     * @param sWorkspace
     * @param sParentProcessWorkspaceId
     * @param oSnapWorkflowViewModel
     * @return
     * @throws Exception
     */
    @POST
    @Path("/graph_id")
    public PrimitiveResult executeGraphFromWorkflowId(@HeaderParam("x-session-token") String sSessionId,
                                                      @QueryParam("workspace") String sWorkspace, @QueryParam("parent") String
                                                              sParentProcessWorkspaceId, SnapWorkflowViewModel oSnapWorkflowViewModel) throws Exception {

        PrimitiveResult oResult = new PrimitiveResult();
        Utils.debugLog("ProcessingResources.executeGraphFromWorkflowId( Ws: " + sWorkspace + ", ... )");

        if (Utils.isNullOrEmpty(sSessionId)) {
            oResult.setBoolValue(false);
            oResult.setIntValue(401);
            return oResult;
        }
        User oUser = Wasdi.getUserFromSession(sSessionId);
        if (oUser == null) {
            Utils.debugLog("ProcessingResources.executeGraphFromWorkflowId( Session: " + sSessionId + ", Ws: " + sWorkspace + ", ... ): invalid session");
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
            	
            	WorkflowSharingRepository oWorkflowSharingRepository = new WorkflowSharingRepository();
            	
            	WorkflowSharing oWorkflowSharing = oWorkflowSharingRepository.getWorkflowSharingByUserIdWorkflowId(sUserId, oSnapWorkflowViewModel.getWorkflowId());
            	
            	if (oWorkflowSharing==null) {
            		
            		Utils.debugLog("ProcessingResources.executeGraphFromWorkflowId: Workflow now owned or shared, exit");
            		
                    oResult.setBoolValue(false);
                    oResult.setIntValue(401);
                    return oResult;
            	}            	
            }

            String sBasePath = Wasdi.getDownloadPath(m_oServletConfig);
            String sWorkflowPath = sBasePath + "workflows/" + oWF.getWorkflowId() + ".xml";
            File oWorkflowFile = new File(sWorkflowPath);

            if (!oWorkflowFile.exists()) {
                Utils.debugLog("ProcessingResources.executeGraphFromWorkflowId: Workflow file not on this node. Try to download it");

                String sDownloadedWorflowPath = Wasdi.downloadWorkflow(oWF.getNodeUrl(), oWF.getWorkflowId(), sSessionId, m_oServletConfig);

                if (Utils.isNullOrEmpty(sDownloadedWorflowPath)) {
                    Utils.debugLog("Error downloading workflow. Return error");
                    oResult.setBoolValue(false);
                    oResult.setIntValue(500);
                    return oResult;
                }

                sWorkflowPath = sDownloadedWorflowPath;
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

                if (oSnapWorkflowViewModel.getInputFileNames().size() > 00) {
                    sSourceProductName = oSnapWorkflowViewModel.getInputFileNames().get(0);
                    // TODO: Output file name
                    sDestinationProdutName = sSourceProductName + "_" + sWorkFlowName;
                }

                return executeOperation(sSessionId, sSourceProductName, sDestinationProdutName, sWorkspace, oGraphSettings, LauncherOperations.GRAPH, sParentProcessWorkspaceId);
            }
        } catch (Exception oEx) {
            Utils.debugLog("ProcessingResources.executeGraphFromWorkflowId: Error " + oEx.toString());
            oResult.setBoolValue(false);
            oResult.setIntValue(500);
            return oResult;
        }
    }


    @GET
    @Path("downloadgraph")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadGraphById(@HeaderParam("x-session-token") String sSessionId,
                                      @QueryParam("token") String sTokenSessionId,
                                      @QueryParam("workflowId") String sWorkflowId) {

        Utils.debugLog("ProcessingResource.downloadGraphByName( WorkflowId: " + sWorkflowId + " )");

        try {

            if (Utils.isNullOrEmpty(sSessionId) == false) {
                sTokenSessionId = sSessionId;
            }

            User oUser = Wasdi.getUserFromSession(sTokenSessionId);

            if (oUser == null) {
                Utils.debugLog("ProcessingResource.downloadGraphByName( Session: " + sSessionId + ", WorkflowId: " + sWorkflowId + " ): invalid session");
                return Response.status(Status.UNAUTHORIZED).build();
            }

            // Take path
            String sDownloadRootPath = Wasdi.getDownloadPath(m_oServletConfig);
            String sWorkflowXmlPath = sDownloadRootPath + "workflows/" + sWorkflowId + ".xml";

            File oFile = new File(sWorkflowXmlPath);

            ResponseBuilder oResponseBuilder = null;

            if (oFile.exists() == false) {
                Utils.debugLog("ProcessingResource.downloadGraphByName: file does not exists " + oFile.getPath());
                oResponseBuilder = Response.serverError();
            } else {

                Utils.debugLog("ProcessingResource.downloadGraphByName: returning file " + oFile.getPath());

                FileStreamingOutput oStream;
                oStream = new FileStreamingOutput(oFile);

                oResponseBuilder = Response.ok(oStream);
                oResponseBuilder.header("Content-Disposition", "attachment; filename=" + oFile.getName());
                oResponseBuilder.header("Content-Length", Long.toString(oFile.length()));
            }

            return oResponseBuilder.build();

        } catch (Exception oEx) {
            Utils.debugLog("ProcessingResource.downloadGraphByName: " + oEx);
        }

        return null;
    }


    @GET
    @Path("/standardfilters")
    @Produces({"application/json"})
    public Map<String, Filter[]> getStandardFilters(@HeaderParam("x-session-token") String sSessionId) {

        Utils.debugLog("ProcessingResources.GetStandardFilters");

        Map<String, Filter[]> aoFiltersMap = new HashMap<String, Filter[]>();
        try {
            User oUser = Wasdi.getUserFromSession(sSessionId);
            if (null == oUser) {
                Utils.debugLog("ProcessingResources.GetStandardFilters( " + sSessionId + " ): invalid session");
                return aoFiltersMap;

            }

            aoFiltersMap.put("Detect Lines", StandardFilters.LINE_DETECTION_FILTERS);
            aoFiltersMap.put("Detect Gradients (Emboss)", StandardFilters.GRADIENT_DETECTION_FILTERS);
            aoFiltersMap.put("Smooth and Blurr", StandardFilters.SMOOTHING_FILTERS);
            aoFiltersMap.put("Sharpen", StandardFilters.SHARPENING_FILTERS);
            aoFiltersMap.put("Enhance Discontinuities", StandardFilters.LAPLACIAN_FILTERS);
            aoFiltersMap.put("Non-Linear Filters", StandardFilters.NON_LINEAR_FILTERS);
            aoFiltersMap.put("Morphological Filters", StandardFilters.MORPHOLOGICAL_FILTERS);
        } catch (Exception oE) {
            Utils.debugLog("ProcessingResources.GetStandardFilters( " + sSessionId + " ): " + oE);
        }
        return aoFiltersMap;
    }

    @GET
    @Path("/productmasks")
    @Produces({"application/json"})
    public ArrayList<ProductMaskViewModel> getProductMasks(@HeaderParam("x-session-token") String sSessionId,
                                                           @QueryParam("file") String sProductFile, @QueryParam("band") String sBandName,
                                                           @QueryParam("workspaceId") String sWorkspaceId) throws Exception {

        Utils.debugLog("ProcessingResources.getProductMasks");

        if (Utils.isNullOrEmpty(sSessionId)) return null;
        User oUser = Wasdi.getUserFromSession(sSessionId);
        if (oUser == null) {
            Utils.debugLog("ProcessingResources.getProductMasks( " + sSessionId + ", " + sProductFile + ", " +
                    sBandName + ", " + sWorkspaceId + " ): invalid session");
            return null;
        }
        if (Utils.isNullOrEmpty(oUser.getUserId())) return null;

        Utils.debugLog("Params. File: " + sProductFile + " - Band: " + sBandName + " - Workspace: " + sWorkspaceId);

        String sProductFileFullPath = Wasdi.getWorkspacePath(m_oServletConfig, Wasdi.getWorkspaceOwner(sWorkspaceId),
                sWorkspaceId) + sProductFile;

        Utils.debugLog("ProcessingResources.getProductMasks: file Path: " + sProductFileFullPath);

        ArrayList<ProductMaskViewModel> aoMasks = new ArrayList<ProductMaskViewModel>();

        try {
            Product product = ProductIO.readProduct(sProductFileFullPath);
            Band band = product.getBand(sBandName);

            final ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
            for (int i = 0; i < maskGroup.getNodeCount(); i++) {
                final Mask mask = maskGroup.get(i);
                if (mask.getRasterWidth() == band.getRasterWidth()
                        && mask.getRasterHeight() == band.getRasterHeight()) {
                    ProductMaskViewModel vm = new ProductMaskViewModel();
                    vm.setName(mask.getName());
                    vm.setDescription(mask.getDescription());
                    vm.setMaskType(mask.getImageType().getName());
                    vm.setColorRed(mask.getImageColor().getRed());
                    vm.setColorGreen(mask.getImageColor().getGreen());
                    vm.setColorBlue(mask.getImageColor().getBlue());
                    aoMasks.add(vm);
                }
            }
        } catch (Exception oEx) {
            Utils.debugLog("ProcessingResources.getProductMasks: " + oEx);
        }

        return aoMasks;
    }

    @GET
    @Path("/productcolormanipulation")
    @Produces({"application/json"})
    public ColorManipulationViewModel getColorManipulation(@HeaderParam("x-session-token") String sSessionId,
                                                           @QueryParam("file") String sProductFile, @QueryParam("band") String sBandName,
                                                           @QueryParam("accurate") boolean bAccurate, @QueryParam("workspaceId") String sWorkspaceId)
            throws Exception {
        try {
            Utils.debugLog("ProcessingResources.getColorManipulation( Product: " + sProductFile + ", Band:" + sBandName + ", Accurate: " + bAccurate + ", WS: " + sWorkspaceId + " )");

            if (Utils.isNullOrEmpty(sSessionId)) return null;
            User oUser = Wasdi.getUserFromSession(sSessionId);

            if (oUser == null) {
                Utils.debugLog("ProcessingResources.getColorManipulation( Session: " + sSessionId +
                        ", Product: " + sProductFile + ", Band:" + sBandName + ", Accurate: " + bAccurate +
                        ", WS: " + sWorkspaceId + " ): invalid session");
                return null;
            }
            if (Utils.isNullOrEmpty(oUser.getUserId())) return null;


            Utils.debugLog("ProcessingResources.getColorManipulation. Params. File: " + sProductFile + " - Band: " + sBandName + " - Workspace: " + sWorkspaceId);

            String sProductFileFullPath = Wasdi.getWorkspacePath(m_oServletConfig, Wasdi.getWorkspaceOwner(sWorkspaceId), sWorkspaceId);

            Utils.debugLog("ProcessingResources.getColorManipulation: file Path: " + sProductFileFullPath);

            Product product = ProductIO.readProduct(sProductFileFullPath);
            BandImageManager manager = new BandImageManager(product);
            return manager.getColorManipulation(sBandName, bAccurate);
        } catch (Exception oE) {
            Utils.debugLog("ProcessingResources.getColorManipulation( Session: " + sSessionId + ", Product: " + sProductFile +
                    ", Band:" + sBandName + ", Accurate: " + bAccurate + ", WS: " + sWorkspaceId + " ): " + oE);
        }
        return null;
    }

    @POST
    @Path("/bandimage")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getBandImage(@HeaderParam("x-session-token") String sSessionId,
                                 @QueryParam("workspace") String sWorkspace, BandImageViewModel oBandImageViewModel) throws IOException {

        try {
            Utils.debugLog("ProcessingResources.getBandImage( WS: " + sWorkspace + ", ... )");

            // Check user session
            String sUserId = acceptedUserAndSession(sSessionId);
            if (Utils.isNullOrEmpty(sUserId)) {
                Utils.debugLog("ProcessingResources.getBandImage( Session: " + sSessionId + ", WS: " + sWorkspace + ", ... ): invalid session");
                return Response.status(401).build();
            }

            // Init the registry for JAI
            OperationRegistry oOperationRegistry = JAI.getDefaultInstance().getOperationRegistry();
            RegistryElementDescriptor oDescriptor = oOperationRegistry.getDescriptor("rendered", "Paint");

            if (oDescriptor == null) {
                Utils.debugLog("getBandImage: REGISTER Descriptor!!!!");
                try {
                    oOperationRegistry.registerServices(this.getClass().getClassLoader());
                } catch (Exception e) {
                    Utils.debugLog("ProcessingResources.getBandImage: " + e);
                }
                oDescriptor = oOperationRegistry.getDescriptor("rendered", "Paint");

                IIORegistry.getDefaultInstance().registerApplicationClasspathSpis();
            }

            String sProductPath = Wasdi.getWorkspacePath(m_oServletConfig, Wasdi.getWorkspaceOwner(sWorkspace), sWorkspace);
            File oProductFile = new File(sProductPath + oBandImageViewModel.getProductFileName());

            if (!oProductFile.exists()) {
                Utils.debugLog("ProcessingResource.getBandImage: FILE NOT FOUND: " + oProductFile.getAbsolutePath());
                return Response.status(500).build();
            }

            Product oSNAPProduct = ProductIO.readProduct(oProductFile);

            if (oSNAPProduct == null) {
                Utils.debugLog("ProcessingResources.getBandImage: SNAP product is null, impossibile to read. Return");
                return Response.status(500).build();
            } else {
                Utils.debugLog("ProcessingResources.getBandImage: product read");
            }

            BandImageManager oBandImageManager = new BandImageManager(oSNAPProduct);

            RasterDataNode oRasterDataNode = null;

            if (oBandImageViewModel.getFilterVM() != null) {
                Filter oFilter = oBandImageViewModel.getFilterVM().getFilter();
                FilterBand oFilteredBand = oBandImageManager.getFilterBand(oBandImageViewModel.getBandName(), oFilter, oBandImageViewModel.getFilterIterationCount());

                if (oFilteredBand == null) {
                    Utils.debugLog("ProcessingResource.getBandImage: CANNOT APPLY FILTER TO BAND " + oBandImageViewModel.getBandName());
                    return Response.status(500).build();
                }
                oRasterDataNode = oFilteredBand;
            } else {
                oRasterDataNode = oSNAPProduct.getBand(oBandImageViewModel.getBandName());
            }

            if (oBandImageViewModel.getVp_x() < 0 || oBandImageViewModel.getVp_y() < 0
                    || oBandImageViewModel.getImg_w() <= 0 || oBandImageViewModel.getImg_h() <= 0) {
                Utils.debugLog("ProcessingResources.getBandImage: Invalid Parameters: VPX= " + oBandImageViewModel.getVp_x()
                        + " VPY= " + oBandImageViewModel.getVp_y() + " VPW= " + oBandImageViewModel.getVp_w() + " VPH= "
                        + oBandImageViewModel.getVp_h() + " OUTW = " + oBandImageViewModel.getImg_w() + " OUTH = "
                        + oBandImageViewModel.getImg_h());
                return Response.status(500).build();
            } else {
                Utils.debugLog("ProcessingResources.getBandImage: parameters OK");
            }

            Rectangle oRectangleViewPort = new Rectangle(oBandImageViewModel.getVp_x(), oBandImageViewModel.getVp_y(), oBandImageViewModel.getVp_w(), oBandImageViewModel.getVp_h());
            Dimension oImgSize = new Dimension(oBandImageViewModel.getImg_w(), oBandImageViewModel.getImg_h());

            // apply product masks
            List<ProductMaskViewModel> aoProductMasksModels = oBandImageViewModel.getProductMasks();
            if (aoProductMasksModels != null) {
                for (ProductMaskViewModel oMaskModel : aoProductMasksModels) {
                    Mask oMask = oSNAPProduct.getMaskGroup().get(oMaskModel.getName());
                    if (oMask == null) {
                        Utils.debugLog("ProcessingResources.getBandImage: cannot find mask by name: " + oMaskModel.getName());
                    } else {
                        // set the user specified color
                        oMask.setImageColor(new Color(oMaskModel.getColorRed(), oMaskModel.getColorGreen(), oMaskModel.getColorBlue()));
                        oMask.setImageTransparency(oMaskModel.getTransparency());
                        oRasterDataNode.getOverlayMaskGroup().add(oMask);
                    }
                }
            }

            // applying range masks
            List<RangeMaskViewModel> aoRangeMasksModels = oBandImageViewModel.getRangeMasks();
            if (aoRangeMasksModels != null) {
                for (RangeMaskViewModel oMaskModel : aoRangeMasksModels) {

                    Mask oMask = createMask(oSNAPProduct, oMaskModel, Mask.RangeType.INSTANCE);

                    String sExternalName = Tokenizer.createExternalName(oBandImageViewModel.getBandName());
                    PropertyContainer oImageConfig = oMask.getImageConfig();
                    oImageConfig.setValue(Mask.RangeType.PROPERTY_NAME_MINIMUM, oMaskModel.getMin());
                    oImageConfig.setValue(Mask.RangeType.PROPERTY_NAME_MAXIMUM, oMaskModel.getMax());
                    oImageConfig.setValue(Mask.RangeType.PROPERTY_NAME_RASTER, sExternalName);
                    oSNAPProduct.addMask(oMask);
                    oRasterDataNode.getOverlayMaskGroup().add(oMask);
                }
            }

            // applying math masks
            List<MathMaskViewModel> aoMathMasksModels = oBandImageViewModel.getMathMasks();
            if (aoMathMasksModels != null) {
                for (MathMaskViewModel oMaskModel : aoMathMasksModels) {

                    Mask oMask = createMask(oSNAPProduct, oMaskModel, Mask.BandMathsType.INSTANCE);

                    PropertyContainer oImageConfig = oMask.getImageConfig();
                    oImageConfig.setValue(Mask.BandMathsType.PROPERTY_NAME_EXPRESSION, oMaskModel.getExpression());
                    oSNAPProduct.addMask(oMask);
                    oRasterDataNode.getOverlayMaskGroup().add(oMask);
                }
            }

            // applying color manipulation

            ColorManipulationViewModel oColorManiputalionViewModel = oBandImageViewModel.getColorManiputalion();
            if (oColorManiputalionViewModel != null) {
                oBandImageManager.applyColorManipulation(oRasterDataNode, oColorManiputalionViewModel);
            }

            // creating the image
            BufferedImage oBufferedImg;
            try {
                oBufferedImg = oBandImageManager.buildImageWithMasks(oRasterDataNode, oImgSize, oRectangleViewPort, oColorManiputalionViewModel == null);
            } catch (Exception e) {
                Utils.debugLog("ProcessingResources.getBandImage: Exception: " + e.toString());
                Utils.debugLog("ProcessingResources.getBandImage: ExMessage: " + e.getMessage());
                e.printStackTrace();
                return Response.status(500).build();
            }

            if (oBufferedImg == null) {
                Utils.debugLog("ProcessingResource.getBandImage: img null");
                return Response.status(500).build();
            }

            Utils.debugLog("ProcessingResource.getBandImage: Generated image for band " + oBandImageViewModel.getBandName()
                    + " X= " + oBandImageViewModel.getVp_x() + " Y= " + oBandImageViewModel.getVp_y() + " W= "
                    + oBandImageViewModel.getVp_w() + " H= " + oBandImageViewModel.getVp_h());

            ByteArrayOutputStream oByteOutStream = new ByteArrayOutputStream();
            ImageIO.write(oBufferedImg, "jpg", oByteOutStream);
            byte[] ayImageData = oByteOutStream.toByteArray();

            return Response.ok(ayImageData).build();
        } catch (Exception oE) {
            Utils.debugLog("ProcessingResources.getBandImage( Session: " + sSessionId + ", WS: " + sWorkspace + ", ... ): " + oE);
        }
        return Response.serverError().build();
    }

    private Mask createMask(Product oSNAPProduct, MaskViewModel maskModel, Mask.ImageType type) {
        Utils.debugLog("ProcessingResource.createMask( Product, MaskViewModel, Mask.ImageType )");
        String maskName = UUID.randomUUID().toString();
        Dimension maskSize = new Dimension(oSNAPProduct.getSceneRasterWidth(), oSNAPProduct.getSceneRasterHeight());
        Mask mask = new Mask(maskName, maskSize.width, maskSize.height, type);
        mask.setImageColor(new Color(maskModel.getColorRed(), maskModel.getColorGreen(), maskModel.getColorBlue()));
        mask.setImageTransparency(maskModel.getTransparency());
        return mask;
    }

    @GET
    @Path("/WPSlist")
    @Produces({"application/xml", "application/json", "text/xml"})
    public ArrayList<WpsViewModel> getWpsList(@HeaderParam("x-session-token") String sSessionId) {
        try {
            Utils.debugLog("ProcessingResource.getWpsList");
            User oUser = Wasdi.getUserFromSession(sSessionId);
            if (null == oUser) {
                Utils.debugLog("ProcessingResource.getWpsList( " + sSessionId + " ): invalid session");
                return null;
            }

            WpsProvidersRepository oWPSrepo = new WpsProvidersRepository();
            ArrayList<WpsProvider> aoWPSProviders = oWPSrepo.getWpsList();

            if (null != aoWPSProviders) {
                ArrayList<WpsViewModel> aoResult = new ArrayList<WpsViewModel>();
                for (WpsProvider oWpsProvider : aoWPSProviders) {
                    if (null != oWpsProvider) {
                        WpsViewModel oWpsViewModel = new WpsViewModel();
                        oWpsViewModel.setAddress(oWpsProvider.getAddress());
                        aoResult.add(oWpsViewModel);
                    }
                }
                return aoResult;
            }
        } catch (Exception oE) {
            Utils.debugLog("ProcessingResource.getWpsList( " + sSessionId + " ): " + oE);
        }
        return null;
    }

    private String acceptedUserAndSession(String sSessionId) {
        try {
            Utils.debugLog("ProcessingResource.acceptedUserAndSession");
            // Check user
            if (Utils.isNullOrEmpty(sSessionId))
                return null;
            User oUser = Wasdi.getUserFromSession(sSessionId);
            if (oUser == null) {
                Utils.debugLog("ProcessingResource.acceptedUserAndSession( " + sSessionId + " ): invalid session");
                return null;
            }
            if (Utils.isNullOrEmpty(oUser.getUserId()))
                return null;

            return oUser.getUserId();
        } catch (Exception oE) {
            Utils.debugLog("ProcessingResource.acceptedUserAndSession( " + sSessionId + " ): " + oE);
        }
        return null;
    }

    /**
     * Trigger the execution in the launcher of a SNAP Operation
     *
     * @param sSessionId                User Session Id
     * @param sSourceProductName        Source Product Name
     * @param sDestinationProductName   Target Product Name
     * @param sWorkspaceId              Active Workspace
     * @param oSetting                  Generic Operation Setting
     * @param oOperation                Launcher Operation Type
     * @param sParentProcessWorkspaceId Id of the parent Process Workspace or null
     * @return
     */
    private PrimitiveResult executeOperation(String sSessionId, String sSourceProductName, String
            sDestinationProductName, String sWorkspaceId, ISetting oSetting, LauncherOperations oOperation, String
                                                     sParentProcessWorkspaceId) {

        Utils.debugLog("ProsessingResources.executeOperation (Source: " + sSourceProductName + ", Destination: "
                + sDestinationProductName + ", WS: " + sWorkspaceId + ", Parent: " + sParentProcessWorkspaceId + " )");
        PrimitiveResult oResult = new PrimitiveResult();
        String sProcessObjId = "";

        // Check the user
        String sUserId = acceptedUserAndSession(sSessionId);

        // Is valid?
        if (Utils.isNullOrEmpty(sUserId)) {

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
            oParameter.setUserId(sUserId);
            oParameter.setExchange(sWorkspaceId);
            oParameter.setProcessObjId(sProcessObjId);
            oParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sWorkspaceId));

            // Do we have settings?
            if (oSetting != null) oParameter.setSettings(oSetting);

            // Serialization Path
            String sPath = m_oServletConfig.getInitParameter("SerializationPath");

            return Wasdi.runProcess(sUserId, sSessionId, oOperation.name(), sSourceProductName, sPath, oParameter, sParentProcessWorkspaceId);

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

    @POST
    @Path("run")
    @Produces({"application/xml", "application/json", "text/xml"})
    public PrimitiveResult runProcess(@HeaderParam("x-session-token") String sSessionId,
                                      @QueryParam("sOperation") String sOperationId, @QueryParam("sProductName") String
                                              sProductName, @QueryParam("parent") String sParentProcessWorkspaceId, @QueryParam("subtype") String
                                              sOperationSubType, String sParameter) throws IOException {

        if (Utils.isNullOrEmpty(sOperationSubType)) {
            sOperationSubType = "";
        }
        if (Utils.isNullOrEmpty(sParentProcessWorkspaceId)) sParentProcessWorkspaceId = "";
        // Log intro
        Utils.debugLog("ProsessingResources.runProcess( Operation: " + sOperationId + ", OperationSubType: " + sOperationSubType + ", Product: " + sProductName + " Parent Id: " + sParentProcessWorkspaceId + ")");
        PrimitiveResult oResult = new PrimitiveResult();

        try {
            if (!LauncherOperationsUtils.isValidLauncherOperation(sOperationId)) {
                // Bad request
                oResult.setIntValue(400);
                oResult.setBoolValue(false);
                return oResult;
            }

            // Check the user
            String sUserId = acceptedUserAndSession(sSessionId);

            // Is valid?
            if (Utils.isNullOrEmpty(sUserId)) {

                // Not authorised
                oResult.setIntValue(401);
                oResult.setBoolValue(false);

                return oResult;
            }

            BaseParameter oParameter = BaseParameter.getParameterFromOperationType(sOperationId);

            if (oParameter == null) {
                // Error
                oResult.setIntValue(500);
                oResult.setBoolValue(false);

                return oResult;
            }

            oParameter = (BaseParameter) SerializationUtils.deserializeStringXMLToObject(sParameter);

            String sPath = m_oServletConfig.getInitParameter("SerializationPath");

            return Wasdi.runProcess(sUserId, sSessionId, sOperationId, sOperationSubType, sProductName, sPath, oParameter, sParentProcessWorkspaceId);
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
     *
     * @param oOperation
     * @return
     */
    private OperatorParameter getParameter(LauncherOperations oOperation) {
        Utils.debugLog("ProcessingResources.OperatorParameter(  LauncherOperations )");
        switch (oOperation) {
            case GRAPH:
                return new GraphParameter();
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
