package wasdi.shared.opensearch;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.abdera.Abdera;
import org.apache.abdera.i18n.templates.Template;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Link;
import org.apache.abdera.parser.Parser;
import org.apache.abdera.parser.ParserOptions;
import org.apache.abdera.protocol.Response.ResponseType;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.protocol.client.RequestOptions;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import wasdi.shared.viewmodels.QueryResultViewModel;

public class QueryExecutorPROBAV extends QueryExecutor  {

	@Override
	protected String[] getUrlPath() {
		return new String[] {"openSearch/findProducts"};
	}

	@Override
	protected Template getTemplate() {
		//http://www.vito-eodata.be/openSearch/findProducts?collection=urn:ogc:def:EOP:VITO:PROBAV_S1-TOC_1KM_V001&start=2016-05-05T00:00&end=2016-05-14T23:59&bbox=-180.00,0.00,180.00,90.00
		return new Template("{scheme}://{-append|.|host}www.vito-eodata.be{-opt|/|path}{-listjoin|/|path}{-prefix|/|page}{-opt|?|q}{-join|&|q,start,rows,orderby}");
	}

	@Override
	protected String getCountUrl(String sQuery) {
		//http://www.vito-eodata.be/openSearch/findProducts?collection=urn:ogc:def:EOP:VITO:PROBAV_S1-TOC_1KM_V001&start=2016-05-05T00:00&end=2016-05-14T23:59&bbox=-180.00,0.00,180.00,90.00
		return "http://www.vito-eodata.be/openSearch/count?filter=" + sQuery;
	}

	@Override
	protected String buildUrl(String sQuery){
		//Template oTemplate = getTemplate();
		//Map<String,Object> oParamsMap = new HashMap<String, Object>();		
		//oParamsMap.put("scheme", getUrlSchema());
		//oParamsMap.put("path", getUrlPath());
		//oParamsMap.put("start", m_sOffset);
		//oParamsMap.put("rows", m_sLimit);
		//oParamsMap.put("orderby", m_sSortedBy + " " + m_sOrder);
		//oParamsMap.put("q", sQuery);

		String sUrl = "http://www.vito-eodata.be/openSearch/findProducts?";
		String polygon = null;
		String date = null;
		String collection = null;
		String cloudCover = null;
		String snowCover = null;
		String[] asFootprint = sQuery.split("AND");
		if (asFootprint.length > 0)
		{
			for (String item : asFootprint) {
				if (item.contains("POLYGON"))
				{
					String refString = item;
					String[] asPolygon = refString.split("POLYGON\\(\\(");
					if (asPolygon.length > 0)
					{
						String[] asCoordinates = asPolygon[1].split("\\)\\)")[0].split(",");
						String sPoints = "";
						for (String sCoord : asCoordinates) {
							if (!sPoints.equals(""))
								sPoints+=",";
							sPoints += sCoord.replace(" ", ",");
							
						}
						polygon = String.format("geometry=polygon((%s))", sPoints);
						
						/*	
						double maxX;
						double minX;
						double maxY;
						double minY;
						//init
						String[] asCordinate = asCoordinates[0].split(" ");
						maxX = Double.parseDouble(asCordinate[0]);
						minX = Double.parseDouble(asCordinate[0]);
						maxY = Double.parseDouble(asCordinate[1]);
						minY = Double.parseDouble(asCordinate[1]);
						for (String sCoord : asCoordinates) {
							asCordinate = sCoord.split(" ");
							maxX = Math.max(maxX, Double.parseDouble(asCordinate[0]));
							minX = Math.min(minX, Double.parseDouble(asCordinate[0]));
							maxY = Math.max(maxY, Double.parseDouble(asCordinate[1]));
							minY = Math.min(minY, Double.parseDouble(asCordinate[1]));
						}
					 	*/
						//bbox = String.format("bbox=%s,%s,%s,%s", String.valueOf(minX), String.valueOf(minY), String.valueOf(maxX), String.valueOf(maxY));
						//refString = asPolygon[1].split("\\)\\)")[0].replace(" ", ",");
						
					}
				}

				if (item.contains("beginPosition"))
				{
					String refString = item;
					String[] asDate = refString.split("\\[")[1].split("\\]")[0].split("TO");
					String startdate = asDate[0].trim().substring(0, 16);
					String enddate = asDate[1].trim().substring(0, 16);

					date = String.format("start=%s&end%s", startdate, enddate);
				}

				if (item.contains("collection"))
				{
					String refString = item;
					String[] asNameColletion = refString.split(":", 2)[1].split("\\)");
					collection = String.format("collection=%s",asNameColletion[0]);
				}

				
				if (item.contains("cloudcoverpercentage"))
				{
					String refString = item;
					String[] asNameColletion = refString.split(":", 2)[1].split("\\)");
					cloudCover = String.format("cloudCover=[0,%s]",asNameColletion[0]);
				}

				if (item.contains("snowcoverpercentage"))
				{
					String refString = item;
					String[] asNameColletion = refString.split(":", 2)[1].split("\\)");
					snowCover = String.format("snowCover=[0,%s]",asNameColletion[0]);
				}
				
			}

		}
		if (collection != null)
			sUrl+=collection;
		if (polygon != null)
			sUrl+="&" + polygon;
		if (date != null)
			sUrl+="&" + date;
		if (cloudCover != null)
			sUrl+="&" + cloudCover;
		if (snowCover != null)
			sUrl+="&" + snowCover;
		if (this.m_sOffset != null)
			sUrl+="&startIndex=" + this.m_sOffset;
		if (this.m_sLimit != null)
			sUrl+="&count=" + this.m_sLimit;

		//addUrlParams(oParamsMap);
		return sUrl;
	}

