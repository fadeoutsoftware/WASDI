import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Config;
import org.esa.snap.runtime.Engine;

import wasdi.ConfigReader;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.utils.Utils;

public class Test2 {

	public static void main(String[] args) throws IOException {
		System.setProperty("user.home", "C:\\Users\\p.campanella.FADEOUT");
        Path oPropFile = Paths.get("C:\\Codice\\Progetti\\WASDI\\Codice\\\\wrappersnap\\launcher\\target\\config.properties");
        Config.instance("snap.auxdata").load(oPropFile);
        Config.instance().load();
        SystemUtils.init3rdPartyLibs(null);
        Engine.start(false);
        
		MongoRepository.SERVER_ADDRESS = ConfigReader.getPropValue("MONGO_ADDRESS");
        MongoRepository.SERVER_PORT = Integer.parseInt(ConfigReader.getPropValue("MONGO_PORT"));
        MongoRepository.DB_NAME = ConfigReader.getPropValue("MONGO_DBNAME");
        MongoRepository.DB_USER = ConfigReader.getPropValue("MONGO_DBUSER");
        MongoRepository.DB_PWD = ConfigReader.getPropValue("MONGO_DBPWD");
        
        
        try {
        	ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
        	
        	String sEventId = "613fc4ac-462c-4d29-b17c-1bafeab1acc0";
        	
        	ProcessWorkspace oEventPWS = oProcessWorkspaceRepository.getProcessByProcessObjId(sEventId);
        	
        	Date oEndDate = Utils.getWasdiDate(oEventPWS.getOperationEndDate());
        	Date oStartDate = Utils.getWasdiDate(oEventPWS.getOperationStartDate());
        	
        	long lTotalDurationMin = oEndDate.getTime() - oStartDate.getTime();
        	long lTotalDownloadMin = 0;
        	long lTotalGraphMin = 0;
        	long lTotalFloodMin = 0;
        	
        	lTotalDurationMin/=1000;
        	lTotalDurationMin/=60;
        			
        	List<ProcessWorkspace> aoEventChilds = oProcessWorkspaceRepository.getProcessByParentId(sEventId);
        	
        	ProcessWorkspace oArchivePWS = null;
        	
        	int iComputedDays = 0;
        	int iImagesOk = 0;
        	int iImageKo = 0;
        	
        	int iGraphOk = 0;
        	int iGraphKo = 0;
        	
        	int iFloodsOk = 0;
        	int iFloodsKo = 0;
        	
        	// Search archive
        	for (ProcessWorkspace oChild : aoEventChilds) {
				if (oChild.getProductName().equals("edrift_archive_generator")) {
					oArchivePWS = oChild;
					break;
				}
			}
        	
        	if (oArchivePWS != null) {
        		List<ProcessWorkspace> aoArchiveChilds = oProcessWorkspaceRepository.getProcessByParentId(oArchivePWS.getProcessObjId());
        		
        		iComputedDays = aoArchiveChilds.size();
        		
        		// for each mosaic tile        		
        		for (ProcessWorkspace oMosaicTilePWS : aoArchiveChilds) {
        			
        			List<ProcessWorkspace> aoMosaicTileChilds = oProcessWorkspaceRepository.getProcessByParentId(oMosaicTilePWS.getProcessObjId());
        			
        			for (ProcessWorkspace oMosaicChild : aoMosaicTileChilds) {
						
        				// Downloads
        				if (oMosaicChild.getOperationType().equals(LauncherOperations.DOWNLOAD.name())) {
        					
        					if (oMosaicChild.getStatus().equals("DONE")) {
        						iImagesOk ++;
        						lTotalDownloadMin += Utils.getProcessWorkspaceSecondsDuration(oMosaicChild)/60;
        					}
        					else {
        						iImageKo ++;
        					}
        				}
        				// Graphs
        				else if (oMosaicChild.getOperationType().equals(LauncherOperations.GRAPH.name())) {
        					if (oMosaicChild.getStatus().equals("DONE")) {
        						iGraphOk++;
        						lTotalGraphMin += Utils.getProcessWorkspaceSecondsDuration(oMosaicChild)/60;
        					}
        					else {
        						iGraphKo++;
        					}
        				}
        				// Floods
        				else if (oMosaicChild.getOperationType().equals(LauncherOperations.RUNIDL.name()) && oMosaicChild.getProductName().equals("edriftlistflood_archive")) {
    						iFloodsOk++;
    						lTotalFloodMin += Utils.getProcessWorkspaceSecondsDuration(oMosaicChild)/60;
    						// NOTA: Qui abbiamo il problema di qualche finto error
        				}        				
					}
				}// End for each mosaic tile
        		
        		System.out.println("Event [" + sEventId + "] Computed Days : " + iComputedDays + " Total Time " + lTotalDurationMin);        		
        		System.out.println("Images OK=" + iImagesOk + "; Images KO="+iImageKo+"; Download Time:"+lTotalDownloadMin);
        		System.out.println("Graphs OK=" + iGraphOk + "; Graphs KO="+iGraphKo+"; Preprocessing Time:"+lTotalGraphMin);
        		System.out.println("Flood OK=" + iFloodsOk + "; Flood KO="+iFloodsKo +"; FloodDetection Time:"+lTotalFloodMin);
        	}
        	
        }
        catch (Exception e) {
        	System.out.println("Exception " + e.toString());
		}

	}
	
	

}
