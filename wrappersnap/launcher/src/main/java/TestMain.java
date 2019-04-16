

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.esa.s1tbx.io.orbits.sentinel1.SentinelPODOrbitFile;
import org.esa.s1tbx.sar.gpf.orbits.ApplyOrbitFileOp;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.util.geotiff.GeoCoding2GeoTIFFMetadata;
import org.esa.snap.core.util.geotiff.GeoTIFF;
import org.esa.snap.core.util.geotiff.GeoTIFFMetadata;
import org.json.JSONObject;

import com.bc.ceres.glevel.MultiLevelImage;

import wasdi.LauncherMain;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.ProductWorkspace;
import wasdi.shared.business.PublishedBand;
import wasdi.shared.business.User;
import wasdi.shared.business.UserSession;
import wasdi.shared.business.Workspace;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProductWorkspaceRepository;
import wasdi.shared.data.PublishedBandsRepository;
import wasdi.shared.data.SessionRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.parameters.ApplyOrbitParameter;
import wasdi.shared.parameters.ApplyOrbitSetting;
import wasdi.shared.parameters.CalibratorParameter;
import wasdi.shared.parameters.MultilookingParameter;
import wasdi.shared.parameters.NDVIParameter;
import wasdi.shared.parameters.PublishBandParameter;
import wasdi.shared.parameters.PublishParameters;
import wasdi.shared.parameters.RangeDopplerGeocodingParameter;
import wasdi.shared.parameters.RangeDopplerGeocodingSetting;
import wasdi.shared.parameters.RasterGeometricResampleParameter;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.ProductViewModel;
import wasdi.snapopearations.ApplyOrbit;
import wasdi.snapopearations.Calibration;
import wasdi.snapopearations.Multilooking;
import wasdi.snapopearations.ReadProduct;

/**
 * Created by p.campanella on 21/10/2016.
 */
public class TestMain {
    public static void main(String[] args) throws Exception {
    	
    	
    	
    	
    	Field[] fields = ApplyOrbitFileOp.class.getDeclaredFields();
    	for (Field field : fields) {
    		System.out.println("FIELD: " + field.getName());
			Annotation[] annotations = field.getAnnotations();			
			for (Annotation annotation : annotations) {
				System.out.println("\tANOTATION: " + annotation.annotationType());
				
				if (annotation instanceof Parameter) {
					Parameter p = (Parameter) annotation;
					System.out.println("\t\tANOTATION: " + Arrays.toString(p.valueSet()));
					
				}
				
			}
		}

    	
        TestMain oTestMain = new TestMain();
        oTestMain.GetLastProcess();

        //oTestMain.UserRepository();
        //oTestMain.WorkspaceRepository();
        //oTestMain.SendRabbit("S1A_IW_GRDH_1SSV_20150213T095824_20150213T095849_004603_005AB7_5539");
        //oTestMain.GenerateBandTiff();
//        oTestMain.TestPublishBand();
        //oTestMain.TestPublish();
        //oTestMain.PublishedBandRepositoryTest();
        //oTestMain.TestProductViewModel();
        //oTestMain.ProductWorkspaceRepositoryTest();
        //oTestMain.SendRabbitExchange("S1A_IW_GRDH_1SSV_20150213T095824_20150213T095849_004603_005AB7_5539");
        //oTestMain.InsertProcess();
        //oTestMain.DeleteProcess();
        //oTestMain.GetProcess();
        //oTestMain.GetBoundingBox();
        //oTestMain.TerrainCorrection();
        //oTestMain.DeleteLayer();
        //oTestMain.InsertProcess();
        //oTestMain.InitRabbit("84904907-3d8a-4579-b9c8-91bcc03d7cc3", "paolo");
        //oTestMain.DeleteSession();
        //oTestMain.GetFileSize();
        //oTestMain.GetBoundingBox();
        //oTestMain.GetDownloadFile();

        //oTestMain.RasterGeometricResampling();
        //oTestMain.TestBB();

//        oTestMain.ApplyOrbit();
//        oTestMain.MultiLooking();
//        oTestMain.RadiometricCalibration();
//        oTestMain.TerrainCorrection();
//        oTestMain.NDVI();
        
    }

