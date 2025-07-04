package wasdi.shared.queryexecutors.probav;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.opensearch.QueryExecutorOpenSearch;
import wasdi.shared.utils.StringUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryResultViewModel;
import wasdi.shared.viewmodels.search.QueryViewModel;

/**
 * Query Executor for the VITO ProbaV Mission.
 * 
 * @author p.campanella
 *
 */
public class QueryExecutorPROBAV extends QueryExecutorOpenSearch  {

	public QueryExecutorPROBAV(){
		m_bUseBasicAuthInHttpQuery = false;
		m_oQueryTranslator = new QueryTranslatorProbaV();
		
	}

	@Override
	protected String[] getUrlPath() {
		return new String[] {"openSearch/findProducts"};
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
			
			PaginatedQuery oQuery = new PaginatedQuery(sQuery, null, null, null, null);
			String sUrl = getSearchUrl(oQuery);
			
			WasdiLog.debugLog("QueryExecutorPROBAV.executeCount");
			QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(sQuery);
			
			if (!m_asSupportedPlatforms.contains(oQueryViewModel.platformName)) {
				return -1;
			}
			
			return super.executeCount(sUrl);
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("QueryExecutorProbaV.executeCount: Exception " + oEx.toString());
		}
		
		return -1;
	}
	
	@Override
	protected String extractNumberOfResults(String sResponse) {
		if (Utils.isNullOrEmpty(sResponse)) {
			return "";
		}
		
		try {
			String sTotal = sResponse.substring(sResponse.lastIndexOf("<os:totalResults>") + 17 , sResponse.indexOf("</os:totalResults>"));
			return sTotal;
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("QueryExecutorProbaV.extractNumberOfResults: Exception " + oEx.toString());
		}
		return "";
	}

	@Override
	protected List<QueryResultViewModel> buildResultLightViewModel(Document oDocument, String sXml) {

		WasdiLog.debugLog("QueryExecutorPROBAV.buildResultLightViewModel");

		ArrayList<QueryResultViewModel> aoResults = new ArrayList<QueryResultViewModel>();
		
//		Feed oFeed = (Feed) oDocument.getRoot();
//
//		Map<String, String> oMap = getFootprint(sXml);		
//
//		for (Entry oEntry : oFeed.getEntries()) {
//
//			WasdiLog.debugLog(oEntry.toString());
//
//			QueryResultViewModel oResult = new QueryResultViewModel();
//			oResult.setProvider(m_sProvider);
//			
//			String sTitle = extractTitle(oEntry.getTitle());
//			
//			//retrive the title
//			oResult.setTitle(sTitle);			
//
//			//retrive the summary
//			oResult.setSummary(oEntry.getSummary());
//
//			//retrieve the id
//			oResult.setId(oEntry.getId().toString());
//
//			//retrieve the link
//			Link oLink = oEntry.getEnclosureLink();
//			if (oLink != null)oResult.setLink(oLink.getHref().toString());
//
//			//retrieve the footprint and all others properties
//			oResult.setFootprint(oMap.get(oResult.getId()));
//
//			oResult.setPreview(null);
//
//			aoResults.add(oResult);
//		} 

		WasdiLog.debugLog("Search Done: found " + aoResults.size() + " results");

		return aoResults;
	}

	@Override
	protected List<QueryResultViewModel> buildResultViewModel(Document oDocument, String sXml) {

		WasdiLog.debugLog("QueryExecutorPROBAV.buildResultViewModel");
		
		ArrayList<QueryResultViewModel> aoResults = new ArrayList<QueryResultViewModel>();
		
//		//int iStreamSize = 1000000;
//		Feed oFeed = (Feed) oDocument.getRoot();
//
//		Map<String, String> oMap = getFootprint(sXml);
//
//		//set new connction timeout
//		oClient.setConnectionTimeout(2000);
//		//oClient.setSocketTimeout(2000);
//		oClient.setConnectionManagerTimeout(2000);
//		
//
//		for (Entry oEntry : oFeed.getEntries()) {
//
//			QueryResultViewModel oResult = new QueryResultViewModel();
//			oResult.setProvider(m_sProvider);
//			
//			String sTitle = extractTitle(oEntry.getTitle());
//						
//			//retrive the title
//			oResult.setTitle(sTitle);			
//
//			//retrive the summary
//			oResult.setSummary(oEntry.getSummary());
//
//			//retrieve the id
//			oResult.setId(oEntry.getId().toString());
//
//			//retrieve the link
//			//			List<Link> aoLinks = oEntry.getLinks();
//			Link oLink = oEntry.getEnclosureLink();
//			if (oLink != null)oResult.setLink(oLink.getHref().toString()); //TODO
//
//			//retrieve the footprint and all others properties
//			oResult.setFootprint(oMap.get(oResult.getId()));
//
//			//retrieve the icon
//			oLink = oEntry.getLink("icon");			
//			if (oLink != null) {
//				//				WasdiLog.debugLog("Icon Link: " + oLink.getHref().toString());
//
//				try {
//					IRI oIri = oLink.getHref();
//					String sHref = oIri.toString();
//					ClientResponse oImageResponse = oClient.get(sHref, oOptions);
//					//					WasdiLog.debugLog("Response Got from the client");
//					if (oImageResponse.getType() == ResponseType.SUCCESS)
//					{
//						//						WasdiLog.debugLog("Success: saving image preview");
//						InputStream oInputStreamImage = oImageResponse.getInputStream();
//						BufferedImage  oImage = ImageIO.read(oInputStreamImage);
//						ByteArrayOutputStream bas = new ByteArrayOutputStream();
//						ImageIO.write(oImage, "png", bas);
//						oResult.setPreview("data:image/png;base64," + Base64.getEncoder().encodeToString((bas.toByteArray())));
//						//						WasdiLog.debugLog("Image Saved");
//					}				
//				}
//				catch (Exception e) {
//					WasdiLog.debugLog("Image Preview Cycle Exception " + e.toString());
//				}					
//			}
//
//			aoResults.add(oResult);
//		} 

		WasdiLog.debugLog("Search Done: found " + aoResults.size() + " results");

		return aoResults;
	}

	private Map<String, String> getFootprint(String sXml){
		WasdiLog.debugLog("QueryExecutorPROBAV.getFootprint");
		Map<String, String> oMap = new HashMap<String, String>();
		
		InputStream oInputStreamReader = new ByteArrayInputStream(sXml.getBytes());
		DocumentBuilderFactory oDocBuildFactory = DocumentBuilderFactory.newInstance();
		
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
			WasdiLog.debugLog("QueryExecutorPROBAV.getFootprint( " + sXml + " ): " + oE1);
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
