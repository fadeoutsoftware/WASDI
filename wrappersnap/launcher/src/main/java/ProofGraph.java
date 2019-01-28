import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;
import org.esa.snap.core.gpf.graph.GraphContext;
import org.esa.snap.core.gpf.graph.GraphException;
import org.esa.snap.core.gpf.graph.GraphProcessingObserver;
import org.esa.snap.core.gpf.graph.Node;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Config;
import org.esa.snap.runtime.Engine;

import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.core.ProgressMonitor;

import wasdi.ConfigReader;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.parameters.GraphParameter;
import wasdi.shared.parameters.GraphSetting;
import wasdi.shared.rabbit.Send;
import wasdi.snapopearations.WasdiGraph;

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
	
	public static void main(String[] args) throws GraphException, NumberFormatException, IOException {
		/*
		File graphXmlFile = new File("c:/temp/wasdi/MultiGraph.xml");
		File inputFile = new File("C:\\Temp\\wasdi\\data\\paolo\\2c1271a4-9e2b-4291-aabd-caf3074adb25\\S1A_IW_GRDH_1SDV_20180129T052722_20180129T052747_020365_022CA8_9D99.zip");
		File outputFile = new File("C:\\Temp\\wasdi\\testchartout.dim");
		 */
		
//		Product oTest = ProductIO.readProduct("C:\\Temp\\wasdi\\data\\paolo\\2c1271a4-9e2b-4291-aabd-caf3074adb25\\S1A_IW_GRDH_1SDV_20180129T052722_20180129T052747_020365_022CA8_9D99.zip");	
		System.setProperty("user.home", "C:\\Users\\p.campanella.FADEOUT");
        Path propFile = Paths.get("C:\\Codice\\Progetti\\WASDI\\server\\launcher\\target\\config.properties");
        Config.instance("snap.auxdata").load(propFile);
        Config.instance().load();
        
		MongoRepository.SERVER_ADDRESS = ConfigReader.getPropValue("MONGO_ADDRESS");
        MongoRepository.SERVER_PORT = Integer.parseInt(ConfigReader.getPropValue("MONGO_PORT"));
        MongoRepository.DB_NAME = ConfigReader.getPropValue("MONGO_DBNAME");
        MongoRepository.DB_USER = ConfigReader.getPropValue("MONGO_DBUSER");
        MongoRepository.DB_PWD = ConfigReader.getPropValue("MONGO_DBPWD");


        //JAI.getDefaultInstance().getTileScheduler().setParallelism(Runtime.getRuntime().availableProcessors());
        //MemUtils.configureJaiTileCache();
        
        SystemUtils.init3rdPartyLibs(null);
        Engine.start(false);
        
        try {
            GraphParameter oParam = new GraphParameter();
            oParam.setDestinationProductName("testchartout.dim");
            oParam.setProcessObjId("123");
            oParam.setSourceProductName("S1A_IW_GRDH_1SDV_20180129T052722_20180129T052747_020365_022CA8_9D99.zip");
            oParam.setUserId("paolo");
            oParam.setWorkspace("2c1271a4-9e2b-4291-aabd-caf3074adb25");
            oParam.setExchange("2c1271a4-9e2b-4291-aabd-caf3074adb25");
            
            GraphSetting oSettings = new GraphSetting();
            
    		FileInputStream fileInputStream = new FileInputStream("c:/temp/wasdi/MultiGraph2.xml");
    		String sGraphXml = IOUtils.toString(fileInputStream, Charset.defaultCharset().name());
            oSettings.setGraphXml(sGraphXml);
            
            oParam.setSettings(oSettings);

			WasdiGraph oWasdiGraph = new WasdiGraph(oParam, new Send("amq.topic"));
			oSettings.setInputNodeNames(oWasdiGraph.getInputNodes());
			oSettings.setOutputNodeNames(oWasdiGraph.getOutputNodes());
			oSettings.getInputFileNames().add("S1A_IW_GRDH_1SDV_20180129T052722_20180129T052747_020365_022CA8_9D99.zip");
			oSettings.getInputFileNames().add("S2A_MSIL1C_20180102T102421_N0206_R065_T32TMQ_20180102T123237.zip");
			//oSettings.getOutputFileNames().add("");
			//oSettings.getOutputFileNames().add("");
			
			oWasdiGraph.execute();
		} catch (Exception e) {
			System.out.println(e.toString());
		}
		
        /*
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
		*/
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
