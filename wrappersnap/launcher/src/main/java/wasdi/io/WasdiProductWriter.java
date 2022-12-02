package wasdi.io;

import java.io.File;

import org.esa.snap.core.dataio.dimap.DimapProductWriterPlugIn;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.common.WriteOp;
import org.esa.snap.core.gpf.internal.OperatorExecutor;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.snapopearations.WasdiProgressMonitor;

/**
 * SNAP Product Write Utility
 * Created by s.adamo on 24/05/2016.
 */
public class WasdiProductWriter  {
	
	private ProcessWorkspaceRepository m_oProcessWorkspaceRepository = null;
	private ProcessWorkspace m_oProcessWorkspace = null;

	public WasdiProductWriter() {
	}
	
	public WasdiProductWriter(ProcessWorkspaceRepository oProcessWorkspaceRepository, ProcessWorkspace oProcessWorkspace) {
		this.m_oProcessWorkspaceRepository = oProcessWorkspaceRepository;
		this.m_oProcessWorkspace = oProcessWorkspace;
	}

	public String WriteBEAMDIMAP(Product oProduct, String sFilePath, String sFileName) throws Exception
    {
        String sFormat = DimapProductWriterPlugIn.DIMAP_FORMAT_NAME;

        return doWriteProduct(oProduct, sFilePath, sFileName, sFormat, ".dim");
    }

    public String WriteGeoTiff(Product oProduct, String sFilePath, String sFileName) throws Exception
    {
        return doWriteProduct(oProduct, sFilePath, sFileName, "GeoTIFF", ".tif");
    }

    private String doWriteProduct(Product oProduct, String sFilePath, String sFileName, String sFormat, String sExtension)
    {
        try {
            if (!sFilePath.endsWith("/")) sFilePath += "/";
            File newFile = new File(sFilePath + sFileName + sExtension);
            WasdiLog.debugLog("WriteProduct: Output File: " + newFile.getAbsolutePath());
            
            WriteOp writeOp = new WriteOp(oProduct, newFile, sFormat);
            writeOp.setDeleteOutputOnFailure(true);
            writeOp.setWriteEntireTileRows(true);
            writeOp.setClearCacheAfterRowWrite(false);
            writeOp.setIncremental(true);
            final OperatorExecutor executor = OperatorExecutor.create(writeOp);
            executor.execute(new WasdiProgressMonitor(m_oProcessWorkspaceRepository, m_oProcessWorkspace));        

            return newFile.getAbsolutePath();
        }
        catch (Exception oEx)
        {
        	oEx.printStackTrace();
            WasdiLog.errorLog("WriteProduct: Error writing product. " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
        }

        return null;
    }



}
