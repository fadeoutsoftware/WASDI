package search;

import org.apache.abdera.Abdera;
import org.apache.abdera.i18n.templates.Template;
import org.apache.abdera.model.*;
import org.apache.abdera.parser.Parser;
import org.apache.abdera.parser.ParserOptions;
import org.apache.abdera.protocol.Response;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.protocol.client.RequestOptions;
import org.apache.abdera.writer.Writer;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

/**
 * Created by s.adamo on 20/02/2017.
 */
public class SentinelSearch {

    public static Logger s_oLogger = Logger.getLogger(SentinelSearch.class);

    private static final Template m_sSentinelTemplate =
            new Template(
                    "{scheme}://{-append|.|host}scihub.copernicus.eu{-opt|/|path}{-listjoin|/|path}{-prefix|/|page}{-opt|?|q}{-join|&|q,start,rows,orderby}");


    public static JSONObject ExecuteQuery(String sQuery, HashMap<String, String> asParams) throws URISyntaxException, IOException
    {
        try {
            String sParameter = URLEncoder.encode(sQuery, "UTF-8");
            //String sParameter = "'( beginPosition:[2016-10-03T00:00:00.000Z TO 2016-10-06T23:59:59.999Z] AND endPosition:[2016-10-03T00:00:00.000Z TO 2016-10-06T23:59:59.999Z] ) AND   (platformname:Sentinel-1 AND filename:S1A_* AND producttype:SLC)'";
            sParameter = URLEncoder.encode(sParameter, "UTF-8");
            Abdera oAbdera = new Abdera();
            String sJsonResult = new String("");
            //create url passing search parameters
            String sUrl = getHttpUrl(sQuery,
                    asParams.getOrDefault("offset", "0"),
                    asParams.getOrDefault("limit", "25"),
                    asParams.getOrDefault("sortedby", "ingestiondate"),
                    asParams.getOrDefault("order", "desc"),
                    asParams.getOrDefault("provider", "scihub.copernicus.eu"));
            //create abdera client
            AbderaClient oClient = new AbderaClient(oAbdera);
            oClient.setConnectionTimeout(5000);
            oClient.setSocketTimeout(20000);
            oClient.setConnectionManagerTimeout(10000);
            oClient.setMaxConnectionsTotal(200);
            oClient.setMaxConnectionsPerHost(50);

            // get default request option
            RequestOptions oOptions = oClient.getDefaultRequestOptions();

            // execute request and get the document (atom+xml format)
            Parser oParser = oAbdera.getParser();
            ParserOptions oParserOptions = oParser.getDefaultParserOptions();

            s_oLogger.debug(String.format("SentinelSearch.ExecuteQuery: Created parser"));

            oParserOptions.setAutodetectCharset(true);
            oParserOptions.setCharset("UTF-8");
            //options.setCompressionCodecs(CompressionCodec.GZIP);
            oParserOptions.setFilterRestrictedCharacterReplacement('_');
            oParserOptions.setFilterRestrictedCharacters(true);
            oParserOptions.setMustPreserveWhitespace(false);
            oParserOptions.setParseFilter(null);
            //ListParseFilter filter = new WhiteListParseFilter();
            // set authorization
            String sUserCredentials = asParams.getOrDefault("OSUser", "") + ":" + asParams.getOrDefault("OSPwd", "");
            String sBasicAuth = "Basic " + Base64.getEncoder().encodeToString(sUserCredentials.getBytes());
            oOptions.setAuthorization(sBasicAuth);
            System.out.println("\nSending 'GET' request to URL : " + sUrl);
            ClientResponse response = oClient.get(sUrl, oOptions);
            Document<Feed> oDocument = null;
            if (response.getType() == Response.ResponseType.SUCCESS)
            {
                System.out.println("Response Success");

                oDocument = oParser.parse(response.getInputStream(), oParserOptions);

                s_oLogger.debug(String.format("SentinelSearch.ExecuteQuery: Document parsed"));

                if (oDocument == null) {
                    System.out.println("OpenSearchQuery.ExecuteQuery: Document response null");
                    return null;
                }
            }
            else
            {
                System.out.println("Response ERROR: " + response.getType());
                return null;
            }

            int iStreamSize = 1000000;
            Feed oFeed = (Feed) oDocument.getRoot();

            //set new connction timeout
            oClient.setConnectionTimeout(2000);
            //oClient.setSocketTimeout(2000);
            oClient.setConnectionManagerTimeout(2000);

            JSONObject oFeedJSON = Atom2Json(oAbdera, new ByteArrayOutputStream(iStreamSize), oFeed);
            if (oFeedJSON == null)
                s_oLogger.debug(String.format("SentinelSearch.ExecuteQuery: FeedJSON null"));

            System.out.println("Search Done");

            return oFeedJSON;

        }
        catch (Exception e) {
            s_oLogger.debug(String.format("SentinelSearch.ExecuteQuery: Exception: " + e.getMessage()));
            System.out.println("ExecuteQuery Exception " + e.toString());
        }

        return null;
    }