    public void ApplyOrbit(){
        LauncherMain oLauncherMain = new LauncherMain();
        ApplyOrbitParameter oParameter = new ApplyOrbitParameter();
        oParameter.setWorkspace("8e91a84c-3dcf-470d-8e36-3ad40de80d54");
        oParameter.setExchange("8e91a84c-3dcf-470d-8e36-3ad40de80d54");
        oParameter.setUserId("test");
        oParameter.setSourceProductName("S1B_IW_GRDH_1SDV_20170226T053524_20170226T053549_004467_007C6B_F7DE.zip");
        oParameter.setDestinationProductName("S1B_IW_GRDH_1SDV_20170226T053524_20170226T053549_004467_007C6B_F7DE_APPLYORBIT");
        ((ApplyOrbitSetting)(oParameter.getSettings())).setOrbitType(SentinelPODOrbitFile.PRECISE + " (Auto Download)");
        
        oLauncherMain.executeOperator(oParameter, new ApplyOrbit(), LauncherOperations.APPLYORBIT);
    }
    
    public void RadiometricCalibration(){
        LauncherMain oLauncherMain = new LauncherMain();
        CalibratorParameter oParameter = new CalibratorParameter();
        oParameter.setWorkspace("8e91a84c-3dcf-470d-8e36-3ad40de80d54");
        oParameter.setExchange("8e91a84c-3dcf-470d-8e36-3ad40de80d54");
        oParameter.setUserId("test");
        oParameter.setSourceProductName("S1B_IW_GRDH_1SDV_20170226T053524_20170226T053549_004467_007C6B_F7DE.zip");
        oParameter.setDestinationProductName("S1B_IW_GRDH_1SDV_20170226T053524_20170226T053549_004467_007C6B_F7DE_RADIOMETRICCALIBRATION");
        oLauncherMain.executeOperator(oParameter, new Calibration(), LauncherOperations.CALIBRATE);

    }
    
    public void MultiLooking(){
        LauncherMain oLauncherMain = new LauncherMain();
        MultilookingParameter oParameter = new MultilookingParameter();
        oParameter.setWorkspace("8e91a84c-3dcf-470d-8e36-3ad40de80d54");
        oParameter.setExchange("8e91a84c-3dcf-470d-8e36-3ad40de80d54");
        oParameter.setUserId("test");
        oParameter.setSourceProductName("S1B_IW_GRDH_1SDV_20170226T053524_20170226T053549_004467_007C6B_F7DE.zip");
        oParameter.setDestinationProductName("S1B_IW_GRDH_1SDV_20170226T053524_20170226T053549_004467_007C6B_F7DE_MULTILOOKING");
        oLauncherMain.executeOperator(oParameter, new Multilooking(), LauncherOperations.MULTILOOKING);
    }

    public void TerrainCorrection(){
        LauncherMain oLauncherMain = new LauncherMain();
        RangeDopplerGeocodingParameter oParameter = new RangeDopplerGeocodingParameter();
        oParameter.setWorkspace("8e91a84c-3dcf-470d-8e36-3ad40de80d54");
        oParameter.setExchange("8e91a84c-3dcf-470d-8e36-3ad40de80d54");
        oParameter.setUserId("test");
        oParameter.setSourceProductName("S1B_IW_GRDH_1SDV_20170226T053524_20170226T053549_004467_007C6B_F7DE.zip");
        oParameter.setDestinationProductName("S1B_IW_GRDH_1SDV_20170226T053524_20170226T053549_004467_007C6B_F7DE_TERRAIN");
		String[] asBands = new String[]{"Amplitude_VH"};
		((RangeDopplerGeocodingSetting)(oParameter.getSettings())).setSourceBandNames(asBands);
        
        oLauncherMain.executeOperator(oParameter, new wasdi.snapopearations.TerrainCorrection(), LauncherOperations.TERRAIN);
    }

