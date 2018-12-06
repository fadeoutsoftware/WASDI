package wasdi.snapopearations;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.log4j.Logger;
import org.esa.snap.core.dataio.dimap.DimapProductWriterPlugIn;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphContext;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.esa.snap.core.gpf.graph.GraphProcessor;
import org.esa.snap.core.gpf.graph.Node;

import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.binding.dom.XppDomElement;

import sun.management.VMManagement;
import wasdi.ConfigReader;
import wasdi.LauncherMain;
import wasdi.rabbit.Send;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.ProductWorkspace;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProductWorkspaceRepository;
import wasdi.shared.parameters.GraphParameter;
import wasdi.shared.parameters.GraphSetting;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.ProductViewModel;


/**
 * Wasdi SNAP Workflow Graph Wrapper and Exectur
 * @author p.campanella
 *
 */
public class WasdiGraph {
	
	private File m_oInputFile;
	private File m_oOutputFile;
	private Graph m_oGraph;
	private Logger m_oLogger = LauncherMain.s_oLogger;
	private ProcessWorkspaceRepository m_oProcessRepository;
	private ProcessWorkspace m_oProcess;
	private Send m_oRabbitSender;
	private GraphParameter m_oParams;
	
	private ArrayList<String> asInputNodes = new ArrayList<>();
	private ArrayList<String> asOutputNodes = new ArrayList<>();
	
	
	/**
	 * Construct the Graph object
	 * @param oParams
	 * @param oRabbitSender
	 * @throws Exception
	 */
	public WasdiGraph(GraphParameter oParams, Send oRabbitSender) throws Exception {
		//set the pgraph parameters
		this.m_oParams = oParams;
		
		//set the rabbit sender
		this.m_oRabbitSender = oRabbitSender;
		
		//build snap graph object
		m_oGraph = GraphIO.read(new StringReader(((GraphSetting)(oParams.getSettings())).getGraphXml()));
		
		//retrieve wasdi process
        m_oProcessRepository = new ProcessWorkspaceRepository();
        m_oProcess = m_oProcessRepository.GetProcessByProcessObjId(oParams.getProcessObjId());        
	}
	
	/**
	 * Find the Read and Write Nodes in this 
	 */
	public void findIONodes() {
		
		Node [] aoNodes = m_oGraph.getNodes();
		
		if (aoNodes == null) return;
		if (aoNodes.length == 0) return;
		
		for (int iNodes=0; iNodes<aoNodes.length;iNodes++) {
			Node oNode = aoNodes[iNodes];
			if (oNode.getOperatorName()=="Read") {
				asInputNodes.add(oNode.getId());
			}
			else if (oNode.getOperatorName() == "Write") {
				asOutputNodes.add(oNode.getId());
			}
		}
	}


	/**
	 * Run the graph
	 * @throws Exception
	 */
	public void execute() throws Exception {
		
		try {

	        //check input file
	        File oBaseDir = new File(ConfigReader.getPropValue("DOWNLOAD_ROOT_PATH"));
	        File oUserDir = new File(oBaseDir, m_oParams.getUserId());
	        File oWorkspaceDir = new File(oUserDir, m_oParams.getWorkspace());

			GraphSetting oGraphSettings = (GraphSetting) m_oParams.getSettings();
			
			for (int iNode = 0; iNode<oGraphSettings.getInputNodeNames().size(); iNode++) {
				
				String sInputNode = oGraphSettings.getInputNodeNames().get(iNode);
				
				//set the input and output files to the graph snap object
				Node oNodeReader = m_oGraph.getNode(sInputNode);
				
				if (oNodeReader==null ) {
					m_oLogger.error("WasdiGraph.execute: Reader node and Writer node are mandatory!!");
					throw new Exception("Reader node and Writer node are mandatory");
				}
				
		        File oInputFile = new File(oWorkspaceDir, oGraphSettings.getInputFileNames().get(iNode));
		        if (!oInputFile.canRead()) throw new Exception("WasdiGraph: cannot access input file: " + oInputFile.getAbsolutePath());
		        
		        if (m_oInputFile == null) {
		        	m_oInputFile= oInputFile;
		        }
				
				if (!setNodeValue(oNodeReader, "file", oInputFile.getAbsolutePath()))
				{
					throw new Exception("Error setting input file");
				}
			}
			
			for (int iNode = 0; iNode<oGraphSettings.getOutputNodeNames().size(); iNode++) {
				
				String sOutputNode = oGraphSettings.getOutputNodeNames().get(iNode);
				
				//set the input and output files to the graph snap object
				Node oNodeWriter = m_oGraph.getNode(sOutputNode);
				
				if (oNodeWriter==null ) {
					m_oLogger.error("WasdiGraph.execute: Reader node and Writer node are mandatory!!");
					throw new Exception("Reader node and Writer node are mandatory");
				}
				
				//TODO: output file name
		        File oOutputFile = new File(oWorkspaceDir, oGraphSettings.getInputFileNames().get(iNode)+"workflow");
		        
		        if (m_oOutputFile == null) {
		        	m_oOutputFile = oOutputFile;
		        }
				
				if (!setNodeValue(oNodeWriter, "file", oOutputFile.getAbsolutePath())) 
						//|| !setNodeValue(nodeWriter, "formatName", DimapProductWriterPlugIn.DIMAP_FORMAT_NAME) 
				{
					throw new Exception("Error setting output file");
				}
			}
			
			// P.Campanella 16/06/2017: should add real file size to the Process Log
            //set file size     
            LauncherMain.SetFileSizeToProcess(m_oInputFile, m_oProcess);
			
			//build the snap graph context and processor
			GraphContext oContext = new GraphContext(m_oGraph);		
			GraphProcessor oProcessor = new GraphProcessor();
			
			//update the wasdi process 
			initProcess();
			
			oProcessor.executeGraph(oContext, new WasdiProgreeMonitor(m_oProcessRepository, m_oProcess));
			
			Product[] aoOutputs = oContext.getOutputProducts();
			if (aoOutputs==null || aoOutputs.length==0) throw new Exception("No output created");
			
			//if (aoOutputs.length>1) m_oLogger.warn("More than 1 output created... keep only the first");
			
			for (int iOutputs = 0; iOutputs<aoOutputs.length; iOutputs++) {
				Product oProduct = aoOutputs[iOutputs];
				
				m_oLogger.debug("WasdiGraph.execute: managing output product ["+ iOutputs+"]: " + oProduct.getName());
				
	            // Get the original Bounding Box
	            DownloadedFilesRepository oDownloadedRepo = new DownloadedFilesRepository();
	            DownloadedFile oDownloadedFile = oDownloadedRepo.GetDownloadedFile(m_oInputFile.getName()) ;
	            
	            String sBbox = "";            
	            if (oDownloadedFile != null) {
	            	sBbox = oDownloadedFile.getBoundingBox();
	            }
	            
	            addProductToDb(oProduct, sBbox);				
			}
			
		} finally {
            closeProcess();
		}
			
	}

