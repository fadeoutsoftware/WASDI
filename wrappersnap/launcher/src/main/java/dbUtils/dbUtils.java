package dbUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import wasdi.ProcessWorkspaceLogger;
import wasdi.processors.DockerProcessorEngine;
import wasdi.processors.WasdiProcessorEngine;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.AppCategory;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.Node;
import wasdi.shared.business.PasswordAuthentication;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.ProductWorkspace;
import wasdi.shared.business.Project;
import wasdi.shared.business.PublishedBand;
import wasdi.shared.business.SnapWorkflow;
import wasdi.shared.business.Subscription;
import wasdi.shared.business.Workspace;
import wasdi.shared.business.comparators.ProcessWorkspaceStartDateComparator;
import wasdi.shared.business.processors.Processor;
import wasdi.shared.business.processors.ProcessorLog;
import wasdi.shared.business.processors.ProcessorUI;
import wasdi.shared.business.statistics.Job;
import wasdi.shared.business.users.User;
import wasdi.shared.business.users.UserResourcePermission;
import wasdi.shared.business.users.UserSession;
import wasdi.shared.business.users.UserType;
import wasdi.shared.config.DockerRegistryConfig;
import wasdi.shared.config.MongoConfig;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.config.StorageUsageControl;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.AppsCategoriesRepository;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.NodeRepository;
import wasdi.shared.data.OrganizationRepository;
import wasdi.shared.data.ParametersRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorLogRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.data.ProcessorUIRepository;
import wasdi.shared.data.ProductWorkspaceRepository;
import wasdi.shared.data.ProjectRepository;
import wasdi.shared.data.PublishedBandsRepository;
import wasdi.shared.data.SessionRepository;
import wasdi.shared.data.SnapWorkflowRepository;
import wasdi.shared.data.SubscriptionRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.data.UserResourcePermissionRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.data.statistics.JobsRepository;
import wasdi.shared.geoserver.GeoServerManager;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.rabbit.Send;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.MailUtils;
import wasdi.shared.utils.OgcProcessesClient;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.S3BucketUtils;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.StringUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.modis.MODISUtils;
import wasdi.shared.utils.wasdiAPI.WorkspaceAPIClient;
import wasdi.shared.viewmodels.HttpCallResponse;
import wasdi.shared.viewmodels.ogcprocesses.ProcessList;
import wasdi.shared.viewmodels.organizations.SubscriptionType;
import wasdi.shared.viewmodels.products.BandViewModel;
import wasdi.shared.viewmodels.products.ProductViewModel;

public class dbUtils {

    public static boolean isProductOnThisNode(DownloadedFile oDownloadedFile, WorkspaceRepository oWorkspaceRepository) {
        try {
            String sPath = oDownloadedFile.getFilePath();

            String[] asSplittedPath = sPath.split("/");

            if (asSplittedPath == null) {
                System.out.println(oDownloadedFile.getFileName() + " - CANNOT SPLIT PATH");
                return false;
            }

            if (asSplittedPath.length < 2) {
                System.out.println(oDownloadedFile.getFileName() + " - SPLITTED PATH HAS ONLY ONE ELEMENT");
                return false;
            }

            String sWorkspaceId = asSplittedPath[asSplittedPath.length - 2];

            Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sWorkspaceId);

            if (oWorkspace == null) {
                System.out.println(oDownloadedFile.getFileName() + " - IMPOSSIBILE TO FIND WORKSPACE " + sWorkspaceId);
                return false;

            }

            String sNode = oWorkspace.getNodeCode();

            if (Utils.isNullOrEmpty(sNode)) {
                sNode = "wasdi";
            }

            if (sNode.equals(s_sMyNodeCode) == false) {
                System.out.println(oDownloadedFile.getFileName() + " - IS ON ANOTHER NODE  [" + sNode + "]");
                return false;
            }

        } catch (Exception oEx) {
            System.out.println(" DOWNLOADED FILE EX " + oEx.toString());
            return false;
        }

