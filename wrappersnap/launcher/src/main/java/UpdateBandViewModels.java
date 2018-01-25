import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Config;
import org.esa.snap.runtime.Engine;

import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.ProductWorkspace;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.ProductWorkspaceRepository;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.MetadataViewModel;
import wasdi.shared.viewmodels.ProductViewModel;
import wasdi.snapopearations.ReadProduct;

public class UpdateBandViewModels {
	
	public static String GetSerializationFileName() {
		return UUID.randomUUID().toString();
	}


	public static void main(String[] args) {
		//System.setProperty("user.home", "/home/doy");
        //Path propFile = Paths.get("/home/doy/workspaces/wasdi/server/launcher/target/config.properties");
		
		System.out.println("UpdateBands Start");
		
		System.setProperty("user.home", "/home/wasdi");
        Path propFile = Paths.get("/usr/lib/wasdi/launcher/config.properties");

        System.out.println("Loading Config..");
		Config.instance("snap.auxdata").load(propFile);
        Config.instance().load();
        System.out.println("Config Loaded\nInit SNAP...");

        SystemUtils.init3rdPartyLibs(null);
        Engine.start(false);
        
        System.out.println("Create Repository");

		// Create repo
		ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();
		DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
		
		List<DownloadedFile> aoDownloaded = oDownloadedFilesRepository.getList();
		
		// For all the downloaded files
		for (int i=0; i<aoDownloaded.size(); i++) {
			
			DownloadedFile oDownloadedFileEntity = aoDownloaded.get(i);
			
			System.out.println("Product [" + i + "] - " + oDownloadedFileEntity.getFilePath());
			
			if (oDownloadedFileEntity.getFileName()!= null) {
				if (oDownloadedFileEntity.getFileName().startsWith("SMCItaly")) {
					System.out.println("MIDA Public Product, jump");
					continue;
				}
			}
			
			
			// Read The Product
            ReadProduct oReadSNAPProduct = new ReadProduct();
            File oProductFile = new File(oDownloadedFileEntity.getFilePath());
            Product oProduct = oReadSNAPProduct.ReadProduct(oProductFile, null);
            
            if (Utils.isNullOrEmpty(oDownloadedFileEntity.getFileName())) {
            	System.out.println("Fixing DownloadedFile - FileName");
            	oDownloadedFileEntity.setFileName(oProductFile.getName());
            	oDownloadedFilesRepository.UpdateDownloadedFile(oDownloadedFileEntity);
            }
            
            // There was any problem?
            if (oProduct == null) {
            	
            	// Product NULL
            	
            	// Check if the file exists
            	if (oProductFile.exists()==false) {
            		
            		System.out.println("File does not Exists: delete it");
            		oProductWorkspaceRepository.DeleteByProductName(oDownloadedFileEntity.getFileName());
            		oDownloadedFilesRepository.DeleteByFilePath(oDownloadedFileEntity.getFilePath());
            		System.out.println("File does not Exists: deleted");
            	}
            	else {            		
            		
            		// Clear Metadata anyway
            		System.out.println("File Exists but could not be read");
            		
            		if (oDownloadedFileEntity.getProductViewModel() != null && oDownloadedFileEntity.getProductViewModel().getMetadata() != null) {
            			oDownloadedFileEntity.getProductViewModel().setMetadata(null);
                        oDownloadedFilesRepository.UpdateDownloadedFile(oDownloadedFileEntity);
                        System.out.println("Cleared Metadata");
            		}
            		
            	}
            	continue;
            }
            
            try {
                ProductViewModel oVM = oReadSNAPProduct.getProductViewModel(oProduct, oProductFile);
                
                if (oVM == null) {
                	System.out.println("WARNING - Product View Model NULL FOR " +  oDownloadedFileEntity.getFilePath());
                	continue;
                }
                
                //oVM.setMetadata(oReadSNAPProduct.getProductMetadataViewModel(oProductFile));
                MetadataViewModel oMetadataViewModel = oReadSNAPProduct.getProductMetadataViewModel(oProductFile);
                
                if (oDownloadedFileEntity.getProductViewModel().getMetadataFileReference() == null || oDownloadedFileEntity.getProductViewModel().getMetadataFileReference().isEmpty()) {
                    System.out.println("Serialize Metadata");
                    String sMetadataFile = GetSerializationFileName();
                    SerializationUtils.serializeObjectToXML("/data/wasdi/metadata/"+sMetadataFile, oMetadataViewModel);
                    oDownloadedFileEntity.getProductViewModel().setBandsGroups(oVM.getBandsGroups());
                    oDownloadedFileEntity.getProductViewModel().setMetadataFileReference(sMetadataFile);                		                	
                }
                else {
                	System.out.println("Metadata Already Serialized");
                }
                
                
                oDownloadedFileEntity.getProductViewModel().setMetadata(null);
                oDownloadedFilesRepository.UpdateDownloadedFile(oDownloadedFileEntity);
                
                //MetadataViewModel oReloaded = (MetadataViewModel) SerializationUtils.deserializeXMLToObject("C:\\Temp\\wasdi\\metadata\\"+oDownloadedFileEntity.getProductViewModel().getMetadataFileReference());
            }
            catch (Exception oEx) {
            	System.out.println("Exception " + oEx);
            }
		}
		
		
		System.out.println("Clearing Product Workspace Table..");
		
		List<ProductWorkspace> aoProductsWSs = oProductWorkspaceRepository.getList();
		
		for (int i=0; i<aoProductsWSs.size(); i++) {
			ProductWorkspace oPW = aoProductsWSs.get(i);
			
			DownloadedFile oDF = oDownloadedFilesRepository.GetDownloadedFile(oPW.getProductName());
			
			if (oDF == null) {
				System.out.println("\nINVALID Product : " + oPW.getProductName() + " WS : " + oPW.getWorkspaceId());
				oProductWorkspaceRepository.DeleteByProductNameWorkspace(oPW.getProductName(), oPW.getWorkspaceId());
				System.out.println("DELETED");
			}
			else {
				System.out.print(".");
			}
		}


		System.out.println("\nBye Bye");
	}

}
