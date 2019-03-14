package wasdi.jwasdilib;

import java.util.ArrayList;

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
        
        oLib.init("C:\\Temp\\wasdi\\JMatLibTest\\config.properties");
        //testUploadFileDUMMYIMAGE(oLib);
        testMosaic(oLib);

        //HashMap<String, String> asHeaders = new HashMap<>();
        //asHeaders.put("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        //asHeaders.put("Host", "gpod.eo.esa.int");
        //asHeaders.put("Upgrade-Insecure-Requests","1");
        //asHeaders.put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:57.0) Gecko/20100101 Firefox/57.0");
        
        
        //String sResult =oLib.httpsGet("https://gpod.eo.esa.int/services/",asHeaders );
        //System.out.println(sResult);

/*
        System.out.println(oLib.getWorkspaces());
        System.out.println(oLib.getWorkflows());
        System.out.println(oLib.getProductsByWorkspace("FirstWS"));

        String sPath = oLib.getFullProductPath("PROBAV_L2A_20180604_104840_2_1KM_V101.HDF5");
        System.out.println("File Path " + sPath);
*/
        
//        oLib.openWorkspace("FirstWS");
//        
//        System.out.println(oLib.getParam("INPUTFILE"));
//        System.out.println(oLib.getParam("SOGLIA"));
//        System.out.println(oLib.getParam("PROVA"));
//        System.out.println(oLib.getParam("MANCA"));
//        
//        long lStartTime = System.currentTimeMillis();
//        String sPath = oLib.getFullProductPath("out.tif");
//        System.out.println("Donwload Time: " + (System.currentTimeMillis() - lStartTime) + " ms");
//        
//        System.out.println("File Path " + sPath);

        
        /*
        ArrayList<String> asInputFileName = new ArrayList<>();
        ArrayList<String> asOutputFileName = new ArrayList<>();
        
        asInputFileName.add("S1A_IW_GRDH_1SDV_20190103T025107_20190103T025132_025307_02CCC6_0BA2.zip");
        asOutputFileName.add("testConversion.tif");
        
        
        System.out.println(oLib.executeWorkflow(asInputFileName.toArray(new String[0]), asOutputFileName.toArray(new String[0]), "SentinelToGeoTiff"));
        */
        /*
        oLib.setProcessPayload("3ed62fce-cb13-4da4-8ff7-a1d3e7f27fc6", "ciao");
        
        System.out.println(oLib.getFullProductPath("S1A_IW_GRDH_1SDV_20190101T171426_20190101T171451_025287_02CC09_757C.zip"));
        */
        System.out.println("JWasdiLib Test Done");
        
    }
    
    public static void testMosaic(WasdiLib oLib) {
    	ArrayList<String> asInputs = new ArrayList<>();
    	asInputs.add("S1A_IW_GRDH_1SDV_20190128T062955_20190128T063020_025674_02DA10_0E8F_LISTSinglePreproc.tif");
    	asInputs.add("S1A_IW_GRDH_1SDV_20190128T063020_20190128T063045_025674_02DA10_0D61_LISTSinglePreproc.tif");
    	String sOutputFile = "mosaicFromLib.tif";
    	
    	oLib.mosaic(asInputs, sOutputFile);
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
    
    
    
}