        return true;
    }

    /**
     * Tools to fix the downloaded products table
     */
    public static void downloadedProducts() {
        try {

            System.out.println("Ok, what we do with downloaded products?");

            System.out.println("\t1 - List products with broken files");
            System.out.println("\t2 - Delete products with broken files");
            System.out.println("\t3 - Clear S1 S2 published bands");
            System.out.println("\t4 - Clean by not existing Product Workspace");
            System.out.println("\tx - back");
            System.out.println("");

            String sInputString = s_oScanner.nextLine();

            boolean bDelete = false;

            if (sInputString.equals("1") || sInputString.equals("2")) {

                if (sInputString.equals("1")) {
                    bDelete = false;
                } else if (sInputString.equals("2")) {
                    bDelete = true;
                }

                WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();

                DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();

                List<DownloadedFile> aoDownloadedFiles = oDownloadedFilesRepository.getList();

                System.out.println("Found " + aoDownloadedFiles.size() + " Downloaded Files");

                int iDeleted = 0;

                for (DownloadedFile oDownloadedFile : aoDownloadedFiles) {

                    String sPath = oDownloadedFile.getFilePath();
                    File oFile = new File(sPath);

                    if (oFile.exists() == false) {

                        if (!isProductOnThisNode(oDownloadedFile, oWorkspaceRepository)) continue;

                        iDeleted++;

                        if (bDelete == false) {
                            System.out.println(oDownloadedFile.getFileName() + " - FILE DOES NOT EXISTS " + oDownloadedFile.getFilePath());
                        } else {

                            System.out.println("DELETING " + oDownloadedFile.getFileName() + " - FILE DOES NOT EXISTS " + oDownloadedFile.getFilePath());
                            oDownloadedFilesRepository.deleteByFilePath(oDownloadedFile.getFilePath());

                            // Delete Product Workspace
                            ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();
                            oProductWorkspaceRepository.deleteByProductName(oDownloadedFile.getFilePath());

                            System.out.println("DELETED " + oDownloadedFile.getFileName());
                        }
                    }
                }

                System.out.println("");
                System.out.println("");
                System.out.println("---------------------------------------------");
                String sSummary = "";
                if (bDelete) {
                    sSummary = "DELETED " + iDeleted + " db Entry";
                } else {
                    sSummary = "Found " + iDeleted + " db Entry to delete";
                }

                System.out.println(sSummary);
            } else if (sInputString.equals("3")) {
                System.out.println("Clean S1 and S2 published bands");
                cleanPublishedBands();
            } else if (sInputString.equals("4")) {

                ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();
                DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
                List<DownloadedFile> aoDownloadedFiles = oDownloadedFilesRepository.getList();

                System.out.println("Found " + aoDownloadedFiles.size() + " Downloaded Files");

                for (DownloadedFile oDownloadedFile : aoDownloadedFiles) {
                    String sPath = oDownloadedFile.getFilePath();

                    List<ProductWorkspace> aoAreThere = oProductWorkspaceRepository.getProductWorkspaceListByPath(sPath);

                    if (aoAreThere.size() == 0) {
                        System.out.println("File " + sPath + " not in ProductWorkspace: delete");
                        oDownloadedFilesRepository.deleteByFilePath(sPath);
                    }
                }

                System.out.println("All downloaded files cleaned");

            } else if (sInputString.equals("x")) {
                return;
            }
        } catch (Exception oEx) {
            System.out.println("downloadedProducts: exception " + oEx.toString());
        }
    }

    /**
     * Utils to fix product workspace table
     */
    public static void productWorkspace() {
        try {

            System.out.println("Ok, what we do with product Workspaces?");

            System.out.println("\t1 - Clean by not existing Workspace");
            System.out.println("\t2 - Clean by not existing Product Name");
            System.out.println("\t3 - Change Workspace Owner");
            System.out.println("\tx - Back");
            System.out.println("");

            String sInputString = s_oScanner.nextLine();

            boolean bWorkspace = false;

            if (sInputString.equals("x")) {
                return;
            }

            if (sInputString.equals("1")) {
                bWorkspace = true;
            } else if (sInputString.equals("2")) {
                bWorkspace = false;
            }

            if (sInputString.equals("1") || sInputString.equals("2")) {
                if (bWorkspace) {
                    System.out.println("Deleting all product workspace with not existing workspace");

                    WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
                    ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();

                    List<ProductWorkspace> aoAllProductWorkspace = oProductWorkspaceRepository.getList();

                    int iDeleted = 0;

                    System.out.println("productWorkspace: found " + aoAllProductWorkspace.size() + " Product Workspace");

                    for (ProductWorkspace oProductWorkspace : aoAllProductWorkspace) {

                        Workspace oWorkspace = oWorkspaceRepository.getWorkspace(oProductWorkspace.getWorkspaceId());

                        if (oWorkspace == null) {
                            System.out.println("productWorkspace: workspace " + oProductWorkspace.getWorkspaceId() + " does not exist, delete entry");
                            oProductWorkspaceRepository.deleteByProductName(oProductWorkspace.getProductName());
                            iDeleted++;
                        }
                    }

                    System.out.println("");
                    System.out.println("---------------------------------------------------");
                    System.out.println("productWorkspace: Deleted " + iDeleted + " Product Workspace");
                } else {
                    System.out.println("Deleting all product workspace with not existing product Name");

                    DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
                    ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();

                    List<ProductWorkspace> aoAllProductWorkspace = oProductWorkspaceRepository.getList();

                    int iDeleted = 0;

                    System.out.println("productWorkspace: found " + aoAllProductWorkspace.size() + " Product Workspace");

                    for (ProductWorkspace oProductWorkspace : aoAllProductWorkspace) {

                        DownloadedFile oDownloadedFile = oDownloadedFilesRepository.getDownloadedFileByPath(oProductWorkspace.getProductName());

                        if (oDownloadedFile == null) {
                            System.out.println("productWorkspace: Downloaded File " + oProductWorkspace.getProductName() + " does not exist, delete entry");
                            oProductWorkspaceRepository.deleteByProductName(oProductWorkspace.getProductName());
                            iDeleted++;
                        }
                    }

                    System.out.println("");
                    System.out.println("---------------------------------------------------");
                    System.out.println("productWorkspace: Deleted " + iDeleted + " Product Workspace");
                }
            }
            else if (sInputString.equals("3")) {
                System.out.println("Workspace Id?");
                String sWorkspaceId = s_oScanner.nextLine();

                System.out.println("Old User Id?");
                String sOldUser = s_oScanner.nextLine();

                System.out.println("New User Id?");
                String sNewUser = s_oScanner.nextLine();
                
                ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();
                
                System.out.println("Reading Product Workspaces");

                List<ProductWorkspace> aoAllProductWorkspace = oProductWorkspaceRepository.getProductsByWorkspace(sWorkspaceId);
                
                System.out.println("Update Product Workspaces");
                
                for (ProductWorkspace oProductWorkspace : aoAllProductWorkspace) {
                	
                	if (oProductWorkspace.getProductName().contains(sOldUser)) {
                		String sOldProductName = oProductWorkspace.getProductName();
                		oProductWorkspace.setProductName(oProductWorkspace.getProductName().replace(sOldUser, sNewUser));
                		oProductWorkspaceRepository.updateProductWorkspace(oProductWorkspace, sOldProductName);
                	}
                	else {
                		System.out.println("WARNING Product Workspace " + oProductWorkspace.getProductName() + " DOES NOT includes the old user!!");
                	}
				}
                
                System.out.println("Reading Downloaded Files");
                
                DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
                
                List<DownloadedFile> aoFiles = oDownloadedFilesRepository.getByWorkspace(sWorkspaceId);
                
                System.out.println("Updating Downloaded Files");
                
                for (DownloadedFile oFile : aoFiles) {
                	if (oFile.getFilePath().contains(sOldUser)) {
                		String sOldPath = oFile.getFilePath();
                		oFile.setFilePath(oFile.getFilePath().replace(sOldUser, sNewUser));
                		oDownloadedFilesRepository.updateDownloadedFile(oFile, sOldPath);
                	}
                	else {
                		System.out.println("WARNING Downloaded File " + oFile.getFilePath() + " DOES NOT includes the old user!!");
                	}
				}                            	
            }

        } catch (Exception oEx) {
            System.out.println("productWorkspace: exception " + oEx);
        }
    }
    
    public static void redeployProcessor(Processor oProcessor) {
        
        String sProcessorFolder = PathsConfig.getProcessorFolder(oProcessor);
        
        if (new File(sProcessorFolder).exists() == false) {
        	System.out.println("Processor " + oProcessor.getName() + " Does not exists in path " + sProcessorFolder);
        	return;
        }

        WasdiProcessorEngine oEngine = WasdiProcessorEngine.getProcessorEngine(oProcessor.getType());

        ProcessorParameter oParameter = new ProcessorParameter();

        oParameter.setName(oProcessor.getName());
        oParameter.setProcessorID(oProcessor.getProcessorId());

        System.out.println("Created Parameter with Name: " + oProcessor.getName() + " ProcessorId: " + oProcessor.getProcessorId());

        oEngine.setParameter(oParameter);

        oEngine.redeploy(oParameter);    	
    }


    public static void processors() {

        try {

            System.out.println("Ok, what we do with processors?");

            System.out.println("\t1 - Extract Log");
            System.out.println("\t2 - Clear Log");
            System.out.println("\t3 - Redeploy");
            System.out.println("\t4 - Fix Processor Creation/Update date");
            System.out.println("\t5 - Update db UI from local ui.json files");
            System.out.println("\t6 - Force Lib Update");
            System.out.println("\t7 - Redeploy all processors");
            System.out.println("\t8 - Force Delete");
            System.out.println("\tx - Back");
            System.out.println("");

            String sInputString = s_oScanner.nextLine();

            if (sInputString.equals("x")) {
                return;
            }

            ProcessorLogRepository oProcessorLogRepository = new ProcessorLogRepository();

            if (sInputString.equals("1")) {

                System.out.println("Please input ProcessWorkspaceId");
                String sProcessWorkspaceId = s_oScanner.nextLine();


                String sOuptutFile = "./" + sProcessWorkspaceId + ".txt";

                System.out.println("Extracting Log of Processor " + sProcessWorkspaceId + " in " + sOuptutFile);

                List<ProcessorLog> aoLogs = oProcessorLogRepository.getLogsByProcessWorkspaceId(sProcessWorkspaceId);

                if (aoLogs == null) {
                    System.out.println("Log row list is null, exit");
                    return;
                }

                System.out.println("Log Rows " + aoLogs.size());

                try (FileWriter oWriter = new FileWriter(sOuptutFile)) {

                    try (BufferedWriter oBufferedWriter = new BufferedWriter(oWriter)) {
                        for (ProcessorLog oLogRow : aoLogs) {
                            oBufferedWriter.write(oLogRow.getLogDate());
                            oBufferedWriter.write(" - ");
                            oBufferedWriter.write(oLogRow.getLogRow());
                            oBufferedWriter.write("\n");
                        }

                        oBufferedWriter.flush();
                        oBufferedWriter.close();
                    }
                } catch (IOException e) {
                    System.err.format("IOException: %s%n", e);
                }

                System.out.println("Log Extraction done");
            } else if (sInputString.equals("2")) {

                System.out.println("Please input ProcessWorkspaceId");
                String sProcessWorkspaceId = s_oScanner.nextLine();

                System.out.println("Deleting logs of " + sProcessWorkspaceId);
                oProcessorLogRepository.deleteLogsByProcessWorkspaceId(sProcessWorkspaceId);
                System.out.println(sProcessWorkspaceId + " logs DELETED");
            } else if (sInputString.equals("3")) {
                System.out.println("Please input Processor Name");
                String sProcessorName = s_oScanner.nextLine();

                ProcessorRepository oProcessorRepository = new ProcessorRepository();
                Processor oProcessor = oProcessorRepository.getProcessorByName(sProcessorName);

                if (oProcessor == null) {
                    System.out.println(sProcessorName + " does NOT exists");
                    return;
                }
                
                redeployProcessor(oProcessor);

            } else if (sInputString.equals("4")) {
                ProcessorRepository oProcessorRepository = new ProcessorRepository();
                List<Processor> aoProcessors = oProcessorRepository.getDeployedProcessors();

                System.out.println("Found " + aoProcessors.size() + " Processors");

                Date oNow = new Date();

                for (Processor oProcessor : aoProcessors) {

                    boolean bUpdate = false;

                    if (oProcessor.getUploadDate() == null) {
                        oProcessor.setUploadDate((double) oNow.getTime());
                        bUpdate = true;
                    } else if (oProcessor.getUploadDate() <= 0) {
                        oProcessor.setUploadDate((double) oNow.getTime());
                        bUpdate = true;
                    }


                    if (oProcessor.getUpdateDate() == null) {
                        oProcessor.setUpdateDate((double) oNow.getTime());
                        bUpdate = true;
                    } else if (oProcessor.getUpdateDate() <= 0) {
                        oProcessor.setUpdateDate((double) oNow.getTime());
                        bUpdate = true;
                    }

                    if (bUpdate) {
                        System.out.println("Updating " + oProcessor.getName());
                        oProcessorRepository.updateProcessor(oProcessor);
                    }
                }
                
                System.out.println("Processors Update Done");
            }
            else if (sInputString.equals("5")) {
                ProcessorUIRepository oProcessorUIRepository = new ProcessorUIRepository();
                ProcessorRepository oProcessorRepository = new ProcessorRepository();
                List<Processor> aoProcessors = oProcessorRepository.getDeployedProcessors();

                System.out.println("Found " + aoProcessors.size() + " Processors");

                for (Processor oProcessor : aoProcessors) {

                    String sProcessorName = oProcessor.getName();
                    String sProcessorPath = PathsConfig.getProcessorFolder(sProcessorName);
                    File oUiFile = new File(sProcessorPath + "/ui.json");

                    if (oUiFile.exists()) {
                        String sJsonUI = new String(Files.readAllBytes(Paths.get(oUiFile.getAbsolutePath())), StandardCharsets.UTF_8);
                        String sEncodedJSON = StringUtils.encodeUrl(sJsonUI);
                        ProcessorUI oProcessorUI = new ProcessorUI();
                        oProcessorUI.setProcessorId(oProcessor.getProcessorId());
                        oProcessorUI.setUi(sEncodedJSON);
                        ProcessorUI oOldOne = oProcessorUIRepository.getProcessorUI(oProcessorUI.getProcessorId());
                        if (oOldOne != null) {
                            System.out.println("Updating " + oProcessor.getName());
                            oProcessorUIRepository.updateProcessorUI(oProcessorUI);
                        } else {
                            System.out.println("Inserting " + oProcessor.getName());
                            oProcessorUIRepository.insertProcessorUI(oProcessorUI);
                        }
                    }
                }
            }            
            else if (sInputString.equals("6")) {
            	
                System.out.println("Please input Processors Type:");
                String sProcessorType = s_oScanner.nextLine();
                
                ProcessorRepository oProcessorRepository = new ProcessorRepository();
                List<Processor> aoProcessors = oProcessorRepository.getDeployedProcessors();

                System.out.println("Updating libs of processors of type " + sProcessorType);

                for (Processor oProcessor : aoProcessors) {
                	
                	if (oProcessor.getType().equals(sProcessorType)) {
                        String sProcessorName = oProcessor.getName();
                        String sProcessorPath = PathsConfig.getProcessorFolder(sProcessorName);
                        File oProcessorFolder = new File(sProcessorPath);

                        if (oProcessorFolder.exists()) {

                            System.out.println("Processor " + sProcessorName + " present in the node");

                            WasdiProcessorEngine oEngine = WasdiProcessorEngine.getProcessorEngine(oProcessor.getType());

                            ProcessorParameter oParameter = new ProcessorParameter();

                            oParameter.setName(oProcessor.getName());
                            oParameter.setProcessorID(oProcessor.getProcessorId());

                            System.out.println("Created Parameter with Name: " + oProcessor.getName() + " ProcessorId: " + oProcessor.getProcessorId());

                            oEngine.setParameter(oParameter);

                            oEngine.libraryUpdate(oParameter);    	


                        } else {
                            System.out.println("Processor " + sProcessorName + " NOT present in the node, JUMP");
                        }                		
                	}
                }

            } 
            else if (sInputString.equals("7")) {
            	
                System.out.println("Please input Processors Type:");
                String sProcessorType = s_oScanner.nextLine();
            	
                System.out.println("Redeploy Processor of selected type start");
                

                ProcessorRepository oProcessorRepository = new ProcessorRepository();
                
                List<Processor> aoProcessors = oProcessorRepository.getDeployedProcessors();
                
                for (Processor oProcessor : aoProcessors) {
                	
                	try {
                		if (oProcessor.getType().equals(sProcessorType)) {
                    		System.out.println("Redeploy " + oProcessor.getName());
                    		redeployProcessor(oProcessor);                			
                		}
                	}
                	catch (Exception oEx) {
                		System.out.println("Exception redeploying " + oProcessor.getName() + " " + oEx.toString());
					}
                }                    
            } 
            else if (sInputString.equals("8")) {
                System.out.println("Please input Processor Name to Delete:");
                String sProcessorName = s_oScanner.nextLine();

                ProcessorRepository oProcessorRepository = new ProcessorRepository();
                Processor oProcessor = oProcessorRepository.getProcessorByName(sProcessorName);

                if (oProcessor == null) {
                    System.out.println(sProcessorName + " does NOT exists");
                    return;
                }
                
                ProcessorParameter oParameter = new ProcessorParameter();
                oParameter.setName(oProcessor.getName());
                oParameter.setUserId(oProcessor.getUserId());
                oParameter.setProcessorID(oProcessor.getProcessorId());
                oParameter.setProcessorType(oProcessor.getType());
                
                ProcessWorkspace oProcessWorkspace = new ProcessWorkspace();
                
    	        WasdiProcessorEngine oEngine = WasdiProcessorEngine.getProcessorEngine(oProcessor.getType());
    	        
    	        if (oEngine instanceof DockerProcessorEngine) {
    	        	((DockerProcessorEngine)oEngine).setDockerRegistry(getDockerRegisterAddress());
    	        }
    	        
    	        ProcessWorkspaceLogger oPsLogger = new ProcessWorkspaceLogger(null);
    	        Send oSendToRabbit = new Send(null);
    	        
    	        oEngine.setSendToRabbit(oSendToRabbit);
    	        oEngine.setParameter(oParameter);
    	        oEngine.setProcessWorkspaceLogger(oPsLogger);
    	        oEngine.setProcessWorkspace(oProcessWorkspace);
    	        boolean bRet = oEngine.delete(oParameter);
    	        System.out.println("Engine.delete return " + bRet);
            }
      
        } catch (Exception oEx) {
            System.out.println("processors redeploying Exception: " + oEx);
        }
    }


    public static String getDockerRegisterAddress() {
    	try {
			// We read  the registers from the config
			List<DockerRegistryConfig> aoRegisters = WasdiConfig.Current.dockers.getRegisters();
			
			if (aoRegisters == null) {
				System.out.println("DockerProcessorEngine.getDockerRegisterAddress: registers list is null, return empty string.");
				return "";
			}
			
			if (aoRegisters.size() == 0) {
				System.out.println("DockerProcessorEngine.getDockerRegisterAddress: registers list is empty, return empty string.");
				return "";			
			}
			
			// And we work with our main register
			return aoRegisters.get(0).address;
    	}
    	catch (Exception oEx) {
    		System.out.println("DockerProcessorEngine.getDockerRegisterAddress: exception creating ws folder: " + oEx);
        }
    	
    	return "";
    }
    public static void metadata() {

        try {

            System.out.println("Ok, what we do with metadata?");

            System.out.println("\t1 - Clear Unlinked metadata");
            System.out.println("\t2 - Clear metadata from DB if file does not exist");
            System.out.println("\t3 - Clear metadata from this node (both from disk and DB)");
            System.out.println("\tx - back");
            System.out.println("");

            String sInputString = s_oScanner.nextLine();

            switch (sInputString) {
                case "1":
                    metadataDeleteOrphanedFiles();
                    break;
                case "2":
                    metadataCleanDB();
                    break;
                case "3":
                    metadataCleanAll();
                    break;
                case "x":
                default:
                    return;
            }

        } catch (Exception oEx) {

            System.out.println("metadata Exception: " + oEx);
        }
    }

    /**
     * Method to delete all metadata file and restore
     * all the db entries to default values on the current node
     */
    private static void metadataCleanAll() {
        System.out.println("Retrieving Metadata entry from DB to delete");


        DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
        List<DownloadedFile> aoDownloadedFileList = oDownloadedFilesRepository.getList();
        WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();

        for (DownloadedFile oDownloaded : aoDownloadedFileList) {

            if (!isProductOnThisNode(oDownloaded, oWorkspaceRepository)) {
                System.out.println("Product " + oDownloaded.getFileName() + " NOT IN THIS NODE SKIP");
                continue;
            }
            // then set to null and false the view model fields
            oDownloaded.getProductViewModel().setMetadataFileReference(null);
            oDownloaded.getProductViewModel().setMetadataFileCreated(false);
            oDownloadedFilesRepository.updateDownloadedFile(oDownloaded);


        }
        // Metadata folder by convention
        File oMetadataPath = new File("/data/wasdi/metadata");
        // For all the files in metadata folder !
        for (File oFile : oMetadataPath.listFiles()) {
            if (oFile.delete() == false) {
                System.out.println("Error Deleting metadata File: " + oFile.getPath());
            }
        }


    }

    private static void metadataCleanDB() {
        // TODO Auto-generated method stub

    }

    private static void metadataDeleteOrphanedFiles() {
        System.out.println("Searching Metadata files to delete");

        File oMetadataPath = new File("/data/wasdi/metadata");

        WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
        // Get all the downloaded files
        DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
        List<DownloadedFile> aoDownloadedFileList = oDownloadedFilesRepository.getList();

        ArrayList<String> asMetadataFileReference = new ArrayList<String>();

        // Generate the list of valid metadata file reference
        for (DownloadedFile oDownloadedFile : aoDownloadedFileList) {

            if (!isProductOnThisNode(oDownloadedFile, oWorkspaceRepository)) {
                System.out.println("Product " + oDownloadedFile.getFileName() + " NOT IN THIS NODE SKIP");
                continue;
            }

            // Get the view model
            ProductViewModel oVM = oDownloadedFile.getProductViewModel();

            if (oVM != null) {
                if (!Utils.isNullOrEmpty(oVM.getMetadataFileReference())) {
                    // Check metadata file refernece
                    if (!asMetadataFileReference.contains(oVM.getMetadataFileReference())) {
                        // Add to the list
                        asMetadataFileReference.add(oVM.getMetadataFileReference());
                    }
                }
            }
        }

        // Files to delete
        ArrayList<File> aoFilesToDelete = new ArrayList<File>();

        // For all the files in metadata
        for (File oFile : oMetadataPath.listFiles()) {

            // Get the name:
            String sName = oFile.getName();

            // Is linked?
            if (!asMetadataFileReference.contains(sName)) {
                // No!!
                aoFilesToDelete.add(oFile);
            }
        }


        for (File oFile : aoFilesToDelete) {
            System.out.println("Deleting metadata File: " + oFile.getPath());
            if (oFile.delete() == false) {
                System.out.println("Error Deleting metadata File: " + oFile.getPath());
            }
        }
    }


    private static void password() {
        try {

            System.out.println("Ok, what we do with Password?");

            System.out.println("\t1 - Encrypt Password");
            System.out.println("\t2 - Force Update User Password");
            System.out.println("\tx - back");
            System.out.println("");

            String sInputString = s_oScanner.nextLine();

            if (sInputString.equals("x")) {
                return;
            }

            PasswordAuthentication oAuth = new PasswordAuthentication();

            if (sInputString.equals("1")) {

                System.out.println("Insert the password to Encrypt:");
                String sInputPw = s_oScanner.nextLine();

                String sToChanget = oAuth.hash(sInputPw.toCharArray());
                System.out.println("Encrypted Password:");
                System.out.println(sToChanget);

            } else if (sInputString.equals("2")) {

                System.out.println("Insert the user Id:");
                String sUserId = s_oScanner.nextLine();

                System.out.println("Insert the password to Encrypt:");
                String sInputPw = s_oScanner.nextLine();

                UserRepository oUserRepo = new UserRepository();
                User oUser = oUserRepo.getUser(sUserId);

                if (oUser == null) {
                    System.out.println("User [" + sUserId + "] not found");
                    return;
                }

                oUser.setPassword(oAuth.hash(sInputPw.toCharArray()));

                oUserRepo.updateUser(oUser);

                System.out.println("Update password for user [" + sUserId + "]");

            }
        } catch (Exception oEx) {
            System.out.println("password Exception: " + oEx);
        }
    }

    private static void workflows() {
        try {

            System.out.println("Ok, what we do with workflows?");

            System.out.println("\t1 - Copy workflows from user folder to generic folder");
            System.out.println("\tx - back");
            System.out.println("");

            String sInputString = s_oScanner.nextLine();

            if (sInputString.equals("x")) {
                return;
            }

            if (sInputString.equals("1")) {

                System.out.println("Getting workflows");

                SnapWorkflowRepository oSnapWorkflowRepository = new SnapWorkflowRepository();
                List<SnapWorkflow> aoWorkflows = oSnapWorkflowRepository.getList();

                String sBasePath = PathsConfig.getWorkflowsPath();

                File oDestinationPath = new File(sBasePath);

                if (!oDestinationPath.exists()) {
                    oDestinationPath.mkdirs();
                }

                // Search one by one
                for (SnapWorkflow oWorkflow : aoWorkflows) {

                    String sWorkflowPath = sBasePath + oWorkflow.getWorkflowId() + ".xml";

                    File oOriginalFile = new File(sWorkflowPath);
                    File oDestinationFile = new File(oDestinationPath, oOriginalFile.getName());

                    if (!oDestinationFile.exists()) {
                        System.out.println("File does not exists, make a copy [" + oDestinationFile.getPath() + "]");

                        try {
                            FileUtils.copyFileToDirectory(oOriginalFile, oDestinationPath);

                            oWorkflow.setFilePath(oDestinationFile.getPath());
                            oSnapWorkflowRepository.updateSnapWorkflow(oWorkflow);
                        } catch (Exception oEx) {
                            System.out.println("File Copy Exception: " + oEx);
                        }
                    } else {
                        System.out.println("File already exists, jump");
                    }
                }

                System.out.println("All workflows copied");
            }

        } catch (Exception oEx) {
            System.out.println("Workflows Exception: " + oEx);
        }
    }

    private static void users() {
        try {

            System.out.println("Ok, what we do with Users?");

            System.out.println("\t1 - Delete User");
            System.out.println("\t2 - Print User Mails");
            System.out.println("\tx - back");
            System.out.println("");

            String sInputString = s_oScanner.nextLine();

            if (sInputString.equals("x")) {
                return;
            }
            if (sInputString.equals("1")) {

                System.out.println("Insert the userId to Delete:");
                String sUserId = s_oScanner.nextLine();

                if (Utils.isNullOrEmpty(sUserId)) {
                    System.out.println("User Id is null or empty");
                    return;
                }

                UserRepository oUserRepo = new UserRepository();
                User oTestUser = oUserRepo.getUser(sUserId);

                if (oTestUser == null) {
                    System.out.println("User Id not valid");
                    return;
                }

                // Get all the workspaces
                WorkspaceRepository oWorkspaceRepo = new WorkspaceRepository();
                List<Workspace> aoWorkspaces = oWorkspaceRepo.getWorkspaceByUser(sUserId);

                // Delete one by one
                for (Workspace oWorkspace : aoWorkspaces) {
                    deleteWorkspace(oWorkspace.getWorkspaceId(), oWorkspace.getUserId());
                }

                // Clean the log/processing history
                ProcessWorkspaceRepository oProcWsRepo = new ProcessWorkspaceRepository();
                ProcessorLogRepository oProcessorLogRepository = new ProcessorLogRepository();

                List<ProcessWorkspace> aoProcWs = oProcWsRepo.getProcessByUser(sUserId);

                System.out.println("Deleting Process Workpsaces and Logs : " + aoProcWs.size());

                for (ProcessWorkspace oProcWorkspace : aoProcWs) {
                    String sProcId = oProcWorkspace.getProcessObjId();

                    oProcessorLogRepository.deleteLogsByProcessWorkspaceId(sProcId);
                    oProcWsRepo.deleteProcessWorkspaceByProcessObjId(sProcId);
                }

                // Clean the user table
                System.out.println("Deleting User Db Entry ");

                UserRepository oUserRepository = new UserRepository();
                oUserRepository.deleteUser(sUserId);

                // Clean the user folder
                System.out.println("Deleting User Folder ");

                String sBasePath = PathsConfig.getWasdiBasePath();
                sBasePath += sUserId;
                sBasePath += "/";

                FileUtils.deleteDirectory(new File(sBasePath));

            } else if (sInputString.equals("2")) {
                UserRepository oUserRepo = new UserRepository();
                ArrayList<User> aoUsers = oUserRepo.getAllUsers();

                for (User oUser : aoUsers) {
                    System.out.println(oUser.getUserId());
                }
            }
        } catch (Exception oEx) {
            System.out.println("USERS Exception: " + oEx);
        }
    }

    private static void deleteWorkspace(String sWorkspaceId, String sWorkspaceOwner) {

        try {
            // repositories
            ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();
            PublishedBandsRepository oPublishRepository = new PublishedBandsRepository();
            WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
            DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();

            Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sWorkspaceId);

            if (oWorkspace.getNodeCode().equals(s_sMyNodeCode) == false) {
                System.out.println("Workspace " + sWorkspaceId + " of user " + sWorkspaceOwner + " IS IN NODE " + oWorkspace.getNodeCode());
                return;
            }

            // get workspace path
            String sWorkspacePath = PathsConfig.getWorkspacePath(sWorkspaceOwner, sWorkspaceId);

            System.out.println("deleting Workspace " + sWorkspaceId + " of user " + sWorkspaceOwner);

            // Delete Workspace Db Entry
            if (oWorkspaceRepository.deleteWorkspace(sWorkspaceId)) {

                // Get all Products in workspace
                List<ProductWorkspace> aoProductsWorkspaces = oProductWorkspaceRepository.getProductsByWorkspace(sWorkspaceId);

                WasdiLog.debugLog("Deleting workspace layers");

                // GeoServer Manager Object
                GeoServerManager oGeoServerManager = new GeoServerManager();

                // For each product in the workspace
                for (ProductWorkspace oProductWorkspace : aoProductsWorkspaces) {

                    // Get the downloaded file
                    DownloadedFile oDownloadedFile = oDownloadedFilesRepository.getDownloadedFileByPath(oProductWorkspace.getProductName());

                    // Is the product used also in other workspaces?
                    List<DownloadedFile> aoDownloadedFileList = oDownloadedFilesRepository.getDownloadedFileListByName(oDownloadedFile.getFileName());

                    if (aoDownloadedFileList.size() > 1) {
                        // Yes, it is in other Ws, jump
                        WasdiLog.debugLog("The file is also in other workspaces, leave the bands as they are");
                        continue;
                    }

                    // We need the View Model product name: start from file name
                    String sProductName = oDownloadedFile.getFileName();

                    // If view model is available (should be), get the name from the view model
                    if (oDownloadedFile.getProductViewModel() != null) {
                        sProductName = oDownloadedFile.getProductViewModel().getName();
                    }

                    // Get the list of published bands by product name
                    List<PublishedBand> aoPublishedBands = oPublishRepository.getPublishedBandsByProductName(sProductName);

                    // For each published band
                    for (PublishedBand oPublishedBand : aoPublishedBands) {

                        try {
                            // Remove Geoserver layer (and file)
                            if (!oGeoServerManager.removeLayer(oPublishedBand.getLayerId())) {
                                WasdiLog.debugLog("error deleting layer " + oPublishedBand.getLayerId() + " from geoserver");
                            }

                            try {
                                // delete published band on database
                                oPublishRepository.deleteByProductNameLayerId(oDownloadedFile.getProductViewModel().getName(), oPublishedBand.getLayerId());
                            } catch (Exception oEx) {
                                WasdiLog.debugLog("error deleting published band on data base " + oEx.toString());
                            }

                        } catch (Exception oEx) {
                            WasdiLog.debugLog("error deleting layer id " + oEx.toString());
                        }

                    }
                }

                try {

                    WasdiLog.debugLog("Delete workspace folder " + sWorkspacePath);

                    // delete directory
                    FileUtils.deleteDirectory(new File(sWorkspacePath));

                    // delete download file on database
                    for (ProductWorkspace oProductWorkspace : aoProductsWorkspaces) {

                        try {

                            WasdiLog.debugLog("Deleting file " + oProductWorkspace.getProductName());
                            oDownloadedFilesRepository.deleteByFilePath(oProductWorkspace.getProductName());

                        } catch (Exception oEx) {
                            WasdiLog.debugLog("Error deleting download on data base: " + oEx);
                        }
                    }

                } catch (Exception oEx) {
                    WasdiLog.debugLog("Error deleting workspace directory: " + oEx);
                }

                // Delete Product Workspace entry
                oProductWorkspaceRepository.deleteByWorkspaceId(sWorkspaceId);

                // Delete also the sharings, it is deleted by the owner..
                UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
                oUserResourcePermissionRepository.deletePermissionsByWorkspaceId(sWorkspaceId);

            } else {
                WasdiLog.debugLog("Error deleting workspace on data base");
            }

        } catch (Exception oEx) {
            WasdiLog.debugLog("WorkspaceResource.DeleteWorkspace: " + oEx);
        }

    }

    public static void sample() {
        System.out.println("sample method running");
    }

    private static void refreshProductsTable() {
        DownloadedFilesRepository oDownloadedFileRepo = new DownloadedFilesRepository();

        List<DownloadedFile> aoProducts = oDownloadedFileRepo.getList();

        for (DownloadedFile oProduct : aoProducts) {

            if (oProduct.getFileName().startsWith("S1") || oProduct.getFileName().startsWith("S2")) {
                ProductViewModel oVM = oProduct.getProductViewModel();

                List<BandViewModel> aoBands = oVM.getBandsGroups().getBands();

                if (aoBands == null) continue;

                boolean bChanged = false;
                for (BandViewModel oBand : aoBands) {
                    if (oBand.getPublished() == true) {
                        oBand.setPublished(false);
                        bChanged = true;
                    }
                }

                if (bChanged) {
                    oDownloadedFileRepo.updateDownloadedFile(oProduct);
                }
            }


        }
    }

    private static void cleanPublishedBands() throws MalformedURLException, IOException {

        try {
            GeoServerManager oGeoServerManager = new GeoServerManager();

            PublishedBandsRepository oPublishedBandRepo = new PublishedBandsRepository();

            List<PublishedBand> aoBands = oPublishedBandRepo.getList();

            for (PublishedBand oPublishedBand : aoBands) {

                if (oPublishedBand.getProductName().startsWith("S1") || oPublishedBand.getProductName().startsWith("S2")) {

                    System.out.println("DELETE " + oPublishedBand.getLayerId());

                    if (!oGeoServerManager.removeLayer(oPublishedBand.getLayerId())) {
                        System.out.println("ProductResource.DeleteProduct: error deleting layer " + oPublishedBand.getLayerId() + " from geoserver");
                    }

                    try {
                        // delete published band on data base
                        oPublishedBandRepo.deleteByProductNameLayerId(oPublishedBand.getProductName(), oPublishedBand.getLayerId());
                    } catch (Exception oEx) {
                        System.out.println("ProductResource.DeleteProduct: error deleting published band on data base " + oEx);
                    }
                } else {
                    System.out.println("KEEP " + oPublishedBand.getLayerId());
                }
            }
        } catch (Exception e) {
            System.out.println("ProductResource.DeleteProduct: error deleting published band on data base " + e);
        }


        refreshProductsTable();
    }

    private static void workspaces() {
        try {

            System.out.println("Ok, what we do with workspaces?");

            System.out.println("\t1 - Clean shared ws errors");
            System.out.println("\t2 - Move Workpsace to new node");
            System.out.println("\t3 - Delete Workspace");
            System.out.println("\t4 - Reconstruct Workspace");
            System.out.println("\t5 - Clean Workspaces not existing in node");
            System.out.println("\t6 - Add Workspace size");
            System.out.println("\tx - back");
            System.out.println("");

            String sInputString = s_oScanner.nextLine();

            if (sInputString.equals("x")) {
                return;
            }

            if (sInputString.equals("1")) {

                System.out.println("Getting workspace sharings");

                WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
                UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();

                List<UserResourcePermission> aoUserResourcePermissions = oUserResourcePermissionRepository.getWorkspaceSharings();

                for (UserResourcePermission oUserResourcePermission : aoUserResourcePermissions) {

                    Workspace oWorkspace = oWorkspaceRepository.getWorkspace(oUserResourcePermission.getResourceId());

                    if (oWorkspace == null) {
                        WasdiLog.debugLog("UserResourcePermissions: DELETE WS Shared not available " + oUserResourcePermission.getResourceId());

                        oUserResourcePermissionRepository.deletePermissionsByUserIdAndWorkspaceId(oUserResourcePermission.getUserId(), oUserResourcePermission.getResourceId());
                        continue;
                    }
                }

                System.out.println("All workspace sharings cleaned");
            } else if (sInputString.equals("2")) {
                // Not easy to use...
                System.out.println("NOTE: feature only updates the DB, you must before backup the tables from the old node and import in the new one");

                // Insert WS and destination node
                System.out.println("Please Insert the WS ID");
                String sWorkspaceId = s_oScanner.nextLine();
                System.out.println("Please Insert the new node code");
                String sNewNodeCode = s_oScanner.nextLine();

                // Find and open the workspace
                WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
                Workspace oWorkpsace = oWorkspaceRepository.getWorkspace(sWorkspaceId);

                if (oWorkpsace == null) {
                    System.out.println("Impossible to find the WS: " + sWorkspaceId);
                    return;
                }

                if (oWorkpsace.getNodeCode().equals(sNewNodeCode)) {
                    System.out.println("The WS: " + sWorkspaceId + " is already in the node " + sNewNodeCode);
                    return;
                }

                NodeRepository oNodeRepository = new NodeRepository();
                Node oNode = oNodeRepository.getNodeByCode(sNewNodeCode);

                if (oNode == null) {
                    System.out.println("Impossible to find the Node: " + sNewNodeCode);
                    return;
                }

                // Get all the process workspace to migrate
                ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
                List<ProcessWorkspace> aoProcessesInOldNode = oProcessWorkspaceRepository.getProcessByWorkspace(oWorkpsace.getWorkspaceId());

                System.out.println("Found " + aoProcessesInOldNode.size() + " process workspace.");

                for (ProcessWorkspace oProcessWorkpsace : aoProcessesInOldNode) {

                    if (oProcessWorkpsace.getNodeCode().equals(sNewNodeCode) == false) {
                        System.out.println("Updating Process Workspace " + oProcessWorkpsace.getProcessObjId());
                        oProcessWorkpsace.setNodeCode(sNewNodeCode);
                        oProcessWorkspaceRepository.updateProcess(oProcessWorkpsace);
                    }
                }

                System.out.println("Updating Workpsace.");
                oWorkpsace.setNodeCode(sNewNodeCode);
                oWorkspaceRepository.updateWorkspace(oWorkpsace);

                System.out.println("Update done");
            } else if (sInputString.equals("3")) {

                System.out.println("Please Insert workspaceId to delete:");
                String sWorkspaceId = s_oScanner.nextLine();

                // repositories
                ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();
                PublishedBandsRepository oPublishRepository = new PublishedBandsRepository();
                WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
                DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();

                Workspace oWS = oWorkspaceRepository.getWorkspace(sWorkspaceId);

                if (oWS == null) {
                    System.out.println("Workspace " + sWorkspaceId + " does not exists");
                    return;
                }

                String sWorkspaceOwner = oWS.getUserId();

                // get workspace path
                String sWorkspacePath = PathsConfig.getWorkspacePath(sWorkspaceOwner, sWorkspaceId);

                System.out.println("Deleting Workspace " + sWorkspaceId + " of user " + sWorkspaceOwner);

                // Delete Workspace Db Entry
                if (oWorkspaceRepository.deleteWorkspace(sWorkspaceId)) {

                    // Get all Products in workspace
                    List<ProductWorkspace> aoProductsWorkspaces = oProductWorkspaceRepository.getProductsByWorkspace(sWorkspaceId);

                    try {
                        System.out.println("Deleting workspace layers");

                        // GeoServer Manager Object
                        GeoServerManager oGeoServerManager = new GeoServerManager();

                        // For each product in the workspace, if is unique, delete published bands and metadata file ref
                        for (ProductWorkspace oProductWorkspace : aoProductsWorkspaces) {

                            // Get the downloaded file
                            DownloadedFile oDownloadedFile = oDownloadedFilesRepository.getDownloadedFileByPath(oProductWorkspace.getProductName());

                            // Is the product used also in other workspaces?
                            List<DownloadedFile> aoDownloadedFileList = oDownloadedFilesRepository.getDownloadedFileListByName(oDownloadedFile.getFileName());

                            if (aoDownloadedFileList.size() > 1) {
                                // Yes, it is in other Ws, jump
                                WasdiLog.debugLog("ProductResource.DeleteProduct: The file is also in other workspaces, leave the bands as they are");
                                continue;
                            }

                            // We need the View Model product name: start from file name
                            String sProductName = oDownloadedFile.getFileName();

                            // If view model is available (should be), get the name from the view model
                            if (oDownloadedFile.getProductViewModel() != null) {
                                sProductName = oDownloadedFile.getProductViewModel().getName();
                            }

                            // Get the list of published bands by product name
                            List<PublishedBand> aoPublishedBands = oPublishRepository.getPublishedBandsByProductName(sProductName);

                            // For each published band
                            for (PublishedBand oPublishedBand : aoPublishedBands) {

                                try {
                                    // Remove Geoserver layer (and file)
                                    if (!oGeoServerManager.removeLayer(oPublishedBand.getLayerId())) {
                                        System.out.println("error deleting layer " + oPublishedBand.getLayerId() + " from geoserver");
                                    }

                                    try {
                                        // delete published band on database
                                        oPublishRepository.deleteByProductNameLayerId(oDownloadedFile.getProductViewModel().getName(), oPublishedBand.getLayerId());
                                    } catch (Exception oEx) {
                                        System.out.println("error deleting published band on data base " + oEx.toString());
                                    }
                                } catch (Exception oEx) {
                                    System.out.println("error deleting layer id " + oEx.toString());
                                }
                            }

                            // If view model is available (should be), get the metadata file reference
                            if (oDownloadedFile.getProductViewModel() != null) {

                                String sMetadataFileRef = oDownloadedFile.getProductViewModel().getMetadataFileReference();

                                if (!Utils.isNullOrEmpty(sMetadataFileRef)) {
                                    System.out.println("deleting metadata file " + sMetadataFileRef);
                                    FileUtils.deleteQuietly(new File(sMetadataFileRef));
                                }
                            }

                        }
                    } catch (Exception oE) {
                        System.out.println("error while trying to delete layers: " + oE);
                    }

                    try {

                        System.out.println("Delete workspace folder " + sWorkspacePath);

                        // delete directory
                        try {
                            File oDir = new File(sWorkspacePath);
                            if (!oDir.exists()) {
                                System.out.println("trying to delete non existing directory " + sWorkspacePath);
                            }
                            // try anyway for two reasons:
                            // 1. non existing directories are handled anyway
                            // 2. try avoiding race conditions
                            FileUtils.deleteDirectory(oDir);
                        } catch (Exception oE) {
                            System.out.println("error while trying to delete directory " + sWorkspacePath + ": " + oE);
                        }

                        // delete download file on database
                        for (ProductWorkspace oProductWorkspace : aoProductsWorkspaces) {

                            try {

                                System.out.println("Deleting file " + oProductWorkspace.getProductName());
                                oDownloadedFilesRepository.deleteByFilePath(oProductWorkspace.getProductName());

                            } catch (Exception oEx) {
                                System.out.println("Error deleting download on data base: " + oEx);
                            }
                        }

                    } catch (Exception oEx) {
                        System.out.println("Error deleting workspace directory: " + oEx);
                    }

                    // Delete Product Workspace entry
                    oProductWorkspaceRepository.deleteByWorkspaceId(sWorkspaceId);

                    // Delete also the sharings, it is deleted by the owner..
                    System.out.println("Delete Workspace sharings");
                    UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
                    oUserResourcePermissionRepository.deletePermissionsByWorkspaceId(sWorkspaceId);

                    System.out.println("Workspace Deleted");
                } else {
                    System.out.println("Impossible to delete workspace from the db");
                }
            } else if (sInputString.equals("4")) {
                System.out.println("Please Insert workspaceId to reconstruct:");
                String sWorkspaceId = s_oScanner.nextLine();

                // repositories
                ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();
                ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
                WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
                SessionRepository oSessionRepository = new SessionRepository();
                DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();

                // Check if workspace exists
                Workspace oWS = oWorkspaceRepository.getWorkspace(sWorkspaceId);

                if (oWS == null) {
                    System.out.println("Workspace " + sWorkspaceId + " does not exists");
                    return;
                }

                String sWorkspaceOwner = oWS.getUserId();

                // get workspace path
                String sWorkspacePath = PathsConfig.getWorkspacePath(sWorkspaceOwner, sWorkspaceId);

                // Check and create the folder
                File oWorkspaceFolder = new File(sWorkspacePath);

                if (!oWorkspaceFolder.exists()) {
                    oWorkspaceFolder.mkdirs();
                }

                // List of process workspaces to re-run
                ArrayList<String> asProcessesToReRun = new ArrayList<String>();

                System.out.println("Getting Workspace Products");

                // Get the list of the files that were in the workspace
                List<ProductWorkspace> aoProductsInWorkspace = oProductWorkspaceRepository.getProductsByWorkspace(sWorkspaceId);

                System.out.println("Search root processors");

                // For each Product in the workspace
                for (ProductWorkspace oProductWorkspace : aoProductsInWorkspace) {

                    // Search the last ingestion of this product
                    String sProductName = oProductWorkspace.getProductName();
                    sProductName = new File(sProductName).getName();

                    List<ProcessWorkspace> aoProductProcesses = oProcessWorkspaceRepository.getProcessByProductName(sProductName);

                    for (ProcessWorkspace oProcess : aoProductProcesses) {

                        if (oProcess.getOperationType().equals(LauncherOperations.INGEST.name())) {

                            // Now search for the grandparent process that created the file
                            String sParentId = oProcess.getParentId();

                            if (asProcessesToReRun.contains(sParentId)) break;

                            String sRootOperation = "";

                            while (!Utils.isNullOrEmpty(sParentId)) {
                                ProcessWorkspace oParent = oProcessWorkspaceRepository.getProcessByProcessObjId(sParentId);
                                sRootOperation = oParent.getProcessObjId();
                                sParentId = oParent.getParentId();
                            }

                            if (Utils.isNullOrEmpty(sRootOperation) == false) {
                                if (!asProcessesToReRun.contains(sRootOperation)) {
                                    asProcessesToReRun.add(sRootOperation);
                                    System.out.println("Added " + sRootOperation + " to the re-run list");
                                }
                            }

                            // This must be done once, only for the last ingestion
                            break;
                        }
                    }
                }

                System.out.println("Delete not existing products from the workspace");

                // Delete Products, otherwise there is the risk that the processor will not re-create it
                for (ProductWorkspace oProductWorkspace : aoProductsInWorkspace) {

                    File oFile = new File(oProductWorkspace.getProductName());

                    if (!oFile.exists()) {
                        oDownloadedFilesRepository.deleteByFilePath(oProductWorkspace.getProductName());
                        oProductWorkspaceRepository.deleteByProductNameWorkspace(oProductWorkspace.getProductName(), oProductWorkspace.getWorkspaceId());
                    }
                }


                ArrayList<ProcessWorkspace> aoProcesses = new ArrayList<ProcessWorkspace>();

                for (String sProcId : asProcessesToReRun) {
                    ProcessWorkspace oProc = oProcessWorkspaceRepository.getProcessByProcessObjId(sProcId);
                    aoProcesses.add(oProc);
                }

                Collections.sort(aoProcesses, new ProcessWorkspaceStartDateComparator());
                ParametersRepository oParametersRepository = new ParametersRepository();

                for (ProcessWorkspace oProcToRun : aoProcesses) {

                    System.out.println("Starting again " + oProcToRun.getProcessObjId());

                    try {

                        // Create the base Parameter
                        BaseParameter oBaseParameter = oParametersRepository.getParameterByProcessObjId(oProcToRun.getProcessObjId());

                        String sSessionId = oBaseParameter.getSessionID();
                        String sUser = oBaseParameter.getUserId();

                        UserSession oSession = oSessionRepository.getSession(sSessionId);

                        if (oSession != null) {
                            oSessionRepository.touchSession(oSession);
                        } else {
                            UserSession oNewSession = new UserSession();
                            oNewSession.setSessionId(sSessionId);
                            oNewSession.setUserId(sUser);
                            oNewSession.setLoginDate(Utils.nowInMillis());
                            oNewSession.setLastTouch(Utils.nowInMillis());

                            oSessionRepository.insertSession(oNewSession);
                        }

                    } catch (Exception oEx) {

                        System.out.println("Exception searching parameter " + oEx.toString());
                    }

                    oProcToRun.setStatus(ProcessStatus.CREATED.name());

                    oProcessWorkspaceRepository.updateProcess(oProcToRun);


                    while (!(oProcToRun.getStatus().equals("DONE") || oProcToRun.getStatus().equals("ERROR") || oProcToRun.getStatus().equals("STOPPED"))) {
                        Thread.sleep(5000);
                        oProcToRun = oProcessWorkspaceRepository.getProcessByProcessObjId(oProcToRun.getProcessObjId());
                    }

                    System.out.println("Process Finished with status: " + oProcToRun.getStatus());

                }
            }
            
            else if (sInputString.equals("5")) {
            	
                System.out.println("Please write DELETE for a real delete");
                String sCommand = s_oScanner.nextLine();
                
                boolean bDelete = false;
                
                if (sCommand.equals("DELETE")) {
                	
                	System.out.println("This is a REAL Delete, are you sure [1/0]?");

                    String sConfirm = s_oScanner.nextLine();
                	
                    if (sConfirm.equals("1")) {
                    	bDelete = true;
                    }
                    else {
                    	System.out.println("Ok just a list");
                    }
                }
                else {
                	System.out.println("Ok just a list");
                }

                System.out.println("Searching Workspace no longer existing on node");

                // repositories
                ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();
                PublishedBandsRepository oPublishRepository = new PublishedBandsRepository();
                WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
                DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
                
                
                List<Workspace> aoWorkspaces = oWorkspaceRepository.getWorkspacesList();
                
                for (Workspace oWorkspace : aoWorkspaces) {
                	
                	String sWorkspaceNode="wasdi";
                	
                	if (!Utils.isNullOrEmpty(oWorkspace.getNodeCode())) {
                		sWorkspaceNode= oWorkspace.getNodeCode();
                	}
                	
                	if (!sWorkspaceNode.equals(WasdiConfig.Current.nodeCode)) continue;
                	
					String sWorkspaceId = oWorkspace.getWorkspaceId();
					
	                Workspace oWS = oWorkspaceRepository.getWorkspace(sWorkspaceId);

	                String sWorkspaceOwner = oWS.getUserId();

	                // get workspace path
	                String sWorkspacePath = PathsConfig.getWorkspacePath(sWorkspaceOwner, sWorkspaceId);
	                
	                File oWorkspacePath = new File(sWorkspacePath);
	                
	                if (oWorkspacePath.exists() == false) {
	                	System.out.println("Deleting Workspace " + sWorkspaceId + " of user " + sWorkspaceOwner);
	                	
	                	if (bDelete) {
			                // Delete Workspace Db Entry
			                if (oWorkspaceRepository.deleteWorkspace(sWorkspaceId)) {

			                    // Get all Products in workspace
			                    List<ProductWorkspace> aoProductsWorkspaces = oProductWorkspaceRepository.getProductsByWorkspace(sWorkspaceId);

			                    try {
			                        System.out.println("Deleting workspace layers");

			                        // GeoServer Manager Object
			                        GeoServerManager oGeoServerManager = new GeoServerManager();

			                        // For each product in the workspace, if is unique, delete published bands and metadata file ref
			                        for (ProductWorkspace oProductWorkspace : aoProductsWorkspaces) {

			                            // Get the downloaded file
			                            DownloadedFile oDownloadedFile = oDownloadedFilesRepository.getDownloadedFileByPath(oProductWorkspace.getProductName());

			                            // Is the product used also in other workspaces?
			                            List<DownloadedFile> aoDownloadedFileList = oDownloadedFilesRepository.getDownloadedFileListByName(oDownloadedFile.getFileName());

			                            if (aoDownloadedFileList.size() > 1) {
			                                // Yes, it is in other Ws, jump
			                                WasdiLog.debugLog("ProductResource.DeleteProduct: The file is also in other workspaces, leave the bands as they are");
			                                continue;
			                            }

			                            // We need the View Model product name: start from file name
			                            String sProductName = oDownloadedFile.getFileName();

			                            // If view model is available (should be), get the name from the view model
			                            if (oDownloadedFile.getProductViewModel() != null) {
			                                sProductName = oDownloadedFile.getProductViewModel().getName();
			                            }

			                            // Get the list of published bands by product name
			                            List<PublishedBand> aoPublishedBands = oPublishRepository.getPublishedBandsByProductName(sProductName);

			                            // For each published band
			                            for (PublishedBand oPublishedBand : aoPublishedBands) {

			                                try {
			                                    // Remove Geoserver layer (and file)
			                                    if (!oGeoServerManager.removeLayer(oPublishedBand.getLayerId())) {
			                                        System.out.println("error deleting layer " + oPublishedBand.getLayerId() + " from geoserver");
			                                    }

			                                    try {
			                                        // delete published band on database
			                                        oPublishRepository.deleteByProductNameLayerId(oDownloadedFile.getProductViewModel().getName(), oPublishedBand.getLayerId());
			                                    } catch (Exception oEx) {
			                                        System.out.println("error deleting published band on data base " + oEx.toString());
			                                    }
			                                } catch (Exception oEx) {
			                                    System.out.println("error deleting layer id " + oEx.toString());
			                                }
			                            }

			                            // If view model is available (should be), get the metadata file reference
			                            if (oDownloadedFile.getProductViewModel() != null) {

			                                String sMetadataFileRef = oDownloadedFile.getProductViewModel().getMetadataFileReference();

			                                if (!Utils.isNullOrEmpty(sMetadataFileRef)) {
			                                    System.out.println("deleting metadata file " + sMetadataFileRef);
			                                    FileUtils.deleteQuietly(new File(sMetadataFileRef));
			                                }
			                            }

			                        }
			                    } catch (Exception oE) {
			                        System.out.println("error while trying to delete layers: " + oE);
			                    }

			                    try {

			                        System.out.println("Delete workspace folder " + sWorkspacePath);

			                        // delete directory
			                        try {
			                            File oDir = new File(sWorkspacePath);
			                            if (!oDir.exists()) {
			                                System.out.println("trying to delete non existing directory " + sWorkspacePath);
			                            }
			                            // try anyway for two reasons:
			                            // 1. non existing directories are handled anyway
			                            // 2. try avoiding race conditions
			                            FileUtils.deleteDirectory(oDir);
			                        } catch (Exception oE) {
			                            System.out.println("error while trying to delete directory " + sWorkspacePath + ": " + oE);
			                        }

			                        // delete download file on database
			                        for (ProductWorkspace oProductWorkspace : aoProductsWorkspaces) {

			                            try {

			                                System.out.println("Deleting file " + oProductWorkspace.getProductName());
			                                oDownloadedFilesRepository.deleteByFilePath(oProductWorkspace.getProductName());

			                            } catch (Exception oEx) {
			                                System.out.println("Error deleting download on data base: " + oEx);
			                            }
			                        }

			                    } catch (Exception oEx) {
			                        System.out.println("Error deleting workspace directory: " + oEx);
			                    }

			                    // Delete Product Workspace entry
			                    oProductWorkspaceRepository.deleteByWorkspaceId(sWorkspaceId);

			                    // Delete also the sharings, it is deleted by the owner..
			                    System.out.println("Delete Workspace sharings");
			                    UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			                    oUserResourcePermissionRepository.deletePermissionsByWorkspaceId(sWorkspaceId);

			                    System.out.println("Workspace Deleted");
			                } else {
			                    System.out.println("Impossible to delete workspace from the db");
			                }	
	                	}
	                	
	                }
				}

            }  
            else if (sInputString.equals("6")) {
            	addStorageToWorkspace();
            }
        } 
        catch (InterruptedException oEx) {
        	Thread.currentThread().interrupt();
        	System.out.println("Current thread was interrupted: " + oEx);
        }
        catch (Exception oEx) {
            System.out.println("Workspace Sharing Exception: " + oEx);
        }
    }
    
    
    private static void addStorageToWorkspace() {
    	
    	System.out.println("Adding storage size to workspace in node");

    	String sCurrentNode = WasdiConfig.Current.nodeCode;
    	
    	WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
    	
    	List<Workspace> aoWorkspacedOnNode = oWorkspaceRepository.getWorkspaceByNode(sCurrentNode);
    	
    	System.out.println("Found " + aoWorkspacedOnNode.size() + " on the node");
    
    	for (Workspace oWorkspace : aoWorkspacedOnNode) {
    		String sUserId = oWorkspace.getUserId();
    		String sWorkspaceId = oWorkspace.getWorkspaceId();
    		String sWorkspacePath = PathsConfig.getWorkspacePath(sUserId, sWorkspaceId);
    		
    		File oWorkspaceDir = new File(sWorkspacePath);
    		long lWorkspaceDirSize = 0;
    		
    		if (oWorkspaceDir.exists()) {
        		lWorkspaceDirSize = FileUtils.sizeOfDirectory(oWorkspaceDir);
    		}
    		
    		oWorkspace.setStorageSize(lWorkspaceDirSize);
    		
    		if (!oWorkspaceRepository.updateWorkspace(oWorkspace)) {
    			System.out.println("Workspace " + sWorkspaceId + " has not been updated with its storage size");
    		}
    	}
    	
    	System.out.println("Update of storage size in workspaces ended");
    }
    

    /*
     *
     */
    public static void migrateToLocal() {

        try {

            System.out.println("Ok, what do we migrate?");

            System.out.println("\t1 - Copy Process Workspace of this Node in the local Database");
            System.out.println("\tx - back");
            System.out.println("");

            String sInputString = s_oScanner.nextLine();

            if (sInputString.equals("x")) {
                return;
            }

            if (sInputString.equals("1")) {

                //connect to main DB
                ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
                oProcessWorkspaceRepository.setRepoDb("wasdi");

                //get processWorkspaces on local node from main DB
                String sNodeCode = s_sMyNodeCode;

                List<ProcessWorkspace> aoProcessesToBePorted = oProcessWorkspaceRepository.getByNode(sNodeCode);

                System.out.println("Got " + aoProcessesToBePorted.size() + " processes to migrate");

                ArrayList<String> asIds = new ArrayList<String>();

                for (ProcessWorkspace oProcessWS : aoProcessesToBePorted) {
                    asIds.add(oProcessWS.getProcessObjId());
                }

                System.out.println("Start logs search ");

                // Find and save corresponding logs
                ProcessorLogRepository oProcessorLogRepository = new ProcessorLogRepository();
                oProcessorLogRepository.setRepoDb("wasdi");

                List<ProcessorLog> aoLogsToBePorted = new ArrayList<>();

                aoLogsToBePorted = oProcessorLogRepository.getLogsByArrayProcessWorkspaceId(asIds);

                // Switch to local db
                oProcessWorkspaceRepository.setRepoDb("local");
                oProcessorLogRepository.setRepoDb("local");

                //insert processes
                oProcessWorkspaceRepository.insertProcessListWorkspace(aoProcessesToBePorted);

                //insert logs
                oProcessorLogRepository.insertProcessLogList(aoLogsToBePorted);

                System.out.println("Do you want to delete entries from the central database? (1=YES, 2=NO)");

                String sDeleteEntries = s_oScanner.nextLine();

                if (sDeleteEntries.equals("1")) {
                    System.out.println("Setting Repositories to main db");
                    oProcessWorkspaceRepository.setRepoDb("wasdi");
                    oProcessorLogRepository.setRepoDb("wasdi");
                    System.out.println("Deleting logs");

                    for (ProcessWorkspace oProcessWorkspace : aoProcessesToBePorted) {

                        oProcessorLogRepository.deleteLogsByProcessWorkspaceId(oProcessWorkspace.getProcessObjId());
                        oProcessWorkspaceRepository.deleteProcessWorkspaceByProcessObjId(oProcessWorkspace.getProcessObjId());
                    }

                    System.out.println("Deleting process workspaces");

                }
            }

        } catch (Exception oEx) {
            System.out.println("Migrate Exception: " + oEx);
        }
    }

    /**
     * Works with process workspaces
     */
    public static void processWorkpsaces() {

        try {

            System.out.println("So you want to work with process workspaces?");

            System.out.println("\t1 - Delete ProcessWorkspace where Workspace does not exist");
            System.out.println("\t2 - Align Local Process Workspaces to Statistics Centralized Db");
            System.out.println("\t3 - Import Local Process Workspaces to mongo");
            System.out.println("\tx - back");
            System.out.println("");

            String sInputString = s_oScanner.nextLine();

            if (sInputString.equals("x")) {
                return;
            }

            if (sInputString.equals("1")) {

                System.out.println("Preparing Valid Workspace Id List");

                // Get the list of workspaces
                WorkspaceRepository oWorkspaceRepo = new WorkspaceRepository();

                List<Workspace> aoWorkspaces = oWorkspaceRepo.getWorkspacesList();
                ArrayList<String> asWorkspacesId = new ArrayList<String>();

                for (Workspace oWorkspace : aoWorkspaces) {
                    asWorkspacesId.add(oWorkspace.getWorkspaceId());
                }

                System.out.println("Query ProcessWorkspaces");

                // Get the total list of process workspaces
                ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
                List<ProcessWorkspace> aoProcessWorkspaces = oProcessWorkspaceRepository.getList();

                System.out.println("Got " + aoProcessWorkspaces.size() + " to analyze");

                // Will be used later to clean logs
                ProcessorLogRepository oProcessorLogRepository = new ProcessorLogRepository();

                // For each one
                for (ProcessWorkspace oProcessWS : aoProcessWorkspaces) {

                    // Is there the workspace?
                    if (!asWorkspacesId.contains(oProcessWS.getWorkspaceId())) {

                        // No, delete it
                        String sProcWSId = oProcessWS.getProcessObjId();
                        System.out.println("Deleting " + sProcWSId + " in not existing workspace " + oProcessWS.getWorkspaceId());

                        oProcessWorkspaceRepository.deleteProcessWorkspaceByProcessObjId(sProcWSId);

                        // It has logs?
                        if (oProcessWS.getOperationType().equals("RUNPROCESSOR") || oProcessWS.getOperationType().equals("RUNIDL") || oProcessWS.getOperationType().equals("RUNMATLAB")) {
                            // Maybe yes, delete logs
                            System.out.println("Deleting also logs of " + sProcWSId);
                            oProcessorLogRepository.deleteLogsByProcessWorkspaceId(sProcWSId);
                        }
                    }
                }

                System.out.println("Clean done!! Bye");
            }
            else if (sInputString.equals("2")) {
            	alignJobsToCentralDb();
            }
            else if (sInputString.equals("3")) {
            	importProcessWorkspacesInMongo();
            }

        } 
        catch (Exception oEx) {
            System.out.println("processWorkpsaces Exception: " + oEx);
        }
    }
    
    public static void importProcessWorkspacesInMongo() {
    	try {
    		System.out.println("Importing all the process workspaces in mongo");
    		
    		ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
    		List<ProcessWorkspace> aoProcessWorkspaces = oProcessWorkspaceRepository.getByNodeUnsorted(WasdiConfig.Current.nodeCode);
    		
    		if (aoProcessWorkspaces == null) {
    			System.out.println("Obtained null list, not good");
    			return;
    		}
    		
    		int iSize = aoProcessWorkspaces.size();
    		System.out.println("Analyzing " + iSize + " Process Workspaces");
    		int iTenPercent = (int) (iSize/10);
    		
    		ParametersRepository oParametersRepository = new ParametersRepository();
    		
    		for(int iProcWs = 0; iProcWs<aoProcessWorkspaces.size(); iProcWs ++) {
    			
    			try {
    				
        			if (iProcWs>0 && iProcWs%iTenPercent == 0) {
        				System.out.println("I'm not sleeping, just done another 10%");
        			}    	
        			
        			ProcessWorkspace oProcessWorkspace = aoProcessWorkspaces.get(iProcWs);
        			
        			if (oProcessWorkspace == null) continue;
        			
        			String sParameterId = oProcessWorkspace.getProcessObjId();
        			
        			if (Utils.isNullOrEmpty(sParameterId)) continue;
        			
        			String sParamPath = PathsConfig.getParameterPath(sParameterId);
        			
        			File oParamFile = new File(sParamPath);
        			
        			if (!oParamFile.exists()) {
        				System.out.println("Parameter file " + sParamPath + " does not exists!!");
        				continue;
        			}
        			
        			BaseParameter oParameter = (BaseParameter) SerializationUtils.deserializeXMLToObject(sParamPath);
        			oParametersRepository.insertParameter(oParameter);
    			}
    	        catch (Exception oEx) {
    	            System.out.println("processWorkpsaces.importProcessWorkspacesInMongo Exception in the import loop: " + oEx);
    	        }
    			
    		}
    		
    		System.out.println("Import done!!");
    		
    	}
        catch (Exception oEx) {
            System.out.println("processWorkpsaces.importProcessWorkspacesInMongo Exception: " + oEx);
        }
    }
    
    public static void alignJobsToCentralDb() {
    	try {
    		
    		System.out.println("alignJobsToCentralDb: search last insterted job for node " + WasdiConfig.Current.nodeCode);
    		JobsRepository oJobsRepository = new JobsRepository();
    		Job oLastJob = oJobsRepository.getLastJobFromNode(WasdiConfig.Current.nodeCode);
    		
    		Long lLastTimestamp = 0L;
    		
    		if (oLastJob != null) {
    			if (oLastJob.getCreatedTimestamp()!=null) {
    				lLastTimestamp = oLastJob.getCreatedTimestamp();	
    			}
    		}

    		if (lLastTimestamp<=0) {
    			System.out.println("alignJobsToCentralDb: no jobs found, import all the db");
    		}
    		else {
    			System.out.println("alignJobsToCentralDb: serach jobs after " + lLastTimestamp);
    		}
    		
    		ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
    		List<ProcessWorkspace> aoProcessWorkspacesToImport = oProcessWorkspaceRepository.getProcessOlderThan(lLastTimestamp);
    		
    		if (aoProcessWorkspacesToImport == null) {
    			System.out.println("alignJobsToCentralDb: error connecting db, the list is null.");
    			return;
    		}
    		else {
    			System.out.println("alignJobsToCentralDb: found "  + aoProcessWorkspacesToImport.size() + " Jobs to import");
    		}
    		
    		int iTenPercent = aoProcessWorkspacesToImport.size()/10;
    		
    		for (int iProcesses = 0; iProcesses < aoProcessWorkspacesToImport.size(); iProcesses++) {
    			ProcessWorkspace oProcessWorkspace = aoProcessWorkspacesToImport.get(iProcesses);
				Job oJob = new Job(oProcessWorkspace);
				oJobsRepository.insertJob(oJob);
				
				if ((iProcesses%iTenPercent)==0) {
					System.out.println("alignJobsToCentralDb: importing, done another 10%");
				}
    		}
    	}
    	catch (Exception oEx) {
            System.out.println("processWorkpsaces Exception: " + oEx);
        }
    }

    /**
     * Works with process workspaces
     */
    public static void logs() {

        try {

            System.out.println("What we do with our logs?");

            System.out.println("\t1 - Extract Logs");
            System.out.println("\t2 - Clean Logs with non existing Process Workspace");
            System.out.println("\t3 - Clean old logs");
            System.out.println("\tx - back");
            System.out.println("");

            String sInputString = s_oScanner.nextLine();

            if (sInputString.equals("x")) {
                return;
            }

            if (sInputString.equals("1")) {
                System.out.println("Well, ok, but this feature is Main Menu -> 3 Processors -> 1 - Extract Log. Go there!");
            } else if (sInputString.equals("2")) {

                System.out.println("This will take more than a while, let's start with a list of valid local process workspaces");

                // Get the total list of process workspaces
                ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
                List<ProcessWorkspace> aoProcessWorkspaces = oProcessWorkspaceRepository.getList();

                System.out.println("Got " + aoProcessWorkspaces.size() + " to analyze");

                ArrayList<String> asProcWorkspacesId = new ArrayList<String>();

                for (ProcessWorkspace oProcWorkspace : aoProcessWorkspaces) {
                    asProcWorkspacesId.add(oProcWorkspace.getProcessObjId());
                }

                ArrayList<String> asAlreadyCleanedProcessWorkspace = new ArrayList<String>();

                // Will be used later to clean logs
                ProcessorLogRepository oProcessorLogRepository = new ProcessorLogRepository();

                List<ProcessorLog> aoAllTheLogs = oProcessorLogRepository.getList();

                // For each one
                for (ProcessorLog oLog : aoAllTheLogs) {

                    // Is there the workspace?
                    if (!asProcWorkspacesId.contains(oLog.getProcessWorkspaceId())) {

                        if (!asAlreadyCleanedProcessWorkspace.contains(oLog.getProcessWorkspaceId())) {
                            // No, delete it
                            System.out.println("Deleting all log rows of ProcessWorkspace " + oLog.getProcessWorkspaceId());

                            oProcessorLogRepository.deleteLogsByProcessWorkspaceId(oLog.getProcessWorkspaceId());

                            asAlreadyCleanedProcessWorkspace.add(oLog.getProcessWorkspaceId());
                        }

                    }
                }

                System.out.println("Clean done!! Bye");
            } else if (sInputString.equals("3")) {
                System.out.println("Please insert upper bound date in format YYYY-MM-DD:");

                String sDate = s_oScanner.nextLine();

                if (Utils.isNullOrEmpty(sDate)) {
                    System.out.println("not valid date " + sDate);
                    return;
                }

                String[] asSplit = sDate.split("-");

                if (asSplit == null) {
                    System.out.println("not valid date " + sDate);
                    return;
                }

                if (asSplit.length != 3) {
                    System.out.println("not valid date " + sDate);
                    return;
                }

                //{"$lt":"2019-05-04 00:00:00"}})
                ProcessorLogRepository oProcessorLogRepository = new ProcessorLogRepository();
                oProcessorLogRepository.deleteLogsOlderThan(sDate);

                System.out.println("Logs cleaned!");
            }

        } catch (Exception oEx) {
            System.out.println("logs Exception: " + oEx);
        }
    }

    /*
     *
     */
    public static void categories() {

        try {

            System.out.println("So, you want to work with categories?");

            System.out.println("\t1 - List Categories");
            System.out.println("\t2 - Add a new Category");
            System.out.println("\t3 - Delete Categories");
            System.out.println("\tx - back");
            System.out.println("");

            String sInputString = s_oScanner.nextLine();

            if (sInputString.equals("x")) {
                return;
            }

            if (sInputString.equals("1")) {

                //connect to main DB
                AppsCategoriesRepository oAppsCategoriesRepository = new AppsCategoriesRepository();

                List<AppCategory> aoCategories = oAppsCategoriesRepository.getCategories();

                for (AppCategory oCategory : aoCategories) {
                    System.out.println("ID: [" + oCategory.getId() + "]: " + oCategory.getCategory());
                }

                System.out.println("Printed " + aoCategories.size() + " Categories");
            } else if (sInputString.equals("2")) {
                System.out.println("Insert Category Name:");
                String sCategory = s_oScanner.nextLine();
                AppCategory oAppCategory = new AppCategory();
                oAppCategory.setCategory(sCategory);
                oAppCategory.setId(Utils.getRandomName());

                AppsCategoriesRepository oAppsCategoriesRepository = new AppsCategoriesRepository();
                oAppsCategoriesRepository.insertCategory(oAppCategory);

                System.out.println("Category Created with ID: " + oAppCategory.getId());
            } else if (sInputString.equals("3")) {
                System.out.println("Insert Id Of the category to delete:");
                String sCategoryId = s_oScanner.nextLine();

                AppsCategoriesRepository oAppsCategoriesRepository = new AppsCategoriesRepository();
                AppCategory oCategory = oAppsCategoriesRepository.getCategoryById(sCategoryId);

                if (oCategory == null) {
                    System.out.println("Category NOT FOUND with ID: " + sCategoryId);
                } else {
                    System.out.println("Category FOUND: " + oCategory.getCategory());

                    oAppsCategoriesRepository.deleteCategory(sCategoryId);

                    ProcessorRepository oProcessorRepository = new ProcessorRepository();
                    List<Processor> aoProcessors = oProcessorRepository.getDeployedProcessors();

                    for (Processor oProcessor : aoProcessors) {

                        if (oProcessor.getCategories() != null) {
                            if (oProcessor.getCategories().size() > 0) {
                                if (oProcessor.getCategories().contains(sCategoryId)) {
                                    oProcessor.getCategories().remove(sCategoryId);
                                    oProcessorRepository.updateProcessor(oProcessor);
                                    System.out.println("Category removed from processor " + oProcessor.getName() + " ID: " + oProcessor.getProcessorId());
                                }
                            }
                        }
                    }

                    System.out.println("Category deleted: " + sCategoryId);
                }

            }

        } catch (Exception oEx) {
            System.out.println("Categories Exception: " + oEx);
        }
    }

    public static String s_sMyNodeCode = "wasdi";
    private static Scanner s_oScanner;
    
    public static void testEOEPCALogin() {
    	
    	String sDeployBody = "{ \"executionUnit\": { \"href\": \"https://test.wasdi.net/wasdiwebserver/rest/processors/getcwl?processorName=hello_eoepca2\", \"type\": \"application/cwl\" }}";
    	//String sDeployBody = "{ \"executionUnit\": { \"href\": \"https://raw.githubusercontent.com/EOEPCA/convert/main/convert-url-app.cwl\", \"type\": \"application/cwl\" }}";
    	
    	
		String sBaseAddress = WasdiConfig.Current.dockers.eoepca.adesServerAddress;
		
		if (!sBaseAddress.endsWith("/")) sBaseAddress += "/";
		
		if (!Utils.isNullOrEmpty(WasdiConfig.Current.dockers.eoepca.user)) {
			sBaseAddress += WasdiConfig.Current.dockers.eoepca.user + "/";
		}
		
		sBaseAddress += "wps3/";
    	
		OgcProcessesClient oOgcProcessesClient = new OgcProcessesClient(sBaseAddress);
		
		// Is this istance under authentication?		
		if (!Utils.isNullOrEmpty(WasdiConfig.Current.dockers.eoepca.user) && !Utils.isNullOrEmpty(WasdiConfig.Current.dockers.eoepca.password)) {
			// Authenticate to the eoepca installation
			String sScope = "openid user_name is_operator";
			
			Map<String, String> asNoCacheHeaders = new HashMap<>();
			asNoCacheHeaders.put("Cache-Control", "no-cache");
			asNoCacheHeaders.put("Accept", "application/json");
			
			// We need an openId Connection Token
			String sToken = HttpUtils.obtainOpenidConnectToken(WasdiConfig.Current.dockers.eoepca.authServerAddress, WasdiConfig.Current.dockers.eoepca.user, WasdiConfig.Current.dockers.eoepca.password
					, WasdiConfig.Current.dockers.eoepca.clientId, sScope, WasdiConfig.Current.dockers.eoepca.clientSecret, asNoCacheHeaders);
			
			// And the relative headers
			Map<String, String> asHeaders = HttpUtils.getOpenIdConnectHeaders(sToken);
			
			// That we inject in all the call to ADES/OGC Processes API
			oOgcProcessesClient.setHeaders(asHeaders);
		}
		
		ProcessList oProcList = oOgcProcessesClient.getProcesses();
		
		// Call the deploy function: is a post of the App Deploy Body
		boolean bApiAnswer = oOgcProcessesClient.deployProcess(sDeployBody);
		WasdiLog.infoLog("Deploy Process answer " + bApiAnswer);
    }
    

	private static void subscriptions() {
		try {
			
            System.out.println("This option allows you to manage subscriptions.");

            System.out.println("\t1 - Create New Subscription");
            System.out.println("\t2 - Generate Free Subscription for All the User");
            System.out.println("\tx - back");
            System.out.println("");

            String sInputString = s_oScanner.nextLine();

            if (sInputString.equals("x")) {
                return;
            }

            else if (sInputString.equals("1")) {
                System.out.println("Insert User Id:");
                String sUserId = s_oScanner.nextLine();
                
                System.out.println("Subscription Type:");
                System.out.println("1: Free");
                System.out.println("2: 1 Day Standard");
                System.out.println("3: 1 Week Standard");
                System.out.println("4: 1 Month Standard");
                System.out.println("5: 1 Year Standard");
                System.out.println("6: 1 Month Professional");
                System.out.println("7: 1 Year Professional");
                String sSubscriptionType = s_oScanner.nextLine();
                
                if (sSubscriptionType.equals("1")) {
                	sSubscriptionType = SubscriptionType.Free.toString();
                }
                else if (sSubscriptionType.equals("2")) {
                	sSubscriptionType = SubscriptionType.OneDayStandard.name();
                } 
                else if (sSubscriptionType.equals("3")) {
                	sSubscriptionType = SubscriptionType.OneWeekStandard.name();
                } 
                else if (sSubscriptionType.equals("4")) {
                	sSubscriptionType = SubscriptionType.OneMonthStandard.name();
                } 
                else if (sSubscriptionType.equals("5")) {
                	sSubscriptionType = SubscriptionType.OneYearStandard.name();
                } 
                else if (sSubscriptionType.equals("6")) {
                	sSubscriptionType = SubscriptionType.OneMonthProfessional.name();
                } 
                else if (sSubscriptionType.equals("7")) {
                	sSubscriptionType = SubscriptionType.OneYearProfessional.name();
                } 
                else {
                	System.out.println("Invalid Subscription type");
                	return;
                }
                
                System.out.println("Name - Description:");
                String sName = s_oScanner.nextLine();  
                
                System.out.println("Do you want to associate an organization with the subscription? (y/n)");
                String sCreateOrganization = s_oScanner.nextLine();  
                
                String sOrganizationId = null;
                if (sCreateOrganization.equals("y")) {
                	System.out.println("Insert the organization id");
                	sOrganizationId = s_oScanner.nextLine();
                	if (Utils.isNullOrEmpty(sOrganizationId)) {
                		System.out.println("The id you typed dosn't look like a valid id. Exiting");
                		return;
                	}
                } else if (sCreateOrganization.equals("n")) {
                	System.out.println("You chose not to associate an organization");
                } else {
                	System.out.println("Answer not valid. Exiting the process");
                	return;
                }
                
                createSubscription(sUserId, sSubscriptionType, sName, sOrganizationId);
            }
            else if (sInputString.equals("2")) {
            	
            	System.out.println("Are you really sure you want to give a FREE subscription to ALL the users? (y/n)");
            	
            	sInputString = s_oScanner.nextLine();

                if (sInputString.equals("y")) {
                	System.out.println("So, lets do it!");
                	
                    UserRepository oUserRepo = new UserRepository();
                    ArrayList<User> aoUsers = oUserRepo.getAllUsers();

                    for (User oUser : aoUsers) {
                    	createSubscription(oUser.getUserId(), "Free", "WASDI Trial", null);
                    }                	
                }
                else {
                	System.out.println("Ah, ok");
                	return;
                }
            }
            
		} catch (Exception e) {
			System.out.println("Exception: " + e.toString());
		}
		
	}
	
	private static void createSubscription(String sUserId, String sSubscriptionType, String sName, String sOrganizationId) {
        UserRepository oUserRepo = new UserRepository();
        User oUser = oUserRepo.getUser(sUserId);
        
        if (oUser==null) {
        	System.out.println("Impossible to find user " + sUserId);
        	return;
        }
        
        SubscriptionType oType = SubscriptionType.valueOf(sSubscriptionType);
        
        if (oType == null) {
        	System.out.println("Impossible to find Subscription type " + sSubscriptionType);
        	return;
        }
        
        if (sOrganizationId != null) {
        	OrganizationRepository oOrganizationRepository = new OrganizationRepository();
        	if (oOrganizationRepository.getById(sOrganizationId) == null) {
        		System.out.println("Impossible to find the organization id");
        		return;
        	}
        }
        
        int iDays = 1;
        
        if (oType.equals(SubscriptionType.Free)) iDays = 90;
        else if (oType.equals(SubscriptionType.OneDayStandard)) iDays = 1;
        else if (oType.equals(SubscriptionType.OneWeekStandard)) iDays = 7;
        else if (oType.equals(SubscriptionType.OneMonthProfessional)) iDays = 30;
        else if (oType.equals(SubscriptionType.OneMonthStandard)) iDays = 30;
        else if (oType.equals(SubscriptionType.OneYearProfessional)) iDays = 365;
        else if (oType.equals(SubscriptionType.OneYearStandard)) iDays = 365;
        
		Subscription oSubscription = new Subscription();
		
		oSubscription.setType(oType.getTypeId());
		oSubscription.setBuyDate(null);
		oSubscription.setUserId(sUserId);
		oSubscription.setSubscriptionId(Utils.getRandomName());
		oSubscription.setName(sName);
		oSubscription.setBuySuccess(true);
		oSubscription.setBuyDate(Utils.getDateAsDouble(new Date()));
		oSubscription.setDescription(sName);
		oSubscription.setDurationDays(iDays);
		double dStartDate = Utils.getDateAsDouble(new Date());
		oSubscription.setStartDate(dStartDate);
		double dEndDate = dStartDate + ((double)iDays)*24.0*60.0*60.0*1000.0;
		oSubscription.setEndDate(dEndDate);
		if (!Utils.isNullOrEmpty(sOrganizationId)) {
			oSubscription.setOrganizationId(sOrganizationId);
		}
		
		SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();
		oSubscriptionRepository.insertSubscription(oSubscription);
		
		Project oProject = new Project();
		oProject.setDescription(sName);
		oProject.setName(sName);
		oProject.setSubscriptionId(oSubscription.getSubscriptionId());
		oProject.setProjectId(Utils.getRandomName());
		
		ProjectRepository oProjectRepository = new  ProjectRepository();
		oProjectRepository.insertProject(oProject);
		
		UserRepository oUserRepository = new UserRepository();
		oUser.setActiveProjectId(oProject.getProjectId());
		oUser.setActiveSubscriptionId(oSubscription.getSubscriptionId());
		oUserRepository.updateUser(oUser);
		
		System.out.println("Subscription Added for user " + sUserId);		
	}

	private static void ecoStress() {

		try {
			
            System.out.println("This tool will parse the config file and ingest in WASDI the Ecostress data hosted on the creodias S3 Bucket. ");

            System.out.println("\t1 - Proceed with import");
            System.out.println("\tx - back");
            System.out.println("");

            String sInputString = s_oScanner.nextLine();

            if (sInputString.equals("x")) {
                return;
            }

            if (sInputString.equals("1")) {
            	S3BucketUtils.parseS3Bucket();
            }

		} catch (Exception e) {
			System.out.println("Exception: " + e.toString());
		}
	}    

	
	private static void modis() {
		try {
			
            System.out.println("This tool will parse the config file and ingest in WASDI the MOD11A2 data from the LP DAAC catalogue. ");

            System.out.println("\t1 - Proceed with the import of all documents from the data provider");
            System.out.println("\t2 - Specify the lines of the CSV file to read and import the documents FROM THE DATA PROVIDER");
            System.out.println("\t3 - Specify the lines of the CSV file to read and import the documents FROM THE CSV FILE");
            System.out.println("\t4 - Repair database reading metadata of missing documents from CSV file");
            System.out.println("\tx - back to main menu");
            System.out.println("");

            String sInputString = s_oScanner.nextLine();

            if (sInputString.equals("x")) {
                return;
            }

            if (sInputString.equals("1")) {
            	System.out.println("you chose to import all the documents");
            	 MODISUtils.insertProducts();
            	 return;
            }
            
            if (sInputString.equals("2") || sInputString.equals("3")) {
            	System.out.println("Specify the number of the starting line and the ending line in the CSV file, separated by a space");
            	System.out.println("");
            	
                String sLineIndeces = s_oScanner.nextLine();
                String[] asIndeces = sLineIndeces.split(" ");
                
                if (asIndeces.length < 2 || asIndeces.length > 2) {
                	System.out.println("Number of starting and ending line not specified in the correct format. Returning to the main menu.");
                	return;
                }
                
                int iStartLine = Integer.MIN_VALUE;
                int iEndLine = Integer.MIN_VALUE;
                try {
                	iStartLine = Integer.parseInt(asIndeces[0]);
                	iEndLine = Integer.parseInt(asIndeces[1]);
                } catch (NumberFormatException oE) {
                	System.out.println("One of the parameters is not a number. Returning to the main menu.");
                	return;
                }
                
                if (iStartLine > iEndLine) {
                	System.out.println("The number of the start line should be less or equal than the number of the ending line. Returning to the main menu.");
                	return;
                }
                
                System.out.println("You chose to import the documents in the lines included between " + iStartLine + " and " + iEndLine);
                
                if (sInputString.equals("2"))
                	MODISUtils.insertProducts(iStartLine, iEndLine);
                else
                	MODISUtils.insertProductsFromCsv(iStartLine, iEndLine);
                
                return;
            }
            
            if (sInputString.equals("4")) {
            	System.out.println("You chose to fill the db with the missing data");
            	MODISUtils.insertMissingProductsFromCsv();
            }
            

		} catch (Exception oEx) {
			System.out.println("Exception: " + oEx.toString());
		}
	}
	
    public static void main(String[] args) {

        try {
        	
        	System.out.println("Wasdi DB Utils");
        	        	
            // create the parser
            CommandLineParser oParser = new DefaultParser();

            // create Options object
            Options oOptions = new Options();

            oOptions.addOption("c", "config", true, "WASDI Configuration File Path");
            oOptions.addOption("ct", "cleantask", true, "Command to start the WASDI Clean Teask for free extra space");

            String sConfigFilePath = "/etc/wasdi/wasdiConfig.json";

            // parse the command line arguments
            CommandLine oLine = oParser.parse(oOptions, args);

            if (oLine.hasOption("config")) {
                // Get the Parameter File
            	sConfigFilePath = oLine.getOptionValue("config");
            }
        	
            if (!WasdiConfig.readConfig(sConfigFilePath)) {
                System.err.println("Db Utils - config file not available. Exit");
                System.exit(-1);            	
            }
        	
            //this is how you read parameters:
            MongoRepository.readConfig();

            String sNode = WasdiConfig.Current.nodeCode;
            
            if (!Utils.isNullOrEmpty(sNode)) {
                s_sMyNodeCode = sNode;
            }
            
            try {
                // get jar directory
                File oCurrentFile = new File(dbUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
                String sThisFilePath = oCurrentFile.getParentFile().getPath();
    			WasdiFileUtils.loadLogConfigFile(sThisFilePath);
    			
            } catch (Exception exp) {
                // no log4j configuration
                System.err.println("DbUtils - Error loading log configuration.  Reason: " + exp.toString());
            }

            // If this is not the main node
            if (!s_sMyNodeCode.equals("wasdi")) {
                System.out.println("Adding local mongo config");
                // Configure also the local connection
                MongoRepository.addMongoConnection("local", WasdiConfig.Current.mongoLocal.user, WasdiConfig.Current.mongoLocal.password, WasdiConfig.Current.mongoLocal.address, WasdiConfig.Current.mongoLocal.replicaName, WasdiConfig.Current.mongoLocal.dbName);
            }
            
            // add connection to ecostress db
            MongoRepository.addMongoConnection("ecostress", WasdiConfig.Current.mongoEcostress.user, WasdiConfig.Current.mongoEcostress.password, WasdiConfig.Current.mongoEcostress.address, WasdiConfig.Current.mongoEcostress.replicaName, WasdiConfig.Current.mongoEcostress.dbName);
            
            Stream<String> oModisConfigStream = null;
            try {
                // add connection to modis db
                String sModisDbConfigPath = WasdiConfig.Current.getDataProviderConfig("LPDAAC").parserConfig;
                if (!Utils.isNullOrEmpty(sModisDbConfigPath)) {
                	try {
                		oModisConfigStream = Files.lines(Paths.get(sModisDbConfigPath), StandardCharsets.UTF_8);
                		String sModisConfigJson = oModisConfigStream.collect(Collectors.joining(System.lineSeparator()));
                		ObjectMapper oMapper = new ObjectMapper(); 
    		            MongoConfig oModisConfig = oMapper.readValue(sModisConfigJson, MongoConfig.class);
    		            MongoRepository.addMongoConnection("modis", oModisConfig.user, oModisConfig.password, oModisConfig.address, oModisConfig.replicaName, oModisConfig.dbName);
    	            } catch (Exception oEx) {
    	            	System.err.println("Db Utils - Error while reading the MODIS db configuration. Exit. " + oEx.getMessage());
    	            }
                } else {
                    System.err.println("Db Utils - Data provider LPDAAC not found. Impossible to retrieve information for MODIS db. Exit");
                }            	
            }
            catch (Exception oEx1) {
            	System.err.println("Db Utils - Data provider LPDAAC not found. Impossible to retrieve information for MODIS db.");
			} finally {
				if (oModisConfigStream != null)
					oModisConfigStream.close();
			}
            
            // add connection to statistics db
            MongoRepository.addMongoConnection("wasdi-stats", WasdiConfig.Current.mongoStatistics.user, WasdiConfig.Current.mongoStatistics.password, WasdiConfig.Current.mongoStatistics.address, WasdiConfig.Current.mongoStatistics.replicaName, WasdiConfig.Current.mongoStatistics.dbName);
            
            
            if (oLine.hasOption("cleantask"))  {
            	runWasdiCleanTask();
            }
            else {
                boolean bExit = false;

                s_oScanner = new Scanner(System.in);

                while (!bExit) {
                    System.out.println("---- WASDI db Utils ----");
                    System.out.println("Welcome, how can I help you?");

                    System.out.println("\t1 - Downloaded Products");
                    System.out.println("\t2 - Product Workspace");
                    System.out.println("\t3 - Processors");
                    System.out.println("\t4 - Metadata");
                    System.out.println("\t5 - Password");
                    System.out.println("\t6 - Users");
                    System.out.println("\t7 - Workflows");
                    System.out.println("\t8 - Workspaces");
                    System.out.println("\t9 - Migrate DB to local");
                    System.out.println("\t10 - Categories");
                    System.out.println("\t11 - ProcessWorkspace");
                    System.out.println("\t12 - Logs");
                    System.out.println("\t13 - EcoStress");
                    System.out.println("\t14 - Subscriptions");
                    System.out.println("\t15 - MOD11A2 data import");
                    System.out.println("\tx - Exit");
                    System.out.println("");


                    String sInputString = s_oScanner.nextLine();

                    if (sInputString.equals("1")) {
                        downloadedProducts();
                    } else if (sInputString.equals("2")) {
                        productWorkspace();
                    } else if (sInputString.equals("3")) {
                        processors();
                    } else if (sInputString.equals("4")) {
                        metadata();
                    } else if (sInputString.equals("5")) {
                        password();
                    } else if (sInputString.equals("6")) {
                        users();
                    } else if (sInputString.equals("7")) {
                        workflows();
                    } else if (sInputString.equals("8")) {
                        workspaces();
                    } else if (sInputString.equals("9")) {
                        migrateToLocal();
                    } else if (sInputString.equals("10")) {
                        categories();
                    } else if (sInputString.equals("11")) {
                        processWorkpsaces();
                    } else if (sInputString.equals("12")) {
                        logs();
                    } else if (sInputString.equals("13")) {
                        ecoStress();
                    } else if (sInputString.equals("14")) {
                        subscriptions();
                    } else if (sInputString.equals("15")) {
                    	modis();
                    }
                    else if (sInputString.toLowerCase().equals("x")) {
                        bExit = true;
                    } else {
                        System.out.println("Please select a valid option or x to exit");
                        System.out.println("");
                        System.out.println("");
                    }
                }            	
            }

            System.out.println("bye bye");

            s_oScanner.close();

        } catch (Exception e) {
        	System.out.println("Exception: " + e.toString());
        }
    }

	private static void runWasdiCleanTask() {
		try {
			
			if (!WasdiConfig.Current.nodeCode.equals("wasdi")) {
				WasdiLog.errorLog("Clean Task must run only on the main node");
				return;
			}
			
			UserRepository oUserRepository = new UserRepository();
			ArrayList<User> aoUsers = oUserRepository.getAllUsers();
			ArrayList<User> aoToChekUsers = new ArrayList<>();
			
			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
			SessionRepository oSessionRepository = new SessionRepository();
			NodeRepository oNodeRepository = new NodeRepository();
			
			for (User oUser : aoUsers) {
				String sType = PermissionsUtils.getUserType(oUser.getUserId());
				if (sType.equals(UserType.NONE.name()) || sType.equals(UserType.FREE.name())) {
					aoToChekUsers.add(oUser);
				}
			}
			
			for (User oCandidate : aoToChekUsers) {
				
				String sCandidateUserId = oCandidate.getUserId();
				
				if (PermissionsUtils.userHasValidSubscription(oCandidate) == false) {
					
					StorageUsageControl oStorageUsageControl = WasdiConfig.Current.storageUsageControl;
					Double dTotalStorageUsage = oWorkspaceRepository.getStorageUsageForUser(sCandidateUserId);
					long lNow = new Date().getTime();
					long lStorageWarningDate = oCandidate.getStorageWarningSentDate().longValue();
					
					if (lStorageWarningDate == 0L) {
						
						String sEmailTitle = oStorageUsageControl.warningEmailConfig.title;
						String sEmailText = oStorageUsageControl.warningEmailConfig.message;
						
						MailUtils.sendEmail(sCandidateUserId, sEmailTitle, sEmailText); // TODO - DONE
						
						oCandidate.setStorageWarningSentDate((double)lNow);
						oUserRepository.updateUser(oCandidate);
						continue;
					}
					
					long lDaysFromWarning = (lNow - lStorageWarningDate) / (1000 * 60 * 60 * 24);
					
					if (lDaysFromWarning > WasdiConfig.Current.storageUsageControl.deletionDelayFromWarning) { // TODO - DONE
						
						UserSession oSession = oSessionRepository.insertUniqueSession(oCandidate.getUserId());
						if (oSession== null) {
							WasdiLog.errorLog("Invalid session. Impossible to proceed with the cleaning task");
							continue;
						}					
						
						List<Workspace> aoWorkspaces = oWorkspaceRepository.getWorkspacesSortedByOldestUpdate(oCandidate.getUserId()); // TODO - DONE				
						
						for (Workspace oWorkspace : aoWorkspaces) {
							String sWorkspaceID = oWorkspace.getWorkspaceId();
							Node oNode = oNodeRepository.getNodeByCode(oWorkspace.getNodeCode());
							HttpCallResponse oResponse = WorkspaceAPIClient.deleteWorkspace(oNode, oSession.getSessionId(), sWorkspaceID);
							int iResponseCode = oResponse.getResponseCode();
							if (iResponseCode < 200 || iResponseCode > 299) {
								WasdiLog.warnLog("Deletion of wokrspace " + sWorkspaceID + "returned error code " + iResponseCode);
							}
							
							dTotalStorageUsage = dTotalStorageUsage - oWorkspace.getStorageSize();
							if (dTotalStorageUsage < WasdiConfig.Current.storageUsageControl.storageSizeFreeSubscription) {
								WasdiLog.infoLog("Workspaces of user " + sCandidateUserId + " have been cleaned. Total usage storage: " + dTotalStorageUsage);
								oCandidate.setStorageWarningSentDate(0.0);
								oUserRepository.updateUser(oCandidate);								
								break;
							}
						}			
					}
				}
				else {
					// Clean the flag
					if (oCandidate.getStorageWarningSentDate() > 0.0 ) {
						oCandidate.setStorageWarningSentDate(0.0);
						oUserRepository.updateUser(oCandidate);
					}
				}
			}
			
		} catch (Exception oEx) {
        	WasdiLog.errorLog("Exception: ", oEx);
        }
		
	}
	
}