    public void NDVI(){
        LauncherMain oLauncherMain = new LauncherMain();
        NDVIParameter oParameter = new NDVIParameter();
        oParameter.setWorkspace("8e91a84c-3dcf-470d-8e36-3ad40de80d54");
        oParameter.setExchange("8e91a84c-3dcf-470d-8e36-3ad40de80d54");
        oParameter.setUserId("test");
        oParameter.setSourceProductName("S1B_IW_GRDH_1SDV_20170226T053524_20170226T053549_004467_007C6B_F7DE.zip");
        oParameter.setDestinationProductName("S1B_IW_GRDH_1SDV_20170226T053524_20170226T053549_004467_007C6B_F7DE_TERRAIN");
		oLauncherMain.executeOperator(oParameter, new wasdi.snapopearations.NDVI(), LauncherOperations.TERRAIN);
    }

    @SuppressWarnings("unused")
	public void GetDownloadFile(){
        LauncherMain oLauncherMain = new LauncherMain();
        DownloadedFilesRepository oRepo = new DownloadedFilesRepository();
        DownloadedFile oFile = oRepo.GetDownloadedFile("");

    }

    @SuppressWarnings("unused")
	public  void InsertProcess() {
        LauncherMain oLauncherMain = new LauncherMain();
        ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
        ProcessWorkspace oProcessWorkspace = new ProcessWorkspace();
        oProcessWorkspace.setOperationType(LauncherOperations.DOWNLOAD.name());
        oProcessWorkspace.setWorkspaceId("test");
        oProcessWorkspace.setProductName("filename");
        String sId = oProcessWorkspaceRepository.InsertProcessWorkspace(oProcessWorkspace);
        oProcessWorkspaceRepository.DeleteProcessWorkspace(sId);
    }

    @SuppressWarnings("unused")
	public  void GetProcess() {
        ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
        ProcessWorkspace oProcessWorkspace = new ProcessWorkspace();
        oProcessWorkspace.setOperationType(LauncherOperations.DOWNLOAD.name());
        oProcessWorkspace.setWorkspaceId("test");
        oProcessWorkspace.setProductName("filename");
        List<ProcessWorkspace> oList = oProcessWorkspaceRepository.GetProcessByWorkspace("test");
    }

    public void GetLastProcess() {
        ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
        List<ProcessWorkspace> oList = oProcessWorkspaceRepository.GetLastProcessByWorkspace("4b18470b-2938-4e0e-8ffe-2162ee55e7cc");   
        if (oList != null) {
        	System.out.println("Found " + oList.size() + " recent processes");
        }
    }
    @SuppressWarnings("unused")
	public  void DeleteProcess() {
        ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
        ProcessWorkspace oProcessWorkspace = new ProcessWorkspace();
        oProcessWorkspace.setOperationType(LauncherOperations.DOWNLOAD.name());
        oProcessWorkspace.setWorkspaceId("test");
        oProcessWorkspace.setProductName("filename");
        //oProcessWorkspaceRepository.DeleteProcessWorkspace(oProcessWorkspace);
    }

    public void TestProductViewModel() throws IOException {
        ReadProduct oReadProduct = new ReadProduct();
        ProductViewModel oVM = oReadProduct.getProductViewModel(new File("C:\\temp\\wasdi\\paolo\\44f2bd69-fdcb-4619-990f-fa3687a466af\\S2A_MSIL1C_20170303T014531_N0204_R131_T48DXF_20170303T014609.zip"));

        String sJSON = MongoRepository.s_oMapper.writeValueAsString(oVM);
        LauncherMain.s_oLogger.debug(sJSON);
//        int i=0;
//        i++;
    }