    public static List<SentinelInfo> GetSentinelinfos(JSONObject oJSONObject)
    {
        String sLink = "https://scihub.copernicus.eu/apihub/odata/v1/Products('{uuid}')/$value";
        List<SentinelInfo> aoInfos = new ArrayList<>();

        if (oJSONObject.has("feed")) {
            JSONObject oFeed = oJSONObject.getJSONObject("feed");
            if (oFeed != null && oFeed.has("entry")) {
                JSONArray oEntries = oFeed.getJSONArray("entry");
                if (oEntries != null)
                {
                    Iterator oEntryIterator = oEntries.iterator();
                    while (oEntryIterator.hasNext()) {
                        JSONObject oEntry = (JSONObject) oEntryIterator.next();
                        if (oEntry != null)
                        {
                            if (oEntry.has("str"))
                            {
                                JSONArray aoStr = oEntry.getJSONArray("str");
                                if (aoStr != null)
                                {
                                    SentinelInfo oSentinelInfo = new SentinelInfo();
                                    Iterator oIt = aoStr.iterator();
                                    while (oIt.hasNext()) {

                                        JSONObject oStr = (JSONObject) oIt.next();
                                        //fileName
                                        if (oStr.has("name") && oStr.get("name").equals("fileName")) {
                                            oSentinelInfo.setFileName(oStr.get("content").toString());

                                        }
                                        //download link
                                        if (oStr.has("name") && oStr.get("name").equals("uuid")) {
                                            oSentinelInfo.setDownloadLink(sLink.replace("{uuid}", oStr.get("content").toString()));

                                        }
                                        //scene center
                                        /*
                                        if (oStr.has("name") && oStr.get("name").equals("footprint")) {
                                            String[] asCenter = GetSceneCenter(oStr.get("content").toString());
                                            oSentinelInfo.setSceneCenterLat(asCenter[0]);
                                            oSentinelInfo.setSceneCenterLon(asCenter[1]);
                                        }
                                        */
                                        //orbit
                                        if (oStr.has("name") && oStr.get("name").equals("relativeorbitnumber")) {
                                            oSentinelInfo.setOrbit(oStr.get("content").toString());
                                        }

                                    }

                                    aoInfos.add(oSentinelInfo);
                                }
                            }
                        }
                    }
                }
            }
        }

        return aoInfos;
    }

    private static String[] GetSceneCenter(String sFootprint) {

        String[] asCenterScene = new String[2];
        sFootprint = sFootprint.replace("POLYGON", "").replace("((", "").replace("))", "");
        String[] asCoordinates = sFootprint.split(",");
        String sUpperLeftLat = asCoordinates[0].trim().split(" ")[0];
        String sUpperLeftLon = asCoordinates[0].trim().split(" ")[1];
        String sUpperRightLat = asCoordinates[1].trim().split(" ")[0];
        String sUpperRightLon = asCoordinates[1].trim().split(" ")[1];
        String sBottomRighttLat = asCoordinates[2].trim().split(" ")[0];
        String sBottomRightLon = asCoordinates[2].trim().split(" ")[1];
        String sBottomleftLat = asCoordinates[3].trim().split(" ")[0];
        String sBottomLeftLon = asCoordinates[3].trim().split(" ")[1];

        String sCenterLat = String.valueOf((Double.valueOf(sUpperLeftLat) + Double.valueOf(sUpperRightLat)) / 2);
        String sCenterLon = String.valueOf((Double.valueOf(sUpperLeftLon) + Double.valueOf(sBottomLeftLon)) / 2);

        asCenterScene[0] = sCenterLat;
        asCenterScene[1] = sCenterLon;

        return asCenterScene;

    }

