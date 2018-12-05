/**
 * Created by Cristiano Nattero on 2018-12-04
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch;

/**
 * @author c.nattero
 *
 */
public class OpenSearch2OdataTranslator extends DiasQueryTranslator {

	
	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.DiasQueryTranslator#translate(java.lang.String)
	 */
	@Override
	public String translate(String sQuery) {
		String sResult = sQuery;
		//SENTINEL 1
		sResult = sResult.replaceAll("platformname:Sentinel-1", "name:S1*");
		//SENTINEL 2
		sResult = sResult.replaceAll("platformname:Sentinel-2", "name:S2*");
		//SENTINEL 3
		sResult = sResult.replaceAll("platformname:Sentinel-3", "name:S3*");


		//SENTINEL 1 - 2 - 3
		sResult = sResult.replaceAll("filename:", "name:");
		
		//specific product types that need asterisks
		sResult = sResult.replaceAll("producttype:SLC","name:\\*SLC\\*");
		sResult = sResult.replaceAll("producttype:GRD","name:\\*GRD\\*");
		sResult = sResult.replaceAll("producttype:OCN","name:\\*OCN\\*");
		sResult = sResult.replaceAll("producttype:S2MSI1C", "name:\\*S2MSI1C\\*");
		sResult = sResult.replaceAll("producttype:", "name:");
		
		//polarisationmode:HH not supported by ONDA? 
		//sensoroperationalmode:SM same name in ONDA 
		//swathidentifier:b not supported by ONDA?
		//cloudcoverpercentage:a same name in ONDA
		sResult = sResult.replaceAll("timeliness:Near Real Time", "timeliness:NRT");
		sResult = sResult.replaceAll("timeliness:Short Time Critical", "timeliness:STC");
		sResult = sResult.replaceAll("timeliness:Non Time Critical", "timeliness:NTC");
		//OS: just for sentinel1, ONDA: just for sentinel3
		sResult = sResult.replaceAll("relativeorbitstart:", "relativeOrbitNumber:");
		
		sResult = sResult.replaceAll("sensoroperationalmode:", "sensorOperationalMode:");
		sResult = sResult.replaceAll("cloudcoverpercentage", "cloudCoverPercentage");
		
		//cloudCoverPercentage should be the same

		
		//remove double spaces
		//trimDoubles(sResult, ' ');
		sResult = sResult.trim().replaceAll(" +", " ");
		
		return sResult;
	}

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.DiasQueryTranslator#encode(java.lang.String)
	 */
	@Override
	public String encode(String sDecoded) {
		String sResult = new String( sDecoded.replaceAll(" ", "%20") );
		//sResult = java.net.URLEncoder.encode(sDecoded, m_sEnconding);
		return sResult;
	}

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.DiasQueryTranslator#decode(java.lang.String)
	 */
	@Override
	public String decode(String sEncoded) {
		// TODO Auto-generated method stub
		return null;
	}

}
