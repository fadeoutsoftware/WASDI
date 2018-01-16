package wasdi.snapopearations;

import java.io.File;
import java.io.StringReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
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
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.ProductViewModel;


public class WasdiGraph {
	private File inputFile, outputFile;
	private Graph graph;
	private Logger logger = LauncherMain.s_oLogger;
	private ProcessWorkspaceRepository processRepository;
	private ProcessWorkspace process;
	private Send rabbitSender;
	private GraphParameter params;
	
	
	public WasdiGraph(GraphParameter params, Send rabbitSender) throws Exception {
		//set the pgraph parameters
		this.params = params;
		
		//set the rabbit sender
		this.rabbitSender = rabbitSender;
		
		//build snap graph object
		graph = GraphIO.read(new StringReader(((GraphSetting)(params.getSettings())).getGraphXml()));
		
		//retrieve wasdi process
        processRepository = new ProcessWorkspaceRepository();
        process = processRepository.GetProcessByProcessObjId(params.getProcessObjId());
        
        //check input file
        File baseDir = new File(ConfigReader.getPropValue("DOWNLOAD_ROOT_PATH"));
        File userDir = new File(baseDir, params.getUserId());
        File workspaceDir = new File(userDir, params.getWorkspace());
        inputFile = new File(workspaceDir, params.getSourceProductName());
        if (!inputFile.canRead()) throw new Exception("WasdiGraph: cannot access input file: " + inputFile.getAbsolutePath());
        
        //build output file path
        outputFile = new File(workspaceDir, params.getDestinationProductName());
	}


	public void execute() throws Exception {
		
		try {
		
			//set the input and output files to the graph snap object
			Node nodeReader = graph.getNode("Read");
			Node nodeWriter = graph.getNode("Write");
			
			if (nodeReader==null || nodeWriter==null) {
				logger.error("WasdiGraph.execute: Reader node and Writer node are mandatory!!");
				throw new Exception("Reader node and Writer node are mandatory");
			}		
			if (!setNodeValue(nodeReader, "file", inputFile.getAbsolutePath()) || 
					!setNodeValue(nodeWriter, "file", outputFile.getAbsolutePath()) ||
					!setNodeValue(nodeWriter, "formatName", DimapProductWriterPlugIn.DIMAP_FORMAT_NAME) ) {
				throw new Exception("Error setting input/output file");
			}
			
			// P.Campanella 16/06/2017: should add real file size to the Process Log
            //set file size     
            LauncherMain.SetFileSizeToProcess(inputFile, process);
			
			//build the snap graph context and processor
			GraphContext context = new GraphContext(graph);		
			GraphProcessor processor = new GraphProcessor();
			
			//update the wasdi process 
			initProcess();
			
			processor.executeGraph(context, new WasdiProgreeMonitor(processRepository, process));
			
			Product[] outputs = context.getOutputProducts();
			if (outputs==null || outputs.length==0) throw new Exception("No output created");
			
			if (outputs.length>1) logger.warn("More than 1 output created... keep only the first");
			
			Product product = outputs[0];
	
			logger.debug("WasdiGraph.execute: managing output product: " + product.getName());
			
            // Get the original Bounding Box
            DownloadedFilesRepository downloadedRepo = new DownloadedFilesRepository();
            DownloadedFile downloadedFile = downloadedRepo.GetDownloadedFile(inputFile.getName()) ;
            String bbox = "";            
            if (downloadedFile != null) {
            	bbox = downloadedFile.getBoundingBox();
            }
            
            addProductToDb(product, bbox);
			
        
		} finally {
            closeProcess();
		}
			
	}


