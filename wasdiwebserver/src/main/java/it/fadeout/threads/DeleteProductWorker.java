package it.fadeout.threads;

import java.util.List;

import it.fadeout.rest.resources.ProductResource;
import wasdi.shared.business.Node;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.rabbit.Send;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.PrimitiveResult;

public class DeleteProductWorker extends Thread {

	List<String> m_asFiles;	// the list of files to delete
	String m_sSessionId;	// the id of the session
	String m_sWorkspaceId; 	// the id of the workspace
	boolean m_bDeleteFile;	// flag to confirm the deletion of the file as well
	boolean m_bDeleteLayer;	// flag to confirm the deletion of the layer

	
	public void init(List<String> asFiles, String sSessionId, String sWorkspaceId, boolean bDeleteFile, boolean bDeleteLayer) {
		m_asFiles = asFiles;
		m_sSessionId = sSessionId;
		m_sWorkspaceId = sWorkspaceId;
		m_bDeleteFile = bDeleteFile;
		m_bDeleteLayer = bDeleteLayer;
	}
	
	
	@Override
	public void run() {
		WasdiLog.debugLog("DeleteProductWorker.run: starting the deletion of files in the workspace");
		
		ProductResource oProductResource = new ProductResource();
		boolean bDirty = false;
		
		try {
			for (String sFile : m_asFiles) {
				PrimitiveResult oResult = oProductResource.deleteProduct(m_sSessionId, sFile, m_bDeleteFile, m_sWorkspaceId, m_bDeleteLayer);
				int iStatusCode = oResult.getIntValue();
				
				if (iStatusCode < 200 || iStatusCode > 299) {
					WasdiLog.debugLog("DeleteProductWorker.run: deletion of file " + sFile + " in workspace " + m_sWorkspaceId + " not successful");
					bDirty = bDirty || true;
				}
				else {
					bDirty = bDirty || false;
				}
				
			}
		} catch (Exception oE){
			WasdiLog.errorLog("DeleteProductWorker.run: exception ", oE);
		}
		
		WasdiLog.debugLog("DeleteProductWorker.run: deletion of files done");
		
		try {
            // Search for exchange name
            String sExchange = WasdiConfig.Current.rabbit.exchange;

            // Set default if is empty
            if (Utils.isNullOrEmpty(sExchange)) {
                sExchange = "amq.topic";
            }

            // Send the Asynch Message to the clients
            Send oSendToRabbit = new Send(sExchange);
            oSendToRabbit.SendRabbitMessage(!bDirty, "DELETE", m_sWorkspaceId, null, m_sWorkspaceId);
            oSendToRabbit.Free();
        } catch (Exception oEx) {
            WasdiLog.errorLog("DeleteProductWorker.deleteProduct: exception sending asynch notification");
        }
	}
	
	
}
