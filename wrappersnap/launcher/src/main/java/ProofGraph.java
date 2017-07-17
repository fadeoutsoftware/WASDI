import java.awt.Rectangle;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphContext;
import org.esa.snap.core.gpf.graph.GraphException;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.esa.snap.core.gpf.graph.GraphProcessingObserver;
import org.esa.snap.core.gpf.graph.GraphProcessor;
import org.esa.snap.core.gpf.graph.Node;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Config;
import org.esa.snap.runtime.Engine;

import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.core.ProgressMonitor;

import wasdi.ConfigReader;

public class ProofGraph {
	
	private static class MyGraphObserver implements GraphProcessingObserver {
		@Override
		public void tileProcessingStopped(GraphContext graphContext, Rectangle tileRectangle) {
//			System.out.println("tileProcessingStopped " + tileRectangle);
		}
		
		@Override
		public void tileProcessingStarted(GraphContext graphContext, Rectangle tileRectangle) {
//			System.out.println("tileProcessingStarted " + tileRectangle);
		}
		
		@Override
		public void graphProcessingStopped(GraphContext graphContext) {
			System.out.println("graphProcessingStopped");
		}
		
		@Override
		public void graphProcessingStarted(GraphContext graphContext) {			
			System.out.println("graphProcessingStarted");
		}		
	}
	
	private static class MyProgressMonitor implements ProgressMonitor {
		
		int stepsDone = 0, stepsTodo = 0, precPercDone = -1;
		
		@Override
		public void worked(int work) {
			stepsDone += work;
			
			int percDone = (int)(((float)stepsDone/(float)stepsTodo) * 100);
			if (percDone % 5 == 0 && percDone!=precPercDone) {
				System.out.println("done " + percDone + "%");
				precPercDone = percDone;
			}
		}
		
		@Override
		public void setTaskName(String taskName) {
			System.out.println("setTaskName " + taskName);
		}
		
		@Override
		public void setSubTaskName(String subTaskName) {
			System.out.println("setSubTaskName " + subTaskName);
		}
		
		@Override
		public void setCanceled(boolean canceled) {
			System.out.println("setCanceled " + canceled);
		}
		
		@Override
		public boolean isCanceled() {
			return false;
		}
		
		@Override
		public void internalWorked(double work) {
			System.out.println("internalWorked " + work);
		}
		
		@Override
		public void done() {
			System.out.println("done");
		}
		
		@Override
		public void beginTask(String taskName, int totalWork) {
			stepsTodo = totalWork;
			System.out.println("beginTask " + taskName + " totalWork: " + totalWork);
		}
		
	}
	
	public static void main(String[] args) throws FileNotFoundException, GraphException {
		
		File graphXmlFile = new File("/home/doy/tmp/wasdi/graph/myGraph.xml");
		File inputFile = new File("/home/doy/tmp/wasdi/graph/S1A_IW_GRDH_1SDV_20160802T051857_20160802T051922_012417_013615_C75B.zip");
		File outputFile = new File("/home/doy/tmp/wasdi/graph/output_product");

		System.setProperty("user.home", "/home/doy");
        Path propFile = Paths.get("/home/doy/workspaces/wasdi/server/launcher/target/config.properties");
        Config.instance("snap.auxdata").load(propFile);
        Config.instance().load();

        //JAI.getDefaultInstance().getTileScheduler().setParallelism(Runtime.getRuntime().availableProcessors());
        //MemUtils.configureJaiTileCache();
        
        SystemUtils.init3rdPartyLibs(null);
        Engine.start(false);

		
		Graph graph = GraphIO.read(new FileReader(graphXmlFile));
		
		
		Node nodeReader = graph.getNode("Read");
		Node nodeWriter = graph.getNode("Write");
		
		if (nodeReader==null || nodeWriter==null) {
			System.out.println("Reader node and Wroter node are mandatory!!");
			return;
		}
		
		if (!setNodeFileValue(nodeReader, inputFile) || !setNodeFileValue(nodeWriter, outputFile)) {
			return;
		}
		
		GraphContext context = new GraphContext(graph);
		
		GraphProcessor processor = new GraphProcessor();
		
		processor.addObserver(new MyGraphObserver());
				
		Product[] outputs = processor.executeGraph(context, new MyProgressMonitor());
		
		for (Product product : outputs) {
			System.out.println("OUTPUT: " + product.getName());			
			File f = product.getFileLocation();
			System.out.println(f);
		}
		
	}

	private static boolean setNodeFileValue(Node node, File file) {
		DomElement el = node.getConfiguration();
		DomElement[] children = el.getChildren("file");
		if (children==null || children.length!=1) {
			System.out.println("Cannot find file child in node");
			return false;
		}
		children[0].setValue(file.getAbsolutePath());
		return true;
	}

}
