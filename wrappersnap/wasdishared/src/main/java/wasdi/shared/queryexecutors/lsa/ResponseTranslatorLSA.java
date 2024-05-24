package wasdi.shared.queryexecutors.lsa;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import wasdi.shared.queryexecutors.ResponseTranslator;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;
import wasdi.shared.viewmodels.search.QueryResultViewModel;

public class ResponseTranslatorLSA extends ResponseTranslator {

	@Override
	public List<QueryResultViewModel> translateBatch(String sJson, boolean bFullViewModel) {
		
		List<QueryResultViewModel> aoResults = new ArrayList<QueryResultViewModel>();
		
		DocumentBuilderFactory oDocBuildFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder oDocBuilder;		
		
		try {
			
			oDocBuilder = oDocBuildFactory.newDocumentBuilder();
			Document oResultsXml = oDocBuilder.parse(new InputSource(new StringReader(sJson)));
			oResultsXml.getDocumentElement().normalize();
			WasdiLog.debugLog("ResponseTranslatorLSA.translateBatch root element: " + oResultsXml.getDocumentElement().getNodeName());
			
			// loop through each Entry item
			NodeList aoItems = oResultsXml.getElementsByTagName("entry");
			
			for (int iItem = 0; iItem < aoItems.getLength(); iItem++)
			{
				// We need an Element Node
				org.w3c.dom.Node oNode = aoItems.item(iItem);
				if (oNode.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE)  continue;
				
				Element oEntry = (Element) oNode;
				
				QueryResultViewModel oResult = new QueryResultViewModel();
				
				// Summary for later
				String sSummary = "";

				// Fixed Data Provider
				oResult.setProvider("LSA");
				
				// Get the Title
				String sTitle = "";
				
				// Search the position (we assume we will receive only one)
				NodeList aoTitleList = oEntry.getElementsByTagName("title");
				
				for (int iTitle = 0; iTitle < aoTitleList.getLength(); iTitle++) {
					org.w3c.dom.Node oTitleNode = aoTitleList.item(iTitle);
					
					if (oTitleNode.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;
					
					Element oTitleEntry = (Element) oTitleNode;
					
					sTitle = oTitleEntry.getTextContent();
					oResult.setTitle(sTitle);					
				}				
				
				// Relative Orbit				
				if (sTitle.startsWith("S1")) {
					
					// Split the title
					String [] asTitleParts = sTitle.split("_");
					
					String sMode = "";
					
					// Safe check
					if (asTitleParts != null) {
						
						if (asTitleParts.length >= 9) {
							
							// Get the absolute orbit
							String sAbsoluteOrbit = asTitleParts[6];
							
							if (sTitle.contains("_SLC_")) {
								sAbsoluteOrbit = asTitleParts[7];
							}
							
							// Cast to int
							int iAbsoluteOrbit = -1;
							try {
								iAbsoluteOrbit = Integer.parseInt(sAbsoluteOrbit);
							}
							catch (Exception oEx) {
								WasdiLog.debugLog("ResponseTranslatorLSA: Exception converting LSA Result Relative Orbit: "  + oEx.toString()) ;
							}
							
							if (iAbsoluteOrbit != -1) {
								
								// Init the relative orbit
								int iRelativeOrbit = -1;
								
								// S1A or S1B?
								if (sTitle.startsWith("S1A")) {
									iRelativeOrbit = (iAbsoluteOrbit-73)%175;
									iRelativeOrbit ++;
								}
								else if (sTitle.startsWith("S1B")) {
									iRelativeOrbit = (iAbsoluteOrbit-27)%175;
									iRelativeOrbit ++;										
								}
								
								// Set the relative orbit to the WASDI View Model
								oResult.getProperties().put("relativeorbitnumber", ""+iRelativeOrbit);
							}
							
							sMode = " Instrument: SAR-C SAR, Mode: "+  asTitleParts[1];
							
							oResult.getProperties().put("sensoroperationalmode", asTitleParts[1]);
						}
					}
					
					sSummary = "Satellite: Sentinel-1, " + sMode;
				}
				else if (sTitle.startsWith("S2")) {
					// Split the title
					String [] asTitleParts = sTitle.split("_");
					
					// Safe check
					if (asTitleParts != null) {
						
						if (asTitleParts.length >= 7) {
							
							// Get the relative orbit
							String sRelativeOrbit = asTitleParts[4];
							
							sRelativeOrbit = sRelativeOrbit.substring(1);
							
							// Cast to int
							int iRelativeOrbit = -1;
							try {
								iRelativeOrbit = Integer.parseInt(sRelativeOrbit);
							}
							catch (Exception oEx) {
								WasdiLog.debugLog("Exception converting LSA Result Relative Orbit: "  + oEx.toString()) ;
							}
							
							if (iRelativeOrbit != -1) {
								// Set the relative orbit to the WASDI View Model
								oResult.getProperties().put("relativeorbitnumber", ""+iRelativeOrbit);
							}
						}
					}
					
					sSummary = "Satellite: Sentinel-2, Mode: MSI, Instrument: INS-NOBS";
				}
				
				if (!Utils.isNullOrEmpty(sTitle)) {
					// Set platform
					oResult.getProperties().put("platform", sTitle.substring(0, 3));
					oResult.getProperties().put("platformname", sTitle.substring(0, 3));					
				}
				
								
				// Get the id
				NodeList aoIdList = oEntry.getElementsByTagName("id");
				
				for (int iId = 0; iId < aoIdList.getLength(); iId++) {
					org.w3c.dom.Node oIdNode = aoIdList.item(iId);
					
					if (oIdNode.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;
					
					Element oIdEntry = (Element) oIdNode;
					
					String sId = oIdEntry.getTextContent();
					oResult.setId(sId);					
				}
				
				// Get the links
				NodeList aoLinks = oEntry.getElementsByTagName("link");
				
				for (int iLink = 0; iLink < aoLinks.getLength(); iLink++) {
					
					org.w3c.dom.Node oLinkNode = aoLinks.item(iLink);
					
					if (oLinkNode.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;
					
					Element oLinkEntry = (Element) oLinkNode;
					
					if (!oLinkEntry.hasAttribute("title")) continue;
						
					// Extract Download link
					if (oLinkEntry.getAttribute("title").equals("Source package download")) {
						String sLink = "https://collgs.lu/" + oLinkEntry.getAttribute("href");
						oResult.setLink(sLink);
					}
					
					// And foot print if needed
					if (bFullViewModel) {
						if (oLinkEntry.getAttribute("title").equals("Quicklook")) {
							try {
								
								HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(oLinkEntry.getAttribute("href"));
								
								if (oHttpCallResponse.getResponseCode()>=200 && oHttpCallResponse.getResponseCode()<=299) {

									InputStream oInputStreamImage = new ByteArrayInputStream(oHttpCallResponse.getResponseBytes());
									BufferedImage  oImage = ImageIO.read(oInputStreamImage);
									ByteArrayOutputStream bas = new ByteArrayOutputStream();
									ImageIO.write(oImage, "png", bas);
									oResult.setPreview("data:image/png;base64," + Base64.getEncoder().encodeToString((bas.toByteArray())));
								}				
							}
							catch (Exception e) {
								WasdiLog.debugLog("ResponseTranslatorLSA.translateBatch: Image Preview Cycle Exception " + e.toString());
							}												
						}
					}
				} // End Cycle on links
				
				// Search the position (we assume we will receive only one)
				NodeList aoFootprint = oEntry.getElementsByTagName("georss:where");
				
				for (int iFootprint = 0; iFootprint < aoFootprint.getLength(); iFootprint++) {
					org.w3c.dom.Node oFootPrintNode = aoFootprint.item(iFootprint);
					
					if (oFootPrintNode.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;
					
					Element oFootPrintEntry = (Element) oFootPrintNode;
					
					NodeList aoPositionListNodeList = oFootPrintEntry.getElementsByTagName("gml:posList");
					
					if (aoPositionListNodeList.getLength()>0) {
						org.w3c.dom.Node oPositionListNode = aoPositionListNodeList.item(iFootprint);
						if (oFootPrintNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
							Element oPositionListEntry = (Element) oPositionListNode;
							
							try {
								String sPoints = oPositionListEntry.getTextContent();
								String sCommaPoints = "";
								
								String [] asPoints = sPoints.split(" ");
								
								for (int i=0; i<asPoints.length-1; i+=2) {
									sCommaPoints += asPoints[i+1];
									sCommaPoints += " ";
									sCommaPoints += asPoints[i];
									if (i!=asPoints.length-2) sCommaPoints += ",";
								}
								
								String sFootPrint = "POLYGON ((" + sCommaPoints + "))";								
								oResult.setFootprint(sFootPrint);									
							}
							catch (Exception oEx) {
								WasdiLog.debugLog("Exception " + oEx.toString());
							}								
						}
					}
				}
				
				// Search the position (we assume we will receive only one)
				NodeList aoDateList = oEntry.getElementsByTagName("dc:date");
				
				for (int iDate = 0; iDate < aoDateList.getLength(); iDate++) {
					org.w3c.dom.Node oDateNode = aoDateList.item(iDate);
					
					if (oDateNode.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;
					
					Element oDateEntry = (Element) oDateNode;
					
					try {
						String sDate = oDateEntry.getTextContent();
						
						String [] asDates = sDate.split("/");
						
						if (asDates.length>0) sSummary += ", Date: " + asDates[0];				
					}
					catch (Exception oEx) {
						WasdiLog.debugLog("Exception " + oEx.toString());
					}						
				}
				
				oResult.setSummary(sSummary);				
				aoResults.add(oResult);		
			}
			
			
			WasdiLog.debugLog("ResponseTranslatorLSA.translateBatch: Search Done: found " + aoResults.size() + " results");		
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("ResponseTranslatorLSA.translateBatch: Exception = " + oEx.toString());
		}			
		return aoResults;
	}

	@Override
	public int getCountResult(String sQueryResult) {
		return 0;
	}

}