	@Override
	public int executeCount(String sQuery) throws IOException
	{
		String sOldLimit = m_sLimit;
		m_sLimit = "0";
		String sUrl = buildUrl(sQuery);
		m_sLimit = sOldLimit;
		
		//create abdera client
		Abdera oAbdera = new Abdera();
		AbderaClient oClient = new AbderaClient(oAbdera);
		oClient.setConnectionTimeout(15000);
		oClient.setSocketTimeout(40000);
		oClient.setConnectionManagerTimeout(20000);
		oClient.setMaxConnectionsTotal(200);
		oClient.setMaxConnectionsPerHost(50);

		// get default request option
		RequestOptions oOptions = oClient.getDefaultRequestOptions();

		// build the parser
		Parser oParser = oAbdera.getParser();
		ParserOptions oParserOptions = oParser.getDefaultParserOptions();
		oParserOptions.setCharset("UTF-8");
		//options.setCompressionCodecs(CompressionCodec.GZIP);
		oParserOptions.setFilterRestrictedCharacterReplacement('_');
		oParserOptions.setFilterRestrictedCharacters(true);
		oParserOptions.setMustPreserveWhitespace(false);
		oParserOptions.setParseFilter(null);
		// set authorization
		if (m_sUser!=null && m_sPassword!=null) {
			String sUserCredentials = m_sUser + ":" + m_sPassword;
			String sBasicAuth = "Basic " + Base64.getEncoder().encodeToString(sUserCredentials.getBytes());
			oOptions.setAuthorization(sBasicAuth);			
		}

		//		System.out.println("\nSending 'GET' request to URL : " + sUrl);
		ClientResponse response = oClient.get(sUrl, oOptions);

		Document<Feed> oDocument = null;


		if (response.getType() != ResponseType.SUCCESS) {
			System.out.println("Response ERROR: " + response.getType());
			return 0;
		}

		System.out.println("Response Success");		

		// Get The Result as a string
		BufferedReader oBuffRead = new BufferedReader(response.getReader());
		String sResponseLine = null;
		StringBuilder oResponseStringBuilder = new StringBuilder();
		while ((sResponseLine = oBuffRead.readLine()) != null) {
			oResponseStringBuilder.append(sResponseLine);
		}

		String sResultAsString = oResponseStringBuilder.toString();

		//		System.out.println(sResultAsString);

		oDocument = oParser.parse(new StringReader(sResultAsString), oParserOptions);

		if (oDocument == null) {
			System.out.println("OpenSearchQuery.ExecuteQuery: Document response null");
			return 0;
		}

		Feed oFeed = (Feed) oDocument.getRoot();

		String sFeed = oFeed.toString();

		String sTotal = sFeed.substring(sFeed.lastIndexOf("<os:totalResults>") + 17 , sFeed.indexOf("</os:totalResults>"));

		return Integer.valueOf(sTotal);

	}

	@Override
	protected ArrayList<QueryResultViewModel> buildResultLightViewModel(Document<Feed> oDocument, AbderaClient oClient, RequestOptions oOptions) {

		Feed oFeed = (Feed) oDocument.getRoot();

		Map<String, String> oMap = getFootprint(oDocument);

		ArrayList<QueryResultViewModel> aoResults = new ArrayList<QueryResultViewModel>();

		for (Entry oEntry : oFeed.getEntries()) {

			System.out.println(oEntry.toString());

			QueryResultViewModel oResult = new QueryResultViewModel();
			oResult.setProvider(m_sProvider);

			//retrive the title
			oResult.setTitle(oEntry.getTitle());			

			//retrive the summary
			oResult.setSummary(oEntry.getSummary());

			//retrieve the id
			oResult.setId(oEntry.getId().toString());

			//retrieve the link
			Link oLink = oEntry.getEnclosureLink();
			if (oLink != null)oResult.setLink(oLink.getHref().toString()); //TODO

			//retrieve the footprint and all others properties
			oResult.setFootprint(oMap.get(oResult.getId()));
			/*
			List<Element> aoElements = oEntry.getElements();
			for (Element element : aoElements) {
				String sName = element.getAttributeValue("name");
				if (sName != null) {
					if (sName.equals("footprint")) {
						oResult.setFootprint(element.getText());
					} else {
						oResult.getProperties().put(sName, element.getText());
					}
				}
			}
			*/

			oResult.setPreview(null);

			aoResults.add(oResult);
		} 

		System.out.println("Search Done: found " + aoResults.size() + " results");

		return aoResults;
	}

