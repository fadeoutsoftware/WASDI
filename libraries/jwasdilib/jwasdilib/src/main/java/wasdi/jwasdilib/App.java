package wasdi.jwasdilib;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.net.io.Util;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        System.out.println( "JWasdiLib Test Start" );
        WasdiLib oLib = new WasdiLib();
        
        oLib.init("C:/Users/c.nattero/workspace/wasdi/wasdilib/config.properties");
        
        testConnection(oLib);
        testDownload(oLib);
    //    testAutomaticUpload(oLib);
        
//        testUploadFileDUMMYFILE(oLib);
//        testUploadFile(oLib);
        
//        testBaseLib(oLib);
        
        //testUploadFileDUMMYIMAGE(oLib);
        //testMosaic(oLib);
        //testSearch(oLib);
//        testSubset(oLib);
        

        System.out.println("JWasdiLib Test Done");
        
    }
    
    public static void testConnection(WasdiLib oLib) {
    	try {
    		
    		//request
	    	String sUrl = oLib.getBaseUrl() + "/wasdi/hello";
			URL oURL = new URL(sUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();
		    oConnection.setDoOutput(true);
		    oConnection.setDoInput(true);
		    oConnection.setUseCaches(false);
	//	    oConnection.setRequestProperty("Accept", "*/*");
		    oConnection.setRequestMethod("GET");
		    oConnection.connect();
		    
		    // response
		    int iResponse = oConnection.getResponseCode();
		    System.out.println("WasdiLib.uploadFile: server returned " + iResponse);
		    InputStream oResponseInputStream = null;
		    ByteArrayOutputStream oByteArrayOutputStream = new ByteArrayOutputStream();
		    if( 200 <= iResponse && 299 >= iResponse ) {
		    	oResponseInputStream = oConnection.getInputStream();
		    } else {
		    	oResponseInputStream = oConnection.getErrorStream();
		    }
		    if(null!=oResponseInputStream) {
		    	Util.copyStream(oResponseInputStream, oByteArrayOutputStream);
		    	String sMessage = oByteArrayOutputStream.toString();
		    	System.out.println(sMessage);
		    } else {
		    	throw new NullPointerException("WasdiLib.uploadFile: stream is null");
		    }
		    
		    
    	} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    public static void testBaseLib(WasdiLib oLib) {
    	
        System.out.println(oLib.getWorkspaces());
        System.out.println(oLib.getWorkflows());
        System.out.println(oLib.getProductsByWorkspace(".dim"));

        String sPath = oLib.getFullProductPath("S2A_MSIL1C_20190321T004701_N0207_R102_T53HPA_20190321T020838_NDVI.dim");
        System.out.println("File Path " + sPath);
        
        oLib.openWorkspace("FirstWS");
        
        System.out.println(oLib.getParam("INPUTFILE"));
        System.out.println(oLib.getParam("SOGLIA"));
        System.out.println(oLib.getParam("PROVA"));
        System.out.println(oLib.getParam("MANCA"));
        
        long lStartTime = System.currentTimeMillis();
        sPath = oLib.getFullProductPath("out.tif");
        System.out.println("Donwload Time: " + (System.currentTimeMillis() - lStartTime) + " ms");
        
        System.out.println("File Path " + sPath);

        
        
        ArrayList<String> asInputFileName = new ArrayList<>();
        ArrayList<String> asOutputFileName = new ArrayList<>();
        
        asInputFileName.add("S1A_IW_GRDH_1SDV_20190103T025107_20190103T025132_025307_02CCC6_0BA2.zip");
        asOutputFileName.add("testConversion.tif");
        
        
        System.out.println(oLib.executeWorkflow(asInputFileName.toArray(new String[0]), asOutputFileName.toArray(new String[0]), "SentinelToGeoTiff"));
        
        
        oLib.setProcessPayload("3ed62fce-cb13-4da4-8ff7-a1d3e7f27fc6", "ciao");
        
        System.out.println(oLib.getFullProductPath("S1A_IW_GRDH_1SDV_20190101T171426_20190101T171451_025287_02CC09_757C.zip"));
          	
    }
    
    public static void testSearchAndMosaic(WasdiLib oLib) {
    	List<Map<String,Object>> aoFound = oLib.searchEOImages("S1", "2019-03-01", "2019-03-15", 45.1510532655634, 6.4193710684776315, 42.732667148204456, 10.188904702663422, "GRD", null, null, null);
    	
    	if (aoFound != null) {
    		if (aoFound.size() > 1) {
    			String sImport = oLib.importProduct(aoFound.get(0));
    			sImport = oLib.importProduct(aoFound.get(1));
    			
    			System.out.println("Import Status = " + sImport);
    			
    	    	ArrayList<String> asInputs = new ArrayList<>();
    	    	asInputs.add(aoFound.get(0).get("title")+".zip");
    	    	asInputs.add(aoFound.get(1).get("title")+".zip");
    	    	String sOutputFile = "mosaicFromLib.tif";
    	    	
    	    	oLib.mosaic(asInputs, sOutputFile);
    	    	oLib.addFileToWASDI(sOutputFile);
    	    	String sMosaic = oLib.getFullProductPath(sOutputFile);
    	    	System.out.println("Mosaic File : " + sMosaic);
    		}
    	}
    }

    
    public static void testSearch(WasdiLib oLib) {
    	List<Map<String,Object>> aoFound = oLib.searchEOImages("S1", "2019-03-01", "2019-03-15", 45.1510532655634, 6.4193710684776315, 42.732667148204456, 10.188904702663422, "GRD", null, null, null);
    	
    	if (aoFound != null) {
    		if (aoFound.size() > 0) {
    			String sImport = oLib.importProduct(aoFound.get(0));
    			System.out.println("Import Status = " + sImport);
    		}
    	}
    }
    
    public static void testMosaic(WasdiLib oLib) {
    	ArrayList<String> asInputs = new ArrayList<>();
    	asInputs.add("S1A_IW_GRDH_1SDV_20190128T062955_20190128T063020_025674_02DA10_0E8F_LISTSinglePreproc.tif");
    	asInputs.add("S1A_IW_GRDH_1SDV_20190128T063020_20190128T063045_025674_02DA10_0D61_LISTSinglePreproc.tif");
    	String sOutputFile = "mosaicFromLib.tif";
    	
    	oLib.mosaic(asInputs, sOutputFile);
    	oLib.addFileToWASDI(sOutputFile);
    	String sMosaic = oLib.getFullProductPath(sOutputFile);
    	System.out.println("Mosaic File : " + sMosaic);
    }
    
    public static void testUploadFile(WasdiLib oLib)
    {
//    	oLib.uploadFile("S1B_IW_GRDH_1SDV_20180101T001334_20180101T001359_008970_010027_F7B8.zip");
//    	oLib.uploadFile("out.tif");
    	oLib.uploadFile("S1A_S3_OCN__2SSH_20181220T000048_20181220T000108_025101_02C553_7619.zip"); //17.5 MB
//    	oLib.uploadFile("S1A_EW_RAW__0SSH_20181219T091407_20181219T091454_025092_02C502_BE3D.zip"); //337 MB
//    	oLib.uploadFile("S1A_IW_SLC__1SDV_20170502T045516_20170502T045544_016398_01B271_D413.zip");
    	
    	
    }
    public static void testUploadFileDUMMYFILE(WasdiLib oLib)
    {
    	oLib.uploadFile("miao.txt");
    	
    }
    public static void testUploadFileDUMMYIMAGE(WasdiLib oLib)
    {
    	oLib.uploadFile("bau.jpg");
    	
    }
    
    
    public static void testSubset(WasdiLib oLib) {
    	
    	oLib.openWorkspace("Grande");
    	String sInputFile = "mosaicFromLib.tif";
    	String sOutputFile = "subsetFromLib2.tif";
    	double dLatN = 56.2;
    	double dLonW = -4.0;
    	double dLatS = 54.9;
    	double dLonE = -2.0;
    	
    	String sOutputState = oLib.subset(sInputFile, sOutputFile, dLatN, dLonW, dLatS, dLonE);
    	System.out.println("Subset result = " + sOutputState);
    }

    public static void testAutomaticUpload(WasdiLib oLib) {
    	String sResult = null;
    	//sResult = oLib.getFullProductPath("S2A_MSIL1C_20181212T001711_N0207_R116_T55MHP_20181212T013828.zip");
    	sResult = oLib.addFileToWASDI("S2A_MSIL1C_20181212T001711_N0207_R116_T55MHP_20181212T013828.zip");
    	System.out.println(sResult);
    }
    
    public static void testDownload(WasdiLib oLib) {
    	
    	String sResult = null;
//    	sResult = oLib.getFullProductPath("S1A_IW_GRDH_1SDV_20190407T171450_20190407T171515_026687_02FECA_7F6E.zip");
//    	System.out.println(sResult);
//    	sResult = oLib.getFullProductPath("S2A_MSIL1C_20190404T101031_N0207_R022_T32TNR_20190404T185546.zip");
//    	System.out.println(sResult);
//    	sResult = oLib.getFullProductPath("out.tif");
//    	System.out.println(sResult);
//    	sResult = oLib.getFullProductPath("S3A_OL_2_LFR____20190401T085814_20190401T090114_20190401T104829_0179_043_107_2160_LN1_O_NR_002.zip");
//    	System.out.println(sResult);
//    	sResult = oLib.getFullProductPath("LC08_L1TP_192029_20180427_20180502_01_T1.tar.gz");
//    	System.out.println(sResult);
//    	sResult = oLib.getFullProductPath("20170525_8d_20170601-ACRI-L4-BBP-GSM_MULTI_4KM-GLO-DT-v02.nc");
//    	System.out.println(sResult);
//    	sResult = oLib.getFullProductPath("S1A_IW_GRDH_1SDV_20190407T171450_20190407T171515_026687_02FECA_7F6E_ApplyOrbit.dim");
//    	System.out.println(sResult);
    	sResult = oLib.getFullProductPath("S2A_MSIL1C_20190322T013651_N0207_R117_T53RQQ_20190322T032332_NDVI");
    	System.out.println(sResult);
//    	sResult = oLib.getFullProductPath("S2A_MSIL1C_20190404T101031_N0207_R022_T32TNR_20190404T185546_NDVI.dim");
//    	System.out.println(sResult);
    }
    
}
