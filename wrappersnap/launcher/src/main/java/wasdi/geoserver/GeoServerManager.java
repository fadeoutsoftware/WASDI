package wasdi.geoserver;

import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.GeoServerRESTReader;
import it.geosolutions.geoserver.rest.HTTPUtils;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder;
import it.geosolutions.geoserver.rest.manager.GeoServerRESTStoreManager;
import org.apache.commons.httpclient.NameValuePair;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * Created by s.adamo on 24/05/2016.
 */
public class GeoServerManager {

    String RESTURL  = "http://localhost:8080/geoserver";
    String RESTUSER = "admin";
    String RESTPW   = "geoserver";
    GeoServerRESTPublisher m_oGeoServerPublisher;
    GeoServerRESTReader m_oGeoServerReader;
    GeoServerRESTStoreManager m_oGeoServerStoreManager;

    public GeoServerManager() throws MalformedURLException {
        this.Init();
    }

    public GeoServerManager(String sRestUrl, String sUser, String sPassword) throws MalformedURLException {
        RESTURL = sRestUrl;
        RESTUSER = sUser;
        RESTPW = sPassword;

        this.Init();
    }

    private void Init() throws MalformedURLException {

        m_oGeoServerReader = new GeoServerRESTReader(RESTURL, RESTUSER, RESTPW);
        m_oGeoServerPublisher = new GeoServerRESTPublisher(RESTURL, RESTUSER, RESTPW);
        m_oGeoServerStoreManager = new GeoServerRESTStoreManager(new URL(RESTURL), RESTUSER, RESTPW);

    }


    public GeoServerRESTPublisher getPublisher()
    {
        return m_oGeoServerPublisher;
    }
    public GeoServerRESTReader getReader()
    {
        return m_oGeoServerReader;
    }
    public GeoServerRESTStoreManager getStoreManager()
    {
        return m_oGeoServerStoreManager;
    }



    public boolean publishCoverage(String workspace, String coveragestore,
                                   GeoServerRESTPublisher.CoverageStoreExtension extension, String mimeType, File file,
                                   GeoServerRESTPublisher.ParameterConfigure configure, NameValuePair... params) throws FileNotFoundException {
        /*
         * This is an example with cUrl:
         *
         * {@code curl -u admin:geoserver -XPUT -H 'Content-type: application/zip' \ --data-binary @$ZIPFILE \ http://$GSIP:$GSPORT/$SERVLET
         * /rest/workspaces/$WORKSPACE/coveragestores /$COVERAGESTORE/file.worldimage
         */
        return createCoverageStore(workspace, coveragestore, GeoServerRESTPublisher.UploadMethod.EXTERNAL, extension,
                mimeType, file.toURI(), configure, params);
    }

    private boolean createCoverageStore(String workspace, String storeName, GeoServerRESTPublisher.UploadMethod method,
                                        GeoServerRESTPublisher.CoverageStoreExtension extension, String mimeType, URI uri,
                                        GeoServerRESTPublisher.ParameterConfigure configure, NameValuePair... params) throws FileNotFoundException,
            IllegalArgumentException {
        return createStore(workspace, GeoServerRESTPublisher.StoreType.COVERAGESTORES, storeName, method, extension,
                mimeType, uri, configure, params);
    }

    private boolean createStore(String workspace, GeoServerRESTPublisher.StoreType dsType, String storeName,
                                GeoServerRESTPublisher.UploadMethod method, Enum extension, String mimeType, URI uri,
                                GeoServerRESTPublisher.ParameterConfigure configure, NameValuePair... params) throws FileNotFoundException,
            IllegalArgumentException {
        if (workspace == null || dsType == null || storeName == null || method == null
                || extension == null || mimeType == null || uri == null) {
            throw new IllegalArgumentException("Null argument");
        }
        StringBuilder sbUrl = new StringBuilder(RESTURL).append("/rest/workspaces/")
                .append(workspace).append("/").append(dsType).append("/").append(storeName).append("/").append(method)
                .append(".").append("imagepyramid");

        if (configure != null) {
            sbUrl.append("?configure=").append(configure);
            if (params != null) {
                final String paramString = appendParameters(params);
                if (!paramString.isEmpty()) {
                    sbUrl.append("&").append(paramString);
                }
            }
        }

        String sentResult = null;

        if (method.equals(GeoServerRESTPublisher.UploadMethod.FILE)) {
            final File file = new File(uri);
            if (!file.exists())
                throw new FileNotFoundException("unable to locate file: " + file);
            sentResult = HTTPUtils.put(sbUrl.toString(), file, mimeType, RESTUSER, RESTPW);
        } else if (method.equals(GeoServerRESTPublisher.UploadMethod.EXTERNAL)) {
            sentResult = HTTPUtils.put(sbUrl.toString(), uri.toString(), mimeType, RESTUSER, RESTPW);
        } else if (method.equals(GeoServerRESTPublisher.UploadMethod.URL)) {
            // TODO check
            sentResult = HTTPUtils.put(sbUrl.toString(), uri.toString(), mimeType, RESTUSER, RESTPW);
        }

        return sentResult != null;

    }


    /**
     * Append params generating a string in the form:
     * <p>
     * NAME_0=VALUE_0&NAME_1=VALUE_1&....&NAME_n-1=VALUE_n-1
     * </p>
     * </br>
     *
     * @param params an array of NameValuePair
     * @return the parameter string or empty an string
     */
    private String appendParameters(NameValuePair... params) {
        StringBuilder sbUrl = new StringBuilder();
        // append parameters
        if (params != null) {
            final int paramsSize = params.length;
            if (paramsSize > 0) {
                int i = 0;
                NameValuePair param = params[i];
                while (param != null && i++ < paramsSize) {
                    final String name = param.getName();
                    final String value = param.getValue();
                    // success
                    if (name != null && !name.isEmpty() && value != null && !value.isEmpty()) {
                        sbUrl.append(name).append("=").append(value);
                        // end cycle
                        param = null;
                    } else {
                        // next value
                        param = params[i];
                    }
                }
                for (; i < paramsSize; i++) {
                    param = params[i];
                    if (param != null) {
                        final String name = param.getName();
                        final String value = param.getValue();
                        sbUrl.append(name).append("=").append(value);
                        if (name != null && !name.isEmpty() && value != null && !value.isEmpty()) {
                            sbUrl.append("&").append(name).append("=").append(value);
                        }

                    }

                }
            }
        }
        return sbUrl.toString();
    }

    public boolean publishImagePyramid(String workspace, String storeName, File zipFile)
            throws FileNotFoundException {

        return publishCoverage(workspace, storeName, GeoServerRESTPublisher.CoverageStoreExtension.IMAGEMOSAIC,
                "image/geotiff", zipFile, GeoServerRESTPublisher.ParameterConfigure.FIRST, (NameValuePair[]) null);
    }


    public boolean publishStandardGeoTiff(String workspace, String storeName, File zipFile, String sEPSG, String sStyle)
            throws FileNotFoundException {

        if (sStyle == null) sStyle="raster";
        if (sStyle.isEmpty()) sStyle = "raster";

        return m_oGeoServerPublisher.publishExternalGeoTIFF(workspace,storeName,zipFile, storeName, sEPSG, GSResourceEncoder.ProjectionPolicy.FORCE_DECLARED,sStyle);
    }
}
