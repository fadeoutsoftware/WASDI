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
				"<wps:Execute service=\"WPS\" version=\"1.0.0\" 	xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" 	xmlns:ows=\"http://www.opengis.net/ows/1.1\" 	xmlns:xlink=\"http://www.w3.org/1999/xlink\" 	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" 	xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 	  http://schemas.opengis.net/wps/1.0.0/wpsExecute_request.xsd\"> " + 
				"	<ows:Identifier>gs:WASDIHello</ows:Identifier> " + 
				"	<wps:DataInputs> " + 
				"		<wps:Input> " + 
				"			<ows:Identifier>name</ows:Identifier> " + 
				"			<wps:Data> " + 
				"				<wps:LiteralData>ciaoCiaoCiao</wps:LiteralData> " + 
				"			</wps:Data> " + 
				"		</wps:Input> " + 
				"	</wps:DataInputs> " + 
				"	<wps:ResponseForm> " + 
				"		<wps:ResponseDocument storeExecuteResponse=\"true\" 	lineage=\"false\" status=\"true\"> " + 
				"			<wps:Output> " + 
				"				<ows:Identifier>result</ows:Identifier> " + 
				"			</wps:Output> " + 
				"		</wps:ResponseDocument> " + 
				"	</wps:ResponseForm> " + 
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
				"	<ows:Identifier>org.n52.wps.server.algorithm.SimpleBufferAlgorithm</ows:Identifier> " + 
				"	<wps:Input id=\"data\"> " + 
				"		<wps:Reference schema=\"http://schemas.opengis.net/gml/3.1.1/base/feature.xsd\" xlink:href=\"http://geoprocessing.demo.52north.org:8080/geoserver/wfs?SERVICE=WFS&amp;VERSION=1.0.0&amp;REQUEST=GetFeature&amp;TYPENAME=topp:tasmania_roads&amp;SRS=EPSG:4326&amp;OUTPUTFORMAT=GML3\"/> " + 
				"	</wps:Input> " + 
				"	<wps:Input id=\"width\"> " + 
				"		<wps:Data> " + 
				"			<wps:LiteralValue>0.05</wps:LiteralValue> " + 
				"		</wps:Data> " + 
				"	</wps:Input> " + 
				"	<wps:Output id=\"result\" transmission=\"value\"/> " + 
				"</wps:Execute>";
		oN52DemoClient.setM_sXmlPayload(sN52Payload);
		iExec = oN52DemoClient.execute();
		sStatus = oN52DemoClient.getStatus();


	}
}
