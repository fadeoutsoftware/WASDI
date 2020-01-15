import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Config;
import org.esa.snap.runtime.Engine;

import wasdi.ConfigReader;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.ProcessorLog;
import wasdi.shared.business.ProductWorkspace;
import wasdi.shared.business.PublishedBand;
import wasdi.shared.business.SnapWorkflow;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorLogRepository;
import wasdi.shared.data.ProductWorkspaceRepository;
import wasdi.shared.data.PublishedBandsRepository;
import wasdi.shared.data.SnapWorkflowRepository;
import wasdi.shared.data.WorkspaceSharingRepository;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.GeorefProductViewModel;

public class TestDataLayer {

	public static void main(String[] args) throws IOException {
		
		
		System.setProperty("user.home", "C:\\Users\\p.campanella.FADEOUT");
        Path oPropFile = Paths.get("C:\\Codice\\Progetti\\WASDI\\Codice\\wrappersnap\\launcher\\target\\config.properties");
        //Config.instance("snap.auxdata").load(oPropFile);
        //Config.instance().load();
        //SystemUtils.init3rdPartyLibs(null);
        //Engine.start(false);
        
		MongoRepository.SERVER_ADDRESS = ConfigReader.getPropValue("MONGO_ADDRESS");
        MongoRepository.SERVER_PORT = Integer.parseInt(ConfigReader.getPropValue("MONGO_PORT"));
        MongoRepository.DB_NAME = ConfigReader.getPropValue("MONGO_DBNAME");
        MongoRepository.DB_USER = ConfigReader.getPropValue("MONGO_DBUSER");
        MongoRepository.DB_PWD = ConfigReader.getPropValue("MONGO_DBPWD");
        
        
        ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();
        DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
        
        eDriftValidation();
        
        //
        /*
        ProductWorkspace oTestUpdate = new ProductWorkspace();
        oTestUpdate.setProductName("/data/wasdi/paolo/4a19da52-73f7-43c3-87a5-2f167aefd46b/PROBAV_L2A_20180604_104840_2_1KM_V101.HDF5");
        oTestUpdate.setWorkspaceId("4a19da52-73f7-43c3-87a5-2f167aefd46b");
        oTestUpdate.setBbox("BBOXTEST");
        oProductWorkspaceRepository.UpdateProductWorkspace( oTestUpdate);
        
        
        List<ProductWorkspace> aoProductWorkspace = oProductWorkspaceRepository.getList();
        
        for (int i = 0; i < aoProductWorkspace.size(); i++) {
			ProductWorkspace oProductWS = aoProductWorkspace.get(i);
			
			DownloadedFile oDowFile = oDownloadedFilesRepository.GetDownloadedFileByPath(oProductWS.getProductName());
			
			if (oDowFile != null) {
				// SET THE BBOX
				oProductWS.setBbox(oDowFile.getBoundingBox());
				// Fare una update "sicura"
				oProductWorkspaceRepository.UpdateProductWorkspace(oProductWS, oProductWS.getProductName());				
			}
			else {
				System.out.println("DONWLOADED FILE NOT PRESENT " + oProductWS.getProductName());
			}
		}
        
        
        PublishedBandsRepository oPublishedBandsRepository = new PublishedBandsRepository();
        List<PublishedBand> aoBands = oPublishedBandsRepository.GetPublishedBandsByProductName("MY_2019-07-18_flood");
        
        
        //ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();
        
        List<DownloadedFile> aoFiles = oDownloadedFilesRepository.GetDownloadedFileListByPath("/data/wasdi/paolo/S1");
        
        for (DownloadedFile oDownFile : aoFiles) {
        	
        	String sOldProductName = oDownFile.getFilePath();
        	ProductWorkspace oProductWorkspace = oProductWorkspaceRepository.GetProductWorkspace(sOldProductName, "");
        	
			oDownFile.setFilePath("/data/wasdi/paolo/ffda028c-88d0-4ffa-a021-71b95a42875c/" + oDownFile.getFileName());
			
			oProductWorkspace.setProductName(oDownFile.getFilePath());
			oProductWorkspace.setWorkspaceId("ffda028c-88d0-4ffa-a021-71b95a42875c");
			
			oDownloadedFilesRepository.UpdateDownloadedFile(oDownFile);
			
			oProductWorkspaceRepository.UpdateProductWorkspace(oProductWorkspace, sOldProductName);
			
		}
        
        
        
        List<ProductWorkspace> aoProdWS = oProductWorkspaceRepository.GetProductWorkspaceListByPath("/data/wasdi/paolo/S1");
        
        
        WorkspaceSharingRepository oWorkspaceSharingRepository = new WorkspaceSharingRepository();
        oWorkspaceSharingRepository.DeleteByUserIdWorkspaceId("paolo", "4e956c23-64bf-4449-96a8-4695a2355452");

        ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
        
        List<ProcessWorkspace> aoLastProcessWS = oProcessWorkspaceRepository.GetProcessByWorkspace("7d05edb2-bb7a-48fb-8c65-1939e1663afc", 0, 20);
        
        System.out.println("aoLastProcessWS count =" + aoLastProcessWS.size());
        
        
        
		ProcessorLog oTestLog = new ProcessorLog();
		oTestLog.setProcessWorkspaceId("fb99a0b1-93cb-40ab-9d44-9701a7b11b9b");
		oTestLog.setLogRow("Now put the second one!");
		oTestLog.setLogDate(Utils.GetFormatDate(new Date()));
		
		ProcessorLogRepository oProcessorLogRepository = new ProcessorLogRepository();
		oProcessorLogRepository.InsertProcessLog(oTestLog);
		
		List<ProcessorLog> aoLogs = oProcessorLogRepository.GetLogsByProcessWorkspaceId("fb99a0b1-93cb-40ab-9d44-9701a7b11b9b");
		
		for (ProcessorLog oLog : aoLogs) {
			System.out.println(oLog.getLogRow());
		}
		*/
	}
	
	private static void eDriftValidation() {
		
        ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
        List<ProcessWorkspace> aoChains = oProcessWorkspaceRepository.getProcessByProductNameAndWorkspace("mosaic_tile", "7d05edb2-bb7a-48fb-8c65-1939e1663afc");
        
        for (ProcessWorkspace oProcess : aoChains) {
        	
        	if (oProcess.getOperationStartDate() == null) {
        		//System.out.println("Process " + oProcess.getProcessObjId() + " start date null");
        		continue;
        	}
        	

        	
        	try {
            	Date oStart = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(oProcess.getOperationStartDate());  
            	Date oEnd = null ;

            	if (oProcess.getOperationEndDate() != null) {
            		oEnd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(oProcess.getOperationEndDate());
            	}
            	
    			
            	if (oProcess.getOperationStartDate().startsWith("2019-06-") || oProcess.getOperationStartDate().startsWith("2019-07-") || oProcess.getOperationStartDate().startsWith("2019-08-")) {

            		long lMinutesSpent = 0;
            		
            		if (oProcess.getProcessObjId().equals("115ee91c-4ada-487d-a32c-47b0217c2ffc")) {
            			int i=0;
            			i++;
            		}
            		if (oEnd != null) {
                		//in milliseconds
            			long lTimeMs = oEnd.getTime() - oStart.getTime();
            			lMinutesSpent = lTimeMs / (60 * 1000);
            		}

            		System.out.println("" + oProcess.getProcessObjId()+";"+oProcess.getStatus()+";"+oProcess.getOperationStartDate()+ ";" + lMinutesSpent);
            	}        		
        	}
        	catch (Exception e) {
        		String sError = e.toString();
			}
		}
		
	}

}
