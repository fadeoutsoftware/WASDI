/**
 * Created by Cristiano Nattero on 2019-02-25
 * 
 * Fadeout software
 *
 */


import wasdi.wps.OLD_WpsAdapter;
import wasdi.wps.OLD_WpsFactory;

/**
 * @author c.nattero
 *
 */
public class TestWps_wasdi {

	public static void main(String[] args) throws Exception {
		OLD_WpsFactory oFactory = new OLD_WpsFactory();
		OLD_WpsAdapter oWasdiDemoClient = oFactory.supply("wasdi");

		String sWasdiPayload = 
				"<wps:Execute service=\"WPS\" version=\"1.0.0\" \txmlns:wps=\"http://www.opengis.net/wps/1.0.0\" \txmlns:ows=\"http://www.opengis.net/ows/1.1\" \txmlns:xlink=\"http://www.w3.org/1999/xlink\" \txmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \txsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 \t  http://schemas.opengis.net/wps/1.0.0/wpsExecute_request.xsd\"> " + 
				"\t<ows:Identifier>gs:WASDIHello</ows:Identifier> " + 
				"\t<wps:DataInputs> " + 
				"\t\t<wps:Input> " + 
				"\t\t\t<ows:Identifier>name</ows:Identifier> " + 
				"\t\t\t<wps:Data> " + 
				"\t\t\t\t<wps:LiteralData>ciaoCiaoCiao</wps:LiteralData> " + 
				"\t\t\t</wps:Data> " + 
				"\t\t</wps:Input> " + 
				"\t</wps:DataInputs> " + 
				"\t<wps:ResponseForm> " + 
				"\t\t<wps:ResponseDocument storeExecuteResponse=\"true\" \tlineage=\"false\" status=\"true\"> " + 
				"\t\t\t<wps:Output> " + 
				"\t\t\t\t<ows:Identifier>result</ows:Identifier> " + 
				"\t\t\t</wps:Output> " + 
				"\t\t</wps:ResponseDocument> " + 
				"\t</wps:ResponseForm> " + 
				"</wps:Execute>";
			
		oWasdiDemoClient.setM_sXmlPayload(sWasdiPayload);
		int iExec = oWasdiDemoClient.execute();
		String sStatus = oWasdiDemoClient.getStatus();

		OLD_WpsAdapter oN52DemoClient = oFactory.supply("n52Demo");
		String sN52Payload =
					"<wps:Execute xmlns:wps=\"http://www.opengis.net/wps/2.0\" " + 
					" xmlns:ows=\"http://www.opengis.net/ows/2.0\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" " + 
					"       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " + 
					"       xsi:schemaLocation=\"http://www.opengis.net/wps/2.0 ../wps.xsd\" service=\"WPS\" " + 
					"       version=\"2.0.0\" response=\"document\" mode=\"sync\"> " + 
					"\t<ows:Identifier>org.n52.wps.server.algorithm.SimpleBufferAlgorithm</ows:Identifier> " + 
					"\t<wps:Input id=\"data\"> " + 
					"\t\t<wps:Reference schema=\"http://schemas.opengis.net/gml/3.1.1/base/feature.xsd\" xlink:href=\"http://geoprocessing.demo.52north.org:8080/geoserver/wfs?SERVICE=WFS&amp;VERSION=1.0.0&amp;REQUEST=GetFeature&amp;TYPENAME=topp:tasmania_roads&amp;SRS=EPSG:4326&amp;OUTPUTFORMAT=GML3\"/> " + 
					"\t</wps:Input> " + 
					"\t<wps:Input id=\"width\"> " + 
					"\t\t<wps:Data> " + 
					"\t\t\t<wps:LiteralValue>0.05</wps:LiteralValue> " + 
					"\t\t</wps:Data> " + 
					"\t</wps:Input> " + 
					"\t<wps:Output id=\"result\" transmission=\"value\"/> " + 
					"</wps:Execute>";
		oN52DemoClient.setM_sXmlPayload(sN52Payload);
		iExec = oN52DemoClient.execute();
		sStatus = oN52DemoClient.getStatus();


	}
	
}