    public void GenerateBandTiff() throws Exception {
        File oFile = (new File("C:\\temp\\wasdi\\paolo\\6de8a7af-c383-4d34-8423-3c229e610d39\\S1A_IW_GRDH_1SDV_20161030T052733_20161030T052758_013715_016011_EBDC.zip"));
        ReadProduct oReadProduct = new ReadProduct();
        ProductViewModel oVM = oReadProduct.getProductViewModel(oFile);

        Product oSentinel = oReadProduct.readSnapProduct(oFile, null);

        GeoCoding oGeoCoding = oSentinel.getSceneGeoCoding();

        Band oBand = oSentinel.getBand(oVM.getBandsGroups().getBands().get(0).getName());
        MultiLevelImage oBandImage = oBand.getSourceImage();
        GeoTIFFMetadata oMetadata = GeoCoding2GeoTIFFMetadata.createGeoTIFFMetadata(oGeoCoding, oBandImage.getWidth(),oBandImage.getHeight());

        GeoTIFF.writeImage(oBandImage, new File("C:\\temp\\wasdi\\paolo\\6de8a7af-c383-4d34-8423-3c229e610d39\\band1.tif"), oMetadata);
//
//        int i=0;
//        i++;

        //String sTiffFile = oReadProduct.writeBigTiff("C:\\temp\\wasdi\\paolo\\6de8a7af-c383-4d34-8423-3c229e610d39\\S1A_IW_GRDH_1SDV_20161030T052733_20161030T052758_013715_016011_EBDC.zip", "C:\\temp\\wasdi\\paolo\\6de8a7af-c383-4d34-8423-3c229e610d39", "6de8a7af-c383-4d34-8423-3c229e610d39");
    }

    public  void TestPublishBand() {
        LauncherMain oLauncherMain = new LauncherMain();
        PublishBandParameter oBandParam = new PublishBandParameter();
        oBandParam.setUserId("Dati Sentinel");
        oBandParam.setWorkspace("8e91a84c-3dcf-470d-8e36-3ad40de80d54");
        oBandParam.setBandName("B1");
        oBandParam.setFileName("S2A_MSIL1C_20170226T102021_N0204_R065_T32TMP_20170226T102458.zip");
        oBandParam.setQueue("6de8a7af-c383-4d34-8423-3c229e610d39");
        oLauncherMain.publishBandImage(oBandParam);
    }

    public  void TestPublish() {
        //LauncherMain oLauncherMain = new LauncherMain();
        PublishParameters oParam = new PublishParameters();
        oParam.setUserId("Dati Sentinel");
        oParam.setWorkspace("6de8a7af-c383-4d34-8423-3c229e610d39");
        oParam.setFileName("S1A_IW_GRDH_1SDV_20170102T045542_20170102T045607_014648_017D27_A1B4.zip");
        oParam.setQueue("6de8a7af-c383-4d34-8423-3c229e610d39");
        //String[] asBands = new String[]{"Amplitude_VH"};
        //oLauncherMain.Publish(oParam);
    }

    public void UserRepository() {

        UserRepository oRepo = new UserRepository();
        oRepo.DeleteUser("test");

        User oUser = new User();
        oUser.setId(1);
        oUser.setUserId("test");
        oUser.setName("Name");
        oUser.setSurname("Surname");
        oUser.setPassword("test");


        oRepo.InsertUser(oUser);

        User oUser2 = oRepo.GetUser("test");

        LauncherMain.s_oLogger.debug("User Letto " + oUser2.getName() + " " + oUser2.getSurname());
    }