	private void initProcess() throws Exception {
		if (process != null) {
			//set source file size in the process
			long inputFileSize = inputFile.length();
		    double inputFileSizeGiga = ( (double) inputFileSize )/ (1024.0 * 1024.0 * 1024.0);
		    DecimalFormat format = new DecimalFormat("#.00"); 	        
		    logger.debug("WasdiGraph.execute: File size [Gb] = " + format.format(inputFileSizeGiga));
		    process.setFileSize(format.format(inputFileSizeGiga));
		    //set process pid, status and progress
			process.setPid(GetProcessId());
			process.setStatus(ProcessStatus.RUNNING.name());
			process.setProgressPerc(0);
			//update the process
		    if (!processRepository.UpdateProcess(process)) {
		    	logger.error("WasdiGraph.execute: Error during process update (pip + starting)");
		    } else {
		    	logger.debug("WasdiGraph.execute: Updated process  " + process.getProcessObjId());
		    }
	        //send update process message
		    if (rabbitSender!=null && !rabbitSender.SendUpdateProcessMessage(process)) {
				logger.error("WasdiGraph.execute: Error sending rabbitmq message to update process list");
			}
		}
		
	}

	
    /**
     * Close the Process on the mongo Db. Set progress to 100 and end date time 
     */
	private void closeProcess() {
		try{
			if (process != null) {
		        //update the process
				process.setProgressPerc(100);
				process.setStatus(ProcessStatus.DONE.name());
				process.setOperationEndDate(Utils.GetFormatDate(new Date()));
		        if (!processRepository.UpdateProcess(process)) {
		        	logger.error("WasdiGraph: Error during process update (terminated)");
		        }
		        //send update process message
				if (rabbitSender!=null && !rabbitSender.SendUpdateProcessMessage(process)) {
				    logger.debug("WasdiGraph: Error sending rabbitmq message to update process list");
				}
			}
		} catch (Exception e) {
		    logger.error("WasdiGraph: Exception closing process", e);
		}
	}

	
    private void addProductToDb(Product product, String sBBox) throws Exception {
        ReadProduct readProduct = new ReadProduct();
        File productFile = new File(outputFile.getAbsolutePath()+".dim");
		ProductViewModel oVM = readProduct.getProductViewModel(product, productFile);
        
        // P.Campanella 12/05/2017: it looks it is done before. Let leave here a check
        DownloadedFilesRepository oDownloadedRepo = new DownloadedFilesRepository();
        DownloadedFile oCheck = oDownloadedRepo.GetDownloadedFile(oVM.getFileName());
        boolean bAddProductToWS = true;
        if (oCheck == null) {
        	logger.debug("Insert in db");
        	
            // Save it in the register
            DownloadedFile oAlreadyDownloaded = new DownloadedFile();
            
            oAlreadyDownloaded.setFileName(productFile.getName());
            oAlreadyDownloaded.setFilePath(productFile.getAbsolutePath());
            oAlreadyDownloaded.setProductViewModel(oVM);
            oAlreadyDownloaded.setBoundingBox(sBBox);
            
            if (!oDownloadedRepo.InsertDownloadedFile(oAlreadyDownloaded)) {

            	logger.error("Impossible to Insert the new Product " + outputFile.getName() + " in the database. Try With out Metadata");
            	
            	oAlreadyDownloaded.getProductViewModel().setMetadata(null);
            	
                if (!oDownloadedRepo.InsertDownloadedFile(oAlreadyDownloaded)) {
                	bAddProductToWS = false;
                	logger.error("Impossible to Insert the new Product " + outputFile.getName() + " in the database also out Metadata");                	
                }
                else {
                	logger.error("Inserted WITHOUT METADATA");
                }
            }
            else {
            	logger.error("Product Inserted with Metadata");
            }
        }
        
        if (bAddProductToWS) {
        	addProductToWorkspace();
        }
        else {
        	logger.error("Product NOT added to the Workspace");
        }
        
        logger.debug("OK DONE");
        
        //P.Campanella 12/05/2017: Metadata are saved in the DB but sent back to the client with a dedicated API. So here metadata are nulled
        oVM.setMetadata(null);

        if (rabbitSender!=null) rabbitSender.SendRabbitMessage(true, LauncherOperations.GRAPH.name(), params.getWorkspace(), oVM, params.getExchange());
    }
	
	
    private void addProductToWorkspace() throws Exception {
    	
		// Create Repository
		ProductWorkspaceRepository productRepository = new ProductWorkspaceRepository();
		
		// Check if product is already in the Workspace
		if (!productRepository.ExistsProductWorkspace(params.getDestinationProductName(), params.getWorkspace())) {
			
    		// Create the entity
    		ProductWorkspace productEntity = new ProductWorkspace();
    		productEntity.setProductName(params.getDestinationProductName() + ".dim");
    		productEntity.setWorkspaceId(params.getWorkspace());
    		
    		// Try to insert
    		if (!productRepository.InsertProductWorkspace(productEntity)) {        			
    			logger.debug("WasdiGraph.addProductToWorkspace:  Error adding " + params.getDestinationProductName() + " in WS " + params.getWorkspace());
    			throw new Exception("unable to insert product in workspace");
    		}
    		
    		logger.debug("WasdiGraph.addProductToWorkspace:  Inserted " + params.getDestinationProductName() + " in WS " + params.getWorkspace());
		}
		else {
			logger.debug("WasdiGraph.addProductToWorkspace: Product " + params.getDestinationProductName() + " Already exists in WS " + params.getWorkspace());
		}
    }

	
	private int GetProcessId() throws Exception {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        Field jvmField = runtimeMXBean.getClass().getDeclaredField("jvm");
        jvmField.setAccessible(true);
        VMManagement vmManagement = (VMManagement) jvmField.get(runtimeMXBean);
        Method getProcessIdMethod = vmManagement.getClass().getDeclaredMethod("getProcessId");
        getProcessIdMethod.setAccessible(true);
        return (int)getProcessIdMethod.invoke(vmManagement);
    }
	
	public static boolean setNodeValue(Node node, String childName, String value) {
		DomElement el = node.getConfiguration();
		DomElement[] children = el.getChildren(childName);
		if (children==null || children.length!=1) {
			
			XppDomElement oFileElement = new XppDomElement("file");
			oFileElement.setValue(value);
			el.addChild(oFileElement);
			
			return true;
		}
		else {
			children[0].setValue(value);
		}
		
		
		return true;
	}
	
	
}