	/**
	 * Initialize the WASDI process
	 * @throws Exception
	 */
	private void initProcess() throws Exception {
		if (m_oProcess != null) {
			//set source file size in the process
			long lInputFileSize = m_oInputFile.length();
		    double dInputFileSizeGiga = ( (double) lInputFileSize )/ (1024.0 * 1024.0 * 1024.0);
		    DecimalFormat oFormat = new DecimalFormat("#.00"); 	        
		    m_oLogger.debug("WasdiGraph.execute: File size [Gb] = " + oFormat.format(dInputFileSizeGiga));
		    m_oProcess.setFileSize(oFormat.format(dInputFileSizeGiga));
		    //set process pid, status and progress
			m_oProcess.setPid(GetProcessId());
			m_oProcess.setStatus(ProcessStatus.RUNNING.name());
			m_oProcess.setProgressPerc(0);
			//update the process
		    if (!m_oProcessRepository.UpdateProcess(m_oProcess)) {
		    	m_oLogger.error("WasdiGraph.execute: Error during process update (pip + starting)");
		    } else {
		    	m_oLogger.debug("WasdiGraph.execute: Updated process  " + m_oProcess.getProcessObjId());
		    }
	        //send update process message
		    if (m_oRabbitSender!=null && !m_oRabbitSender.SendUpdateProcessMessage(m_oProcess)) {
				m_oLogger.error("WasdiGraph.execute: Error sending rabbitmq message to update process list");
			}
		}
		
	}

	
    /**
     * Close the Process on the mongo Db. Set progress to 100 and end date time 
     */
	private void closeProcess() {
		try{
			if (m_oProcess != null) {
		        //update the process
				m_oProcess.setProgressPerc(100);
				m_oProcess.setStatus(ProcessStatus.DONE.name());
				m_oProcess.setOperationEndDate(Utils.GetFormatDate(new Date()));
		        if (!m_oProcessRepository.UpdateProcess(m_oProcess)) {
		        	m_oLogger.error("WasdiGraph: Error during process update (terminated)");
		        }
		        //send update process message
				if (m_oRabbitSender!=null && !m_oRabbitSender.SendUpdateProcessMessage(m_oProcess)) {
				    m_oLogger.debug("WasdiGraph: Error sending rabbitmq message to update process list");
				}
			}
		} catch (Exception e) {
		    m_oLogger.error("WasdiGraph: Exception closing process", e);
		}
	}