    public static HashMap<String, String> CreateQueryParams(String sOffset, String sLimit, String sSortedBy, String sOrder, String sUser, String sPwd)
    {
        HashMap<String, String> asParameterMap = new HashMap<>();
        if (sOffset != null)
            asParameterMap.put("offset", sOffset);
        if (sLimit != null)
            asParameterMap.put("limit", sLimit);
        if (sSortedBy != null)
            asParameterMap.put("sortedby", sSortedBy);
        if (sOrder != null)
            asParameterMap.put("order", sOrder);


        asParameterMap.put("provider", "SENTINEL");
        asParameterMap.put("OSUser", sUser);
        asParameterMap.put("OSPwd", sPwd);

        return  asParameterMap;

    }

    private static JSONObject Atom2Json(Abdera oAbdera, ByteArrayOutputStream oOutputStream, Base oFeed) throws IOException{
        Writer oWriter = oAbdera.getWriterFactory().getWriter("prettyxml");
        if (oWriter == null)
            s_oLogger.debug("SentinelSearch.Atom2Json: writer prettyxml is null");
        oWriter.writeTo(oFeed, oOutputStream);
        JSONObject oJson = XML.toJSONObject(oOutputStream.toString());
        return oJson;
    }

    private static String getHttpUrl(String qParams, String sStart, String sRows, String sOrderBy, String sOrder, String sProvider)
    {
        switch(sProvider)
        {
            case "SENTINEL":
                Map<String,Object> oSentinelMap = new HashMap<String, Object>();
                oSentinelMap.put("scheme","https");
                oSentinelMap.put("path", new String[] {"apihub","search"});
                oSentinelMap.put("start", sStart);
                oSentinelMap.put("rows", sRows);
                oSentinelMap.put("orderby", sOrderBy + " " + sOrder);
                oSentinelMap.put("q", qParams);
                return m_sSentinelTemplate.expand(oSentinelMap);

            default:
                break;
        }

        return "";

    }

    public static String ExecuteQueryCount(String sQuery, String sOSUser, String sOSPwd, String sProvider) throws URISyntaxException, IOException
    {
        String sParameter = URLEncoder.encode(sQuery, "UTF-8");
        //String sParameter = "'( beginPosition:[2016-10-03T00:00:00.000Z TO 2016-10-06T23:59:59.999Z] AND endPosition:[2016-10-03T00:00:00.000Z TO 2016-10-06T23:59:59.999Z] ) AND   (platformname:Sentinel-1 AND filename:S1A_* AND producttype:SLC)&offset=0&limit=25'";
        String sUrl = "";
        if (sProvider.equals("SENTINEL"))
            sUrl = "https://scihub.copernicus.eu/dhus/api/stub/products/count?filter=";
        if (sProvider.equals("MATERA"))
            sUrl = "https://collaborative.mt.asi.it/api/stub/products/count?filter=";

        final String USER_AGENT = "Mozilla/5.0";

        URL obj = new URL(sUrl + sParameter);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");

        //add request header
        con.setRequestProperty("User-Agent", USER_AGENT);
        String sUserCredentials = sOSUser + ":" + sOSPwd;
        String sBasicAuth = "Basic " + Base64.getEncoder().encodeToString(sUserCredentials.getBytes("UTF-8"));
        con.setRequestProperty ("Authorization", sBasicAuth);

        int responseCode = con.getResponseCode();
        if (responseCode == 200)
        {
            System.out.println("\nSending 'GET' request to URL : " + sUrl + sParameter);
            System.out.println("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            //print result
            System.out.println("Count Done: Response " + response.toString());

            return response.toString();
        }

        return null;
    }



}
