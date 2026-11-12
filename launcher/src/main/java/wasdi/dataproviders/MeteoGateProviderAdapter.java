package wasdi.dataproviders;

import java.io.File;
import java.io.PrintWriter;

import org.json.JSONArray;
import org.json.JSONObject;

import dev.zarr.zarrjava.store.FilesystemStore;
import dev.zarr.zarrjava.v3.DataType;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;

public class MeteoGateProviderAdapter extends EDRProviderAdapter {

    @Override
    protected void internalReadConfig() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'internalReadConfig'");
    }

    @Override
    public long getDownloadFileSize(String sFileURL) throws Exception {
        return 0;
    }

    @Override
    public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
            String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {

        try {

            if (!sFileURL.contains(";")) {
                WasdiLog.warnLog("MeteoGateProviderAdapter.executeDownloadFile. File url does not contain ;");
                return null;
            }

            String [] asParts = sFileURL.split(";");
            if (asParts.length < 4) {
                WasdiLog.warnLog("MeteoGateProviderAdapter.executeDownloadFile. URL does not contain all the necessary information");
                return null;
            }
            String sTimeSeriesUrl = asParts[0];
            String sFileName = asParts[1];
            String sOutputFormat = asParts[2];
            String sParameterName = asParts[3];

            HttpCallResponse oHttpResponse = HttpUtils.httpGet(sTimeSeriesUrl);

            if (oHttpResponse == null) {
                WasdiLog.warnLog("MeteoGateProviderAdapter.executeDownloadFile. Http response is null");
                return null;
            }

            if (oHttpResponse.getResponseCode() < 200 || oHttpResponse.getResponseCode() >= 300) {
                WasdiLog.warnLog("MeteoGateProviderAdapter.executeDownloadFile. Http response code not OK");
                return null;
            }

            String sTimeSeriesJsonString = oHttpResponse.getResponseBody();

            JSONObject oJsonRoot = new JSONObject(sTimeSeriesJsonString);

            String sWigosId = oJsonRoot.optString("metocean:wigosId", "unknown");
            JSONObject oAxes = oJsonRoot.getJSONObject("domain").getJSONObject("axes");
            double dLongitude = oAxes.getJSONObject("x").getJSONArray("values").getDouble(0);
            double dLatitude = oAxes.getJSONObject("y").getJSONArray("values").getDouble(0);
            
            //Extract time and parameter arrays
            JSONArray aoTimeArray = oAxes.getJSONObject("t").getJSONArray("values");
            
            // Extract the exact parameter key from the URL or define it statically
            String parameterKey = sParameterName;
            JSONArray aoValuesArray = oJsonRoot.getJSONObject("ranges")
                                        .getJSONObject(parameterKey)
                                        .getJSONArray("values");
            int iLength = aoValuesArray.length();

            String sOutputFilePath = sSaveDirOnServer + File.separator + sFileName;

            if (sOutputFormat.toLowerCase().equals("csv")) {
                try (PrintWriter oWriter = new PrintWriter(new File(sOutputFilePath))) {
                    oWriter.println("wigosId,latitude,longitude,timestamp," + sParameterName);
                    
                    for (int i = 0; i < iLength; i++) {
                        String timestamp = aoTimeArray.getString(i);
                        double pressure = aoValuesArray.getDouble(i);
                        
                        oWriter.printf("%s,%f,%f,%s,%.1f%n", 
                                sWigosId, dLatitude, dLongitude, timestamp, pressure);
                    }
                    WasdiLog.debugLog("MeteoGateProviderAdapter.executeDonwloadFile. File generated: " + sOutputFilePath);
                }
                return sOutputFilePath;
        }
            else if (sOutputFormat.toLowerCase().equals("zarr")) {
                // Initialize the Zarr filesystem store
                dev.zarr.zarrjava.store.FilesystemStore oStore = new dev.zarr.zarrjava.store.FilesystemStore(sOutputFilePath);
                
                String sSanitizedParameterName = sParameterName.replace(":", "_");
                // Create a 1D Array to store the time series data
                // Shape is based on the number of timestamps
                dev.zarr.zarrjava.v3.Array oZarrArray = dev.zarr.zarrjava.v3.Array.create(
                    oStore.resolve(sSanitizedParameterName), 
                    dev.zarr.zarrjava.v3.Array.metadataBuilder()
                        .withShape(iLength)
                        .withDataType(dev.zarr.zarrjava.v3.DataType.FLOAT64) 
                        .withChunkShape(Math.min(iLength, 1000)) // TODO: set chunk size depending on expected payload
                        .build()
                );
                
                // Prepare the data to be written into the Zarr array
                // Creating a UCAR ma2 Array representing a 1D array of doubles
                ucar.ma2.Array dataToWrite = ucar.ma2.Array.factory(ucar.ma2.DataType.DOUBLE, new int[]{iLength});
                
                for (int i = 0; i < iLength; i++) {
                    // Populate the array with values from JSON
                    dataToWrite.setDouble(i, aoValuesArray.getDouble(i));
                }
                
                // Write the populated data into the Zarr Array starting at offset 0
                oZarrArray.write(new long[]{0}, dataToWrite);
                
                WasdiLog.debugLog("MeteoGateProviderAdapter.executeDownloadFile. Zarr Store generated at: " + sOutputFilePath);
            }
        

                                        
        } catch (Exception oE) {
            WasdiLog.errorLog("MeteoGateProviderAdapter.executeDownloadFinpm le. Exception: ", oE);
        }

        return null;
    }


    @Override
    public String getFileName(String sFileURL, String sDownloadPath) throws Exception {
        if (sFileURL == null) {
            WasdiLog.warnLog("MeteoGateProviderAdapter.getFileName. File url is null");
            return null;
        }

        if (!sFileURL.contains(";")) {
            WasdiLog.warnLog("MeteoGateProviderAdapter.getFileName. File url does not contain ;");
            return null;
        }

        return sFileURL.split(";")[1];
    }

    @Override
    protected int internalGetScoreForFile(String sFileName, String sPlatformType) {
        if (sPlatformType.equals(Platforms.METEO_GATE)) {
            return DataProviderScores.DOWNLOAD.getValue();
        }
        return 0;
    }

    public static void main (String[] args) throws Exception {
        WasdiConfig.readConfig("C:\\temp\\wasdi\\wasdiLocalTESTConfig_develop.json");
        MeteoGateProviderAdapter oAdapter = new MeteoGateProviderAdapter();
        String sUrl = "https://api.meteogate.eu/eu-eumetnet-surface-observations/collections/observations/locations/0-20000-0-10609?parameter-name=wind_gust_from_direction:10.0:point:PT3H;output.zarr;zarr;wind_gust_from_direction:10.0:point:PT3H";
        String sCompositeUrl = sUrl + ";output.csv";
        String sSavePath = "C:\\Users\\valentina.leone\\Downloads";
        oAdapter.executeDownloadFile(sCompositeUrl, null, null, sSavePath, null, 0);
    }
    
}
