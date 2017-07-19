package wasdi.mulesme;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.log4j.Logger;
import org.esa.snap.core.dataio.dimap.DimapProductWriterPlugIn;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphContext;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.esa.snap.core.gpf.graph.GraphProcessor;
import org.esa.snap.core.gpf.graph.Node;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Config;
import org.esa.snap.runtime.Engine;

import com.bc.ceres.core.ProgressMonitor;

import wasdi.ConfigReader;
import wasdi.snapopearations.WasdiGraph;

public class GraphManager {
	
	
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
	
	public static void main(String[] args) throws Exception{
		
		
		System.setProperty("user.home", ConfigReader.getPropValue("USER_HOME"));
        Path propFile = Paths.get(ConfigReader.getPropValue("SNAP_AUX_PROPERTIES"));
        Config.instance("snap.auxdata").load(propFile);
        Config.instance().load();
        SystemUtils.init3rdPartyLibs(null);
        Engine.start(false);
		
//		System.setProperty("user.home", "/home/doy");
//		Path propFile = Paths.get("/home/doy/workspaces/wasdi/server/launcher/target/config.properties");
//		Config.instance("snap.auxdata").load(propFile);
//		Config.instance().load();
//		SystemUtils.init3rdPartyLibs(null);
//		Engine.start(false);
		
		String dateYesterday = args[0];
		
		File inputDir = new File(args[2]);
		
		File outputDir = new File(args[3]);
		
		File[] files = inputDir.listFiles(new FilenameFilter() {			
			@Override
			public boolean accept(File dir, String name) {
				return name.contains("S1") && name.contains("GRD") && name.contains(dateYesterday);
			}
		});
		
		for (File inputFile : files) {
			
			String outFileName = inputFile.getName().split("\\.")[0];
			File outputFile = new File(outputDir, outFileName);
			
			System.out.println("applying graph to " + inputFile.getAbsolutePath() + " --> " + outputFile.getAbsolutePath());
			
			try {
				Graph graph = GraphIO.read(new FileReader(new File(args[1])));
				
				Node nodeReader = graph.getNode("Read");
				Node nodeWriter = graph.getNode("Write");
				
				if (nodeReader==null || nodeWriter==null) {
					System.out.println("WasdiGraph.execute: Reader node and Wroter node are mandatory!!");
					throw new Exception("Reader node and Wroter node are mandatory");
				}		
				if (!WasdiGraph.setNodeValue(nodeReader, "file", inputFile.getAbsolutePath()) || 
						!WasdiGraph.setNodeValue(nodeWriter, "file", outputFile.getAbsolutePath()) ||
						!WasdiGraph.setNodeValue(nodeWriter, "formatName", DimapProductWriterPlugIn.DIMAP_FORMAT_NAME) ) {
					throw new Exception("Error setting input/output file");
				}

				
				GraphContext context = new GraphContext(graph);		
				GraphProcessor processor = new GraphProcessor();
				
				processor.executeGraph(context, new MyProgressMonitor());
				
				context.dispose();
			} catch (Exception e) {
				e.printStackTrace();				
			}
			
		}
		
	}
	
}