    public void WorkspaceRepository() {

        WorkspaceRepository oWSRepo = new WorkspaceRepository();

        Workspace oWS = new Workspace();

        oWS.setUserId("test");
        oWS.setName("WS 1");
        oWS.setCreationDate((double) new Date().getTime());
        oWS.setLastEditDate((double) new Date().getTime());
        oWS.setWorkspaceId(Utils.GetRandomName());
        oWSRepo.InsertWorkspace(oWS);

        oWS.setUserId("test");
        oWS.setName("WS 2");
        oWS.setCreationDate((double) new Date().getTime());
        oWS.setLastEditDate((double) new Date().getTime());
        oWS.setWorkspaceId(Utils.GetRandomName());
        oWSRepo.InsertWorkspace(oWS);

        oWS.setUserId("test");
        oWS.setName("WS 3");
        oWS.setCreationDate((double) new Date().getTime());
        oWS.setLastEditDate((double) new Date().getTime());
        oWS.setWorkspaceId(Utils.GetRandomName());
        oWSRepo.InsertWorkspace(oWS);



        List<Workspace> aoList = oWSRepo.GetWorkspaceByUser("test");

        for (int i=0; i<aoList.size(); i++) {
            Workspace oWork = aoList.get(i);
            LauncherMain.s_oLogger.debug("WS ID = " + oWork.getWorkspaceId());
        }
    }

    public void PublishedBandRepositoryTest() {
        PublishedBandsRepository oRepo = new PublishedBandsRepository();
        PublishedBand oBand = new PublishedBand();
        oBand.setProductName("S1A_IW_GRDH_1SDV_20161019T171429_20161019T171454_013562_015B60_8ABB");
        oBand.setBandName("Intensity_VH");
        oBand.setLayerId("S1A_IW_GRDH_1SDV_20161019T171429_20161019T171454_013562_015B60_8ABB_Intensity_VH");
        oRepo.InsertPublishedBand(oBand);

        PublishedBand oReadIt = oRepo.GetPublishedBand("S1A_IW_GRDH_1SDV_20161019T171429_20161019T171454_013562_015B60_8ABB","Intensity_VH");

        if (oReadIt == null) {
            LauncherMain.s_oLogger.debug("Error");
        }
        else {
            LauncherMain.s_oLogger.debug("Ok");
        }
    }

    public void ProductWorkspaceRepositoryTest() {
        @SuppressWarnings("unused")
		LauncherMain oLauncherMain = new LauncherMain();
        ProductWorkspace oPW = new ProductWorkspace();
        oPW.setProductName("S1A_IW_GRDH_1SDV_20161030T052733_20161030T052758_013715_016011_EBDC.zip");
        oPW.setWorkspaceId("6de8a7af-c383-4d34-8423-3c229e610d39");

        ProductWorkspaceRepository oRepo = new ProductWorkspaceRepository();
        oRepo.InsertProductWorkspace(oPW);
        List<ProductWorkspace> aoList =  oRepo.GetProductsByWorkspace("6de8a7af-c383-4d34-8423-3c229e610d39");

        if (aoList == null) {
            LauncherMain.s_oLogger.debug("Error");
        }
    }

//    public void GetBoundingBox(){
//
//        LauncherMain oLauncherMain = new LauncherMain();
//        GeoserverUtils oGeo = new GeoserverUtils();
//        String sResult = oGeo.GetBoundingBox("S1B_EW_GRDM_1SSH_20170306T001540_20170306T001615_004580_007FC2_FD34_Intensity_HH", "json");
//        System.out.println(sResult);
//    }

//    public void TerrainCorrection(){
//
//        LauncherMain oLauncherMain = new LauncherMain();
//        RangeDopplerGeocodingParameter oParameter = new RangeDopplerGeocodingParameter();
//        oParameter.setUserId("Dati Sentinel");
//        oParameter.setWorkspace("6de8a7af-c383-4d34-8423-3c229e610d39");
//        oParameter.setSourceProductName("S1A_IW_GRDH_1SDV_20170102T045542_20170102T045607_014648_017D27_A1B4.zip");
//        String[] asBands = new String[]{"Amplitude_VH"};
//        //oParameter.getSettings().setSourceBandNames(asBands);
//        InitRabbit("6de8a7af-c383-4d34-8423-3c229e610d39", "Dati Sentinel");
//        //oLauncherMain.TerrainOperation(oParameter);
//
//    }

//    public void DeleteLayer(){
//
//        LauncherMain oLauncherMain = new LauncherMain();
//        String sResponse = GeoserverUtils.DeleteLayer("S1A_IW_GRDH_1SDV_20170116T172218_20170116T172243_014860_0183A6_78B2_Amplitude_VH", "json");
//
//    }

