package wasdi.shared.queryexecutors.probav;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.abdera.Abdera;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.i18n.templates.Template;
import org.apache.abdera.model.Document;
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

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.opensearch.QueryExecutorOpenSearch;
import wasdi.shared.utils.StringUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryResultViewModel;

/**
 * Query Executor for the VITO ProbaV Mission.
 * 
 * @author p.campanella
 *
 */
public class QueryExecutorPROBAV extends QueryExecutorOpenSearch  {

	public QueryExecutorPROBAV(){
		m_sProvider = "PROBAV";
		m_bUseBasicAuthInHttpQuery = false;
		m_oQueryTranslator = new QueryTranslatorProbaV();
		
	}

	@Override
	protected String[] getUrlPath() {
		return new String[] {"openSearch/findProducts"};
	}

	@Override
	protected Template getTemplate() {
		// Sample
		//http://www.vito-eodata.be/openSearch/findProducts?collection=urn:ogc:def:EOP:VITO:PROBAV_S1-TOC_1KM_V001&start=2016-05-05T00:00&end=2016-05-14T23:59&bbox=-180.00,0.00,180.00,90.00
		return new Template("{scheme}://{-append|.|host}www.vito-eodata.be{-opt|/|path}{-listjoin|/|path}{-prefix|/|page}{-opt|?|q}{-join|&|q,start,rows,orderby}");
	}

	@Override
	protected String getCountUrl(String sQuery) {
		//http://www.vito-eodata.be/openSearch/findProducts?collection=urn:ogc:def:EOP:VITO:PROBAV_S1-TOC_1KM_V001&start=2016-05-05T00:00&end=2016-05-14T23:59&bbox=-180.00,0.00,180.00,90.00
		return "http://www.vito-eodata.be/openSearch/count?filter=" + sQuery;
	}

	@Override
	protected String getSearchUrl(PaginatedQuery oQuery){
		WasdiLog.debugLog("QueryExecutorPROBAV.buildUrl");

		String sUrl = "https://services.terrascope.be/catalogue/products?";
		String sPolygon = null;
		String sDate = null;
		String sCollection = null;
		String sCloudCover = null;
		String sSnowCover = null;
		String[] asFootprint = oQuery.getQuery().split("AND");
		if (asFootprint.length > 0)
		{
			for (String sItem : asFootprint) {
				if (sItem.contains("POLYGON"))
				{
					String sRefString = sItem;
					String[] asPolygon = sRefString.split("POLYGON\\(\\(");
					if (asPolygon.length > 0)
					{
						String[] asCoordinates = asPolygon[1].split("\\)\\)");
						asCoordinates[0] = StringUtils.encodeUrl(asCoordinates[0]);
												
						sPolygon = String.format("geometry=POLYGON((%s))", asCoordinates[0]);
					}
				}

				if (sItem.contains("beginPosition"))
				{
					String sRefString = sItem;
					String[] asDate = sRefString.split("\\[")[1].split("\\]")[0].split("TO");
					String sStartdate = asDate[0].trim().substring(0, 16);
					String sEnddate = asDate[1].trim().substring(0, 16);

					sDate = String.format("start=%s:00&end=%s:00", sStartdate, sEnddate);
				}

				if (sItem.contains("collection"))
				{
					String sRefString = sItem;
					String[] asNameColletion = sRefString.split(":", 2)[1].split("\\)");
					sCollection = String.format("collection=%s",asNameColletion[0]);
				}


				if (sItem.contains("cloudcoverpercentage"))
				{
					String sRefString = sItem;
					String[] asNameColletion = sRefString.split(":", 2)[1].split("\\)");
					sCloudCover = String.format("cloudCover=[0,%s]",asNameColletion[0]);
				}

				if (sItem.contains("snowcoverpercentage"))
				{
					String sRefString = sItem;
					String[] asNameColletion = sRefString.split(":", 2)[1].split("\\)");
					sSnowCover = String.format("snowCover=[0,%s]",asNameColletion[0]);
				}

			}

		}
		if (sCollection != null)
			sUrl+=sCollection;
		if (sPolygon != null)
			sUrl+="&" + sPolygon;
		if (sDate != null)
			sUrl+="&" + sDate;
		if (sCloudCover != null)
			sUrl+="&" + sCloudCover;
		if (sSnowCover != null)
			sUrl+="&" + sSnowCover;
		if (oQuery.getOffset() != null) {
			
			int iStartIndex = 1;
			
			try {
				iStartIndex = Integer.parseInt(oQuery.getOffset());
				iStartIndex++;
			}
			catch (Exception oEx) {
			}
			
			sUrl+="&startIndex=" + iStartIndex;
		}
		if (oQuery.getLimit() != null)
			sUrl+="&count=" + oQuery.getLimit();
		
		sUrl+= "&httpAccept=application%2Fatom%2Bxml";
		
		return sUrl;
	}

