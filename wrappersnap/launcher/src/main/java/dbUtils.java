import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;

import wasdi.ConfigReader;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.PasswordAuthentication;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.ProcessorLog;
import wasdi.shared.business.ProductWorkspace;
import wasdi.shared.business.PublishedBand;
import wasdi.shared.business.User;
import wasdi.shared.business.Workspace;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorLogRepository;
import wasdi.shared.data.ProductWorkspaceRepository;
import wasdi.shared.data.PublishedBandsRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.data.WorkspaceSharingRepository;
import wasdi.shared.geoserver.GeoServerManager;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.BandViewModel;
import wasdi.shared.viewmodels.ProductViewModel;

public class dbUtils {

	/**
	 * Tools to fix the downloaded products table
	 */
	public static void downloadedProducts() {
		try {
			
	        System.out.println("Ok, what we do with downloaded products?");
	        
	        System.out.println("\t1 - List products with broken files");
	        System.out.println("\t2 - Delete products with broken files");
	        System.out.println("\t3 - Clear S1 S2 published bands");
	        System.out.println("");
	        
	        Scanner oScanner = new Scanner( System.in);
	        String sInputString = oScanner.nextLine();
	        
	        boolean bDelete = false;
	        
	        if (sInputString.equals("1") || sInputString.equals("2")) {
	        	
		        if (sInputString.equals("1")) {
		        	bDelete = false;
		        }
		        else if (sInputString.equals("2")) {
		        	bDelete = true;
		        }		        
				
				
				DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
				
				List<DownloadedFile> aoDownloadedFiles = oDownloadedFilesRepository.getList();
				
				System.out.println("Found " + aoDownloadedFiles.size() + " Downloaded Files");
				
				int iDeleted = 0;
				
				for (DownloadedFile oDownloadedFile : aoDownloadedFiles) {
					
					String sPath = oDownloadedFile.getFilePath();
					File oFile = new File(sPath);
					
					if (oFile.exists() == false) {
						
						iDeleted ++;
						
						if (bDelete == false) {
							System.out.println(oDownloadedFile.getFileName() + " - FILE DOES NOT EXISTS " + oDownloadedFile.getFilePath());
						}
						else {
							
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
				}
				else {
					sSummary = "Found " + iDeleted + " db Entry to delete";
				}
				
				System.out.println(sSummary);
	        }
	        else if (sInputString.equals("3")) {
	        	System.out.println("Clean S1 and S2 published bands");
	        	cleanPublishedBands();
	        }
		}
		catch (Exception oEx) {
			System.out.println("downloadedProducts: exception " + oEx.toString());
			oEx.printStackTrace();
		}
	}
	
	/**
	 * Utils to fix product workspace table
	 */
	public static void productWorkspace() {
		try {
			
	        System.out.println("Ok, what we do with downloaded products?");
	        
	        System.out.println("\t1 - Clean by Workspace");
	        System.out.println("\t2 - Clean by Product Name");
	        System.out.println("");
	        
	        Scanner oScanner = new Scanner( System.in);
	        String sInputString = oScanner.nextLine();
	        
	        boolean bWorkspace = false;
	        
	        if (sInputString.equals("1")) {
	        	bWorkspace = true;
	        }
	        else if (sInputString.equals("2")) {
	        	bWorkspace = false;
	        }		        
	        
	        if (bWorkspace) {
	        	System.out.println("Deleting all product workspace with not existing workspace");
	        	
	        	WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
	        	ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();
	        	
	        	List<ProductWorkspace> aoAllProductWorkspace = oProductWorkspaceRepository.getList();
	        	
	        	int iDeleted=0;
	        	
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
	        }
	        else {
	        	System.out.println("Deleting all product workspace with not existing product Name");
	        	
	        	DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
	        	ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();
	        	
	        	List<ProductWorkspace> aoAllProductWorkspace = oProductWorkspaceRepository.getList();
	        	
	        	int iDeleted=0;
	        	
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
		catch (Exception oEx) {
			System.out.println("productWorkspace: exception " + oEx);
			oEx.printStackTrace();
		}
	}

	
	public static void logs() {
		
		try {
			
	        System.out.println("Ok, what we do with logs?");
	        
	        System.out.println("\t1 - Extract Log");
	        System.out.println("\t2 - Clear Log");
	        System.out.println("");
	        
	        Scanner oScanner = new Scanner( System.in);
	        String sInputString = oScanner.nextLine();

	        System.out.println("Please input ProcessWorkspaceId");
	        String sProcessWorkspaceId = oScanner.nextLine();
	        
	        ProcessorLogRepository oProcessorLogRepository = new ProcessorLogRepository();

	        if (sInputString.equals("1")) {
	        	
	        	String sOuptutFile = "./" + sProcessWorkspaceId + ".txt";
	        	
				System.out.println("Extracting Log of Processor " + sProcessWorkspaceId + " in " + sOuptutFile);
				
				List<ProcessorLog> aoLogs = oProcessorLogRepository.getLogsByProcessWorkspaceId(sProcessWorkspaceId);
				
				if (aoLogs == null) {
					System.out.println("Log row list is null, exit");
					return;			
				}
				
				System.out.println("Log Rows " + aoLogs.size());
				
				try {
					FileWriter oWriter = new FileWriter(sOuptutFile);
					BufferedWriter oBufferedWriter = new BufferedWriter(oWriter);
					
					for (ProcessorLog oLogRow : aoLogs) {
						oBufferedWriter.write(oLogRow.getLogDate());
						oBufferedWriter.write(" - " );
						oBufferedWriter.write(oLogRow.getLogRow());
						oBufferedWriter.write("\n");
					}
					
					oBufferedWriter.flush();
					oBufferedWriter.close();
				} 
				catch (IOException e) {
					System.err.format("IOException: %s%n", e);
				}
				
				System.out.println("Log Extraction done");	        	
	        }
	        else if (sInputString.equals("2")) {
	        	System.out.println("Deleting logs of " + sProcessWorkspaceId);
	        	oProcessorLogRepository.deleteLogsByProcessWorkspaceId(sProcessWorkspaceId);
	        	System.out.println(sProcessWorkspaceId + " logs DELETED");
	        }
		}
		catch (Exception oEx) {
			System.out.println("logs Exception: " + oEx);
			oEx.printStackTrace();
		}
	}
	
	
	public static void metadata() {
		
		try {
			
	        System.out.println("Ok, what we do with metadata?");
	        
	        System.out.println("\t1 - Clear Unlinked metadata");
	        System.out.println("");
	        
	        Scanner oScanner = new Scanner( System.in);
	        String sInputString = oScanner.nextLine();


	        if (sInputString.equals("1")) {
	        	
	        	System.out.println("Searching Metadata files to delete");
	        	
	        	File oMetadataPath = new File("/data/wasdi/metadata");
	        	
	        	// Get all the downloaded files
	        	DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
	        	List<DownloadedFile> aoDownloadedFileList = oDownloadedFilesRepository.getList();
	        	
	        	ArrayList<String> asMetadataFileReference = new ArrayList<String>();
	        	
	        	// Generate the list of valid metadata file reference
	        	for (DownloadedFile oDownloadedFile : aoDownloadedFileList) {
	        		
	        		// Get the view model
	        		ProductViewModel oVM = oDownloadedFile.getProductViewModel();
	        		
	        		if (oVM != null) {
	        			if (!Utils.isNullOrEmpty(oVM.getMetadataFileReference())){
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
	        		oFile.delete();
				}
      	
	        }
		}
		catch (Exception oEx) {
			System.out.println("metadata Exception: " + oEx);
			oEx.printStackTrace();
		}
	}

	
	private static void password() {
		try {
			
	        System.out.println("Ok, what we do with Password?");
	        
	        System.out.println("\t1 - Encrypt Password");
	        System.out.println("\t2 - Force Update User Password");
	        System.out.println("");
	        
	        Scanner oScanner = new Scanner( System.in);
	        String sInputString = oScanner.nextLine();

	        PasswordAuthentication oAuth = new PasswordAuthentication();

	        if (sInputString.equals("1")) {
	        	
	        	System.out.println("Insert the password to Encrypt:");
	        	String sInputPw = oScanner.nextLine();
	        	
	    		String sToChanget = oAuth.hash(sInputPw.toCharArray());
	    		System.out.println("Encrypted Password:");
	    		System.out.println(sToChanget);
      	
	        }
	        else if (sInputString.equals("2")) {

	        	System.out.println("Insert the user Id:");
	        	String sUserId = oScanner.nextLine();

	        	System.out.println("Insert the password to Encrypt:");
	        	String sInputPw = oScanner.nextLine();
	        	
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
		}
		catch (Exception oEx) {
			System.out.println("password Exception: " + oEx);
			oEx.printStackTrace();
		}
	}
	
	
	private static void users() {
		try {
			
	        System.out.println("Ok, what we do with Users?");
	        
	        System.out.println("\t1 - Delete User");
	        System.out.println("");
	        
	        Scanner oScanner = new Scanner( System.in);
	        String sInputString = oScanner.nextLine();

	        if (sInputString.equals("1")) {
	        	
	        	System.out.println("Insert the userId to Delete:");
	        	String sUserId = oScanner.nextLine();
	        	
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
	        	
	    		String sBasePath = ConfigReader.getPropValue("DOWNLOAD_ROOT_PATH");
	    		if (!sBasePath.endsWith("/")) {
	    			sBasePath += "/";
	    		}
	    		sBasePath +=sUserId;
	    		sBasePath += "/";
	    		
				FileUtils.deleteDirectory(new File(sBasePath));
	        	
	        }
		}
		catch (Exception oEx) {
			System.out.println("password Exception: " + oEx);
			oEx.printStackTrace();
		}
	}
	
	private static String getWorkspacePath(String sWorkspaceOwner,String  sWorkspaceId) throws IOException {
		String sBasePath = ConfigReader.getPropValue("DOWNLOAD_ROOT_PATH");
		if (!sBasePath.endsWith("/")) {
			sBasePath += "/";
		}
		sBasePath +=sWorkspaceOwner;
		sBasePath += "/";
		sBasePath += sWorkspaceId;
		sBasePath += "/";
		
		return sBasePath;
	}
	
	private static void deleteWorkspace(String sWorkspaceId, String sWorkspaceOwner) {
		
		try {
			// repositories
			ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();
			PublishedBandsRepository oPublishRepository = new PublishedBandsRepository();
			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
			DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
			
			// get workspace path
			String sWorkspacePath = getWorkspacePath(sWorkspaceOwner, sWorkspaceId);
			
			System.out.println("deleting Workspace " + sWorkspaceId + " of user " + sWorkspaceOwner);

			// Delete Workspace Db Entry
			if (oWorkspaceRepository.deleteWorkspace(sWorkspaceId)) {
				
				// Get all Products in workspace
				List<ProductWorkspace> aoProductsWorkspaces = oProductWorkspaceRepository.getProductsByWorkspace(sWorkspaceId);

				Utils.debugLog("Deleting workspace layers");

				// GeoServer Manager Object
				GeoServerManager oGeoServerManager = new GeoServerManager(ConfigReader.getPropValue("GS_URL"), ConfigReader.getPropValue("GS_USER"), ConfigReader.getPropValue("GS_PASSWORD"));
				
				// For each product in the workspace
				for (ProductWorkspace oProductWorkspace : aoProductsWorkspaces) {
					
					// Get the downloaded file
					DownloadedFile oDownloadedFile = oDownloadedFilesRepository.getDownloadedFileByPath(oProductWorkspace.getProductName());
					
					// Is the product used also in other workspaces?
					List<DownloadedFile> aoDownloadedFileList = oDownloadedFilesRepository.getDownloadedFileListByName(oDownloadedFile.getFileName());
					
					if (aoDownloadedFileList.size()>1) {
						// Yes, it is in other Ws, jump
						Utils.debugLog("The file is also in other workspaces, leave the bands as they are");
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
								Utils.debugLog("error deleting layer " + oPublishedBand.getLayerId() + " from geoserver");
							}

							try {									
								// delete published band on database
								oPublishRepository.deleteByProductNameLayerId(oDownloadedFile.getProductViewModel().getName(), oPublishedBand.getLayerId());
							} 
							catch (Exception oEx) {
								Utils.debugLog("error deleting published band on data base " + oEx.toString());}

						} catch (Exception oEx) {
							Utils.debugLog("error deleting layer id " + oEx.toString());
						}

					}
				}			
				
				try {

					Utils.debugLog("Delete workspace folder " + sWorkspacePath);
					
					// delete directory
					FileUtils.deleteDirectory(new File(sWorkspacePath));
					
					// delete download file on database
					for (ProductWorkspace oProductWorkspace : aoProductsWorkspaces) {
						
						try {
							
							Utils.debugLog("Deleting file " + oProductWorkspace.getProductName());
							oDownloadedFilesRepository.deleteByFilePath(oProductWorkspace.getProductName());
							
						} 
						catch (Exception oEx) {
							Utils.debugLog( "Error deleting download on data base: " + oEx);
						}
					}

				} catch (Exception oEx) {
					Utils.debugLog("Error deleting workspace directory: " + oEx);
				}
				
				// Delete Product Workspace entry 
				oProductWorkspaceRepository.deleteByWorkspaceId(sWorkspaceId);
				
				// Delete also the sharings, it is deleted by the owner..
				WorkspaceSharingRepository oWorkspaceSharingRepository = new WorkspaceSharingRepository();
				oWorkspaceSharingRepository.deleteByWorkspaceId(sWorkspaceId);

			} 
			else {
				Utils.debugLog("Error deleting workspace on data base");
			}

		} catch (Exception oEx) {
			Utils.debugLog("WorkspaceResource.DeleteWorkspace: " + oEx);
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
				
				if (aoBands== null) continue;
				
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
			GeoServerManager oGeoServerManager = new GeoServerManager(ConfigReader.getPropValue("GEOSERVER_ADDRESS"), ConfigReader.getPropValue("GEOSERVER_USER"), ConfigReader.getPropValue("GEOSERVER_PASSWORD"));

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
						System.out.println( "ProductResource.DeleteProduct: error deleting published band on data base " + oEx);
					}				
				}
				else {
					System.out.println("KEEP " + oPublishedBand.getLayerId());
				}
			}		
		}
		catch (Exception e) {
			System.out.println( "ProductResource.DeleteProduct: error deleting published band on data base " + e);
		}
		

		refreshProductsTable();
	}	

		
	public static void main(String[] args) {
		
        try {
        	//this is how you read parameters:
			MongoRepository.SERVER_ADDRESS = ConfigReader.getPropValue("MONGO_ADDRESS");
	        MongoRepository.SERVER_PORT = Integer.parseInt(ConfigReader.getPropValue("MONGO_PORT"));
	        MongoRepository.DB_NAME = ConfigReader.getPropValue("MONGO_DBNAME");
	        MongoRepository.DB_USER = ConfigReader.getPropValue("MONGO_DBUSER");
	        MongoRepository.DB_PWD = ConfigReader.getPropValue("MONGO_DBPWD");
	        
	        boolean bExit = false;
	        
	        Scanner oScanner = new Scanner( System.in);
	        
	        while (!bExit) {
		        System.out.println("---- WASDI db Utils ----");
		        System.out.println("Welcome, how can I help you?");
		        
		        System.out.println("\t1 - Downloaded Products");
		        System.out.println("\t2 - Product Workspace");
		        System.out.println("\t3 - Logs");
		        System.out.println("\t4 - Metadata");
		        System.out.println("\t5 - Password");
		        System.out.println("\t6 - Users");
		        System.out.println("\tx - Exit");
		        System.out.println("");
		        
		        
		        String sInputString = oScanner.nextLine();
		        
		        if (sInputString.equals("1")) {
		        	downloadedProducts();
		        }
		        else if (sInputString.equals("2")) {
		        	productWorkspace();
		        }		        
		        else if (sInputString.equals("3")) {
		        	logs();
		        }		        
		        else if (sInputString.equals("4")) {
		        	metadata();
		        }		       
		        else if (sInputString.equals("5")) {
		        	password();
		        }
		        else if (sInputString.equals("6")) {
		        	users();
		        }
		        else if (sInputString.toLowerCase().equals("x")) {
		        	bExit = true;
		        }		        
		        else {
		        	System.out.println("Please select a valid option or x to exit");
		        	System.out.println("");
		        	System.out.println("");
		        }
	        }
	        
	        
	        System.out.println("bye bye");
	        
	        oScanner.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}



}
