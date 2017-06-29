
//import publish.Publisher;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;

import wasdi.LauncherMain;
import wasdi.shared.LauncherOperations;
import wasdi.shared.parameters.GraphParameter;
import wasdi.shared.parameters.GraphSetting;
import wasdi.shared.utils.SerializationUtils;
import wasdi.snapopearations.ReadProduct;

/**
 * Created by s.adamo on 18/05/2016.
 */
public class Test {

	public static void main(String[] args) throws Exception {

		
		String s = new ReadProduct().getProductBoundingBox(new File("/home/doy/tmp/wasdi/tmp/CSKS4_SCS_B_HI_05_HH_RA_SF_20141130050141_20141130050148.h5"));
		
		
		
		
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
		
		System.out.println("CIAO");
		
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