	@Override
	public int executeCount(String sQuery)
	{
		try {
			WasdiLog.debugLog("QueryExecutorPROBAV.executeCount");
			PaginatedQuery oQuery = new PaginatedQuery(sQuery, null, null, null, null);
			String sUrl = getSearchUrl(oQuery);

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
			
			if (m_bUseBasicAuthInHttpQuery) {
				// set authorization
				if (m_sUser!=null && m_sPassword!=null) {
					String sUserCredentials = m_sUser + ":" + m_sPassword;
					String sBasicAuth = "Basic " + Base64.getEncoder().encodeToString(sUserCredentials.getBytes());
					oOptions.setAuthorization(sBasicAuth);			
				}				
			}
			
			ClientResponse response = oClient.get(sUrl, oOptions);

			Document<Feed> oDocument = null;

			if (response.getType() != ResponseType.SUCCESS) {
				WasdiLog.debugLog("QueryExecutor.executeCount: Response ERROR: " + response.getType());
				return 0;
			}

			WasdiLog.debugLog("Response Success");		

			// Get The Result as a string
			BufferedReader oBuffRead = new BufferedReader(response.getReader());
			String sResponseLine = null;
			StringBuilder oResponseStringBuilder = new StringBuilder();
			while ((sResponseLine = oBuffRead.readLine()) != null) {
				oResponseStringBuilder.append(sResponseLine);
			}

			String sResultAsString = oResponseStringBuilder.toString();

			//		WasdiLog.debugLog(sResultAsString);

			oDocument = oParser.parse(new StringReader(sResultAsString), oParserOptions);

			if (oDocument == null) {
				WasdiLog.debugLog("OpenSearchQuery.ExecuteQuery: Document response null");
				return 0;
			}

			Feed oFeed = (Feed) oDocument.getRoot();

			String sFeed = oFeed.toString();

			String sTotal = sFeed.substring(sFeed.lastIndexOf("<os:totalResults>") + 17 , sFeed.indexOf("</os:totalResults>"));

			return Integer.valueOf(sTotal);			
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("QueryExecutorProbaV.executeCount: Exception " + oEx.toString());
		}
		
		return -1;
	}