	/**
	 * Adds Output product to WASDI DB
	 * @param product
	 * @param sBBox
	 * @throws Exception
	 */
    private void addProductToDb(Product product, String sBBox) throws Exception {
        ReadProduct oReadProduct = new ReadProduct();
        File oProductFile = new File(m_oOutputFile.getAbsolutePath()+".dim");
		ProductViewModel oVM = oReadProduct.getProductViewModel(product, oProductFile);
        
        // P.Campanella 12/05/2017: it looks it is done before. Let leave here a check
        DownloadedFilesRepository oDownloadedRepo = new DownloadedFilesRepository();
        DownloadedFile oCheck = oDownloadedRepo.GetDownloadedFile(oVM.getFileName());
        boolean bAddProductToWS = true;
        if (oCheck == null) {
        	m_oLogger.debug("Insert in db");
        	
            // Save it in the register
            DownloadedFile oOutputProduct = new DownloadedFile();
            
            oOutputProduct.setFileName(oProductFile.getName());
            oOutputProduct.setFilePath(oProductFile.getAbsolutePath());
            oOutputProduct.setProductViewModel(oVM);
            oOutputProduct.setBoundingBox(sBBox);
            
    		// Write Metadata to file system
            try {
                // Get Metadata Path a Random File Name
                String sMetadataPath = ConfigReader.getPropValue("METADATA_PATH");
        		if (!sMetadataPath.endsWith("/")) sMetadataPath += "/";
        		String sMetadataFileName = Utils.GetRandomName();

    			SerializationUtils.serializeObjectToXML(sMetadataPath+sMetadataFileName, oReadProduct.getProductMetadataViewModel(oProductFile));
    			
    			oOutputProduct.getProductViewModel().setMetadataFileReference(sMetadataFileName);
    			
    		} catch (IOException e) {
    			e.printStackTrace();
    		} catch (Exception e) {
    			e.printStackTrace();
    		}            
            
            if (!oDownloadedRepo.InsertDownloadedFile(oOutputProduct)) {
            	m_oLogger.error("Impossible to Insert the new Product " + m_oOutputFile.getName() + " in the database.");            	
            }
            else {
            	m_oLogger.error("Product Inserted " + m_oOutputFile.getName());
            }
        }
        
        if (bAddProductToWS) {
        	addProductToWorkspace();
        }
        else {
        	m_oLogger.error("Product NOT added to the Workspace");
        }
        
        m_oLogger.debug("OK DONE");
        
        //P.Campanella 12/05/2017: Metadata are saved in the DB but sent back to the client with a dedicated API. So here metadata are nulled
        //oVM.setMetadata(null);

        if (m_oRabbitSender!=null) m_oRabbitSender.SendRabbitMessage(true, LauncherOperations.GRAPH.name(), m_oParams.getWorkspace(), oVM, m_oParams.getExchange());
    }
	
	
    /**
     * Add product to the workspace
     * @throws Exception
     */
    private void addProductToWorkspace() throws Exception {
    	
		// Create Repository
		ProductWorkspaceRepository oProductRepository = new ProductWorkspaceRepository();
		
		// Check if product is already in the Workspace
		if (!oProductRepository.ExistsProductWorkspace(m_oParams.getDestinationProductName(), m_oParams.getWorkspace())) {
			
    		// Create the entity
    		ProductWorkspace oProductEntity = new ProductWorkspace();
    		oProductEntity.setProductName(m_oParams.getDestinationProductName() + ".dim");
    		oProductEntity.setWorkspaceId(m_oParams.getWorkspace());
    		
    		// Try to insert
    		if (!oProductRepository.InsertProductWorkspace(oProductEntity)) {        			
    			m_oLogger.debug("WasdiGraph.addProductToWorkspace:  Error adding " + m_oParams.getDestinationProductName() + " in WS " + m_oParams.getWorkspace());
    			throw new Exception("unable to insert product in workspace");
    		}
    		
    		m_oLogger.debug("WasdiGraph.addProductToWorkspace:  Inserted " + m_oParams.getDestinationProductName() + " in WS " + m_oParams.getWorkspace());
		}
		else {
			m_oLogger.debug("WasdiGraph.addProductToWorkspace: Product " + m_oParams.getDestinationProductName() + " Already exists in WS " + m_oParams.getWorkspace());
		}
    }

	/**
	 * Get Process Id
	 * @return
	 * @throws Exception
	 */
	private int GetProcessId() throws Exception {
        RuntimeMXBean oRuntimeMXBean = ManagementFactory.getRuntimeMXBean();
        Field oJvmField = oRuntimeMXBean.getClass().getDeclaredField("jvm");
        oJvmField.setAccessible(true);
        VMManagement oVmManagement = (VMManagement) oJvmField.get(oRuntimeMXBean);
        Method oGetProcessIdMethod = oVmManagement.getClass().getDeclaredMethod("getProcessId");
        oGetProcessIdMethod.setAccessible(true);
        return (int)oGetProcessIdMethod.invoke(oVmManagement);
    }
	
	/**
	 * Set the value of a node of the XML chart
	 * @param node
	 * @param childName
	 * @param value
	 * @return
	 */
	public static boolean setNodeValue(Node node, String childName, String value) {
		DomElement oDomElement = node.getConfiguration();
		DomElement[] aoChildren = oDomElement.getChildren(childName);
		if (aoChildren==null || aoChildren.length!=1) {
			
			XppDomElement oFileElement = new XppDomElement("file");
			oFileElement.setValue(value);
			oDomElement.addChild(oFileElement);
			
			return true;
		}
		else {
			aoChildren[0].setValue(value);
		}

		return true;
	}
	
	
}
