import com.bc.ceres.core.ProgressMonitor;
import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Feed;
import org.apache.abdera.parser.Parser;
import org.apache.abdera.parser.ParserOptions;
import org.apache.abdera.writer.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.esa.s1tbx.sar.gpf.geometric.CRSGeoCodingHandler;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.graph.GraphException;
import org.esa.snap.graphbuilder.rcp.dialogs.support.GraphExecuter;
import org.json.JSONObject;
import org.json.XML;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import search.SentinelInfo;

import java.io.*;

/**
 * Created by s.adamo on 21/02/2017.
 */
public class Test {
    public static void main(String[] args) throws Exception {

        //UpdateGraphXml();
        //Rename();
        //Read();
        //Operation();
        AbderaParser();
    }

    private static void Read()
    {
        try {
            Product oProduct = ProductIO.readProduct(new File("C:\\Users\\s.adamo\\.snap\\auxdata\\dem\\SRTM 3Sec\\srtm_39_04.zip"));
            System.out.println("oProduct.getSceneGeoCoding().toString()");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void UpdateGraphXml()
    {

        File oFile = new File("c:\\temp\\wasdi\\Graph.xml");
        try {
            GraphExecuter m_oGraphEx = new GraphExecuter();
            String fileContext = FileUtils.readFileToString(oFile, "UTF-8");
            Product oProduct = ProductIO.readProduct(new File("C:/Users/s.adamo/Documents/S1B_IW_GRDH_1SDV_20170302T050413_20170302T050438_004525_007E0D_0EB9.zip"));
            final CRSGeoCodingHandler crsHandler = new CRSGeoCodingHandler(oProduct, "AUTO:42001", 20, 20);
            CoordinateReferenceSystem targetCRS = crsHandler.getTargetCRS();
            fileContext = fileContext.replace("{InputFile}", "C:/Users/s.adamo/Documents/S1B_IW_GRDH_1SDV_20170302T050413_20170302T050438_004525_007E0D_0EB9.zip");
            fileContext = fileContext.replace("{OutputFile}", "C:\\temp\\wasdi\\" + oProduct.getName());
            fileContext = fileContext.replace("{MPROJ}", targetCRS.toString());
            InputStream in = IOUtils.toInputStream(fileContext, "UTF-8");
            m_oGraphEx.loadGraph(in, oFile, false);
            m_oGraphEx.InitGraph();
            m_oGraphEx.executeGraph(ProgressMonitor.NULL);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GraphException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private static void Operation() throws IOException {
        Product oProduct = ProductIO.readProduct(new File("C:/Users/s.adamo/Documents/S1B_IW_GRDH_1SDV_20170302T050413_20170302T050438_004525_007E0D_0EB9.zip"));
        SentinelThread othread = new SentinelThread("C:/Users/s.adamo/Documents/S1B_IW_GRDH_1SDV_20170302T050413_20170302T050438_004525_007E0D_0EB9.zip");
        othread.ExecuteSNAPOperation(oProduct, "C:\\temp\\wasdi\\Mulesme\\S1A_IW_GRDH_1SDV_20170222T051945_20170222T052010_015392_019421_6625_TC\\");



    }

    private static void Rename()
    {
        SentinelInfo oSentinelInfo = new SentinelInfo();
        oSentinelInfo.setSceneCenterLat("45.8");
        oSentinelInfo.setSceneCenterLon("8.6");
        oSentinelInfo.setOrbit("44");
        oSentinelInfo.setFileName("Test.txt");
        oSentinelInfo.setDownloadLink("www.prova.it");
        SentinelThread oSentinelThread = new SentinelThread(null);
        oSentinelThread.setSentinelInfo(oSentinelInfo);
        try {
            Utils.SerializeObjectToXML("C:\\temp\\wasdi\\Mulesme\\", oSentinelInfo);
            oSentinelThread.SARToMulesmeFormat("S1A_IW_GRDH_1SDV_20170222T051945_20170222T052010_015392_019421_6625", "C:\\temp\\wasdi\\Mulesme\\S1A_IW_GRDH_1SDV_20170222T051945_20170222T052010_015392_019421_6625_TC\\", "C:\\temp\\wasdi\\Mulesme\\");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void AbderaParser()
    {
        String sAbdera =  "<?xml version=\"1.0\" encoding=\"utf-8\"?><feed xmlns:opensearch=\"http://a9.com/-/spec/opensearch/1.1/\" xmlns=\"http://www.w3.org/2005/Atom\">\n" +
                "<title>Sentinels Scientific Data Hub search results for: ( beginPosition:[2017-03-07T00:00:00.000Z TO 2017-03-08T23:59:59.999Z] AND endPosition:[2017-03-07T00:00:00.000Z TO 2017-03-08T23:59:59.999Z] ) AND   (platformname:Sentinel-1 AND filename:S1A_*)</title>\n" +
                "<subtitle>Displaying 0 to 24 of 3000 total results. Request done in 0.111 seconds.</subtitle>\n" +
                "<updated>2017-03-10T08:21:03.241Z</updated>\n" +
                "<author>\n" +
                "<name>Sentinels Scientific Data Hub</name>\n" +
                "</author>\n" +
                "<id>https://scihub.copernicus.eu/apihub/search?q=( beginPosition:[2017-03-07T00:00:00.000Z TO 2017-03-08T23:59:59.999Z] AND endPosition:[2017-03-07T00:00:00.000Z TO 2017-03-08T23:59:59.999Z] ) AND   (platformname:Sentinel-1 AND filename:S1A_*)</id>\n" +
                "<opensearch:totalResults>3000</opensearch:totalResults>\n" +
                "<opensearch:startIndex>0</opensearch:startIndex>\n" +
                "<opensearch:itemsPerPage>25</opensearch:itemsPerPage>\n" +
                "<opensearch:Query role=\"request\" searchTerms=\"( beginPosition:[2017-03-07T00:00:00.000Z TO 2017-03-08T23:59:59.999Z] AND endPosition:[2017-03-07T00:00:00.000Z TO 2017-03-08T23:59:59.999Z] ) AND   (platformname:Sentinel-1 AND filename:S1A_*)\" startPage=\"1\"/>\n" +
                "<link rel=\"self\" type=\"application/atom+xml\" href=\"https://scihub.copernicus.eu/apihub/search?q=( beginPosition:[2017-03-07T00:00:00.000Z TO 2017-03-08T23:59:59.999Z] AND endPosition:[2017-03-07T00:00:00.000Z TO 2017-03-08T23:59:59.999Z] ) AND   (platformname:Sentinel-1 AND filename:S1A_*)&amp;start=0&amp;rows=25&amp;orderby=ingestiondate asc\"/>\n" +
                "<link rel=\"first\" type=\"application/atom+xml\" href=\"https://scihub.copernicus.eu/apihub/search?q=( beginPosition:[2017-03-07T00:00:00.000Z TO 2017-03-08T23:59:59.999Z] AND endPosition:[2017-03-07T00:00:00.000Z TO 2017-03-08T23:59:59.999Z] ) AND   (platformname:Sentinel-1 AND filename:S1A_*)&amp;start=0&amp;rows=25&amp;orderby=ingestiondate asc\"/>\n" +
                "<link rel=\"next\" type=\"application/atom+xml\" href=\"https://scihub.copernicus.eu/apihub/search?q=( beginPosition:[2017-03-07T00:00:00.000Z TO 2017-03-08T23:59:59.999Z] AND endPosition:[2017-03-07T00:00:00.000Z TO 2017-03-08T23:59:59.999Z] ) AND   (platformname:Sentinel-1 AND filename:S1A_*)&amp;start=25&amp;rows=25&amp;orderby=ingestiondate asc\"/>\n" +
                "<link rel=\"last\" type=\"application/atom+xml\" href=\"https://scihub.copernicus.eu/apihub/search?q=( beginPosition:[2017-03-07T00:00:00.000Z TO 2017-03-08T23:59:59.999Z] AND endPosition:[2017-03-07T00:00:00.000Z TO 2017-03-08T23:59:59.999Z] ) AND   (platformname:Sentinel-1 AND filename:S1A_*)&amp;start=2999&amp;rows=25&amp;orderby=ingestiondate asc\"/>\n" +
                "<link rel=\"search\" type=\"application/opensearchdescription+xml\" href=\"opensearch_description.xml\"/>\n" +
                "<entry>\n" +
                "<title>S1A_IW_RAW__0SSH_20170307T013009_20170307T013032_015579_0199CD_BFD2</title>\n" +
                "<link href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('714aed33-a8c6-4fdb-a13f-49702a0367a2')/$value\"/>\n" +
                "<link rel=\"alternative\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('714aed33-a8c6-4fdb-a13f-49702a0367a2')/\"/>\n" +
                "<link rel=\"icon\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('714aed33-a8c6-4fdb-a13f-49702a0367a2')/Products('Quicklook')/$value\"/>\n" +
                "<id>714aed33-a8c6-4fdb-a13f-49702a0367a2</id>\n" +
                "<summary>Date: 2017-03-07T01:30:09.764Z, Instrument: SAR-C SAR, Mode: HH, Satellite: Sentinel-1, Size: 508.77 MB</summary>\n" +
                "<str name=\"acquisitiontype\">NOMINAL</str>\n" +
                "<str name=\"filename\">S1A_IW_RAW__0SSH_20170307T013009_20170307T013032_015579_0199CD_BFD2.SAFE</str>\n" +
                "<str name=\"gmlfootprint\">&lt;gml:Polygon srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\" xmlns:gml=\"http://www.opengis.net/gml\"&gt;\n" +
                "   &lt;gml:outerBoundaryIs&gt;\n" +
                "      &lt;gml:LinearRing&gt;\n" +
                "         &lt;gml:coordinates&gt;-48.8662,49.4753 -50.1689,48.6655 -50.8875,52.0373 -49.5611,52.7659 -48.8662,49.4753 -48.8662,49.4753&lt;/gml:coordinates&gt;\n" +
                "      &lt;/gml:LinearRing&gt;\n" +
                "   &lt;/gml:outerBoundaryIs&gt;\n" +
                "&lt;/gml:Polygon&gt;</str>\n" +
                "<str name=\"format\">SAFE</str>\n" +
                "<str name=\"identifier\">S1A_IW_RAW__0SSH_20170307T013009_20170307T013032_015579_0199CD_BFD2</str>\n" +
                "<date name=\"ingestiondate\">2017-03-07T03:32:05.474Z</date>\n" +
                "<str name=\"instrumentshortname\">SAR-C SAR</str>\n" +
                "<str name=\"sensoroperationalmode\">IW</str>\n" +
                "<str name=\"instrumentname\">Synthetic Aperture Radar (C-band)</str>\n" +
                "<str name=\"footprint\">POLYGON ((49.4753 -48.8662,48.6655 -50.1689,52.0373 -50.8875,52.7659 -49.5611,49.4753 -48.8662,49.4753 -48.8662))</str>\n" +
                "<int name=\"missiondatatakeid\">104909</int>\n" +
                "<str name=\"platformidentifier\">2014-016A</str>\n" +
                "<int name=\"orbitnumber\">15579</int>\n" +
                "<int name=\"lastorbitnumber\">15579</int>\n" +
                "<str name=\"orbitdirection\">DESCENDING</str>\n" +
                "<str name=\"polarisationmode\">HH</str>\n" +
                "<str name=\"productclass\">S</str>\n" +
                "<str name=\"productconsolidation\">SLICE</str>\n" +
                "<str name=\"producttype\">RAW</str>\n" +
                "<int name=\"relativeorbitnumber\">107</int>\n" +
                "<int name=\"lastrelativeorbitnumber\">107</int>\n" +
                "<str name=\"platformname\">Sentinel-1</str>\n" +
                "<date name=\"beginposition\">2017-03-07T01:30:09.764Z</date>\n" +
                "<date name=\"endposition\">2017-03-07T01:30:32.703Z</date>\n" +
                "<str name=\"size\">508.77 MB</str>\n" +
                "<int name=\"slicenumber\">5</int>\n" +
                "<str name=\"status\">ARCHIVED</str>\n" +
                "<str name=\"uuid\">714aed33-a8c6-4fdb-a13f-49702a0367a2</str>\n" +
                "</entry>\n" +
                "<entry>\n" +
                "<title>S1A_IW_RAW__0SSH_20170307T012919_20170307T012952_015579_0199CD_A9C7</title>\n" +
                "<link href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('63977f41-8169-423c-b6c7-280170f22c42')/$value\"/>\n" +
                "<link rel=\"alternative\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('63977f41-8169-423c-b6c7-280170f22c42')/\"/>\n" +
                "<link rel=\"icon\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('63977f41-8169-423c-b6c7-280170f22c42')/Products('Quicklook')/$value\"/>\n" +
                "<id>63977f41-8169-423c-b6c7-280170f22c42</id>\n" +
                "<summary>Date: 2017-03-07T01:29:19.764Z, Instrument: SAR-C SAR, Mode: HH, Satellite: Sentinel-1, Size: 723.32 MB</summary>\n" +
                "<str name=\"acquisitiontype\">NOMINAL</str>\n" +
                "<str name=\"filename\">S1A_IW_RAW__0SSH_20170307T012919_20170307T012952_015579_0199CD_A9C7.SAFE</str>\n" +
                "<str name=\"gmlfootprint\">&lt;gml:Polygon srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\" xmlns:gml=\"http://www.opengis.net/gml\"&gt;\n" +
                "   &lt;gml:outerBoundaryIs&gt;\n" +
                "      &lt;gml:LinearRing&gt;\n" +
                "         &lt;gml:coordinates&gt;-46.0077,51.1048 -47.8629,50.0690 -48.5404,53.3003 -46.6554,54.2335 -46.0077,51.1048 -46.0077,51.1048&lt;/gml:coordinates&gt;\n" +
                "      &lt;/gml:LinearRing&gt;\n" +
                "   &lt;/gml:outerBoundaryIs&gt;\n" +
                "&lt;/gml:Polygon&gt;</str>\n" +
                "<str name=\"format\">SAFE</str>\n" +
                "<str name=\"identifier\">S1A_IW_RAW__0SSH_20170307T012919_20170307T012952_015579_0199CD_A9C7</str>\n" +
                "<date name=\"ingestiondate\">2017-03-07T03:32:06.199Z</date>\n" +
                "<str name=\"instrumentshortname\">SAR-C SAR</str>\n" +
                "<str name=\"sensoroperationalmode\">IW</str>\n" +
                "<str name=\"instrumentname\">Synthetic Aperture Radar (C-band)</str>\n" +
                "<str name=\"footprint\">POLYGON ((51.1048 -46.0077,50.0690 -47.8629,53.3003 -48.5404,54.2335 -46.6554,51.1048 -46.0077,51.1048 -46.0077))</str>\n" +
                "<int name=\"missiondatatakeid\">104909</int>\n" +
                "<str name=\"platformidentifier\">2014-016A</str>\n" +
                "<int name=\"orbitnumber\">15579</int>\n" +
                "<int name=\"lastorbitnumber\">15579</int>\n" +
                "<str name=\"orbitdirection\">DESCENDING</str>\n" +
                "<str name=\"polarisationmode\">HH</str>\n" +
                "<str name=\"productclass\">S</str>\n" +
                "<str name=\"productconsolidation\">SLICE</str>\n" +
                "<str name=\"producttype\">RAW</str>\n" +
                "<int name=\"relativeorbitnumber\">107</int>\n" +
                "<int name=\"lastrelativeorbitnumber\">107</int>\n" +
                "<str name=\"platformname\">Sentinel-1</str>\n" +
                "<date name=\"beginposition\">2017-03-07T01:29:19.764Z</date>\n" +
                "<date name=\"endposition\">2017-03-07T01:29:52.164Z</date>\n" +
                "<str name=\"size\">723.32 MB</str>\n" +
                "<int name=\"slicenumber\">3</int>\n" +
                "<str name=\"status\">ARCHIVED</str>\n" +
                "<str name=\"uuid\">63977f41-8169-423c-b6c7-280170f22c42</str>\n" +
                "</entry>\n" +
                "<entry>\n" +
                "<title>S1A_IW_RAW__0SSH_20170307T012829_20170307T012902_015579_0199CD_039C</title>\n" +
                "<link href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('30584dec-19d3-48a2-9adc-209a666692be')/$value\"/>\n" +
                "<link rel=\"alternative\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('30584dec-19d3-48a2-9adc-209a666692be')/\"/>\n" +
                "<link rel=\"icon\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('30584dec-19d3-48a2-9adc-209a666692be')/Products('Quicklook')/$value\"/>\n" +
                "<id>30584dec-19d3-48a2-9adc-209a666692be</id>\n" +
                "<summary>Date: 2017-03-07T01:28:29.763Z, Instrument: SAR-C SAR, Mode: HH, Satellite: Sentinel-1, Size: 711.79 MB</summary>\n" +
                "<str name=\"acquisitiontype\">NOMINAL</str>\n" +
                "<str name=\"filename\">S1A_IW_RAW__0SSH_20170307T012829_20170307T012902_015579_0199CD_039C.SAFE</str>\n" +
                "<str name=\"gmlfootprint\">&lt;gml:Polygon srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\" xmlns:gml=\"http://www.opengis.net/gml\"&gt;\n" +
                "   &lt;gml:outerBoundaryIs&gt;\n" +
                "      &lt;gml:LinearRing&gt;\n" +
                "         &lt;gml:coordinates&gt;-43.1264,52.5752 -44.9958,51.6390 -45.6284,54.7154 -43.7327,55.5610 -43.1264,52.5752 -43.1264,52.5752&lt;/gml:coordinates&gt;\n" +
                "      &lt;/gml:LinearRing&gt;\n" +
                "   &lt;/gml:outerBoundaryIs&gt;\n" +
                "&lt;/gml:Polygon&gt;</str>\n" +
                "<str name=\"format\">SAFE</str>\n" +
                "<str name=\"identifier\">S1A_IW_RAW__0SSH_20170307T012829_20170307T012902_015579_0199CD_039C</str>\n" +
                "<date name=\"ingestiondate\">2017-03-07T03:32:06.263Z</date>\n" +
                "<str name=\"instrumentshortname\">SAR-C SAR</str>\n" +
                "<str name=\"sensoroperationalmode\">IW</str>\n" +
                "<str name=\"instrumentname\">Synthetic Aperture Radar (C-band)</str>\n" +
                "<str name=\"footprint\">POLYGON ((52.5752 -43.1264,51.6390 -44.9958,54.7154 -45.6284,55.5610 -43.7327,52.5752 -43.1264,52.5752 -43.1264))</str>\n" +
                "<int name=\"missiondatatakeid\">104909</int>\n" +
                "<str name=\"platformidentifier\">2014-016A</str>\n" +
                "<int name=\"orbitnumber\">15579</int>\n" +
                "<int name=\"lastorbitnumber\">15579</int>\n" +
                "<str name=\"orbitdirection\">DESCENDING</str>\n" +
                "<str name=\"polarisationmode\">HH</str>\n" +
                "<str name=\"productclass\">S</str>\n" +
                "<str name=\"productconsolidation\">SLICE</str>\n" +
                "<str name=\"producttype\">RAW</str>\n" +
                "<int name=\"relativeorbitnumber\">107</int>\n" +
                "<int name=\"lastrelativeorbitnumber\">107</int>\n" +
                "<str name=\"platformname\">Sentinel-1</str>\n" +
                "<date name=\"beginposition\">2017-03-07T01:28:29.763Z</date>\n" +
                "<date name=\"endposition\">2017-03-07T01:29:02.163Z</date>\n" +
                "<str name=\"size\">711.79 MB</str>\n" +
                "<int name=\"slicenumber\">1</int>\n" +
                "<str name=\"status\">ARCHIVED</str>\n" +
                "<str name=\"uuid\">30584dec-19d3-48a2-9adc-209a666692be</str>\n" +
                "</entry>\n" +
                "<entry>\n" +
                "<title>S1A_IW_RAW__0SSH_20170307T012854_20170307T012927_015579_0199CD_7425</title>\n" +
                "<link href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('8c72b621-8fd7-4f40-9a81-2da40737d6ae')/$value\"/>\n" +
                "<link rel=\"alternative\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('8c72b621-8fd7-4f40-9a81-2da40737d6ae')/\"/>\n" +
                "<link rel=\"icon\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('8c72b621-8fd7-4f40-9a81-2da40737d6ae')/Products('Quicklook')/$value\"/>\n" +
                "<id>8c72b621-8fd7-4f40-9a81-2da40737d6ae</id>\n" +
                "<summary>Date: 2017-03-07T01:28:54.764Z, Instrument: SAR-C SAR, Mode: HH, Satellite: Sentinel-1, Size: 722.02 MB</summary>\n" +
                "<str name=\"acquisitiontype\">NOMINAL</str>\n" +
                "<str name=\"filename\">S1A_IW_RAW__0SSH_20170307T012854_20170307T012927_015579_0199CD_7425.SAFE</str>\n" +
                "<str name=\"gmlfootprint\">&lt;gml:Polygon srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\" xmlns:gml=\"http://www.opengis.net/gml\"&gt;\n" +
                "   &lt;gml:outerBoundaryIs&gt;\n" +
                "      &lt;gml:LinearRing&gt;\n" +
                "         &lt;gml:coordinates&gt;-44.5697,51.8581 -46.4322,50.8744 -47.0866,54.0258 -45.1961,54.9131 -44.5697,51.8581 -44.5697,51.8581&lt;/gml:coordinates&gt;\n" +
                "      &lt;/gml:LinearRing&gt;\n" +
                "   &lt;/gml:outerBoundaryIs&gt;\n" +
                "&lt;/gml:Polygon&gt;</str>\n" +
                "<str name=\"format\">SAFE</str>\n" +
                "<str name=\"identifier\">S1A_IW_RAW__0SSH_20170307T012854_20170307T012927_015579_0199CD_7425</str>\n" +
                "<date name=\"ingestiondate\">2017-03-07T03:32:06.357Z</date>\n" +
                "<str name=\"instrumentshortname\">SAR-C SAR</str>\n" +
                "<str name=\"sensoroperationalmode\">IW</str>\n" +
                "<str name=\"instrumentname\">Synthetic Aperture Radar (C-band)</str>\n" +
                "<str name=\"footprint\">POLYGON ((51.8581 -44.5697,50.8744 -46.4322,54.0258 -47.0866,54.9131 -45.1961,51.8581 -44.5697,51.8581 -44.5697))</str>\n" +
                "<int name=\"missiondatatakeid\">104909</int>\n" +
                "<str name=\"platformidentifier\">2014-016A</str>\n" +
                "<int name=\"orbitnumber\">15579</int>\n" +
                "<int name=\"lastorbitnumber\">15579</int>\n" +
                "<str name=\"orbitdirection\">DESCENDING</str>\n" +
                "<str name=\"polarisationmode\">HH</str>\n" +
                "<str name=\"productclass\">S</str>\n" +
                "<str name=\"productconsolidation\">SLICE</str>\n" +
                "<str name=\"producttype\">RAW</str>\n" +
                "<int name=\"relativeorbitnumber\">107</int>\n" +
                "<int name=\"lastrelativeorbitnumber\">107</int>\n" +
                "<str name=\"platformname\">Sentinel-1</str>\n" +
                "<date name=\"beginposition\">2017-03-07T01:28:54.764Z</date>\n" +
                "<date name=\"endposition\">2017-03-07T01:29:27.163Z</date>\n" +
                "<str name=\"size\">722.02 MB</str>\n" +
                "<int name=\"slicenumber\">2</int>\n" +
                "<str name=\"status\">ARCHIVED</str>\n" +
                "<str name=\"uuid\">8c72b621-8fd7-4f40-9a81-2da40737d6ae</str>\n" +
                "</entry>\n" +
                "<entry>\n" +
                "<title>S1A_IW_RAW__0SSH_20170307T012944_20170307T013017_015579_0199CD_B8E8</title>\n" +
                "<link href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('3439c62f-5a49-4373-8795-9455daf40df2')/$value\"/>\n" +
                "<link rel=\"alternative\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('3439c62f-5a49-4373-8795-9455daf40df2')/\"/>\n" +
                "<link rel=\"icon\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('3439c62f-5a49-4373-8795-9455daf40df2')/Products('Quicklook')/$value\"/>\n" +
                "<id>3439c62f-5a49-4373-8795-9455daf40df2</id>\n" +
                "<summary>Date: 2017-03-07T01:29:44.764Z, Instrument: SAR-C SAR, Mode: HH, Satellite: Sentinel-1, Size: 727.09 MB</summary>\n" +
                "<str name=\"acquisitiontype\">NOMINAL</str>\n" +
                "<str name=\"filename\">S1A_IW_RAW__0SSH_20170307T012944_20170307T013017_015579_0199CD_B8E8.SAFE</str>\n" +
                "<str name=\"gmlfootprint\">&lt;gml:Polygon srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\" xmlns:gml=\"http://www.opengis.net/gml\"&gt;\n" +
                "   &lt;gml:outerBoundaryIs&gt;\n" +
                "      &lt;gml:LinearRing&gt;\n" +
                "         &lt;gml:coordinates&gt;-47.4400,50.3118 -49.2871,49.2187 -49.9894,52.5350 -48.1106,53.5190 -47.4400,50.3118 -47.4400,50.3118&lt;/gml:coordinates&gt;\n" +
                "      &lt;/gml:LinearRing&gt;\n" +
                "   &lt;/gml:outerBoundaryIs&gt;\n" +
                "&lt;/gml:Polygon&gt;</str>\n" +
                "<str name=\"format\">SAFE</str>\n" +
                "<str name=\"identifier\">S1A_IW_RAW__0SSH_20170307T012944_20170307T013017_015579_0199CD_B8E8</str>\n" +
                "<date name=\"ingestiondate\">2017-03-07T03:32:06.561Z</date>\n" +
                "<str name=\"instrumentshortname\">SAR-C SAR</str>\n" +
                "<str name=\"sensoroperationalmode\">IW</str>\n" +
                "<str name=\"instrumentname\">Synthetic Aperture Radar (C-band)</str>\n" +
                "<str name=\"footprint\">POLYGON ((50.3118 -47.4400,49.2187 -49.2871,52.5350 -49.9894,53.5190 -48.1106,50.3118 -47.4400,50.3118 -47.4400))</str>\n" +
                "<int name=\"missiondatatakeid\">104909</int>\n" +
                "<str name=\"platformidentifier\">2014-016A</str>\n" +
                "<int name=\"orbitnumber\">15579</int>\n" +
                "<int name=\"lastorbitnumber\">15579</int>\n" +
                "<str name=\"orbitdirection\">DESCENDING</str>\n" +
                "<str name=\"polarisationmode\">HH</str>\n" +
                "<str name=\"productclass\">S</str>\n" +
                "<str name=\"productconsolidation\">SLICE</str>\n" +
                "<str name=\"producttype\">RAW</str>\n" +
                "<int name=\"relativeorbitnumber\">107</int>\n" +
                "<int name=\"lastrelativeorbitnumber\">107</int>\n" +
                "<str name=\"platformname\">Sentinel-1</str>\n" +
                "<date name=\"beginposition\">2017-03-07T01:29:44.764Z</date>\n" +
                "<date name=\"endposition\">2017-03-07T01:30:17.163Z</date>\n" +
                "<str name=\"size\">727.09 MB</str>\n" +
                "<int name=\"slicenumber\">4</int>\n" +
                "<str name=\"status\">ARCHIVED</str>\n" +
                "<str name=\"uuid\">3439c62f-5a49-4373-8795-9455daf40df2</str>\n" +
                "</entry>\n" +
                "<entry>\n" +
                "<title>S1A_IW_GRDH_1SSH_20170307T013013_20170307T013033_015579_0199CD_906C</title>\n" +
                "<link href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('861b8324-fdc9-4edc-8451-30e31aca8174')/$value\"/>\n" +
                "<link rel=\"alternative\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('861b8324-fdc9-4edc-8451-30e31aca8174')/\"/>\n" +
                "<link rel=\"icon\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('861b8324-fdc9-4edc-8451-30e31aca8174')/Products('Quicklook')/$value\"/>\n" +
                "<id>861b8324-fdc9-4edc-8451-30e31aca8174</id>\n" +
                "<summary>Date: 2017-03-07T01:30:13.464Z, Instrument: SAR-C SAR, Mode: HH, Satellite: Sentinel-1, Size: 648.99 MB</summary>\n" +
                "<str name=\"acquisitiontype\">NOMINAL</str>\n" +
                "<str name=\"filename\">S1A_IW_GRDH_1SSH_20170307T013013_20170307T013033_015579_0199CD_906C.SAFE</str>\n" +
                "<str name=\"gmlfootprint\">&lt;gml:Polygon srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\" xmlns:gml=\"http://www.opengis.net/gml\"&gt;\n" +
                "   &lt;gml:outerBoundaryIs&gt;\n" +
                "      &lt;gml:LinearRing&gt;\n" +
                "         &lt;gml:coordinates&gt;-50.807358,52.205193 -49.993320,48.836304 -48.870728,49.511101 -49.667778,52.811920 -50.807358,52.205193&lt;/gml:coordinates&gt;\n" +
                "      &lt;/gml:LinearRing&gt;\n" +
                "   &lt;/gml:outerBoundaryIs&gt;\n" +
                "&lt;/gml:Polygon&gt;</str>\n" +
                "<str name=\"format\">SAFE</str>\n" +
                "<str name=\"identifier\">S1A_IW_GRDH_1SSH_20170307T013013_20170307T013033_015579_0199CD_906C</str>\n" +
                "<date name=\"ingestiondate\">2017-03-07T04:02:13.161Z</date>\n" +
                "<str name=\"instrumentshortname\">SAR-C SAR</str>\n" +
                "<str name=\"sensoroperationalmode\">IW</str>\n" +
                "<str name=\"instrumentname\">Synthetic Aperture Radar (C-band)</str>\n" +
                "<str name=\"swathidentifier\">IW</str>\n" +
                "<str name=\"footprint\">POLYGON ((52.205193 -50.807358,48.836304 -49.993320,49.511101 -48.870728,52.811920 -49.667778,52.205193 -50.807358))</str>\n" +
                "<int name=\"missiondatatakeid\">104909</int>\n" +
                "<str name=\"platformidentifier\">2014-016A</str>\n" +
                "<int name=\"orbitnumber\">15579</int>\n" +
                "<int name=\"lastorbitnumber\">15579</int>\n" +
                "<str name=\"orbitdirection\">DESCENDING</str>\n" +
                "<str name=\"polarisationmode\">HH</str>\n" +
                "<str name=\"productclass\">S</str>\n" +
                "<str name=\"producttype\">GRD</str>\n" +
                "<int name=\"relativeorbitnumber\">107</int>\n" +
                "<int name=\"lastrelativeorbitnumber\">107</int>\n" +
                "<str name=\"platformname\">Sentinel-1</str>\n" +
                "<date name=\"beginposition\">2017-03-07T01:30:13.464Z</date>\n" +
                "<date name=\"endposition\">2017-03-07T01:30:33.05Z</date>\n" +
                "<str name=\"size\">648.99 MB</str>\n" +
                "<int name=\"slicenumber\">5</int>\n" +
                "<str name=\"status\">ARCHIVED</str>\n" +
                "<str name=\"uuid\">861b8324-fdc9-4edc-8451-30e31aca8174</str>\n" +
                "</entry>\n" +
                "<entry>\n" +
                "<title>S1A_IW_GRDH_1SSH_20170307T012923_20170307T012948_015579_0199CD_F1D3</title>\n" +
                "<link href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('5e5c0b2e-d2b9-45df-91d8-70a5559fa657')/$value\"/>\n" +
                "<link rel=\"alternative\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('5e5c0b2e-d2b9-45df-91d8-70a5559fa657')/\"/>\n" +
                "<link rel=\"icon\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('5e5c0b2e-d2b9-45df-91d8-70a5559fa657')/Products('Quicklook')/$value\"/>\n" +
                "<id>5e5c0b2e-d2b9-45df-91d8-70a5559fa657</id>\n" +
                "<summary>Date: 2017-03-07T01:29:23.463Z, Instrument: SAR-C SAR, Mode: HH, Satellite: Sentinel-1, Size: 827.03 MB</summary>\n" +
                "<str name=\"acquisitiontype\">NOMINAL</str>\n" +
                "<str name=\"filename\">S1A_IW_GRDH_1SSH_20170307T012923_20170307T012948_015579_0199CD_F1D3.SAFE</str>\n" +
                "<str name=\"gmlfootprint\">&lt;gml:Polygon srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\" xmlns:gml=\"http://www.opengis.net/gml\"&gt;\n" +
                "   &lt;gml:outerBoundaryIs&gt;\n" +
                "      &lt;gml:LinearRing&gt;\n" +
                "         &lt;gml:coordinates&gt;-48.208916,53.548790 -47.433334,50.334560 -45.988976,51.109802 -46.745750,54.247402 -48.208916,53.548790&lt;/gml:coordinates&gt;\n" +
                "      &lt;/gml:LinearRing&gt;\n" +
                "   &lt;/gml:outerBoundaryIs&gt;\n" +
                "&lt;/gml:Polygon&gt;</str>\n" +
                "<str name=\"format\">SAFE</str>\n" +
                "<str name=\"identifier\">S1A_IW_GRDH_1SSH_20170307T012923_20170307T012948_015579_0199CD_F1D3</str>\n" +
                "<date name=\"ingestiondate\">2017-03-07T04:02:15.385Z</date>\n" +
                "<str name=\"instrumentshortname\">SAR-C SAR</str>\n" +
                "<str name=\"sensoroperationalmode\">IW</str>\n" +
                "<str name=\"instrumentname\">Synthetic Aperture Radar (C-band)</str>\n" +
                "<str name=\"swathidentifier\">IW</str>\n" +
                "<str name=\"footprint\">POLYGON ((53.548790 -48.208916,50.334560 -47.433334,51.109802 -45.988976,54.247402 -46.745750,53.548790 -48.208916))</str>\n" +
                "<int name=\"missiondatatakeid\">104909</int>\n" +
                "<str name=\"platformidentifier\">2014-016A</str>\n" +
                "<int name=\"orbitnumber\">15579</int>\n" +
                "<int name=\"lastorbitnumber\">15579</int>\n" +
                "<str name=\"orbitdirection\">DESCENDING</str>\n" +
                "<str name=\"polarisationmode\">HH</str>\n" +
                "<str name=\"productclass\">S</str>\n" +
                "<str name=\"producttype\">GRD</str>\n" +
                "<int name=\"relativeorbitnumber\">107</int>\n" +
                "<int name=\"lastrelativeorbitnumber\">107</int>\n" +
                "<str name=\"platformname\">Sentinel-1</str>\n" +
                "<date name=\"beginposition\">2017-03-07T01:29:23.463Z</date>\n" +
                "<date name=\"endposition\">2017-03-07T01:29:48.463Z</date>\n" +
                "<str name=\"size\">827.03 MB</str>\n" +
                "<int name=\"slicenumber\">3</int>\n" +
                "<str name=\"status\">ARCHIVED</str>\n" +
                "<str name=\"uuid\">5e5c0b2e-d2b9-45df-91d8-70a5559fa657</str>\n" +
                "</entry>\n" +
                "<entry>\n" +
                "<title>S1A_IW_GRDH_1SSH_20170307T012948_20170307T013013_015579_0199CD_ED01</title>\n" +
                "<link href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('b1549433-0c23-4e8b-9adf-be53f5b1cae4')/$value\"/>\n" +
                "<link rel=\"alternative\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('b1549433-0c23-4e8b-9adf-be53f5b1cae4')/\"/>\n" +
                "<link rel=\"icon\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('b1549433-0c23-4e8b-9adf-be53f5b1cae4')/Products('Quicklook')/$value\"/>\n" +
                "<id>b1549433-0c23-4e8b-9adf-be53f5b1cae4</id>\n" +
                "<summary>Date: 2017-03-07T01:29:48.464Z, Instrument: SAR-C SAR, Mode: HH, Satellite: Sentinel-1, Size: 827.67 MB</summary>\n" +
                "<str name=\"acquisitiontype\">NOMINAL</str>\n" +
                "<str name=\"filename\">S1A_IW_GRDH_1SSH_20170307T012948_20170307T013013_015579_0199CD_ED01.SAFE</str>\n" +
                "<str name=\"gmlfootprint\">&lt;gml:Polygon srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\" xmlns:gml=\"http://www.opengis.net/gml\"&gt;\n" +
                "   &lt;gml:outerBoundaryIs&gt;\n" +
                "      &lt;gml:LinearRing&gt;\n" +
                "         &lt;gml:coordinates&gt;-49.667690,52.811966 -48.871235,49.513435 -47.432777,50.332035 -48.209003,53.548748 -49.667690,52.811966&lt;/gml:coordinates&gt;\n" +
                "      &lt;/gml:LinearRing&gt;\n" +
                "   &lt;/gml:outerBoundaryIs&gt;\n" +
                "&lt;/gml:Polygon&gt;</str>\n" +
                "<str name=\"format\">SAFE</str>\n" +
                "<str name=\"identifier\">S1A_IW_GRDH_1SSH_20170307T012948_20170307T013013_015579_0199CD_ED01</str>\n" +
                "<date name=\"ingestiondate\">2017-03-07T04:02:15.545Z</date>\n" +
                "<str name=\"instrumentshortname\">SAR-C SAR</str>\n" +
                "<str name=\"sensoroperationalmode\">IW</str>\n" +
                "<str name=\"instrumentname\">Synthetic Aperture Radar (C-band)</str>\n" +
                "<str name=\"swathidentifier\">IW</str>\n" +
                "<str name=\"footprint\">POLYGON ((52.811966 -49.667690,49.513435 -48.871235,50.332035 -47.432777,53.548748 -48.209003,52.811966 -49.667690))</str>\n" +
                "<int name=\"missiondatatakeid\">104909</int>\n" +
                "<str name=\"platformidentifier\">2014-016A</str>\n" +
                "<int name=\"orbitnumber\">15579</int>\n" +
                "<int name=\"lastorbitnumber\">15579</int>\n" +
                "<str name=\"orbitdirection\">DESCENDING</str>\n" +
                "<str name=\"polarisationmode\">HH</str>\n" +
                "<str name=\"productclass\">S</str>\n" +
                "<str name=\"producttype\">GRD</str>\n" +
                "<int name=\"relativeorbitnumber\">107</int>\n" +
                "<int name=\"lastrelativeorbitnumber\">107</int>\n" +
                "<str name=\"platformname\">Sentinel-1</str>\n" +
                "<date name=\"beginposition\">2017-03-07T01:29:48.464Z</date>\n" +
                "<date name=\"endposition\">2017-03-07T01:30:13.462Z</date>\n" +
                "<str name=\"size\">827.67 MB</str>\n" +
                "<int name=\"slicenumber\">4</int>\n" +
                "<str name=\"status\">ARCHIVED</str>\n" +
                "<str name=\"uuid\">b1549433-0c23-4e8b-9adf-be53f5b1cae4</str>\n" +
                "</entry>\n" +
                "<entry>\n" +
                "<title>S1A_IW_GRDH_1SSH_20170307T012858_20170307T012923_015579_0199CD_9928</title>\n" +
                "<link href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('84baa37b-bfaa-4c8d-98b3-18f129eccce4')/$value\"/>\n" +
                "<link rel=\"alternative\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('84baa37b-bfaa-4c8d-98b3-18f129eccce4')/\"/>\n" +
                "<link rel=\"icon\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('84baa37b-bfaa-4c8d-98b3-18f129eccce4')/Products('Quicklook')/$value\"/>\n" +
                "<id>84baa37b-bfaa-4c8d-98b3-18f129eccce4</id>\n" +
                "<summary>Date: 2017-03-07T01:28:58.464Z, Instrument: SAR-C SAR, Mode: HH, Satellite: Sentinel-1, Size: 826.36 MB</summary>\n" +
                "<str name=\"acquisitiontype\">NOMINAL</str>\n" +
                "<str name=\"filename\">S1A_IW_GRDH_1SSH_20170307T012858_20170307T012923_015579_0199CD_9928.SAFE</str>\n" +
                "<str name=\"gmlfootprint\">&lt;gml:Polygon srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\" xmlns:gml=\"http://www.opengis.net/gml\"&gt;\n" +
                "   &lt;gml:outerBoundaryIs&gt;\n" +
                "      &lt;gml:LinearRing&gt;\n" +
                "         &lt;gml:coordinates&gt;-46.745663,54.247440 -45.989483,51.112148 -44.539909,51.847980 -45.278580,54.911518 -46.745663,54.247440&lt;/gml:coordinates&gt;\n" +
                "      &lt;/gml:LinearRing&gt;\n" +
                "   &lt;/gml:outerBoundaryIs&gt;\n" +
                "&lt;/gml:Polygon&gt;</str>\n" +
                "<str name=\"format\">SAFE</str>\n" +
                "<str name=\"identifier\">S1A_IW_GRDH_1SSH_20170307T012858_20170307T012923_015579_0199CD_9928</str>\n" +
                "<date name=\"ingestiondate\">2017-03-07T04:02:16.405Z</date>\n" +
                "<str name=\"instrumentshortname\">SAR-C SAR</str>\n" +
                "<str name=\"sensoroperationalmode\">IW</str>\n" +
                "<str name=\"instrumentname\">Synthetic Aperture Radar (C-band)</str>\n" +
                "<str name=\"swathidentifier\">IW</str>\n" +
                "<str name=\"footprint\">POLYGON ((54.247440 -46.745663,51.112148 -45.989483,51.847980 -44.539909,54.911518 -45.278580,54.247440 -46.745663))</str>\n" +
                "<int name=\"missiondatatakeid\">104909</int>\n" +
                "<str name=\"platformidentifier\">2014-016A</str>\n" +
                "<int name=\"orbitnumber\">15579</int>\n" +
                "<int name=\"lastorbitnumber\">15579</int>\n" +
                "<str name=\"orbitdirection\">DESCENDING</str>\n" +
                "<str name=\"polarisationmode\">HH</str>\n" +
                "<str name=\"productclass\">S</str>\n" +
                "<str name=\"producttype\">GRD</str>\n" +
                "<int name=\"relativeorbitnumber\">107</int>\n" +
                "<int name=\"lastrelativeorbitnumber\">107</int>\n" +
                "<str name=\"platformname\">Sentinel-1</str>\n" +
                "<date name=\"beginposition\">2017-03-07T01:28:58.464Z</date>\n" +
                "<date name=\"endposition\">2017-03-07T01:29:23.462Z</date>\n" +
                "<str name=\"size\">826.36 MB</str>\n" +
                "<int name=\"slicenumber\">2</int>\n" +
                "<str name=\"status\">ARCHIVED</str>\n" +
                "<str name=\"uuid\">84baa37b-bfaa-4c8d-98b3-18f129eccce4</str>\n" +
                "</entry>\n" +
                "<entry>\n" +
                "<title>S1A_IW_GRDH_1SSH_20170307T012829_20170307T012858_015579_0199CD_5C21</title>\n" +
                "<link href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('400ece81-b3a8-41ee-b2a9-4d137a2d7be9')/$value\"/>\n" +
                "<link rel=\"alternative\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('400ece81-b3a8-41ee-b2a9-4d137a2d7be9')/\"/>\n" +
                "<link rel=\"icon\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('400ece81-b3a8-41ee-b2a9-4d137a2d7be9')/Products('Quicklook')/$value\"/>\n" +
                "<id>400ece81-b3a8-41ee-b2a9-4d137a2d7be9</id>\n" +
                "<summary>Date: 2017-03-07T01:28:29.465Z, Instrument: SAR-C SAR, Mode: HH, Satellite: Sentinel-1, Size: 957.86 MB</summary>\n" +
                "<str name=\"acquisitiontype\">NOMINAL</str>\n" +
                "<str name=\"filename\">S1A_IW_GRDH_1SSH_20170307T012829_20170307T012858_015579_0199CD_5C21.SAFE</str>\n" +
                "<str name=\"gmlfootprint\">&lt;gml:Polygon srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\" xmlns:gml=\"http://www.opengis.net/gml\"&gt;\n" +
                "   &lt;gml:outerBoundaryIs&gt;\n" +
                "      &lt;gml:LinearRing&gt;\n" +
                "         &lt;gml:coordinates&gt;-45.278492,54.911556 -44.540401,51.850273 -42.852863,52.659229 -43.572105,55.642990 -45.278492,54.911556&lt;/gml:coordinates&gt;\n" +
                "      &lt;/gml:LinearRing&gt;\n" +
                "   &lt;/gml:outerBoundaryIs&gt;\n" +
                "&lt;/gml:Polygon&gt;</str>\n" +
                "<str name=\"format\">SAFE</str>\n" +
                "<str name=\"identifier\">S1A_IW_GRDH_1SSH_20170307T012829_20170307T012858_015579_0199CD_5C21</str>\n" +
                "<date name=\"ingestiondate\">2017-03-07T04:02:17.347Z</date>\n" +
                "<str name=\"instrumentshortname\">SAR-C SAR</str>\n" +
                "<str name=\"sensoroperationalmode\">IW</str>\n" +
                "<str name=\"instrumentname\">Synthetic Aperture Radar (C-band)</str>\n" +
                "<str name=\"swathidentifier\">IW</str>\n" +
                "<str name=\"footprint\">POLYGON ((54.911556 -45.278492,51.850273 -44.540401,52.659229 -42.852863,55.642990 -43.572105,54.911556 -45.278492))</str>\n" +
                "<int name=\"missiondatatakeid\">104909</int>\n" +
                "<str name=\"platformidentifier\">2014-016A</str>\n" +
                "<int name=\"orbitnumber\">15579</int>\n" +
                "<int name=\"lastorbitnumber\">15579</int>\n" +
                "<str name=\"orbitdirection\">DESCENDING</str>\n" +
                "<str name=\"polarisationmode\">HH</str>\n" +
                "<str name=\"productclass\">S</str>\n" +
                "<str name=\"producttype\">GRD</str>\n" +
                "<int name=\"relativeorbitnumber\">107</int>\n" +
                "<int name=\"lastrelativeorbitnumber\">107</int>\n" +
                "<str name=\"platformname\">Sentinel-1</str>\n" +
                "<date name=\"beginposition\">2017-03-07T01:28:29.465Z</date>\n" +
                "<date name=\"endposition\">2017-03-07T01:28:58.462Z</date>\n" +
                "<str name=\"size\">957.86 MB</str>\n" +
                "<int name=\"slicenumber\">1</int>\n" +
                "<str name=\"status\">ARCHIVED</str>\n" +
                "<str name=\"uuid\">400ece81-b3a8-41ee-b2a9-4d137a2d7be9</str>\n" +
                "</entry>\n" +
                "<entry>\n" +
                "<title>S1A_IW_SLC__1SSH_20170307T013012_20170307T013033_015579_0199CD_F927</title>\n" +
                "<link href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('8ae0bf1e-8d51-4b6d-970a-fe35c607f6c6')/$value\"/>\n" +
                "<link rel=\"alternative\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('8ae0bf1e-8d51-4b6d-970a-fe35c607f6c6')/\"/>\n" +
                "<link rel=\"icon\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('8ae0bf1e-8d51-4b6d-970a-fe35c607f6c6')/Products('Quicklook')/$value\"/>\n" +
                "<id>8ae0bf1e-8d51-4b6d-970a-fe35c607f6c6</id>\n" +
                "<summary>Date: 2017-03-07T01:30:12.458Z, Instrument: SAR-C SAR, Mode: HH, Satellite: Sentinel-1, Size: 2.6 GB</summary>\n" +
                "<str name=\"acquisitiontype\">NOMINAL</str>\n" +
                "<str name=\"filename\">S1A_IW_SLC__1SSH_20170307T013012_20170307T013033_015579_0199CD_F927.SAFE</str>\n" +
                "<str name=\"gmlfootprint\">&lt;gml:Polygon srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\" xmlns:gml=\"http://www.opengis.net/gml\"&gt;\n" +
                "   &lt;gml:outerBoundaryIs&gt;\n" +
                "      &lt;gml:LinearRing&gt;\n" +
                "         &lt;gml:coordinates&gt;-50.808384,52.205917 -49.997070,48.847321 -48.817215,49.560539 -49.609596,52.843384 -50.808384,52.205917&lt;/gml:coordinates&gt;\n" +
                "      &lt;/gml:LinearRing&gt;\n" +
                "   &lt;/gml:outerBoundaryIs&gt;\n" +
                "&lt;/gml:Polygon&gt;</str>\n" +
                "<str name=\"format\">SAFE</str>\n" +
                "<str name=\"identifier\">S1A_IW_SLC__1SSH_20170307T013012_20170307T013033_015579_0199CD_F927</str>\n" +
                "<date name=\"ingestiondate\">2017-03-07T04:14:18.115Z</date>\n" +
                "<str name=\"instrumentshortname\">SAR-C SAR</str>\n" +
                "<str name=\"sensoroperationalmode\">IW</str>\n" +
                "<str name=\"instrumentname\">Synthetic Aperture Radar (C-band)</str>\n" +
                "<str name=\"swathidentifier\">IW1 IW2 IW3</str>\n" +
                "<str name=\"footprint\">POLYGON ((52.205917 -50.808384,48.847321 -49.997070,49.560539 -48.817215,52.843384 -49.609596,52.205917 -50.808384))</str>\n" +
                "<int name=\"missiondatatakeid\">104909</int>\n" +
                "<str name=\"platformidentifier\">2014-016A</str>\n" +
                "<int name=\"orbitnumber\">15579</int>\n" +
                "<int name=\"lastorbitnumber\">15579</int>\n" +
                "<str name=\"orbitdirection\">DESCENDING</str>\n" +
                "<str name=\"polarisationmode\">HH</str>\n" +
                "<str name=\"productclass\">S</str>\n" +
                "<str name=\"producttype\">SLC</str>\n" +
                "<int name=\"relativeorbitnumber\">107</int>\n" +
                "<int name=\"lastrelativeorbitnumber\">107</int>\n" +
                "<str name=\"platformname\">Sentinel-1</str>\n" +
                "<date name=\"beginposition\">2017-03-07T01:30:12.458Z</date>\n" +
                "<date name=\"endposition\">2017-03-07T01:30:33.061Z</date>\n" +
                "<str name=\"size\">2.6 GB</str>\n" +
                "<int name=\"slicenumber\">5</int>\n" +
                "<str name=\"status\">ARCHIVED</str>\n" +
                "<str name=\"uuid\">8ae0bf1e-8d51-4b6d-970a-fe35c607f6c6</str>\n" +
                "</entry>\n" +
                "<entry>\n" +
                "<title>S1A_IW_SLC__1SSH_20170307T012922_20170307T012949_015579_0199CD_4631</title>\n" +
                "<link href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('0a4435d6-7489-40fe-8ed4-dd77aab93e27')/$value\"/>\n" +
                "<link rel=\"alternative\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('0a4435d6-7489-40fe-8ed4-dd77aab93e27')/\"/>\n" +
                "<link rel=\"icon\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('0a4435d6-7489-40fe-8ed4-dd77aab93e27')/Products('Quicklook')/$value\"/>\n" +
                "<id>0a4435d6-7489-40fe-8ed4-dd77aab93e27</id>\n" +
                "<summary>Date: 2017-03-07T01:29:22.806Z, Instrument: SAR-C SAR, Mode: HH, Satellite: Sentinel-1, Size: 3.5 GB</summary>\n" +
                "<str name=\"acquisitiontype\">NOMINAL</str>\n" +
                "<str name=\"filename\">S1A_IW_SLC__1SSH_20170307T012922_20170307T012949_015579_0199CD_4631.SAFE</str>\n" +
                "<str name=\"gmlfootprint\">&lt;gml:Polygon srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\" xmlns:gml=\"http://www.opengis.net/gml\"&gt;\n" +
                "   &lt;gml:outerBoundaryIs&gt;\n" +
                "      &lt;gml:LinearRing&gt;\n" +
                "         &lt;gml:coordinates&gt;-48.285042,53.512585 -47.512253,50.308777 -45.956520,51.150616 -46.707619,54.266270 -48.285042,53.512585&lt;/gml:coordinates&gt;\n" +
                "      &lt;/gml:LinearRing&gt;\n" +
                "   &lt;/gml:outerBoundaryIs&gt;\n" +
                "&lt;/gml:Polygon&gt;</str>\n" +
                "<str name=\"format\">SAFE</str>\n" +
                "<str name=\"identifier\">S1A_IW_SLC__1SSH_20170307T012922_20170307T012949_015579_0199CD_4631</str>\n" +
                "<date name=\"ingestiondate\">2017-03-07T04:14:22.574Z</date>\n" +
                "<str name=\"instrumentshortname\">SAR-C SAR</str>\n" +
                "<str name=\"sensoroperationalmode\">IW</str>\n" +
                "<str name=\"instrumentname\">Synthetic Aperture Radar (C-band)</str>\n" +
                "<str name=\"swathidentifier\">IW1 IW2 IW3</str>\n" +
                "<str name=\"footprint\">POLYGON ((53.512585 -48.285042,50.308777 -47.512253,51.150616 -45.956520,54.266270 -46.707619,53.512585 -48.285042))</str>\n" +
                "<int name=\"missiondatatakeid\">104909</int>\n" +
                "<str name=\"platformidentifier\">2014-016A</str>\n" +
                "<int name=\"orbitnumber\">15579</int>\n" +
                "<int name=\"lastorbitnumber\">15579</int>\n" +
                "<str name=\"orbitdirection\">DESCENDING</str>\n" +
                "<str name=\"polarisationmode\">HH</str>\n" +
                "<str name=\"productclass\">S</str>\n" +
                "<str name=\"producttype\">SLC</str>\n" +
                "<int name=\"relativeorbitnumber\">107</int>\n" +
                "<int name=\"lastrelativeorbitnumber\">107</int>\n" +
                "<str name=\"platformname\">Sentinel-1</str>\n" +
                "<date name=\"beginposition\">2017-03-07T01:29:22.806Z</date>\n" +
                "<date name=\"endposition\">2017-03-07T01:29:49.758Z</date>\n" +
                "<str name=\"size\">3.5 GB</str>\n" +
                "<int name=\"slicenumber\">3</int>\n" +
                "<str name=\"status\">ARCHIVED</str>\n" +
                "<str name=\"uuid\">0a4435d6-7489-40fe-8ed4-dd77aab93e27</str>\n" +
                "</entry>\n" +
                "<entry>\n" +
                "<title>S1A_IW_SLC__1SSH_20170307T012857_20170307T012924_015579_0199CD_8CE5</title>\n" +
                "<link href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('687e5196-c3b4-4490-8bd9-a4c671f3e230')/$value\"/>\n" +
                "<link rel=\"alternative\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('687e5196-c3b4-4490-8bd9-a4c671f3e230')/\"/>\n" +
                "<link rel=\"icon\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('687e5196-c3b4-4490-8bd9-a4c671f3e230')/Products('Quicklook')/$value\"/>\n" +
                "<id>687e5196-c3b4-4490-8bd9-a4c671f3e230</id>\n" +
                "<summary>Date: 2017-03-07T01:28:57.039Z, Instrument: SAR-C SAR, Mode: HH, Satellite: Sentinel-1, Size: 3.62 GB</summary>\n" +
                "<str name=\"acquisitiontype\">NOMINAL</str>\n" +
                "<str name=\"filename\">S1A_IW_SLC__1SSH_20170307T012857_20170307T012924_015579_0199CD_8CE5.SAFE</str>\n" +
                "<str name=\"gmlfootprint\">&lt;gml:Polygon srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\" xmlns:gml=\"http://www.opengis.net/gml\"&gt;\n" +
                "   &lt;gml:outerBoundaryIs&gt;\n" +
                "      &lt;gml:LinearRing&gt;\n" +
                "         &lt;gml:coordinates&gt;-46.832310,54.208286 -46.079556,51.085892 -44.463455,51.912521 -45.195267,54.949345 -46.832310,54.208286&lt;/gml:coordinates&gt;\n" +
                "      &lt;/gml:LinearRing&gt;\n" +
                "   &lt;/gml:outerBoundaryIs&gt;\n" +
                "&lt;/gml:Polygon&gt;</str>\n" +
                "<str name=\"format\">SAFE</str>\n" +
                "<str name=\"identifier\">S1A_IW_SLC__1SSH_20170307T012857_20170307T012924_015579_0199CD_8CE5</str>\n" +
                "<date name=\"ingestiondate\">2017-03-07T04:14:23.31Z</date>\n" +
                "<str name=\"instrumentshortname\">SAR-C SAR</str>\n" +
                "<str name=\"sensoroperationalmode\">IW</str>\n" +
                "<str name=\"instrumentname\">Synthetic Aperture Radar (C-band)</str>\n" +
                "<str name=\"swathidentifier\">IW1 IW2 IW3</str>\n" +
                "<str name=\"footprint\">POLYGON ((54.208286 -46.832310,51.085892 -46.079556,51.912521 -44.463455,54.949345 -45.195267,54.208286 -46.832310))</str>\n" +
                "<int name=\"missiondatatakeid\">104909</int>\n" +
                "<str name=\"platformidentifier\">2014-016A</str>\n" +
                "<int name=\"orbitnumber\">15579</int>\n" +
                "<int name=\"lastorbitnumber\">15579</int>\n" +
                "<str name=\"orbitdirection\">DESCENDING</str>\n" +
                "<str name=\"polarisationmode\">HH</str>\n" +
                "<str name=\"productclass\">S</str>\n" +
                "<str name=\"producttype\">SLC</str>\n" +
                "<int name=\"relativeorbitnumber\">107</int>\n" +
                "<int name=\"lastrelativeorbitnumber\">107</int>\n" +
                "<str name=\"platformname\">Sentinel-1</str>\n" +
                "<date name=\"beginposition\">2017-03-07T01:28:57.039Z</date>\n" +
                "<date name=\"endposition\">2017-03-07T01:29:24.933Z</date>\n" +
                "<str name=\"size\">3.62 GB</str>\n" +
                "<int name=\"slicenumber\">2</int>\n" +
                "<str name=\"status\">ARCHIVED</str>\n" +
                "<str name=\"uuid\">687e5196-c3b4-4490-8bd9-a4c671f3e230</str>\n" +
                "</entry>\n" +
                "<entry>\n" +
                "<title>S1A_IW_SLC__1SSH_20170307T012947_20170307T013014_015579_0199CD_550E</title>\n" +
                "<link href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('bd3ccdc3-5517-4999-9f4a-00c2ffdd0475')/$value\"/>\n" +
                "<link rel=\"alternative\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('bd3ccdc3-5517-4999-9f4a-00c2ffdd0475')/\"/>\n" +
                "<link rel=\"icon\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('bd3ccdc3-5517-4999-9f4a-00c2ffdd0475')/Products('Quicklook')/$value\"/>\n" +
                "<id>bd3ccdc3-5517-4999-9f4a-00c2ffdd0475</id>\n" +
                "<summary>Date: 2017-03-07T01:29:47.629Z, Instrument: SAR-C SAR, Mode: HH, Satellite: Sentinel-1, Size: 3.5 GB</summary>\n" +
                "<str name=\"acquisitiontype\">NOMINAL</str>\n" +
                "<str name=\"filename\">S1A_IW_SLC__1SSH_20170307T012947_20170307T013014_015579_0199CD_550E.SAFE</str>\n" +
                "<str name=\"gmlfootprint\">&lt;gml:Polygon srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\" xmlns:gml=\"http://www.opengis.net/gml\"&gt;\n" +
                "   &lt;gml:outerBoundaryIs&gt;\n" +
                "      &lt;gml:LinearRing&gt;\n" +
                "         &lt;gml:coordinates&gt;-49.733421,52.778992 -48.939140,49.488510 -47.389572,50.377071 -48.160587,53.573692 -49.733421,52.778992&lt;/gml:coordinates&gt;\n" +
                "      &lt;/gml:LinearRing&gt;\n" +
                "   &lt;/gml:outerBoundaryIs&gt;\n" +
                "&lt;/gml:Polygon&gt;</str>\n" +
                "<str name=\"format\">SAFE</str>\n" +
                "<str name=\"identifier\">S1A_IW_SLC__1SSH_20170307T012947_20170307T013014_015579_0199CD_550E</str>\n" +
                "<date name=\"ingestiondate\">2017-03-07T04:14:23.872Z</date>\n" +
                "<str name=\"instrumentshortname\">SAR-C SAR</str>\n" +
                "<str name=\"sensoroperationalmode\">IW</str>\n" +
                "<str name=\"instrumentname\">Synthetic Aperture Radar (C-band)</str>\n" +
                "<str name=\"swathidentifier\">IW1 IW2 IW3</str>\n" +
                "<str name=\"footprint\">POLYGON ((52.778992 -49.733421,49.488510 -48.939140,50.377071 -47.389572,53.573692 -48.160587,52.778992 -49.733421))</str>\n" +
                "<int name=\"missiondatatakeid\">104909</int>\n" +
                "<str name=\"platformidentifier\">2014-016A</str>\n" +
                "<int name=\"orbitnumber\">15579</int>\n" +
                "<int name=\"lastorbitnumber\">15579</int>\n" +
                "<str name=\"orbitdirection\">DESCENDING</str>\n" +
                "<str name=\"polarisationmode\">HH</str>\n" +
                "<str name=\"productclass\">S</str>\n" +
                "<str name=\"producttype\">SLC</str>\n" +
                "<int name=\"relativeorbitnumber\">107</int>\n" +
                "<int name=\"lastrelativeorbitnumber\">107</int>\n" +
                "<str name=\"platformname\">Sentinel-1</str>\n" +
                "<date name=\"beginposition\">2017-03-07T01:29:47.629Z</date>\n" +
                "<date name=\"endposition\">2017-03-07T01:30:14.583Z</date>\n" +
                "<str name=\"size\">3.5 GB</str>\n" +
                "<int name=\"slicenumber\">4</int>\n" +
                "<str name=\"status\">ARCHIVED</str>\n" +
                "<str name=\"uuid\">bd3ccdc3-5517-4999-9f4a-00c2ffdd0475</str>\n" +
                "</entry>\n" +
                "<entry>\n" +
                "<title>S1A_IW_SLC__1SSH_20170307T012829_20170307T012859_015579_0199CD_36B8</title>\n" +
                "<link href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('b31e3f32-0483-47dc-8911-6e163e0ad0fd')/$value\"/>\n" +
                "<link rel=\"alternative\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('b31e3f32-0483-47dc-8911-6e163e0ad0fd')/\"/>\n" +
                "<link rel=\"icon\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('b31e3f32-0483-47dc-8911-6e163e0ad0fd')/Products('Quicklook')/$value\"/>\n" +
                "<id>b31e3f32-0483-47dc-8911-6e163e0ad0fd</id>\n" +
                "<summary>Date: 2017-03-07T01:28:29.454Z, Instrument: SAR-C SAR, Mode: HH, Satellite: Sentinel-1, Size: 3.89 GB</summary>\n" +
                "<str name=\"acquisitiontype\">NOMINAL</str>\n" +
                "<str name=\"filename\">S1A_IW_SLC__1SSH_20170307T012829_20170307T012859_015579_0199CD_36B8.SAFE</str>\n" +
                "<str name=\"gmlfootprint\">&lt;gml:Polygon srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\" xmlns:gml=\"http://www.opengis.net/gml\"&gt;\n" +
                "   &lt;gml:outerBoundaryIs&gt;\n" +
                "      &lt;gml:LinearRing&gt;\n" +
                "         &lt;gml:coordinates&gt;-45.327053,54.891251 -44.593616,51.847767 -42.859268,52.686230 -43.571850,55.644188 -45.327053,54.891251&lt;/gml:coordinates&gt;\n" +
                "      &lt;/gml:LinearRing&gt;\n" +
                "   &lt;/gml:outerBoundaryIs&gt;\n" +
                "&lt;/gml:Polygon&gt;</str>\n" +
                "<str name=\"format\">SAFE</str>\n" +
                "<str name=\"identifier\">S1A_IW_SLC__1SSH_20170307T012829_20170307T012859_015579_0199CD_36B8</str>\n" +
                "<date name=\"ingestiondate\">2017-03-07T04:14:26.007Z</date>\n" +
                "<str name=\"instrumentshortname\">SAR-C SAR</str>\n" +
                "<str name=\"sensoroperationalmode\">IW</str>\n" +
                "<str name=\"instrumentname\">Synthetic Aperture Radar (C-band)</str>\n" +
                "<str name=\"swathidentifier\">IW1 IW2 IW3</str>\n" +
                "<str name=\"footprint\">POLYGON ((54.891251 -45.327053,51.847767 -44.593616,52.686230 -42.859268,55.644188 -43.571850,54.891251 -45.327053))</str>\n" +
                "<int name=\"missiondatatakeid\">104909</int>\n" +
                "<str name=\"platformidentifier\">2014-016A</str>\n" +
                "<int name=\"orbitnumber\">15579</int>\n" +
                "<int name=\"lastorbitnumber\">15579</int>\n" +
                "<str name=\"orbitdirection\">DESCENDING</str>\n" +
                "<str name=\"polarisationmode\">HH</str>\n" +
                "<str name=\"productclass\">S</str>\n" +
                "<str name=\"producttype\">SLC</str>\n" +
                "<int name=\"relativeorbitnumber\">107</int>\n" +
                "<int name=\"lastrelativeorbitnumber\">107</int>\n" +
                "<str name=\"platformname\">Sentinel-1</str>\n" +
                "<date name=\"beginposition\">2017-03-07T01:28:29.454Z</date>\n" +
                "<date name=\"endposition\">2017-03-07T01:28:59.282Z</date>\n" +
                "<str name=\"size\">3.89 GB</str>\n" +
                "<int name=\"slicenumber\">1</int>\n" +
                "<str name=\"status\">ARCHIVED</str>\n" +
                "<str name=\"uuid\">b31e3f32-0483-47dc-8911-6e163e0ad0fd</str>\n" +
                "</entry>\n" +
                "<entry>\n" +
                "<title>S1A_EW_RAW__0SDH_20170307T023445_20170307T023543_015580_0199D0_9A8D</title>\n" +
                "<link href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('2760011c-5a72-4d97-9b47-403489d11913')/$value\"/>\n" +
                "<link rel=\"alternative\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('2760011c-5a72-4d97-9b47-403489d11913')/\"/>\n" +
                "<link rel=\"icon\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('2760011c-5a72-4d97-9b47-403489d11913')/Products('Quicklook')/$value\"/>\n" +
                "<id>2760011c-5a72-4d97-9b47-403489d11913</id>\n" +
                "<summary>Date: 2017-03-07T02:34:45.038Z, Instrument: SAR-C SAR, Mode: HH HV, Satellite: Sentinel-1, Size: 925.03 MB</summary>\n" +
                "<str name=\"acquisitiontype\">NOMINAL</str>\n" +
                "<str name=\"filename\">S1A_EW_RAW__0SDH_20170307T023445_20170307T023543_015580_0199D0_9A8D.SAFE</str>\n" +
                "<str name=\"gmlfootprint\">&lt;gml:Polygon srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\" xmlns:gml=\"http://www.opengis.net/gml\"&gt;\n" +
                "   &lt;gml:outerBoundaryIs&gt;\n" +
                "      &lt;gml:LinearRing&gt;\n" +
                "         &lt;gml:coordinates&gt;73.6857,59.2704 70.2003,57.2454 69.3539,67.5948 72.6907,71.5239 73.6857,59.2704 73.6857,59.2704&lt;/gml:coordinates&gt;\n" +
                "      &lt;/gml:LinearRing&gt;\n" +
                "   &lt;/gml:outerBoundaryIs&gt;\n" +
                "&lt;/gml:Polygon&gt;</str>\n" +
                "<str name=\"format\">SAFE</str>\n" +
                "<str name=\"identifier\">S1A_EW_RAW__0SDH_20170307T023445_20170307T023543_015580_0199D0_9A8D</str>\n" +
                "<date name=\"ingestiondate\">2017-03-07T05:02:07.008Z</date>\n" +
                "<str name=\"instrumentshortname\">SAR-C SAR</str>\n" +
                "<str name=\"sensoroperationalmode\">EW</str>\n" +
                "<str name=\"instrumentname\">Synthetic Aperture Radar (C-band)</str>\n" +
                "<str name=\"footprint\">POLYGON ((59.2704 73.6857,57.2454 70.2003,67.5948 69.3539,71.5239 72.6907,59.2704 73.6857,59.2704 73.6857))</str>\n" +
                "<int name=\"missiondatatakeid\">104912</int>\n" +
                "<str name=\"platformidentifier\">2014-016A</str>\n" +
                "<int name=\"orbitnumber\">15580</int>\n" +
                "<int name=\"lastorbitnumber\">15580</int>\n" +
                "<str name=\"orbitdirection\">DESCENDING</str>\n" +
                "<str name=\"polarisationmode\">HH HV</str>\n" +
                "<str name=\"productclass\">S</str>\n" +
                "<str name=\"productconsolidation\">SLICE</str>\n" +
                "<str name=\"producttype\">RAW</str>\n" +
                "<int name=\"relativeorbitnumber\">108</int>\n" +
                "<int name=\"lastrelativeorbitnumber\">108</int>\n" +
                "<str name=\"platformname\">Sentinel-1</str>\n" +
                "<date name=\"beginposition\">2017-03-07T02:34:45.038Z</date>\n" +
                "<date name=\"endposition\">2017-03-07T02:35:43.587Z</date>\n" +
                "<str name=\"size\">925.03 MB</str>\n" +
                "<int name=\"slicenumber\">3</int>\n" +
                "<str name=\"status\">ARCHIVED</str>\n" +
                "<str name=\"uuid\">2760011c-5a72-4d97-9b47-403489d11913</str>\n" +
                "</entry>\n" +
                "<entry>\n" +
                "<title>S1A_EW_RAW__0SDH_20170307T023345_20170307T023453_015580_0199D0_C7D8</title>\n" +
                "<link href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('8b2b1fc9-3214-490a-8058-83eb39d54f6a')/$value\"/>\n" +
                "<link rel=\"alternative\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('8b2b1fc9-3214-490a-8058-83eb39d54f6a')/\"/>\n" +
                "<link rel=\"icon\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('8b2b1fc9-3214-490a-8058-83eb39d54f6a')/Products('Quicklook')/$value\"/>\n" +
                "<id>8b2b1fc9-3214-490a-8058-83eb39d54f6a</id>\n" +
                "<summary>Date: 2017-03-07T02:33:45.038Z, Instrument: SAR-C SAR, Mode: HH HV, Satellite: Sentinel-1, Size: 1.06 GB</summary>\n" +
                "<str name=\"acquisitiontype\">NOMINAL</str>\n" +
                "<str name=\"filename\">S1A_EW_RAW__0SDH_20170307T023345_20170307T023453_015580_0199D0_C7D8.SAFE</str>\n" +
                "<str name=\"gmlfootprint\">&lt;gml:Polygon srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\" xmlns:gml=\"http://www.opengis.net/gml\"&gt;\n" +
                "   &lt;gml:outerBoundaryIs&gt;\n" +
                "      &lt;gml:LinearRing&gt;\n" +
                "         &lt;gml:coordinates&gt;77.2369,62.2219 73.1986,58.9482 72.2280,70.8951 76.0101,77.2549 77.2369,62.2219 77.2369,62.2219&lt;/gml:coordinates&gt;\n" +
                "      &lt;/gml:LinearRing&gt;\n" +
                "   &lt;/gml:outerBoundaryIs&gt;\n" +
                "&lt;/gml:Polygon&gt;</str>\n" +
                "<str name=\"format\">SAFE</str>\n" +
                "<str name=\"identifier\">S1A_EW_RAW__0SDH_20170307T023345_20170307T023453_015580_0199D0_C7D8</str>\n" +
                "<date name=\"ingestiondate\">2017-03-07T05:02:08.947Z</date>\n" +
                "<str name=\"instrumentshortname\">SAR-C SAR</str>\n" +
                "<str name=\"sensoroperationalmode\">EW</str>\n" +
                "<str name=\"instrumentname\">Synthetic Aperture Radar (C-band)</str>\n" +
                "<str name=\"footprint\">POLYGON ((62.2219 77.2369,58.9482 73.1986,70.8951 72.2280,77.2549 76.0101,62.2219 77.2369,62.2219 77.2369))</str>\n" +
                "<int name=\"missiondatatakeid\">104912</int>\n" +
                "<str name=\"platformidentifier\">2014-016A</str>\n" +
                "<int name=\"orbitnumber\">15580</int>\n" +
                "<int name=\"lastorbitnumber\">15580</int>\n" +
                "<str name=\"orbitdirection\">DESCENDING</str>\n" +
                "<str name=\"polarisationmode\">HH HV</str>\n" +
                "<str name=\"productclass\">S</str>\n" +
                "<str name=\"productconsolidation\">SLICE</str>\n" +
                "<str name=\"producttype\">RAW</str>\n" +
                "<int name=\"relativeorbitnumber\">108</int>\n" +
                "<int name=\"lastrelativeorbitnumber\">108</int>\n" +
                "<str name=\"platformname\">Sentinel-1</str>\n" +
                "<date name=\"beginposition\">2017-03-07T02:33:45.038Z</date>\n" +
                "<date name=\"endposition\">2017-03-07T02:34:53.237Z</date>\n" +
                "<str name=\"size\">1.06 GB</str>\n" +
                "<int name=\"slicenumber\">2</int>\n" +
                "<str name=\"status\">ARCHIVED</str>\n" +
                "<str name=\"uuid\">8b2b1fc9-3214-490a-8058-83eb39d54f6a</str>\n" +
                "</entry>\n" +
                "<entry>\n" +
                "<title>S1A_EW_RAW__0SDH_20170307T023245_20170307T023353_015580_0199D0_73D0</title>\n" +
                "<link href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('fa98a1af-70b1-431f-a7c5-789c042bbfab')/$value\"/>\n" +
                "<link rel=\"alternative\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('fa98a1af-70b1-431f-a7c5-789c042bbfab')/\"/>\n" +
                "<link rel=\"icon\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('fa98a1af-70b1-431f-a7c5-789c042bbfab')/Products('Quicklook')/$value\"/>\n" +
                "<id>fa98a1af-70b1-431f-a7c5-789c042bbfab</id>\n" +
                "<summary>Date: 2017-03-07T02:32:45.037Z, Instrument: SAR-C SAR, Mode: HH HV, Satellite: Sentinel-1, Size: 1.04 GB</summary>\n" +
                "<str name=\"acquisitiontype\">NOMINAL</str>\n" +
                "<str name=\"filename\">S1A_EW_RAW__0SDH_20170307T023245_20170307T023353_015580_0199D0_73D0.SAFE</str>\n" +
                "<str name=\"gmlfootprint\">&lt;gml:Polygon srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\" xmlns:gml=\"http://www.opengis.net/gml\"&gt;\n" +
                "   &lt;gml:outerBoundaryIs&gt;\n" +
                "      &lt;gml:LinearRing&gt;\n" +
                "         &lt;gml:coordinates&gt;80.7451,67.0594 76.7535,61.7406 75.5650,76.3293 79.1371,86.1383 80.7451,67.0594 80.7451,67.0594&lt;/gml:coordinates&gt;\n" +
                "      &lt;/gml:LinearRing&gt;\n" +
                "   &lt;/gml:outerBoundaryIs&gt;\n" +
                "&lt;/gml:Polygon&gt;</str>\n" +
                "<str name=\"format\">SAFE</str>\n" +
                "<str name=\"identifier\">S1A_EW_RAW__0SDH_20170307T023245_20170307T023353_015580_0199D0_73D0</str>\n" +
                "<date name=\"ingestiondate\">2017-03-07T05:02:09.023Z</date>\n" +
                "<str name=\"instrumentshortname\">SAR-C SAR</str>\n" +
                "<str name=\"sensoroperationalmode\">EW</str>\n" +
                "<str name=\"instrumentname\">Synthetic Aperture Radar (C-band)</str>\n" +
                "<str name=\"footprint\">POLYGON ((67.0594 80.7451,61.7406 76.7535,76.3293 75.5650,86.1383 79.1371,67.0594 80.7451,67.0594 80.7451))</str>\n" +
                "<int name=\"missiondatatakeid\">104912</int>\n" +
                "<str name=\"platformidentifier\">2014-016A</str>\n" +
                "<int name=\"orbitnumber\">15580</int>\n" +
                "<int name=\"lastorbitnumber\">15580</int>\n" +
                "<str name=\"orbitdirection\">DESCENDING</str>\n" +
                "<str name=\"polarisationmode\">HH HV</str>\n" +
                "<str name=\"productclass\">S</str>\n" +
                "<str name=\"productconsolidation\">SLICE</str>\n" +
                "<str name=\"producttype\">RAW</str>\n" +
                "<int name=\"relativeorbitnumber\">108</int>\n" +
                "<int name=\"lastrelativeorbitnumber\">108</int>\n" +
                "<str name=\"platformname\">Sentinel-1</str>\n" +
                "<date name=\"beginposition\">2017-03-07T02:32:45.037Z</date>\n" +
                "<date name=\"endposition\">2017-03-07T02:33:53.237Z</date>\n" +
                "<str name=\"size\">1.04 GB</str>\n" +
                "<int name=\"slicenumber\">1</int>\n" +
                "<str name=\"status\">ARCHIVED</str>\n" +
                "<str name=\"uuid\">fa98a1af-70b1-431f-a7c5-789c042bbfab</str>\n" +
                "</entry>\n" +
                "<entry>\n" +
                "<title>S1A_IW_RAW__0SDV_20170307T002615_20170307T002647_015578_0199C8_84DB</title>\n" +
                "<link href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('9ab00153-71ed-4b9c-8619-36f16782c40c')/$value\"/>\n" +
                "<link rel=\"alternative\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('9ab00153-71ed-4b9c-8619-36f16782c40c')/\"/>\n" +
                "<link rel=\"icon\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('9ab00153-71ed-4b9c-8619-36f16782c40c')/Products('Quicklook')/$value\"/>\n" +
                "<id>9ab00153-71ed-4b9c-8619-36f16782c40c</id>\n" +
                "<summary>Date: 2017-03-07T00:26:15.009Z, Instrument: SAR-C SAR, Mode: VH VV, Satellite: Sentinel-1, Size: 1.6 GB</summary>\n" +
                "<str name=\"acquisitiontype\">NOMINAL</str>\n" +
                "<str name=\"filename\">S1A_IW_RAW__0SDV_20170307T002615_20170307T002647_015578_0199C8_84DB.SAFE</str>\n" +
                "<str name=\"gmlfootprint\">&lt;gml:Polygon srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\" xmlns:gml=\"http://www.opengis.net/gml\"&gt;\n" +
                "   &lt;gml:outerBoundaryIs&gt;\n" +
                "      &lt;gml:LinearRing&gt;\n" +
                "         &lt;gml:coordinates&gt;-1.5216,-90.6852 0.4310,-91.1048 0.1151,-93.2882 -1.8434,-92.8697 -1.5216,-90.6852 -1.5216,-90.6852&lt;/gml:coordinates&gt;\n" +
                "      &lt;/gml:LinearRing&gt;\n" +
                "   &lt;/gml:outerBoundaryIs&gt;\n" +
                "&lt;/gml:Polygon&gt;</str>\n" +
                "<str name=\"format\">SAFE</str>\n" +
                "<str name=\"identifier\">S1A_IW_RAW__0SDV_20170307T002615_20170307T002647_015578_0199C8_84DB</str>\n" +
                "<date name=\"ingestiondate\">2017-03-07T05:02:11.255Z</date>\n" +
                "<str name=\"instrumentshortname\">SAR-C SAR</str>\n" +
                "<str name=\"sensoroperationalmode\">IW</str>\n" +
                "<str name=\"instrumentname\">Synthetic Aperture Radar (C-band)</str>\n" +
                "<str name=\"footprint\">POLYGON ((-90.6852 -1.5216,-91.1048 0.4310,-93.2882 0.1151,-92.8697 -1.8434,-90.6852 -1.5216,-90.6852 -1.5216))</str>\n" +
                "<int name=\"missiondatatakeid\">104904</int>\n" +
                "<str name=\"platformidentifier\">2014-016A</str>\n" +
                "<int name=\"orbitnumber\">15578</int>\n" +
                "<int name=\"lastorbitnumber\">15578</int>\n" +
                "<str name=\"orbitdirection\">ASCENDING</str>\n" +
                "<str name=\"polarisationmode\">VH VV</str>\n" +
                "<str name=\"productclass\">S</str>\n" +
                "<str name=\"productconsolidation\">SLICE</str>\n" +
                "<str name=\"producttype\">RAW</str>\n" +
                "<int name=\"relativeorbitnumber\">106</int>\n" +
                "<int name=\"lastrelativeorbitnumber\">106</int>\n" +
                "<str name=\"platformname\">Sentinel-1</str>\n" +
                "<date name=\"beginposition\">2017-03-07T00:26:15.009Z</date>\n" +
                "<date name=\"endposition\">2017-03-07T00:26:47.408Z</date>\n" +
                "<str name=\"size\">1.6 GB</str>\n" +
                "<int name=\"slicenumber\">1</int>\n" +
                "<str name=\"status\">ARCHIVED</str>\n" +
                "<str name=\"uuid\">9ab00153-71ed-4b9c-8619-36f16782c40c</str>\n" +
                "</entry>\n" +
                "<entry>\n" +
                "<title>S1A_EW_RAW__0SSH_20170307T032149_20170307T032243_015580_0199D5_87DE</title>\n" +
                "<link href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('0f5e7aad-d2e0-45e4-a202-960b468c7f9d')/$value\"/>\n" +
                "<link rel=\"alternative\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('0f5e7aad-d2e0-45e4-a202-960b468c7f9d')/\"/>\n" +
                "<link rel=\"icon\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('0f5e7aad-d2e0-45e4-a202-960b468c7f9d')/Products('Quicklook')/$value\"/>\n" +
                "<id>0f5e7aad-d2e0-45e4-a202-960b468c7f9d</id>\n" +
                "<summary>Date: 2017-03-07T03:21:49.33Z, Instrument: SAR-C SAR, Mode: HH, Satellite: Sentinel-1, Size: 378.55 MB</summary>\n" +
                "<str name=\"acquisitiontype\">NOMINAL</str>\n" +
                "<str name=\"filename\">S1A_EW_RAW__0SSH_20170307T032149_20170307T032243_015580_0199D5_87DE.SAFE</str>\n" +
                "<str name=\"gmlfootprint\">&lt;gml:Polygon srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\" xmlns:gml=\"http://www.opengis.net/gml\"&gt;\n" +
                "   &lt;gml:outerBoundaryIs&gt;\n" +
                "      &lt;gml:LinearRing&gt;\n" +
                "         &lt;gml:coordinates&gt;-74.0453,-79.8881 -72.2306,-89.3692 -74.9896,-98.4633 -77.1909,-88.2530 -74.0453,-79.8881 -74.0453,-79.8881&lt;/gml:coordinates&gt;\n" +
                "      &lt;/gml:LinearRing&gt;\n" +
                "   &lt;/gml:outerBoundaryIs&gt;\n" +
                "&lt;/gml:Polygon&gt;</str>\n" +
                "<str name=\"format\">SAFE</str>\n" +
                "<str name=\"identifier\">S1A_EW_RAW__0SSH_20170307T032149_20170307T032243_015580_0199D5_87DE</str>\n" +
                "<date name=\"ingestiondate\">2017-03-07T05:12:02.784Z</date>\n" +
                "<str name=\"instrumentshortname\">SAR-C SAR</str>\n" +
                "<str name=\"sensoroperationalmode\">EW</str>\n" +
                "<str name=\"instrumentname\">Synthetic Aperture Radar (C-band)</str>\n" +
                "<str name=\"footprint\">POLYGON ((-79.8881 -74.0453,-89.3692 -72.2306,-98.4633 -74.9896,-88.2530 -77.1909,-79.8881 -74.0453,-79.8881 -74.0453))</str>\n" +
                "<int name=\"missiondatatakeid\">104917</int>\n" +
                "<str name=\"platformidentifier\">2014-016A</str>\n" +
                "<int name=\"orbitnumber\">15580</int>\n" +
                "<int name=\"lastorbitnumber\">15580</int>\n" +
                "<str name=\"orbitdirection\">ASCENDING</str>\n" +
                "<str name=\"polarisationmode\">HH</str>\n" +
                "<str name=\"productclass\">S</str>\n" +
                "<str name=\"productconsolidation\">SLICE</str>\n" +
                "<str name=\"producttype\">RAW</str>\n" +
                "<int name=\"relativeorbitnumber\">108</int>\n" +
                "<int name=\"lastrelativeorbitnumber\">108</int>\n" +
                "<str name=\"platformname\">Sentinel-1</str>\n" +
                "<date name=\"beginposition\">2017-03-07T03:21:49.33Z</date>\n" +
                "<date name=\"endposition\">2017-03-07T03:22:43.306Z</date>\n" +
                "<str name=\"size\">378.55 MB</str>\n" +
                "<int name=\"slicenumber\">1</int>\n" +
                "<str name=\"status\">ARCHIVED</str>\n" +
                "<str name=\"uuid\">0f5e7aad-d2e0-45e4-a202-960b468c7f9d</str>\n" +
                "</entry>\n" +
                "<entry>\n" +
                "<title>S1A_IW_RAW__0SDV_20170307T002640_20170307T002711_015578_0199C8_01C4</title>\n" +
                "<link href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('592c09a6-bd4c-41f4-8c4d-0e36839dc089')/$value\"/>\n" +
                "<link rel=\"alternative\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('592c09a6-bd4c-41f4-8c4d-0e36839dc089')/\"/>\n" +
                "<link rel=\"icon\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('592c09a6-bd4c-41f4-8c4d-0e36839dc089')/Products('Quicklook')/$value\"/>\n" +
                "<id>592c09a6-bd4c-41f4-8c4d-0e36839dc089</id>\n" +
                "<summary>Date: 2017-03-07T00:26:40.009Z, Instrument: SAR-C SAR, Mode: VH VV, Satellite: Sentinel-1, Size: 1.52 GB</summary>\n" +
                "<str name=\"acquisitiontype\">NOMINAL</str>\n" +
                "<str name=\"filename\">S1A_IW_RAW__0SDV_20170307T002640_20170307T002711_015578_0199C8_01C4.SAFE</str>\n" +
                "<str name=\"gmlfootprint\">&lt;gml:Polygon srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\" xmlns:gml=\"http://www.opengis.net/gml\"&gt;\n" +
                "   &lt;gml:outerBoundaryIs&gt;\n" +
                "      &lt;gml:LinearRing&gt;\n" +
                "         &lt;gml:coordinates&gt;-0.0150,-91.0096 1.8986,-91.4157 1.5870,-93.6000 -0.3322,-93.1930 -0.0150,-91.0096 -0.0150,-91.0096&lt;/gml:coordinates&gt;\n" +
                "      &lt;/gml:LinearRing&gt;\n" +
                "   &lt;/gml:outerBoundaryIs&gt;\n" +
                "&lt;/gml:Polygon&gt;</str>\n" +
                "<str name=\"format\">SAFE</str>\n" +
                "<str name=\"identifier\">S1A_IW_RAW__0SDV_20170307T002640_20170307T002711_015578_0199C8_01C4</str>\n" +
                "<date name=\"ingestiondate\">2017-03-07T05:12:09.404Z</date>\n" +
                "<str name=\"instrumentshortname\">SAR-C SAR</str>\n" +
                "<str name=\"sensoroperationalmode\">IW</str>\n" +
                "<str name=\"instrumentname\">Synthetic Aperture Radar (C-band)</str>\n" +
                "<str name=\"footprint\">POLYGON ((-91.0096 -0.0150,-91.4157 1.8986,-93.6000 1.5870,-93.1930 -0.3322,-91.0096 -0.0150,-91.0096 -0.0150))</str>\n" +
                "<int name=\"missiondatatakeid\">104904</int>\n" +
                "<str name=\"platformidentifier\">2014-016A</str>\n" +
                "<int name=\"orbitnumber\">15578</int>\n" +
                "<int name=\"lastorbitnumber\">15579</int>\n" +
                "<str name=\"orbitdirection\">ASCENDING</str>\n" +
                "<str name=\"polarisationmode\">VH VV</str>\n" +
                "<str name=\"productclass\">S</str>\n" +
                "<str name=\"productconsolidation\">SLICE</str>\n" +
                "<str name=\"producttype\">RAW</str>\n" +
                "<int name=\"relativeorbitnumber\">106</int>\n" +
                "<int name=\"lastrelativeorbitnumber\">107</int>\n" +
                "<str name=\"platformname\">Sentinel-1</str>\n" +
                "<date name=\"beginposition\">2017-03-07T00:26:40.009Z</date>\n" +
                "<date name=\"endposition\">2017-03-07T00:27:11.75Z</date>\n" +
                "<str name=\"size\">1.52 GB</str>\n" +
                "<int name=\"slicenumber\">2</int>\n" +
                "<str name=\"status\">ARCHIVED</str>\n" +
                "<str name=\"uuid\">592c09a6-bd4c-41f4-8c4d-0e36839dc089</str>\n" +
                "</entry>\n" +
                "<entry>\n" +
                "<title>S1A_EW_RAW__0SDH_20170307T041119_20170307T041147_015581_0199D7_6C31</title>\n" +
                "<link href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('fd8dee37-5b73-4765-870b-062056ab3d5b')/$value\"/>\n" +
                "<link rel=\"alternative\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('fd8dee37-5b73-4765-870b-062056ab3d5b')/\"/>\n" +
                "<link rel=\"icon\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('fd8dee37-5b73-4765-870b-062056ab3d5b')/Products('Quicklook')/$value\"/>\n" +
                "<id>fd8dee37-5b73-4765-870b-062056ab3d5b</id>\n" +
                "<summary>Date: 2017-03-07T04:11:19.969Z, Instrument: SAR-C SAR, Mode: HH HV, Satellite: Sentinel-1, Size: 416.57 MB</summary>\n" +
                "<str name=\"acquisitiontype\">NOMINAL</str>\n" +
                "<str name=\"filename\">S1A_EW_RAW__0SDH_20170307T041119_20170307T041147_015581_0199D7_6C31.SAFE</str>\n" +
                "<str name=\"gmlfootprint\">&lt;gml:Polygon srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\" xmlns:gml=\"http://www.opengis.net/gml\"&gt;\n" +
                "   &lt;gml:outerBoundaryIs&gt;\n" +
                "      &lt;gml:LinearRing&gt;\n" +
                "         &lt;gml:coordinates&gt;81.3027,43.4828 79.7135,40.6345 78.2401,58.3620 79.6105,63.3520 81.3027,43.4828 81.3027,43.4828&lt;/gml:coordinates&gt;\n" +
                "      &lt;/gml:LinearRing&gt;\n" +
                "   &lt;/gml:outerBoundaryIs&gt;\n" +
                "&lt;/gml:Polygon&gt;</str>\n" +
                "<str name=\"format\">SAFE</str>\n" +
                "<str name=\"identifier\">S1A_EW_RAW__0SDH_20170307T041119_20170307T041147_015581_0199D7_6C31</str>\n" +
                "<date name=\"ingestiondate\">2017-03-07T05:22:04.328Z</date>\n" +
                "<str name=\"instrumentshortname\">SAR-C SAR</str>\n" +
                "<str name=\"sensoroperationalmode\">EW</str>\n" +
                "<str name=\"instrumentname\">Synthetic Aperture Radar (C-band)</str>\n" +
                "<str name=\"footprint\">POLYGON ((43.4828 81.3027,40.6345 79.7135,58.3620 78.2401,63.3520 79.6105,43.4828 81.3027,43.4828 81.3027))</str>\n" +
                "<int name=\"missiondatatakeid\">104919</int>\n" +
                "<str name=\"platformidentifier\">2014-016A</str>\n" +
                "<int name=\"orbitnumber\">15581</int>\n" +
                "<int name=\"lastorbitnumber\">15581</int>\n" +
                "<str name=\"orbitdirection\">DESCENDING</str>\n" +
                "<str name=\"polarisationmode\">HH HV</str>\n" +
                "<str name=\"productclass\">S</str>\n" +
                "<str name=\"productconsolidation\">SLICE</str>\n" +
                "<str name=\"producttype\">RAW</str>\n" +
                "<int name=\"relativeorbitnumber\">109</int>\n" +
                "<int name=\"lastrelativeorbitnumber\">109</int>\n" +
                "<str name=\"platformname\">Sentinel-1</str>\n" +
                "<date name=\"beginposition\">2017-03-07T04:11:19.969Z</date>\n" +
                "<date name=\"endposition\">2017-03-07T04:11:47.398Z</date>\n" +
                "<str name=\"size\">416.57 MB</str>\n" +
                "<int name=\"slicenumber\">6</int>\n" +
                "<str name=\"status\">ARCHIVED</str>\n" +
                "<str name=\"uuid\">fd8dee37-5b73-4765-870b-062056ab3d5b</str>\n" +
                "</entry>\n" +
                "<entry>\n" +
                "<title>S1A_IW_GRDH_1SDV_20170307T002643_20170307T002712_015578_0199C8_B8B1</title>\n" +
                "<link href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('d2220696-439a-4c1c-b075-8167548e925c')/$value\"/>\n" +
                "<link rel=\"alternative\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('d2220696-439a-4c1c-b075-8167548e925c')/\"/>\n" +
                "<link rel=\"icon\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('d2220696-439a-4c1c-b075-8167548e925c')/Products('Quicklook')/$value\"/>\n" +
                "<id>d2220696-439a-4c1c-b075-8167548e925c</id>\n" +
                "<summary>Date: 2017-03-07T00:26:43.709Z, Instrument: SAR-C SAR, Mode: VV VH, Satellite: Sentinel-1, Size: 1.79 GB</summary>\n" +
                "<str name=\"acquisitiontype\">NOMINAL</str>\n" +
                "<str name=\"filename\">S1A_IW_GRDH_1SDV_20170307T002643_20170307T002712_015578_0199C8_B8B1.SAFE</str>\n" +
                "<str name=\"gmlfootprint\">&lt;gml:Polygon srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\" xmlns:gml=\"http://www.opengis.net/gml\"&gt;\n" +
                "   &lt;gml:outerBoundaryIs&gt;\n" +
                "      &lt;gml:LinearRing&gt;\n" +
                "         &lt;gml:coordinates&gt;1.835941,-93.535835 2.305825,-91.320389 0.594634,-90.964233 0.119711,-93.177246 1.835941,-93.535835&lt;/gml:coordinates&gt;\n" +
                "      &lt;/gml:LinearRing&gt;\n" +
                "   &lt;/gml:outerBoundaryIs&gt;\n" +
                "&lt;/gml:Polygon&gt;</str>\n" +
                "<str name=\"format\">SAFE</str>\n" +
                "<str name=\"identifier\">S1A_IW_GRDH_1SDV_20170307T002643_20170307T002712_015578_0199C8_B8B1</str>\n" +
                "<date name=\"ingestiondate\">2017-03-07T05:24:24.968Z</date>\n" +
                "<str name=\"instrumentshortname\">SAR-C SAR</str>\n" +
                "<str name=\"sensoroperationalmode\">IW</str>\n" +
                "<str name=\"instrumentname\">Synthetic Aperture Radar (C-band)</str>\n" +
                "<str name=\"swathidentifier\">IW</str>\n" +
                "<str name=\"footprint\">POLYGON ((-93.535835 1.835941,-91.320389 2.305825,-90.964233 0.594634,-93.177246 0.119711,-93.535835 1.835941))</str>\n" +
                "<int name=\"missiondatatakeid\">104904</int>\n" +
                "<str name=\"platformidentifier\">2014-016A</str>\n" +
                "<int name=\"orbitnumber\">15578</int>\n" +
                "<int name=\"lastorbitnumber\">15579</int>\n" +
                "<str name=\"orbitdirection\">ASCENDING</str>\n" +
                "<str name=\"polarisationmode\">VV VH</str>\n" +
                "<str name=\"productclass\">S</str>\n" +
                "<str name=\"producttype\">GRD</str>\n" +
                "<int name=\"relativeorbitnumber\">106</int>\n" +
                "<int name=\"lastrelativeorbitnumber\">107</int>\n" +
                "<str name=\"platformname\">Sentinel-1</str>\n" +
                "<date name=\"beginposition\">2017-03-07T00:26:43.709Z</date>\n" +
                "<date name=\"endposition\">2017-03-07T00:27:12.082Z</date>\n" +
                "<str name=\"size\">1.79 GB</str>\n" +
                "<int name=\"slicenumber\">2</int>\n" +
                "<str name=\"status\">ARCHIVED</str>\n" +
                "<str name=\"uuid\">d2220696-439a-4c1c-b075-8167548e925c</str>\n" +
                "</entry>\n" +
                "<entry>\n" +
                "<title>S1A_EW_RAW__0SDH_20170307T040819_20170307T040928_015581_0199D7_9106</title>\n" +
                "<link href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('43b3c0dd-26c6-46b7-bb9e-d71436731bb5')/$value\"/>\n" +
                "<link rel=\"alternative\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('43b3c0dd-26c6-46b7-bb9e-d71436731bb5')/\"/>\n" +
                "<link rel=\"icon\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('43b3c0dd-26c6-46b7-bb9e-d71436731bb5')/Products('Quicklook')/$value\"/>\n" +
                "<id>43b3c0dd-26c6-46b7-bb9e-d71436731bb5</id>\n" +
                "<summary>Date: 2017-03-07T04:08:19.968Z, Instrument: SAR-C SAR, Mode: HH HV, Satellite: Sentinel-1, Size: 1.03 GB</summary>\n" +
                "<str name=\"acquisitiontype\">NOMINAL</str>\n" +
                "<str name=\"filename\">S1A_EW_RAW__0SDH_20170307T040819_20170307T040928_015581_0199D7_9106.SAFE</str>\n" +
                "<str name=\"gmlfootprint\">&lt;gml:Polygon srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\" xmlns:gml=\"http://www.opengis.net/gml\"&gt;\n" +
                "   &lt;gml:outerBoundaryIs&gt;\n" +
                "      &lt;gml:LinearRing&gt;\n" +
                "         &lt;gml:coordinates&gt;86.4863,163.0761 87.0282,84.7166 83.6187,102.9941 83.3474,140.1607 86.4863,163.0761 86.4863,163.0761&lt;/gml:coordinates&gt;\n" +
                "      &lt;/gml:LinearRing&gt;\n" +
                "   &lt;/gml:outerBoundaryIs&gt;\n" +
                "&lt;/gml:Polygon&gt;</str>\n" +
                "<str name=\"format\">SAFE</str>\n" +
                "<str name=\"identifier\">S1A_EW_RAW__0SDH_20170307T040819_20170307T040928_015581_0199D7_9106</str>\n" +
                "<date name=\"ingestiondate\">2017-03-07T05:24:30.405Z</date>\n" +
                "<str name=\"instrumentshortname\">SAR-C SAR</str>\n" +
                "<str name=\"sensoroperationalmode\">EW</str>\n" +
                "<str name=\"instrumentname\">Synthetic Aperture Radar (C-band)</str>\n" +
                "<str name=\"footprint\">POLYGON ((163.0761 86.4863,84.7166 87.0282,102.9941 83.6187,140.1607 83.3474,163.0761 86.4863,163.0761 86.4863))</str>\n" +
                "<int name=\"missiondatatakeid\">104919</int>\n" +
                "<str name=\"platformidentifier\">2014-016A</str>\n" +
                "<int name=\"orbitnumber\">15581</int>\n" +
                "<int name=\"lastorbitnumber\">15581</int>\n" +
                "<str name=\"orbitdirection\">DESCENDING</str>\n" +
                "<str name=\"polarisationmode\">HH HV</str>\n" +
                "<str name=\"productclass\">S</str>\n" +
                "<str name=\"productconsolidation\">SLICE</str>\n" +
                "<str name=\"producttype\">RAW</str>\n" +
                "<int name=\"relativeorbitnumber\">109</int>\n" +
                "<int name=\"lastrelativeorbitnumber\">109</int>\n" +
                "<str name=\"platformname\">Sentinel-1</str>\n" +
                "<date name=\"beginposition\">2017-03-07T04:08:19.968Z</date>\n" +
                "<date name=\"endposition\">2017-03-07T04:09:28.168Z</date>\n" +
                "<str name=\"size\">1.03 GB</str>\n" +
                "<int name=\"slicenumber\">3</int>\n" +
                "<str name=\"status\">ARCHIVED</str>\n" +
                "<str name=\"uuid\">43b3c0dd-26c6-46b7-bb9e-d71436731bb5</str>\n" +
                "</entry>\n" +
                "<entry>\n" +
                "<title>S1A_IW_GRDH_1SDV_20170307T002614_20170307T002643_015578_0199C8_6C47</title>\n" +
                "<link href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('d49d3a22-3c8c-4fa7-a020-f5d6761b05c0')/$value\"/>\n" +
                "<link rel=\"alternative\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('d49d3a22-3c8c-4fa7-a020-f5d6761b05c0')/\"/>\n" +
                "<link rel=\"icon\" href=\"https://scihub.copernicus.eu/apihub/odata/v1/Products('d49d3a22-3c8c-4fa7-a020-f5d6761b05c0')/Products('Quicklook')/$value\"/>\n" +
                "<id>d49d3a22-3c8c-4fa7-a020-f5d6761b05c0</id>\n" +
                "<summary>Date: 2017-03-07T00:26:14.707Z, Instrument: SAR-C SAR, Mode: VV VH, Satellite: Sentinel-1, Size: 1.83 GB</summary>\n" +
                "<str name=\"acquisitiontype\">NOMINAL</str>\n" +
                "<str name=\"filename\">S1A_IW_GRDH_1SDV_20170307T002614_20170307T002643_015578_0199C8_6C47.SAFE</str>\n" +
                "<str name=\"gmlfootprint\">&lt;gml:Polygon srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\" xmlns:gml=\"http://www.opengis.net/gml\"&gt;\n" +
                "   &lt;gml:outerBoundaryIs&gt;\n" +
                "      &lt;gml:LinearRing&gt;\n" +
                "         &lt;gml:coordinates&gt;0.119711,-93.177246 0.594765,-90.963623 -1.153940,-90.595055 -1.634274,-92.808159 0.119711,-93.177246&lt;/gml:coordinates&gt;\n" +
                "      &lt;/gml:LinearRing&gt;\n" +
                "   &lt;/gml:outerBoundaryIs&gt;\n" +
                "&lt;/gml:Polygon&gt;</str>\n" +
                "<str name=\"format\">SAFE</str>\n" +
                "<str name=\"identifier\">S1A_IW_GRDH_1SDV_20170307T002614_20170307T002643_015578_0199C8_6C47</str>\n" +
                "<date name=\"ingestiondate\">2017-03-07T05:24:31.364Z</date>\n" +
                "<str name=\"instrumentshortname\">SAR-C SAR</str>\n" +
                "<str name=\"sensoroperationalmode\">IW</str>\n" +
                "<str name=\"instrumentname\">Synthetic Aperture Radar (C-band)</str>\n" +
                "<str name=\"swathidentifier\">IW</str>\n" +
                "<str name=\"footprint\">POLYGON ((-93.177246 0.119711,-90.963623 0.594765,-90.595055 -1.153940,-92.808159 -1.634274,-93.177246 0.119711))</str>\n" +
                "<int name=\"missiondatatakeid\">104904</int>\n" +
                "<str name=\"platformidentifier\">2014-016A</str>\n" +
                "<int name=\"orbitnumber\">15578</int>\n" +
                "<int name=\"lastorbitnumber\">15578</int>\n" +
                "<str name=\"orbitdirection\">ASCENDING</str>\n" +
                "<str name=\"polarisationmode\">VV VH</str>\n" +
                "<str name=\"productclass\">S</str>\n" +
                "<str name=\"producttype\">GRD</str>\n" +
                "<int name=\"relativeorbitnumber\">106</int>\n" +
                "<int name=\"lastrelativeorbitnumber\">106</int>\n" +
                "<str name=\"platformname\">Sentinel-1</str>\n" +
                "<date name=\"beginposition\">2017-03-07T00:26:14.707Z</date>\n" +
                "<date name=\"endposition\">2017-03-07T00:26:43.709Z</date>\n" +
                "<str name=\"size\">1.83 GB</str>\n" +
                "<int name=\"slicenumber\">1</int>\n" +
                "<str name=\"status\">ARCHIVED</str>\n" +
                "<str name=\"uuid\">d49d3a22-3c8c-4fa7-a020-f5d6761b05c0</str>\n" +
                "</entry>\n" +
                "</feed>";

        Abdera oAbdera = new Abdera();
        Parser oParser = oAbdera.getParser();
        ParserOptions oParserOptions = oParser.getDefaultParserOptions();

        oParserOptions.setAutodetectCharset(true);
        oParserOptions.setCharset("UTF-8");
        //options.setCompressionCodecs(CompressionCodec.GZIP);
        oParserOptions.setFilterRestrictedCharacterReplacement('_');
        oParserOptions.setFilterRestrictedCharacters(true);
        oParserOptions.setMustPreserveWhitespace(false);
        oParserOptions.setParseFilter(null);

        Document<Feed> oDocument = oParser.parse(new StringReader(sAbdera), oParserOptions);
        Feed oFeed = (Feed) oDocument.getRoot();
        org.apache.abdera.writer.Writer writer = oAbdera.getWriterFactory().getWriter("prettyxml");
        ByteArrayOutputStream oOutputStream = new ByteArrayOutputStream(1000000);
        try {
            writer.writeTo(oFeed, oOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        JSONObject oJson = XML.toJSONObject(oOutputStream.toString());

    }
}