    @SuppressWarnings("unused")
	public void DeleteSession(){

        LauncherMain oLauncherMain = new LauncherMain();
        SessionRepository oRepo = new SessionRepository();
        UserSession oSession = oRepo.GetSession("20365d0e-16e7-4370-ba60-5fb85e5beeb7");
        oRepo.DeleteSession(oSession);


    }

    @SuppressWarnings("unused")
	public void GetFileSize() {

    	/*
    	//replaced by the next one
        //DownloadFile oDownloadFile = DownloadFile.getDownloadFile("SENTINEL");
        DownloadFile oDownloadFile = new DownloadSupplier().supplyDownloader("SENTINEL");
        LauncherMain oLauncherMain = new LauncherMain();

        try {
            long lValue = oDownloadFile.GetDownloadFileSize("https://scihub.copernicus.eu/apihub/odata/v1/Products('cedf3b1b-4ff1-4e3d-8b98-044ca7aaf121')/$value");
            long lFileSizeMega = lValue / (1024 * 1024);

            //set file size
            String sValue = Long.toString(lFileSizeMega);

            System.out.println(sValue);
        } catch (Exception e) {
            e.printStackTrace();
        }
        */
    }

    public void RasterGeometricResampling() {


        LauncherMain oLauncher = new LauncherMain();

        RasterGeometricResampleParameter oParams = new RasterGeometricResampleParameter();

        oParams.setBandName("B1");
        oParams.setDestinationProductName("resampled");
        oParams.setExchange("6de8a7af-c383-4d34-8423-3c229e610d39");
        oParams.setProcessObjId("58bd67d5b68e5a3dab5ea48d");
        oParams.setSourceProductName("S2A_MSIL1C_20170302T100021_N0204_R122_T32TPN_20170302T100020.zip");
        oParams.setUserId("paolo");
        oParams.setWorkspace("6de8a7af-c383-4d34-8423-3c229e610d39");

        oLauncher.rasterGeometricResample(oParams);
    }

