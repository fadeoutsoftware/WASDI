
//import publish.Publisher;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Config;
import org.esa.snap.runtime.Engine;

import wasdi.LauncherMain;
import wasdi.shared.LauncherOperations;
import wasdi.shared.parameters.GraphParameter;
import wasdi.shared.parameters.GraphSetting;
import wasdi.shared.utils.SerializationUtils;
import wasdi.snapopearations.ReadProduct;

/**
 * Created by s.adamo on 18/05/2016.
 */
public class Test_BBOX {

	public static void main(String[] args) throws Exception {

		
		Path propFile = Paths.get(args[0]);
        Config.instance("snap.auxdata").load(propFile);
        Config.instance().load();

        //JAI.getDefaultInstance().getTileScheduler().setParallelism(Runtime.getRuntime().availableProcessors());
        //MemUtils.configureJaiTileCache();
        
        SystemUtils.init3rdPartyLibs(null);
        Engine.start(false);

		
		
		//String s = new ReadProduct().getProductBoundingBox(new File("/home/doy/tmp/wasdi/tmp/S2A_MSIL2A_20170626T102021_N0205_R065_T32TMQ_20170626T102321.zip"));
		
		System.out.println(args[0]);
		String s = new ReadProduct().getProductBoundingBox(new File(args[1]));
		
//		String s = "GEOGCS[\"WGS84(DD)\",DATUM[\"WGS84\",SPHEROID[\"WGS84\", 6378137.0, 298.257223563]],PRIMEM[\"Greenwich\", 0.0],UNIT[\"degree\", 0.017453292519943295],AXIS[\"Geodetic longitude\", EAST],AXIS[\"Geodetic latitude\", NORTH]]";
//		s = s.replace("\"", "\\\\\\\"");
//		String ret = String.format("{\"miny\":1,\"minx\":1,\"crs\":\"%s\",\"maxy\":1,\"maxx\":1}", s);
//		System.out.println(ret);
		
//		LauncherMain theMain = new  LauncherMain();
//		
//		File graphXmlFile = new File("/home/doy/tmp/wasdi/graph/myGraph.xml");
//		GraphSetting settings = new GraphSetting();		
//		String graphXml = IOUtils.toString(new FileInputStream(graphXmlFile), Charset.defaultCharset());
//		settings.setGraphXml(graphXml);
//		GraphParameter params = new GraphParameter();
//		params.setSettings(settings);
//		params.setUserId("paolo");
//		params.setExchange("8e91a84c-3dcf-470d-8e36-3ad40de80d54");
//		params.setWorkspace("8e91a84c-3dcf-470d-8e36-3ad40de80d54");		
//		params.setProcessObjId("PPPP");
//		params.setDestinationProductName("S1A_IW_GRDH_1SDV_20160802T051857_20160802T051922_012417_013615_C75B_Graph");
//		params.setSourceProductName("S1A_IW_GRDH_1SDV_20160802T051857_20160802T051922_012417_013615_C75B.zip");
//		
//		theMain.ExecuteGraph(params);
		
		System.out.println(s);
		
//		LauncherMain theMain = new  LauncherMain();
//		IngestFileParameter params = new IngestFileParameter();
//		params.setUserId("paolo");
//		params.setExchange("8e91a84c-3dcf-470d-8e36-3ad40de80d54");
//		params.setWorkspace("8e91a84c-3dcf-470d-8e36-3ad40de80d54");
//		params.setProcessObjId("PPPP");
//		params.setFilePath("/home/doy/tmp/wasdi/tmp/CSKS4_SCS_B_HI_05_HH_RA_SF_20141130050141_20141130050148.h5");
//		theMain.Ingest(params, "/home/doy/tmp/wasdi/data/download");
		
		
    }



}
