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

	public static void main(String[] args) throws Exception {
		
		
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
        
        

        ProcessWorkspaceRepository oProcessWorkspaceRepo = new ProcessWorkspaceRepository();
        ArrayList<String> asProcesses = new ArrayList<String>();

        asProcesses.add("d5d074b1-e221-4cc7-82ce-3aa9056497a9");
        asProcesses.add("a1ca3ff4-0f80-4ec0-8acc-e7985e4bb1ba");
        asProcesses.add("dfa0898d-e092-46b9-bab5-b020acd5dfef");
        asProcesses.add("4191a049-1aaf-470a-a4cf-825066beae2d");
        asProcesses.add("8faa6fd7-8412-42bf-a138-50d2b1916c19");
        asProcesses.add("37e086ca-c2a4-4b41-b5ae-e331b6ca512f");
        asProcesses.add("0d11e9d6-a027-4251-b204-ccadeba820f1");
        asProcesses.add("b42669d9-5b54-478d-b8cc-747c2dd437e2");
        asProcesses.add("8fb6f174-aa0d-4b4c-9a84-94f4b73b7c38");
        asProcesses.add("d7d89223-8aa0-4f64-bd55-185a56db6c1b");
        asProcesses.add("601b5319-29b7-4914-af76-cb40ee37140c");
        asProcesses.add("f4d6a3bf-118f-40a1-8ded-e7dbbfe9610f");
        asProcesses.add("71638ba3-e745-4028-b06d-1d893e23ec8e");
        asProcesses.add("5fc39293-9276-4594-81f5-672fd10a1bda");
        asProcesses.add("cc974c85-12be-4c43-83de-73cbca8471f5");
        asProcesses.add("f4a7413a-837b-4f09-becb-95f21114d8de");
        asProcesses.add("06e0448e-0e7a-47b4-be23-80af26bdb2d0");
        asProcesses.add("f6e4ac60-6ec3-483d-9dc1-9364452c1b47");
        asProcesses.add("10cbfc78-c156-42b8-bc9e-d3b787f3177b");
        asProcesses.add("e65afa2d-3aaa-45e3-a93b-aefed5b576d9");
        asProcesses.add("b7412640-6fb8-4e8a-be3e-d441132ae078");
        asProcesses.add("d836da37-d2f5-4ad0-971e-2e4a238f983c");
        asProcesses.add("4e283580-4e04-484c-8328-323cefbcdd31");
        asProcesses.add("940d0301-c980-4dde-bed7-3ff0e920da34");
        asProcesses.add("76b6a3a2-adcb-4bf0-bb8f-61b3e0f6ddf8");
        asProcesses.add("d96aa699-b083-4891-93d0-2d54e5e63768");
        asProcesses.add("ec7fe480-2819-4784-ba21-eb3f9eaf3f28");
        asProcesses.add("17bf28e5-cec8-431b-8759-cea0b46a76ad");
        asProcesses.add("e68f9c9e-6776-45e0-b396-05d21181a81e");
        asProcesses.add("5a5d1ce3-77fd-480b-bd5d-db40b30b92ab");
        asProcesses.add("c365cd4d-6650-45e9-bb54-5515168f0209");
        asProcesses.add("c239687b-0d96-4acc-b31c-1a94feeb61a7");
        asProcesses.add("2ab6d222-72b0-4edf-b828-a729aba49752");
        asProcesses.add("c7b5db94-e23f-4ef8-a70d-11025194b633");
        asProcesses.add("26da9792-ba2f-480d-99c7-f332b14fe074");
        asProcesses.add("c826071f-f45d-4966-8158-84d75be1af29");
        asProcesses.add("1487fd51-05fe-4a49-b5f4-49ef7c7cd0be");
        asProcesses.add("0438252c-c245-4b7e-86f2-2d9bde6afe9f");
        asProcesses.add("fc0e2a7a-a268-4a4f-873f-d2ea4d07a594");
        asProcesses.add("394554c0-e655-409c-90ee-99e20e7e4119");
        asProcesses.add("dd428802-30b0-4a80-ba44-5440f818b4ee");
        asProcesses.add("f099fde2-1b8e-4db3-9003-b774e13fcb31");
        asProcesses.add("8c2468bb-48b7-4046-959e-f2e02d2b4619");
        asProcesses.add("6548c5a2-8615-4e9b-8abf-8a46e860fd16");
        asProcesses.add("abb573fa-665f-445c-a269-a8c104df8d01");
        asProcesses.add("03a85e28-b0a3-4fa3-9436-da0ae29f5be9");
        asProcesses.add("93aee852-843d-4f28-8ed3-fbda20203263");
        asProcesses.add("20dd51fb-3e41-4986-a9ec-bcba956fee48");
        asProcesses.add("bce2d1af-305a-4ab5-97c1-7fd0ad64ebb1");
        asProcesses.add("a49aa2e3-2e7e-4eb2-a5a8-a01345442a06");
        asProcesses.add("f873042f-427b-43e3-baf0-3494a6ae933d");
        asProcesses.add("3ffd9400-b650-4521-8ef9-769240df84fb");
        asProcesses.add("73a06429-da94-41c3-abb9-529d42ac7d0b");
        asProcesses.add("6a07d313-8f12-4242-aa2b-a3387f1f3ce3");
        asProcesses.add("e2c21a08-a222-414e-806a-d3f1331a3199");
        asProcesses.add("c1b51852-89ea-4a5b-8413-3924a23e6c58");
        asProcesses.add("3d575ca5-c6cb-4374-a760-004d67f8fe94");
        asProcesses.add("88f19f23-0ba3-461a-adff-de5a0e92bc1c");
        asProcesses.add("9405dc40-8908-4b9e-bf31-d2c4e8bd6358");
        asProcesses.add("eaf0be6b-e216-4d30-86e4-5f6440934f18");
        asProcesses.add("9e44e793-47d5-4561-8390-a2e2e130d50e");
        asProcesses.add("234595ff-f698-4b07-8e00-91af1e93eaa3");
        asProcesses.add("a274f63b-a66d-4649-9569-9ec5448b7198");
        asProcesses.add("de005d44-c4a7-452c-81a6-c0c285272b9e");
        asProcesses.add("34ec820c-9895-4fec-911f-9c80cc68e60e");
        asProcesses.add("bb3b3e33-c10f-4b87-92a8-23feef68c5aa");
        asProcesses.add("9a8da494-5829-495b-9ecd-da41bcd41681");
        asProcesses.add("ecf37e68-d135-4f68-b29e-78426138bb2f");
        asProcesses.add("b3dee34a-02c4-43eb-8899-6bdc02e1a12d");
        asProcesses.add("6de6476d-9f73-4f61-af3f-43b73cfc9c6b");
        asProcesses.add("9ad2b987-b97f-43ee-bda7-08f2651364c4");
        asProcesses.add("752b7901-ba79-4366-8dbc-fd1d4f7207ac");
        asProcesses.add("23ba5694-262c-424b-bf9d-2769544333d2");
        asProcesses.add("04f32a54-6637-4002-8421-47ebda7ddb9b");
        asProcesses.add("7ff574e5-ca3c-4163-b797-38b7dc8a659f");
        asProcesses.add("717b8e20-55cf-4f5a-971d-44663ade3536");
        asProcesses.add("d6c6379b-e452-4c10-8715-fdecaed9fe7e");
        asProcesses.add("68969546-3cac-409e-a522-39125203a205");
        asProcesses.add("f6b12080-afc4-4161-b6bd-efb51f76abbf");
        asProcesses.add("bc7b7631-0235-492c-a046-619c13c764fe");
        asProcesses.add("b734ebeb-873a-4a43-8b7c-1d378b00758f");
        asProcesses.add("0a10ceee-0958-4405-9f04-4de6d07f07cb");
        asProcesses.add("ee680dd8-03fa-43fb-8a77-d25cfb23aedf");
        asProcesses.add("c95b46e8-25ff-474a-ae4e-667163b59af8");
        asProcesses.add("61e47129-7626-4a83-a4de-7116035bbf41");
        asProcesses.add("1af2a49c-51b3-4b2e-9108-ad08a86d2b19");
        asProcesses.add("a2ea77e4-4859-4b82-8049-94ee7febe3e5");
        asProcesses.add("699ae128-84a4-41c2-b38c-9178b3f914e6");
        asProcesses.add("50250cf6-ece8-4c4e-9f6e-793935264378");
        asProcesses.add("b51ca795-bd31-4d51-80dd-bafb2b108313");
        asProcesses.add("a1848a09-f64d-4d5b-ade0-bfa0ec46f9b4");
        asProcesses.add("aa3fa7cd-2664-40c7-81b2-3902b2502c4a");
        asProcesses.add("c7b2c840-6125-44cb-90c5-1537bfed2777");
        asProcesses.add("7e35ee7c-3215-4f5d-b1a2-def317111894");
        asProcesses.add("ea791991-6fd5-4838-be0a-18897226912c");
        asProcesses.add("fea03d30-6282-4231-89c7-d0fba7e445c2");
        asProcesses.add("c7243a74-3440-445d-897b-c4c300e606bc");
        asProcesses.add("784bd98e-7d2f-4a10-867e-f588abd80882");
        asProcesses.add("858fee0b-bf35-4e58-a5fe-f690e023a0d0");
        asProcesses.add("d629d772-0a9a-4632-912c-d0abf3983084");
        asProcesses.add("6b8e5fc9-8344-4ba6-afb8-a37fd160f293");
        asProcesses.add("06228d4e-df28-47d3-9642-3ac1b9f01bd3");
        asProcesses.add("6533a561-bf39-496f-bd8d-3cf4ed3e933c");
        asProcesses.add("7c45b9a0-b4eb-4d1f-b29d-51853dc04d4e");
        asProcesses.add("d1025ac8-c6f3-486b-9216-852d9dce1779");
        asProcesses.add("308c4d7c-8412-4338-839f-60bc2ba013ef");
        asProcesses.add("da2fd969-da98-41bf-8750-fcb4df3cd9db");
        asProcesses.add("61d53450-c1f5-4c2f-9d9f-d8acc7f30066");
        asProcesses.add("36d7bd1e-88b2-4355-9ef7-8b6c57b5897c");
        asProcesses.add("e0b7ff1b-d084-4694-8aab-c2ac5c52de7a");
        asProcesses.add("9ffad770-8c06-4217-aa73-59b83f320946");
        asProcesses.add("10ec4d3e-7d68-4dae-8bad-c018c6577ee9");
        asProcesses.add("58b051f2-380f-4bd5-8e0c-fb343a58aa8c");
        asProcesses.add("f76a68fc-3b4b-4166-9658-301216bdced4");
        asProcesses.add("61bc946d-3788-4397-ad25-343b1fd87c07");
        asProcesses.add("def9fabb-cc2c-4aef-9bbd-a46f815caca3");
        asProcesses.add("ac29d9db-5c02-43f9-8dcb-84ce074e1861");
        asProcesses.add("8daf91bd-29f9-468a-ae1d-c36fbc035c86");
        asProcesses.add("6b482677-7190-4a69-9b74-db3babc37a12");
        asProcesses.add("ab09709c-f0ea-45a3-9e17-cdac1073d73b");
        asProcesses.add("0b3e9b6f-58c6-4558-80c0-ac786f940356");
        asProcesses.add("273b5955-30e7-463c-9162-00c580b5cf1b");
        asProcesses.add("9ae2c1a2-607e-47c2-a2a7-68edff6546b8");
        asProcesses.add("cb08085c-afa7-4216-b62f-f7e742f29ed7");
        asProcesses.add("17479bf5-a34d-436f-a065-c1c54fbf3f65");
        asProcesses.add("427c9417-f067-4f06-95a4-2786a88f2d85");
        asProcesses.add("48d61525-140f-4e9f-aee0-4c2106f090a5");
        asProcesses.add("44476cff-5fa2-4375-921b-cee2bc7d6339");
        asProcesses.add("43218610-ec6b-49db-b893-493042dcdfa6");
        asProcesses.add("b2c01720-a8b2-4cf1-bcc4-30cdc115ff8a");
        asProcesses.add("35c79277-e3e2-4a14-aa35-66ef8661346f");
        asProcesses.add("c5eedab6-105e-43ea-8d31-867acb8af486");
        asProcesses.add("11ee3f29-74d8-42ed-91b8-a98b08b70525");
        asProcesses.add("ab370180-55ca-4ff4-869c-91a306969e85");
        asProcesses.add("69bfa288-1abe-4340-96f0-1ddda5ef51cd");
        asProcesses.add("6adb17a1-b9d1-4043-8b13-0368a28af3aa");
        asProcesses.add("cfded1a6-100c-472d-a79e-f45398b42898");
        asProcesses.add("c4da6743-df0a-4596-ac8c-d48c2bd291a9");
        asProcesses.add("f85aa416-f62b-4981-b7a7-e88be926bcdb");
        asProcesses.add("053f0d62-f42e-430d-81da-dbdabce25adf");
        
        System.out.println("Query = " + asProcesses.size());
        
        List<String> asReturnStatus = oProcessWorkspaceRepo.getProcessesStatusByProcessObjId(asProcesses);
        
        System.out.println("Returned = " + asReturnStatus.size());
        
        //eDriftValidation();
        
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
