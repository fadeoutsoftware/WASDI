package wasdi.jwasdilib;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        testBaseLib(oLib);
        
        //testUploadFileDUMMYIMAGE(oLib);
        //testMosaic(oLib);
        //testSearch(oLib);
//        testSubset(oLib);
        

        System.out.println("JWasdiLib Test Done");
        
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
    	oLib.uploadFile("S1B_IW_GRDH_1SDV_20180101T001334_20180101T001359_008970_010027_F7B8.zip");
    	
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
    
    
}