	@Override
	protected ArrayList<QueryResultViewModel> buildResultViewModel(Document<Feed> oDocument, AbderaClient oClient, RequestOptions oOptions) {
		int iStreamSize = 1000000;
		Feed oFeed = (Feed) oDocument.getRoot();

		Map<String, String> oMap = getFootprint(oDocument);
		
		//set new connction timeout
		oClient.setConnectionTimeout(2000);
		//oClient.setSocketTimeout(2000);
		oClient.setConnectionManagerTimeout(2000);

		ArrayList<QueryResultViewModel> aoResults = new ArrayList<QueryResultViewModel>();

		for (Entry oEntry : oFeed.getEntries()) {

			QueryResultViewModel oResult = new QueryResultViewModel();
			oResult.setProvider(m_sProvider);
			//			System.out.println("Parsing new Entry");

			//retrive the title
			oResult.setTitle(oEntry.getTitle());			

			//retrive the summary
			oResult.setSummary(oEntry.getSummary());

			//retrieve the id
			oResult.setId(oEntry.getId().toString());

			//retrieve the link
			//			List<Link> aoLinks = oEntry.getLinks();
			Link oLink = oEntry.getEnclosureLink();
			if (oLink != null)oResult.setLink(oLink.getHref().toString()); //TODO

			//retrieve the footprint and all others properties
			oResult.setFootprint(oMap.get(oResult.getId()));
			
			/*
			List<Element> aoElements = oEntry.getElements();
			for (Element element : aoElements) {
				String sName = element.getAttributeValue("name");
				if (sName != null) {
					if (sName.equals("footprint")) {
						oResult.setFootprint(element.getText());
					} else {
						oResult.getProperties().put(sName, element.getText());
					}
				}
			}
			 */

			//retrieve the icon
			oLink = oEntry.getLink("icon");			
			if (oLink != null) {
				//				System.out.println("Icon Link: " + oLink.getHref().toString());

				try {
					ClientResponse oImageResponse = oClient.get(oLink.getHref().toString(), oOptions);
					//					System.out.println("Response Got from the client");
					if (oImageResponse.getType() == ResponseType.SUCCESS)
					{
						//						System.out.println("Success: saving image preview");
						InputStream oInputStreamImage = oImageResponse.getInputStream();
						BufferedImage  oImage = ImageIO.read(oInputStreamImage);
						ByteArrayOutputStream bas = new ByteArrayOutputStream();
						ImageIO.write(oImage, "png", bas);
						oResult.setPreview("data:image/png;base64," + Base64.getEncoder().encodeToString((bas.toByteArray())));
						//						System.out.println("Image Saved");
					}				
				}
				catch (Exception e) {
					System.out.println("Image Preview Cycle Exception " + e.toString());
				}					
			}
			else {
				System.out.println("Link Not Available" );
			}

			aoResults.add(oResult);
		} 

		System.out.println("Search Done: found " + aoResults.size() + " results");

		return aoResults;
	}
	
	private Map<String, String> getFootprint(Document<Feed> oDocument)
	{
		Map<String, String> oMap = new HashMap<String, String>();
		
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		try {
			oDocument.getRoot().writeTo(outStream);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		InputStream reader = new ByteArrayInputStream(outStream.toByteArray());
		DocumentBuilderFactory f = 
				DocumentBuilderFactory.newInstance();
		DocumentBuilder b;
		try {
			b = f.newDocumentBuilder();
			org.w3c.dom.Document doc = b.parse(reader);
			doc.getDocumentElement().normalize();
			System.out.println ("Root element: " + 
					doc.getDocumentElement().getNodeName());
			// loop through each item
			doc.getDocumentElement().toString();
			NodeList items = doc.getChildNodes().item(0).getChildNodes();
			for (int i = 0; i < items.getLength(); i++)
			{
				if (items.item(i).getNodeName().equals("atom:entry"))
				{
					String sEntryId = "";
					String sFootprint = "";
					for (int k = 0; k < items.item(i).getChildNodes().getLength(); k++)
					{
						
						Node oPolygon = items.item(i).getChildNodes().item(k);
						
						if (oPolygon.getNodeName().equals("atom:id"))
						{
							sEntryId = oPolygon.getChildNodes().item(0).getNodeValue();
							oMap.put(sEntryId, null);
						}
						if (oPolygon.getNodeName().equals("georss:polygon"))
						{
							String points = oPolygon.getChildNodes().item(0).getNodeValue();
							String[] asPoints = points.split(" ");
							for(int iPointCount = 0;iPointCount < asPoints.length;iPointCount=iPointCount+2)
							{
								if (iPointCount + 1 < asPoints.length)
								{
									//switch lat and lon
									sFootprint += String.format("%s %s", asPoints[iPointCount + 1], asPoints[iPointCount]);
									
									if (iPointCount + 2 < asPoints.length)
										sFootprint += ",";
								}
							}
							
							sFootprint = String.format("POLYGON ((%s))", sFootprint);
							
							
						}
					}
					
					if (sEntryId != null)
					{
						oMap.put(sEntryId, sFootprint);
					}

				}
			}

		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return oMap;
	}

}
