package wasdi.snapopearations;

import java.io.File;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.dimap.DimapProductWriterPlugIn;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.common.WriteOp;
import org.esa.snap.core.gpf.internal.OperatorExecutor;

import com.bc.ceres.core.ProgressMonitor;

import wasdi.LauncherMain;

/**
 * Created by s.adamo on 24/05/2016.
 */
public class WriteProduct {
	
	private static final class RabbitOperationProgressMonitor implements ProgressMonitor {
		
		
		private int totalSteps = 0;
		private int computedStep = 0;
		private int computedIntervals = 0;
		private int intervalPercStep = 10;
		
		@Override
		public void worked(int work) {
			computedStep += work;
			if (totalSteps!=0) {
				int tmp = (int)(((float)computedStep / (float) totalSteps) * 100);
				if (tmp%intervalPercStep == 0 && tmp != computedIntervals) {
					computedIntervals = tmp;
					
					LauncherMain.s_oLogger.debug("WriteProduct.RabbitOperationProgressMonitor: PROGRESS " + computedIntervals + "%");
				}
			}
		}

		@Override
		public void setTaskName(String taskName) {
			LauncherMain.s_oLogger.debug("WriteProduct.RabbitOperationProgressMonitor: setTaskName: " + taskName);
		}

		@Override
		public void setSubTaskName(String subTaskName) {
			LauncherMain.s_oLogger.debug("WriteProduct.RabbitOperationProgressMonitor: setSubTaskName: " + subTaskName);
		}

		@Override
		public void setCanceled(boolean canceled) {
			LauncherMain.s_oLogger.debug("WriteProduct.RabbitOperationProgressMonitor: setCanceled: " + canceled);		
		}

		@Override
		public boolean isCanceled() {
			return false;
		}

		@Override
		public void internalWorked(double work) {
		}

		@Override
		public void done() {
			LauncherMain.s_oLogger.debug("WriteProduct.RabbitOperationProgressMonitor: done");				
		}

		@Override
		public void beginTask(String taskName, int totalWork) {
			LauncherMain.s_oLogger.debug("WriteProduct.RabbitOperationProgressMonitor: beginTask: " + taskName + " total work: " +totalWork );
			totalSteps = totalWork;
		}
	}


    public String WriteBEAMDIMAP(Product oProduct, String sFilePath, String sFileName) throws Exception
    {
        String sFormat = DimapProductWriterPlugIn.DIMAP_FORMAT_NAME;

        return WriteProduct(oProduct, sFilePath, sFileName, sFormat, ".dim");
    }

    public String WriteGeoTiff(Product oProduct, String sFilePath, String sFileName) throws Exception
    {
        return WriteProduct(oProduct, sFilePath, sFileName, "GeoTIFF", ".tif");
    }

    private String WriteProduct(Product oProduct, String sFilePath, String sFileName, String sFormat, String sExtension)
    {
        try {
            if (!sFilePath.endsWith("/")) sFilePath += "/";
            File newFile = new File(sFilePath + sFileName + sExtension);
            LauncherMain.s_oLogger.debug("WriteProduct: Output File: " + newFile.getAbsolutePath());
            
            
            
            WriteOp writeOp = new WriteOp(oProduct, newFile, sFormat);
            writeOp.setDeleteOutputOnFailure(true);
            writeOp.setWriteEntireTileRows(true);
            writeOp.setClearCacheAfterRowWrite(false);
            writeOp.setIncremental(true);
            final OperatorExecutor executor = OperatorExecutor.create(writeOp);
            executor.execute(new RabbitOperationProgressMonitor());        

            
            
            ProductIO.writeProduct(oProduct, newFile.getAbsolutePath(), sFormat);
            return newFile.getAbsolutePath();
        }
        catch (Exception oEx)
        {
        	oEx.printStackTrace();
            LauncherMain.s_oLogger.debug("WriteProduct: Error writing product. " + oEx.getMessage());
        }

        return null;
    }



}
