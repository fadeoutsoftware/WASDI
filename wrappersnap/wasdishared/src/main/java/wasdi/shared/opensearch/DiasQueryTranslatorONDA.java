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
public class DiasQueryTranslatorONDA extends DiasQueryTranslator {

	
	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.DiasQueryTranslator#translate(java.lang.String)
	 * 
	 * translates from WASDI query (OpenSearch) to OpenData format used by ONDA DIAS
	 * 
	 * https://github.com/fadeoutsoftware/WASDI/issues/18
	 * 
	 */
	@Override
	public String translate(String sQuery) {
		String sResult = sQuery;
		//Safe to assume:
			//no leading/ending whitespaces
			//no double whitespaces
		sResult = sResult.trim().replaceAll(" +", " ");
		
		
		//What is supported by ONDA?
		//checked on 2018.12.06
		//https://catalogue.onda-dias.eu/catalogue/ supports:
		//(by mission)
		
		
		//Sentinel-1
			//YES
				//Platform:[S1A_*|S1B_*] (optional)
				//type:[SLC|GRD|OCN] (optional)
				//relativeOrbitNumber:[integer in [1-175]] (optional)
				//Sensor Mode:[SM|IW|EW|WV] (optional)
			//NO
				//Polarisation
				//Swath
		//Sentinel-2
			//YES
				//Platform:[S2A_*|S2B-*] (optional)
				//Type:[MSI1C|MSI2A|MSI2Ap] (optional)
				//Cloud Cover %:interval [a,b] between float [0,100] (optional)
		//Sentinel-3
			//YES
				//Type:[SR_1_SRA__|SR_1_SRA_A_|SR_1_SRA_BS|SR_2_LAN__] (optional)
				//Timeliness:[Near Real time|Short Time Critical|Non Time Critical]
			//NO
				//instrument
				//product level
				//CRelative Orbit Start
		//ENVISAT
			//YES
				//type: [ASA_IM__0P|ASA_WS__0P] (optional)
				//Orbit Position:[ASCENDING|DESCENDING]
			//NO
		//LANDSAT
			//YES
				//Type:[L1T|L1G|L1GT|L1GS|L1TP] (optional)
				//Cloud Cover %:interval [a,b] between float [0,100] (optional)
			//NO
		//PROBAV
			//not supported
		
		//begin translation
		
		//SENTINEL 1 2 3 platform
		sResult = sResult.replaceAll("platformname:Sentinel-1", "name:S1*");
		sResult = sResult.replaceAll("platformname:Sentinel-2", "name:S2*");
		sResult = sResult.replaceAll("platformname:Sentinel-3", "name:S3*");

		//SENTINEL 1 2 3 filename
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
		sResult = sResult.replaceAll("relativeorbitnumber:", "relativeOrbitNumber:");
		//if there's a dot "." remove it and the decimals that follow
		String sPattern = "(?<=relativeOrbitNumber:\\d{1,50})"+
				"\\.\\d*"+
				"(?=\\s{0,1}|\\)|[a-z]|[A-Z])";
		sResult = sResult.replaceAll(sPattern, "");

		
		sResult = sResult.replaceAll("sensoroperationalmode:", "sensorOperationalMode:");
		sResult = sResult.replaceAll("cloudcoverpercentage", "cloudCoverPercentage");
		
		//cloudCoverPercentage should be the same
		
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