	@Override
	protected List<QueryResultViewModel> buildResultLightViewModel(Document<Feed> oDocument, AbderaClient oClient, RequestOptions oOptions) {

		WasdiLog.debugLog("QueryExecutorPROBAV.buildResultLightViewModel");
		Feed oFeed = (Feed) oDocument.getRoot();

		Map<String, String> oMap = getFootprint(oDocument);

		ArrayList<QueryResultViewModel> aoResults = new ArrayList<QueryResultViewModel>();

		for (Entry oEntry : oFeed.getEntries()) {

			WasdiLog.debugLog(oEntry.toString());

			QueryResultViewModel oResult = new QueryResultViewModel();
			oResult.setProvider(m_sProvider);
			
			String sTitle = extractTitle(oEntry.getTitle());
			
			//retrive the title
			oResult.setTitle(sTitle);			

			//retrive the summary
			oResult.setSummary(oEntry.getSummary());

			//retrieve the id
			oResult.setId(oEntry.getId().toString());

			//retrieve the link
			Link oLink = oEntry.getEnclosureLink();
			if (oLink != null)oResult.setLink(oLink.getHref().toString());

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

		WasdiLog.debugLog("Search Done: found " + aoResults.size() + " results");

		return aoResults;
	}

	@Override
	protected List<QueryResultViewModel> buildResultViewModel(Document<Feed> oDocument, AbderaClient oClient, RequestOptions oOptions) {

		WasdiLog.debugLog("QueryExecutorPROBAV.buildResultViewModel");
		//int iStreamSize = 1000000;
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
			
			String sTitle = extractTitle(oEntry.getTitle());
						
			//retrive the title
			oResult.setTitle(sTitle);			

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

			//retrieve the icon
			oLink = oEntry.getLink("icon");			
			if (oLink != null) {
				//				WasdiLog.debugLog("Icon Link: " + oLink.getHref().toString());

				try {
					IRI oIri = oLink.getHref();
					String sHref = oIri.toString();
					ClientResponse oImageResponse = oClient.get(sHref, oOptions);
					//					WasdiLog.debugLog("Response Got from the client");
					if (oImageResponse.getType() == ResponseType.SUCCESS)
					{
						//						WasdiLog.debugLog("Success: saving image preview");
						InputStream oInputStreamImage = oImageResponse.getInputStream();
						BufferedImage  oImage = ImageIO.read(oInputStreamImage);
						ByteArrayOutputStream bas = new ByteArrayOutputStream();
						ImageIO.write(oImage, "png", bas);
						oResult.setPreview("data:image/png;base64," + Base64.getEncoder().encodeToString((bas.toByteArray())));
						//						WasdiLog.debugLog("Image Saved");
					}				
				}
				catch (Exception e) {
					WasdiLog.debugLog("Image Preview Cycle Exception " + e.toString());
				}					
			}

			aoResults.add(oResult);
		} 

		WasdiLog.debugLog("Search Done: found " + aoResults.size() + " results");

		return aoResults;
	}

	private Map<String, String> getFootprint(Document<Feed> oDocument){
		WasdiLog.debugLog("QueryExecutorPROBAV.getFootprint");
		Map<String, String> oMap = new HashMap<String, String>();

		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		try {
			oDocument.getRoot().writeTo(outStream);
		} catch (IOException oE) {
			String sDocSummary = null;
			if( null != oDocument) {
				sDocSummary = oDocument.toString();
				if(null!=sDocSummary) {
					sDocSummary = sDocSummary.substring(0, 200);
					sDocSummary += "...";
				}
			}
			WasdiLog.debugLog("QueryExecutorPROBAV.getFootprint( " + sDocSummary+ " ): " + oE);
		}
		
		InputStream oInputStreamReader = new ByteArrayInputStream(outStream.toByteArray());
		DocumentBuilderFactory oDocBuildFactory = DocumentBuilderFactory.newInstance();
		//oDocBuildFactory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "");
		//oDocBuildFactory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalSchema", "");
				
		DocumentBuilder oDocBuilder;
		try {
			oDocBuilder = oDocBuildFactory.newDocumentBuilder();
			org.w3c.dom.Document doc = oDocBuilder.parse(oInputStreamReader);
			doc.getDocumentElement().normalize();
			WasdiLog.debugLog("Root element: " + doc.getDocumentElement().getNodeName());
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
									sFootprint += String.format("%s %s", asPoints[iPointCount], asPoints[iPointCount+1]);

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

		} catch (Exception oE1) {
			String sDocSummary = null;
			if( null != oDocument) {
				sDocSummary = oDocument.toString();
				if(null!=sDocSummary) {
					sDocSummary = sDocSummary.substring(0, 200);
					sDocSummary += "...";
				}
			}
			WasdiLog.debugLog("QueryExecutorPROBAV.getFootprint( " + sDocSummary+ " ): " + oE1);
		}
		return oMap;
	}

	public String extractTitle(String sTitle) {
		
		String [] asTitleParts = sTitle.split(":");
		if (asTitleParts!=null) {
			if (asTitleParts.length>6) {
				sTitle = asTitleParts[6];
			}
		}
		
		return sTitle;
	}

}
