import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.collections.FactoryUtils;

import wasdi.ConfigReader;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.PasswordAuthentication;
import wasdi.shared.business.ProcessorLog;
import wasdi.shared.business.ProductWorkspace;
import wasdi.shared.business.User;
import wasdi.shared.business.Workspace;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorLogRepository;
import wasdi.shared.data.ProductWorkspaceRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.utils.Utils;
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
	        System.out.println("");
	        
	        Scanner oScanner = new Scanner( System.in);
	        String sInputString = oScanner.nextLine();
	        
	        boolean bDelete = false;
	        
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
						oDownloadedFilesRepository.DeleteByFilePath(oDownloadedFile.getFilePath());
						
						// Delete Product Workspace
						ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();
						oProductWorkspaceRepository.DeleteByProductName(oDownloadedFile.getFilePath());

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
					
	        		Workspace oWorkspace = oWorkspaceRepository.GetWorkspace(oProductWorkspace.getWorkspaceId());
	        		
	        		if (oWorkspace == null) {
	        			System.out.println("productWorkspace: workspace " + oProductWorkspace.getWorkspaceId() + " does not exist, delete entry");
	        			oProductWorkspaceRepository.DeleteByProductName(oProductWorkspace.getProductName());
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
					
	        		DownloadedFile oDownloadedFile = oDownloadedFilesRepository.GetDownloadedFileByPath(oProductWorkspace.getProductName());
	        		
	        		if (oDownloadedFile == null) {
	        			System.out.println("productWorkspace: Downloaded File " + oProductWorkspace.getProductName() + " does not exist, delete entry");
	        			oProductWorkspaceRepository.DeleteByProductName(oProductWorkspace.getProductName());
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
				
				List<ProcessorLog> aoLogs = oProcessorLogRepository.GetLogsByProcessWorkspaceId(sProcessWorkspaceId);
				
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
	        	oProcessorLogRepository.DeleteLogsByProcessWorkspaceId(sProcessWorkspaceId);
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
	        	User oUser = oUserRepo.GetUser(sUserId);
	        	
	        	if (oUser == null) {
	        		System.out.println("User [" + sUserId + "] not found");
	        		return;
	        	}
	        	
	        	oUser.setPassword(oAuth.hash(sInputPw.toCharArray()));
	        	
	        	oUserRepo.UpdateUser(oUser);
	        	
	        	System.out.println("Update password for user [" + sUserId + "]");

	        }
		}
		catch (Exception oEx) {
			System.out.println("password Exception: " + oEx);
			oEx.printStackTrace();
		}
	}
	
	public static void sample() {
		System.out.println("sample method running");
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
