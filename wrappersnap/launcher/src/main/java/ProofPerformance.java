import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.esa.snap.core.dataio.dimap.DimapProductWriterPlugIn;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.common.WriteOp;
import org.esa.snap.core.gpf.internal.OperatorExecutor;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Config;
import org.esa.snap.runtime.Engine;

import com.bc.ceres.core.ProgressMonitor;
import wasdi.ConfigReader;
import wasdi.shared.parameters.ApplyOrbitParameter;
import wasdi.shared.parameters.ApplyOrbitSetting;
import wasdi.snapopearations.ApplyOrbit;
import wasdi.snapopearations.ReadProduct;

public class ProofPerformance {

	private static final class OperationProgressMonitor implements ProgressMonitor {
		
		
		private int totalSteps = 0;
		private int computedStep = 0;
		private int computedIntervals = 0;
		private int intervalPercStep = 10;
		
		@Override
		public void worked(int work) {
			//System.out.println("PROGRESS: worked: " + work);
			computedStep += work;
			if (totalSteps!=0) {
				int tmp = (int)(((float)computedStep / (float) totalSteps) * 100);
				if (tmp%intervalPercStep == 0 && tmp != computedIntervals) {
					computedIntervals = tmp;
					
					System.out.println("PROGRESS: " + computedIntervals + "%");
				}
			}
			
		}

		@Override
		public void setTaskName(String taskName) {
			System.out.println("PROGRESS: setTaskName: " + taskName);
		}

		@Override
		public void setSubTaskName(String subTaskName) {
			System.out.println("PROGRESS: setSubTaskName: " + subTaskName);
		}

		@Override
		public void setCanceled(boolean canceled) {
			System.out.println("PROGRESS: setCanceled: " + canceled);		
		}

		@Override
		public boolean isCanceled() {
			return false;
		}

		@Override
		public void internalWorked(double work) {
//				System.out.println("PROGRESS: internalWorked: " + work);				
		}

		@Override
		public void done() {
			System.out.println("PROGRESS: done");				
		}

		@Override
		public void beginTask(String taskName, int totalWork) {
			System.out.println("PROGRESS: beginTask: " + taskName + " total work: " +totalWork );
			totalSteps = totalWork;
		}
	}

	public static void main(String[] args) throws Exception {
		File wDir = new File("/home/doy/tmp/wasdi/prova_snap");
		String inName = "S1A_IW_GRDH_1SDV_20160802T051857_20160802T051922_012417_013615_C75B";
		File inFile = new File(wDir, inName + ".zip");
		String outName = inName + "_out";
		File outFile = new File(wDir, outName);
		
		Path propFile = Paths.get(ConfigReader.getPropValue("SNAP_AUX_PROPERTIES"));
		Config.instance("snap.auxdata").load(propFile);
        Config.instance().load();
//        Config.instance().load(propFile);
		
		SystemUtils.LOG.setLevel(Level.FINEST);
		SystemUtils.LOG.addHandler(new Handler() {
			
			@Override
			public void publish(LogRecord record) {
				System.out.println("SNAP_LOG: " + record.getMessage());	
			}
			
			@Override
			public void flush() {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void close() throws SecurityException {
				// TODO Auto-generated method stub
				
			}
		});
		SystemUtils.init3rdPartyLibs(null);

		Engine.start(false);
		
		long t = System.currentTimeMillis();
		
		ApplyOrbitParameter parameters = new ApplyOrbitParameter();
		parameters.setDestinationProductName("prova_doy_performance");
		ApplyOrbit operation = new ApplyOrbit();
//        WriteProduct writer = new WriteProduct();
        ReadProduct reader = new ReadProduct();
        Product inProduct = reader.ReadProduct(inFile, null);
                
        System.out.println("input product read: " + (System.currentTimeMillis()-t));
        t = System.currentTimeMillis();

        ApplyOrbitSetting settings = (ApplyOrbitSetting)parameters.getSettings();
        settings.setSourceBandNames(new String[] {"Amplitude_VV"});
        
		Product outProduct = operation.getOperation(inProduct, settings);

		
        System.out.println("output product built: " + (System.currentTimeMillis()-t));
        t = System.currentTimeMillis();

        //writing product in default snap format
//        writer.WriteBEAMDIMAP(inProduct, outFile.getAbsolutePath(), outName);
//        ProductIO.writeProduct(inProduct, outFile, DimapProductWriterPlugIn.DIMAP_FORMAT_NAME, true, new OperationProgressMonitor());
        
        WriteOp writeOp = new WriteOp(outProduct, outFile, DimapProductWriterPlugIn.DIMAP_FORMAT_NAME);
        writeOp.setDeleteOutputOnFailure(true);
        writeOp.setWriteEntireTileRows(true);
        writeOp.setClearCacheAfterRowWrite(false);
        writeOp.setIncremental(true);
        final OperatorExecutor executor = OperatorExecutor.create(writeOp);
        executor.execute(new OperationProgressMonitor());        
        
        
        System.out.println("output file written: " + (System.currentTimeMillis()-t));
        t = System.currentTimeMillis();
        

	}

	
	
	
}