    public void TestBB()
    {
        String sResponse = "{\"coverage\":{\"name\":\"S2A_MSIL1C_20170302T100021_N0204_R122_T32TPN_20170302T100020_B6\",\"nativeName\":\"S2A_MSIL1C_20170302T100021_N0204_R122_T32TPN_20170302T100020_B6\",\"namespace\":{\"name\":\"wasdi\",\"href\":\"http:\\/\\/178.22.66.96:8080\\/geoserver\\/rest\\/namespaces\\/wasdi.json\"},\"title\":\"S2A_MSIL1C_20170302T100021_N0204_R122_T32TPN_20170302T100020_B6\",\"nativeCRS\":{\"@class\":\"projected\",\"$\":\"PROJCS[\\\"WGS 84 \\/ UTM zone 32N\\\", \\n  GEOGCS[\\\"WGS 84\\\", \\n    DATUM[\\\"World Geodetic System 1984\\\", \\n      SPHEROID[\\\"WGS 84\\\", 6378137.0, 298.257223563, AUTHORITY[\\\"EPSG\\\",\\\"7030\\\"]], \\n      AUTHORITY[\\\"EPSG\\\",\\\"6326\\\"]], \\n    PRIMEM[\\\"Greenwich\\\", 0.0, AUTHORITY[\\\"EPSG\\\",\\\"8901\\\"]], \\n    UNIT[\\\"degree\\\", 0.017453292519943295], \\n    AXIS[\\\"Geodetic longitude\\\", EAST], \\n    AXIS[\\\"Geodetic latitude\\\", NORTH], \\n    AUTHORITY[\\\"EPSG\\\",\\\"4326\\\"]], \\n  PROJECTION[\\\"Transverse_Mercator\\\", AUTHORITY[\\\"EPSG\\\",\\\"9807\\\"]], \\n  PARAMETER[\\\"central_meridian\\\", 9.0], \\n  PARAMETER[\\\"latitude_of_origin\\\", 0.0], \\n  PARAMETER[\\\"scale_factor\\\", 0.9996], \\n  PARAMETER[\\\"false_easting\\\", 500000.0], \\n  PARAMETER[\\\"false_northing\\\", 0.0], \\n  UNIT[\\\"m\\\", 1.0], \\n  AXIS[\\\"Easting\\\", EAST], \\n  AXIS[\\\"Northing\\\", NORTH], \\n  AUTHORITY[\\\"EPSG\\\",\\\"32632\\\"]]\"},\"srs\":\"EPSG:32632\",\"nativeBoundingBox\":{\"minx\":600000,\"maxx\":709800,\"miny\":4690200,\"maxy\":4800000,\"crs\":{\"@class\":\"projected\",\"$\":\"EPSG:32632\"}},\"latLonBoundingBox\":{\"minx\":10.214287134173002,\"maxx\":11.587536184095345,\"miny\":42.33578223878378,\"maxy\":43.3462000093767,\"crs\":\"GEOGCS[\\\"WGS84(DD)\\\", \\n  DATUM[\\\"WGS84\\\", \\n    SPHEROID[\\\"WGS84\\\", 6378137.0, 298.257223563]], \\n  PRIMEM[\\\"Greenwich\\\", 0.0], \\n  UNIT[\\\"degree\\\", 0.017453292519943295], \\n  AXIS[\\\"Geodetic longitude\\\", EAST], \\n  AXIS[\\\"Geodetic latitude\\\", NORTH]]\"},\"projectionPolicy\":\"FORCE_DECLARED\",\"enabled\":true,\"advertised\":true,\"store\":{\"@class\":\"coverageStore\",\"name\":\"wasdi:S2A_MSIL1C_20170302T100021_N0204_R122_T32TPN_20170302T100020_B6\",\"href\":\"http:\\/\\/178.22.66.96:8080\\/geoserver\\/rest\\/workspaces\\/wasdi\\/coveragestores\\/S2A_MSIL1C_20170302T100021_N0204_R122_T32TPN_20170302T100020_B6.json\"},\"grid\":{\"@dimension\":\"2\",\"range\":{\"low\":\"0 0\",\"high\":\"5490 5490\"},\"transform\":{\"scaleX\":20,\"scaleY\":-20,\"shearX\":0,\"shearY\":0,\"translateX\":600010,\"translateY\":4799990},\"crs\":\"EPSG:32632\"}}}";
        JSONObject oJSONObject = new JSONObject(sResponse);
        if (oJSONObject.has("coverage")) {
            LauncherMain.s_oLogger.debug("GeoserverUtils.GetBoundingBox: Coverage object found");
            JSONObject oCoverage = oJSONObject.getJSONObject("coverage");
            if (oCoverage != null && oCoverage.has("latLonBoundingBox")) {
                LauncherMain.s_oLogger.debug("GeoserverUtils.GetBoundingBox: latLonBoundingBox object found");
                JSONObject oBBox = oCoverage.getJSONObject("latLonBoundingBox");
                String sBBox = oBBox.toString();
                LauncherMain.s_oLogger.debug("GeoserverUtils.GetBoundingBox: latLonBoundingBox " + sBBox);
            }
        }
        else {

            LauncherMain.s_oLogger.debug("GeoserverUtils.GetBoundingBox: Json: " + oJSONObject.toString());
            LauncherMain.s_oLogger.debug("GeoserverUtils.GetBoundingBox: Coverage not found");
        }
    }




}
