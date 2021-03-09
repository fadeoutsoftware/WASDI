package wasdi.shared.opensearch.lsa;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Link;
import org.apache.abdera.parser.Parser;
import org.apache.abdera.parser.ParserOptions;

import wasdi.shared.opensearch.DiasResponseTranslator;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.QueryResultViewModel;

public class DiasResponseTranslatorLSA extends DiasResponseTranslator {

	@Override
	public List<QueryResultViewModel> translateBatch(String sJson, boolean bFullViewModel, String sDownloadProtocol) {
		
		List<QueryResultViewModel> aoResults = new ArrayList<QueryResultViewModel>();
		
		try {
			
			// Parse results with abdera
			Abdera oAbdera = new Abdera();
			
			Parser oParser = oAbdera.getParser();
			ParserOptions oParserOptions = oParser.getDefaultParserOptions();
			
			oParserOptions.setCharset("UTF-8");
			oParserOptions.setFilterRestrictedCharacterReplacement('_');
			oParserOptions.setFilterRestrictedCharacters(true);
			oParserOptions.setMustPreserveWhitespace(false);
			oParserOptions.setParseFilter(null);

			org.apache.abdera.model.Document<Feed> oDocument = null;
			
			oDocument = oParser.parse(new StringReader(sJson), oParserOptions);
			
			if (oDocument == null) {
				Utils.debugLog("QueryExecutorLSA.executeAndRetrieve: Document response null, aborting");
				return aoResults;
			}
			
			// Extract the count
			Feed oFeed = (Feed) oDocument.getRoot();

			for (Entry oEntry : oFeed.getEntries()) {

				QueryResultViewModel oResult = new QueryResultViewModel();
				
				oResult.setProvider("LSA");
				//retrive the title
				String sTitle = oEntry.getTitle();
				oResult.setTitle(sTitle);
				
				// Relative Orbit				
				if (sTitle.startsWith("S1")) {
					
					// Split the title
					String [] asTitleParts = sTitle.split("_");
					
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
								Utils.debugLog("Exception converting LSA Result Relative Orbit: "  + oEx.toString()) ;
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
						}
					}
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
								Utils.debugLog("Exception converting LSA Result Relative Orbit: "  + oEx.toString()) ;
							}
							
							if (iRelativeOrbit != -1) {
								// Set the relative orbit to the WASDI View Model
								oResult.getProperties().put("relativeorbitnumber", ""+iRelativeOrbit);
							}
						}
					}					
				}
				
				// Set platform
				oResult.getProperties().put("platform", sTitle.substring(0, 3));
				oResult.getProperties().put("platformname", sTitle.substring(0, 3));
				
				
				//retrieve the id
				oResult.setId(oEntry.getId().toString());

				//retrieve the link
				List<Link> aoLinks = oEntry.getLinks();
				
				for (Link oLink : aoLinks) {
					if (oLink.getTitle().contains("Source package download")) {
						String sLink = "https://collgs.lu/" + oLink.getHref().toString();
						oResult.setLink(sLink);
						break;
					}
				}

				//retrieve the footprint and all others properties
				List<Element> aoElements = oEntry.getElements();
				
				for (Element oElement : aoElements) {
					
					String sName = oElement.getQName().toString();
					
					if (sName != null) {
						if (sName.equals("{http://www.georss.org/georss}where")) {
							
							try {
								// Get Polygon
								Element oChild = oElement.getFirstChild();
								// Get exterior
								oChild = oChild.getFirstChild();
								// Get Linear Ring
								oChild = oChild.getFirstChild();
								// Get PosList
								oChild = oChild.getFirstChild();
							
								String sPoints = oChild.getText();
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
								Utils.debugLog("Exception " + oEx.toString());
							}
						}
						else if (sName.equals("{http://purl.org/dc/elements/1.1/}date")) {
							oResult.setSummary(oElement.getText());
						}
					}
				}
				
				aoResults.add(oResult);
			} 
			
			Utils.debugLog("QueryExecutorLSA.buildResultViewModel: Search Done: found " + aoResults.size() + " results");		
		}
		catch (Exception oEx) {
			Utils.debugLog("QueryExecutorLSA.executeAndRetrieve: Exception = " + oEx.toString());
		}			
		return aoResults;
	}

}
