package wasdi.snapopearations;
import java.io.File;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.dimap.DimapProductWriterPlugIn;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Engine;

import com.bc.ceres.core.ProgressMonitor;

import wasdi.shared.parameters.ApplyOrbitParameter;
import wasdi.shared.parameters.ISetting;
import wasdi.snapopearations.ApplyOrbit;
import wasdi.snapopearations.ReadProduct;
import wasdi.snapopearations.WriteProduct;

public class ProofPerformance {

	
	private static final class OperationProgressMonitor implements ProgressMonitor {
		@Override
		public void worked(int work) {
			System.out.println("PROGRESS: worked: " + work);				
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
		}
	}

	public static void main(String[] args) throws Exception {
		File wDir = new File("/home/doy/tmp/wasdi/prova_snap");
		String inName = "S1A_IW_GRDH_1SDV_20160802T051857_20160802T051922_012417_013615_C75B";
		File inFile = new File(wDir, inName + ".zip");
		String outName = inName + "_out";
		File outFile = new File(wDir, outName);
		
		SystemUtils.init3rdPartyLibs(null);
		Engine.start(false);
		
		long t = System.currentTimeMillis();
		ApplyOrbitParameter parameters = new ApplyOrbitParameter();		
		ApplyOrbit operation = new ApplyOrbit();
		//note: check the next, commented out because unused
        //WriteProduct writer = new WriteProduct();
        ReadProduct reader = new ReadProduct();
        Product inProduct = reader.ReadProduct(inFile, null);
                
        System.out.println("input product read: " + (System.currentTimeMillis()-t));
        t = System.currentTimeMillis();

        ISetting settings = parameters.getSettings();

        //note: check the next, commented out because unused
        //Product outProduct = operation.getOperation(inProduct, settings);
		operation.getOperation(inProduct, settings);

        System.out.println("output product built: " + (System.currentTimeMillis()-t));
        t = System.currentTimeMillis();

        //writing product in default snap format
//        writer.WriteBEAMDIMAP(inProduct, outFile.getAbsolutePath(), outName);
        
        ProductIO.writeProduct(inProduct, outFile.getAbsolutePath(), DimapProductWriterPlugIn.DIMAP_FORMAT_NAME, new OperationProgressMonitor());
        
        System.out.println("output file written: " + (System.currentTimeMillis()-t));
        t = System.currentTimeMillis();
        

	}

	
	
	
}
