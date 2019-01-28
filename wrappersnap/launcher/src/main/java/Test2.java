/**
 * Created by a.corrado on 05/09/2016.
 */

//import publish.Publisher;
import java.util.ArrayList;

import wasdi.ConfigReader;

public class Test2 {
	
	private static String getBye() {
		ArrayList<String> m_asByes = new ArrayList<>();
		m_asByes.add("----WASDI:Operation Done----");
		m_asByes.add("----WASDI:Operation Done---");
		m_asByes.add("----WASDI:Operation Done--");
		m_asByes.add("----WASDI:Operation Done-");
		m_asByes.add("----WASDI:Operation Done");
		m_asByes.add("----WASDI:Operation Don");
		m_asByes.add("----WASDI:Operation Do");
		m_asByes.add("----WASDI:Operation D");
		m_asByes.add("----WASDI:Operation");
		m_asByes.add("----WASDI:Operatio");
		m_asByes.add("----WASDI:Operat");
		m_asByes.add("1");
		m_asByes.add("2");
		m_asByes.add("3");
		m_asByes.add("4");
		m_asByes.add("5");
		m_asByes.add("6");
		m_asByes.add("7");
		m_asByes.add("8");
		m_asByes.add("9");
		
		
		return m_asByes.get(((int)(Math.random()*100000))%m_asByes.size());
	}

	
    public static void main(String[] args) throws Exception
    {
    	
    	for (int i=0; i<10; i++) {
    		System.out.println(getBye());
    	}
    	
    	/*
    	HashMap<String, LocalFileDescriptor> m_asCollectionsFolders = new HashMap<>();
    	
		LocalFileDescriptor oDescriptor = new LocalFileDescriptor("PROBAV_S1-TOA_1KM_V001", "/data/MTDA/PROBAV_L3_S1_TOA_1KM", false);
		m_asCollectionsFolders.put("PROBAV_S1-TOA_1KM_V001", oDescriptor);
		
		oDescriptor = new LocalFileDescriptor("PROBAV_S1-TOC_1KM_V001", "/data/MTDA/PROBAV_L3_S1_TOC_1KM", false);
		m_asCollectionsFolders.put("PROBAV_S1-TOC_1KM_V001", oDescriptor);
		
		oDescriptor = new LocalFileDescriptor("PROBAV_S10-TOC_1KM_V001", "/data/MTDA/PROBAV_L3_S10_TOC_1KM", false);
		m_asCollectionsFolders.put("PROBAV_S10-TOC_1KM_V001", oDescriptor);
		
		oDescriptor = new LocalFileDescriptor("PROBAV_S10-TOC-NDVI_1KM_V001", "/data/MTDA/PROBAV_L3_S10_TOC_NDVI_1KM", false);
		m_asCollectionsFolders.put("PROBAV_S10-TOC-NDVI_1KM_V001", oDescriptor);
		
		oDescriptor = new LocalFileDescriptor("PROBAV_L2A_1KM_V001", "/data/MTDA/PROBAV_L2A_1KM", true);
		m_asCollectionsFolders.put("PROBAV_L2A_1KM_V001", oDescriptor);
		
		oDescriptor = new LocalFileDescriptor("PROBAV_S1-TOA_333M_V001", "/data/MTDA/PROBAV_L3_S1_TOA_333M", false);
		m_asCollectionsFolders.put("PROBAV_S1-TOA_333M_V001", oDescriptor);
		
		oDescriptor = new LocalFileDescriptor("PROBAV_S1-TOC_333M_V001", "/data/MTDA/PROBAV_L3_S1_TOC_333M", false);
		m_asCollectionsFolders.put("PROBAV_S1-TOC_333M_V001", oDescriptor);
		
		oDescriptor = new LocalFileDescriptor("PROBAV_S10-TOC_333M_V001", "/data/MTDA/PROBAV_L3_S10_TOC_333M", false);
		m_asCollectionsFolders.put("PROBAV_S10-TOC_333M_V001", oDescriptor);
		
		oDescriptor = new LocalFileDescriptor("PROBAV_S10-TOC-NDVI_333M_V001", "/data/MTDA/PROBAV_L3_S1_TOC_NDVI_100M", false);
		m_asCollectionsFolders.put("PROBAV_S10-TOC-NDVI_333M_V001", oDescriptor);
		
		oDescriptor = new LocalFileDescriptor("PROBAV_L2A_333M_V001", "/data/MTDA/PROBAV_L2A_333M", true);
		m_asCollectionsFolders.put("PROBAV_L2A_333M_V001", oDescriptor);
		
		oDescriptor = new LocalFileDescriptor("PROBAV_S1-TOA_100M_V001", "/data/MTDA/PROBAV_L3_S1_TOA_100M", false);
		m_asCollectionsFolders.put("PROBAV_S1-TOA_100M_V001", oDescriptor);
		
		oDescriptor = new LocalFileDescriptor("PROBAV_S1-TOC_100M_V001", "/data/MTDA/PROBAV_L3_S1_TOC_100M", false);
		m_asCollectionsFolders.put("PROBAV_S1-TOC_100M_V001", oDescriptor);
		
		oDescriptor = new LocalFileDescriptor("PROBAV_S1-TOC-NDVI_100M_V001", "/data/MTDA/PROBAV_L3_S1_TOC_NDVI_100M", false);
		m_asCollectionsFolders.put("PROBAV_S1-TOC-NDVI_100M_V001", oDescriptor);
		
		oDescriptor = new LocalFileDescriptor("PROBAV_S5-TOA_100M_V001", "/data/MTDA/PROBAV_L3_S5_TOA_100M", false);
		m_asCollectionsFolders.put("PROBAV_S5-TOA_100M_V001", oDescriptor);
		
		oDescriptor = new LocalFileDescriptor("PROBAV_S5-TOC_100M_V001", "/data/MTDA/PROBAV_L3_S5_TOC_100M", false);
		m_asCollectionsFolders.put("PROBAV_S5-TOC_100M_V001", oDescriptor);
		
		oDescriptor = new LocalFileDescriptor("PROBAV_S5-TOC-NDVI_100M_V001", "/data/MTDA/PROBAV_L3_S5_TOC_NDVI_100M", false);
		m_asCollectionsFolders.put("PROBAV_S5-TOC-NDVI_100M_V001", oDescriptor);
		
		oDescriptor = new LocalFileDescriptor("PROBAV_L2A_100M_V001", "/data/MTDA/PROBAV_L2A_100M", true);
		m_asCollectionsFolders.put("PROBAV_L2A_100M_V001", oDescriptor);
		
		oDescriptor = new LocalFileDescriptor("PROBAV_P_V001", "/data/MTDA/PROBAV_L1C", true);
		m_asCollectionsFolders.put("PROBAV_P_V001", oDescriptor);
		
		SerializationUtils.serializeObjectToXML("C:/temp/wasdi/probavcollections.xml", m_asCollectionsFolders);
		
    	*/
    	
    	//HashMap<String, LocalFileDescriptor> m_asCollectionsFolders = (HashMap<String, LocalFileDescriptor>) SerializationUtils.deserializeXMLToObject("C:/temp/wasdi/probavcollections.xml");
    	
    	//DownloadFile oDownloadFile = DownloadFile.getDownloadFile("PROBAV");
    	//DownloadFile oDownloadFile = new DownloadSupplier().supplyDownloader("PROBAV");
    	
    	String sLink = "https://www.vito-eodata.be/PDF/dataaccess?service=DSEO&request=GetProduct&version=1.0.0&collectionID=1000060&productID=271466625&ProductURI=urn:ogc:def:EOP:VITO:PROBAV_L2A_1KM_V001:PROBAV_CENTER_L2A_20180521_225405_1KM:V101&";
    	//String sLink = "https://www.vito-eodata.be/PDF/dataaccess?service=DSEO&request=GetProduct&version=1.0.0&collectionID=1000060&productID=200462849&ProductURI=urn:ogc:def:EOP:VITO:PROBAV_L2A_1KM_V001:PROBAV_CENTER_L2A_20160505_010431_1KM:V101&";
    	/*
    	// CHECK USR AND PW ON THE config.properties file!!!!
    	String sUser = ConfigReader.getPropValue("DHUS_USER");
    	String sPassword = ConfigReader.getPropValue("DHUS_PASSWORD");
    	
    	oDownloadFile.setProviderPassword(sPassword);
    	oDownloadFile.setProviderUser(sUser);
    	
    	String sFileName = oDownloadFile.GetFileName(sLink);
    	
    	System.out.println("File Name: " + sFileName);
    	
    	long lSize = oDownloadFile.GetDownloadFileSize(sLink);
    	
    	System.out.println("File Size: " + lSize);
    	
    	oDownloadFile.ExecuteDownloadFile(sLink, sUser, sPassword, "C:/temp/wasdi/paolo", null);


        final JFileChooser fc = new JFileChooser();

        //In response to a button click:
        int returnVal = fc.showOpenDialog(null);

        if (returnVal == JFileChooser.APPROVE_OPTION)
        {
            String bigGeoTiffFormatName = BigGeoTiffProductReaderPlugIn.FORMAT_NAME;
            String formatName = "SENTINEL-1";
            File file = fc.getSelectedFile();
            //This is where a real application would open the file.

            Product exportProduct = null;
            Product oCalibratedProduct = null;
            Product oFilteredProduct = null;
            Product oMultilookedProduct = null;
            Product terrainProduct = null;

            Calibration oCalibration = new Calibration();
            Filter oFilter = new Filter();
            Multilooking oMultilooking = new Multilooking();
            TerrainCorrection oTerrainCorrection = new TerrainCorrection();
            try
            {
                //read product
                exportProduct = ProductIO.readProduct(file, formatName);

                System.out.println("Calibrate");
                oCalibratedProduct = oCalibration.getCalibration(exportProduct, null);

                returnVal = fc.showOpenDialog(null);
                    if (returnVal != JFileChooser.APPROVE_OPTION)
                        System.out.println("Errore");
                file = fc.getSelectedFile();
                exportProduct = ProductIO.readProduct(file, formatName);

                System.out.println("Filter");
                oFilteredProduct = oFilter.getFilter(exportProduct, null);
                System.out.println("PostFilter");

                returnVal = fc.showOpenDialog(null);
                if (returnVal != JFileChooser.APPROVE_OPTION)
                    System.out.println("Errore");
                file = fc.getSelectedFile();
                exportProduct = ProductIO.readProduct(file, formatName);

                System.out.println("Multilook");
                oMultilookedProduct = oMultilooking.getMultilooking(exportProduct, null);

                    //multi look product
                    File multiFile = new File("C:\\Users\\a.corrado\\Documents\\test_multi.tif");
                    ProductIO.writeProduct(oMultilookedProduct, multiFile.getAbsolutePath(), bigGeoTiffFormatName);
                    MemUtils.freeAllMemory();

                System.out.println("PostMultilook");

                returnVal = fc.showOpenDialog(null);
                if (returnVal != JFileChooser.APPROVE_OPTION)
                    System.out.println("Errore");
                file = fc.getSelectedFile();
                exportProduct = ProductIO.readProduct(file, formatName);
                System.out.println("terrainProduct");
                terrainProduct=oTerrainCorrection.getTerrainCorrection(exportProduct, null);
                System.out.println("PostterrainProduct");

                //String bigGeoTiffFormatName = BigGeoTiffProductReaderPlugIn.FORMAT_NAME;


                File terrainFile = new File("C:\\Users\\a.corrado\\Documents\\test_terrain.tif");
                ProductIO.writeProduct(terrainProduct, terrainFile.getAbsolutePath(), bigGeoTiffFormatName);
                MemUtils.freeAllMemory();
                System.out.println("Prova son oarrivato in fondo");
            }catch(IOException e)
            {
                e.printStackTrace();

            }
        }
        else
        {

        }
        */
    }


}
