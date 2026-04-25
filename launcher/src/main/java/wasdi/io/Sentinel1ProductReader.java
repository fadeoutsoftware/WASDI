package wasdi.io;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.gis.GdalUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.runtime.RunTimeUtils;
import wasdi.shared.utils.runtime.ShellExecReturn;
import wasdi.shared.viewmodels.products.AttributeViewModel;
import wasdi.shared.viewmodels.products.BandViewModel;
import wasdi.shared.viewmodels.products.MetadataViewModel;
import wasdi.shared.viewmodels.products.NodeGroupViewModel;
import wasdi.shared.viewmodels.products.ProductViewModel;

public class Sentinel1ProductReader extends WasdiProductReader {

    private static final Pattern s_oPolarizationPattern = Pattern.compile("-(VV|VH|HH|HV)-", Pattern.CASE_INSENSITIVE);
    private static final Pattern s_oPosListPattern = Pattern.compile("<gml:posList[^>]*>([^<]+)</gml:posList>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private List<MeasurementSource> m_aoMeasurementSources = null;

    public Sentinel1ProductReader(File oProductFile) {
        super(oProductFile);
    }

    @Override
    public ProductViewModel getProductViewModel() {
        ProductViewModel oViewModel = new ProductViewModel();
        oViewModel.setFileName(m_oProductFile.getName());
        oViewModel.setName(WasdiFileUtils.getFileNameWithoutLastExtension(m_oProductFile.getName()));

        NodeGroupViewModel oBandsGroup = new NodeGroupViewModel("Bands");
        oBandsGroup.setBands(new ArrayList<BandViewModel>());

        List<MeasurementSource> aoSources = getMeasurementSources();

        Map<String, Integer> oBandCounters = new HashMap<String, Integer>();

        for (MeasurementSource oSource : aoSources) {
            String sBaseBandName = inferBandNameFromMeasurement(oSource.sDatasetKey);
            int iCounter = 1;

            if (oBandCounters.containsKey(sBaseBandName)) {
                iCounter = oBandCounters.get(sBaseBandName) + 1;
            }

            oBandCounters.put(sBaseBandName, iCounter);

            String sBandName = sBaseBandName;
            if (iCounter > 1) {
                sBandName = sBaseBandName + "_" + iCounter;
            }

            BandViewModel oBand = new BandViewModel(sBandName);

            int[] aiSize = getRasterSize(oSource.sDatasetPath);
            oBand.setWidth(aiSize[0]);
            oBand.setHeight(aiSize[1]);

            oBandsGroup.getBands().add(oBand);
        }

        oViewModel.setBandsGroups(oBandsGroup);

        return oViewModel;
    }

    @Override
    public String getProductBoundingBox() {
        try {
            List<MeasurementSource> aoSources = getMeasurementSources();
            if (!aoSources.isEmpty()) {
                String sBB = getBoundingBoxFromGdal(aoSources.get(0).sDatasetPath);
                if (!Utils.isNullOrEmpty(sBB)) {
                    return sBB;
                }
            }

            return getBoundingBoxFromManifest();
        } catch (Exception oEx) {
            WasdiLog.errorLog("Sentinel1ProductReader.getProductBoundingBox: error ", oEx);
            return "";
        }
    }

    @Override
    public MetadataViewModel getProductMetadataViewModel() {
        MetadataViewModel oMetadata = new MetadataViewModel("Metadata");
        oMetadata.setAttributes(new ArrayList<AttributeViewModel>());

        addMetadataAttribute(oMetadata, "Product Name", m_oProductFile.getName());
        addMetadataAttribute(oMetadata, "Zip Product", String.valueOf(isZipProduct()));

        List<MeasurementSource> aoSources = getMeasurementSources();
        addMetadataAttribute(oMetadata, "Measurement Count", String.valueOf(aoSources.size()));

        for (int iIndex = 0; iIndex < aoSources.size(); iIndex++) {
            MeasurementSource oSource = aoSources.get(iIndex);
            addMetadataAttribute(oMetadata, "Measurement " + (iIndex + 1), oSource.sDatasetKey);
        }

        String sManifestContent = getManifestXmlContent();
        if (!Utils.isNullOrEmpty(sManifestContent)) {
            String sPosList = getFirstPosList(sManifestContent);
            if (!Utils.isNullOrEmpty(sPosList)) {
                addMetadataAttribute(oMetadata, "Footprint PosList", sPosList);
            }
        }

        return oMetadata;
    }

    @Override
    public String adjustFileAfterDownload(String sDownloadedFileFullPath, String sFileNameFromProvider, String sPlatform) {
        // Sentinel-1 reader works directly on ZIP using ZipFileSystem and GDAL /vsizip.
        return sDownloadedFileFullPath;
    }

    @Override
    public File getFileForPublishBand(String sBand, String sLayerId, String sPlatform) {
        try {
            if (Utils.isNullOrEmpty(sBand)) {
                WasdiLog.warnLog("Sentinel1ProductReader.getFileForPublishBand: band is null or empty");
                return null;
            }

            MeasurementSource oSource = findSourceForBand(sBand);
            if (oSource == null) {
                WasdiLog.warnLog("Sentinel1ProductReader.getFileForPublishBand: no source found for band " + sBand);
                return null;
            }

            String sOutputPath = m_oProductFile.getParentFile().getAbsolutePath() + File.separator + sLayerId + ".tif";

            String sGdalCommand = GdalUtils.adjustGdalFolder("gdal_translate");
            ArrayList<String> asArgs = new ArrayList<String>();
            asArgs.add(sGdalCommand);
            asArgs.add("-of");
            asArgs.add("GTiff");
            asArgs.add(oSource.sDatasetPath);
            asArgs.add(sOutputPath);

            ShellExecReturn oTranslateReturn = RunTimeUtils.shellExec(asArgs, true, true, true, true);
            String sLogs = oTranslateReturn.getOperationLogs();
            WasdiLog.debugLog("Sentinel1ProductReader.getFileForPublishBand [gdal_translate]: " + sLogs);

            File oOutputFile = new File(sOutputPath);
            if (oOutputFile.exists()) {
                return oOutputFile;
            }

            WasdiLog.errorLog("Sentinel1ProductReader.getFileForPublishBand: output file does not exist " + sOutputPath);
            return null;
        } catch (Exception oEx) {
            WasdiLog.errorLog("Sentinel1ProductReader.getFileForPublishBand: error ", oEx);
            return null;
        }
    }

    @Override
    public String getEPSG() {
        try {
            List<MeasurementSource> aoSources = getMeasurementSources();
            if (!aoSources.isEmpty()) {
                String sWkt = getCoordinateSystemWktFromGdal(aoSources.get(0).sDatasetPath);
                if (!Utils.isNullOrEmpty(sWkt)) {
                    CoordinateReferenceSystem oCRS = CRS.parseWKT(sWkt);
                    String sEPSG = CRS.lookupIdentifier(oCRS, true);
                    if (!Utils.isNullOrEmpty(sEPSG)) {
                        return sEPSG;
                    }
                }
            }
        } catch (Exception oEx) {
            WasdiLog.errorLog("Sentinel1ProductReader.getEPSG: exception ", oEx);
        }

        return super.getEPSG();
    }

    private List<MeasurementSource> getMeasurementSources() {
        if (m_aoMeasurementSources != null) return m_aoMeasurementSources;

        List<MeasurementSource> aoSources = new ArrayList<MeasurementSource>();

        try {
            if (isZipProduct()) {
                URI oUri = URI.create("jar:" + m_oProductFile.toURI().toString());
                try (FileSystem oZipFs = FileSystems.newFileSystem(oUri, Map.of())) {
                    Path oRoot = oZipFs.getPath("/");
                    addMeasurementSourcesFromPath(oRoot, true, aoSources);
                }
            }
            else if (m_oProductFile.isDirectory()) {
                addMeasurementSourcesFromPath(m_oProductFile.toPath(), false, aoSources);
            } 
            else if (isMeasurementTiffPath(m_oProductFile.getName())) {
                MeasurementSource oSingle = new MeasurementSource();
                oSingle.sDatasetKey = m_oProductFile.getName();
                oSingle.sDatasetPath = m_oProductFile.getAbsolutePath();
                aoSources.add(oSingle);
            }
        } catch (Exception oEx) {
            WasdiLog.errorLog("Sentinel1ProductReader.getMeasurementSources: error ", oEx);
        }

        aoSources.sort(Comparator.comparing(oSource -> oSource.sDatasetKey));
        m_aoMeasurementSources = aoSources;
        return m_aoMeasurementSources;
    }

    private void addMeasurementSourcesFromPath(Path oBasePath, boolean bZipPath, List<MeasurementSource> aoSources) throws IOException {
        try (var oPathStream = Files.walk(oBasePath)) {
            oPathStream.filter(Files::isRegularFile)
                    .filter(oPath -> isMeasurementTiffPath(oPath.getFileName().toString()))
                    .filter(oPath -> oPath.toString().toLowerCase().contains("measurement"))
                    .forEach(oPath -> {
                        MeasurementSource oSource = new MeasurementSource();

                        if (bZipPath) {
                            String sInternalPath = oPath.toString().replace('\\', '/');
                            if (sInternalPath.startsWith("/")) {
                                sInternalPath = sInternalPath.substring(1);
                            }

                            oSource.sDatasetKey = sInternalPath;
                            oSource.sDatasetPath = buildVsiZipPath(sInternalPath);
                        } else {
                            oSource.sDatasetKey = oPath.getFileName().toString();
                            oSource.sDatasetPath = oPath.toFile().getAbsolutePath();
                        }

                        aoSources.add(oSource);
                    });
        }
    }

    private boolean isMeasurementTiffPath(String sPath) {
        String sLower = sPath.toLowerCase();
        return sLower.endsWith(".tif") || sLower.endsWith(".tiff");
    }

    private String inferBandNameFromMeasurement(String sMeasurementKey) {
        String sFallback = "Band";
        String sKeyUpper = sMeasurementKey.toUpperCase();

        Matcher oMatcher = s_oPolarizationPattern.matcher(sKeyUpper);
        if (oMatcher.find()) {
            return oMatcher.group(1);
        }

        return sFallback;
    }

    private MeasurementSource findSourceForBand(String sBand) {
        List<MeasurementSource> aoSources = getMeasurementSources();

        Map<String, Integer> oBandCounters = new HashMap<String, Integer>();

        for (MeasurementSource oSource : aoSources) {
            String sBaseBandName = inferBandNameFromMeasurement(oSource.sDatasetKey);
            int iCounter = 1;

            if (oBandCounters.containsKey(sBaseBandName)) {
                iCounter = oBandCounters.get(sBaseBandName) + 1;
            }

            oBandCounters.put(sBaseBandName, iCounter);

            String sCandidateBandName = sBaseBandName;
            if (iCounter > 1) {
                sCandidateBandName = sBaseBandName + "_" + iCounter;
            }

            if (sCandidateBandName.equalsIgnoreCase(sBand)) {
                return oSource;
            }
        }

        // Backward-friendly fallback: if UI sends just VV/VH, return first matching polarization.
        for (MeasurementSource oSource : aoSources) {
            String sBaseBandName = inferBandNameFromMeasurement(oSource.sDatasetKey);
            if (sBaseBandName.equalsIgnoreCase(sBand)) {
                return oSource;
            }
        }

        return null;
    }

    private int[] getRasterSize(String sDatasetPath) {
        try {
            Map<String, Object> oInfoMap = getGdalInfoAsMap(sDatasetPath);
            if (oInfoMap != null && oInfoMap.containsKey("size")) {
                @SuppressWarnings("unchecked")
                List<Object> aoSize = (List<Object>) oInfoMap.get("size");
                if (aoSize != null && aoSize.size() >= 2) {
                    int iWidth = ((Number) aoSize.get(0)).intValue();
                    int iHeight = ((Number) aoSize.get(1)).intValue();
                    return new int[] { iWidth, iHeight };
                }
            }
        } catch (Exception oEx) {
            WasdiLog.errorLog("Sentinel1ProductReader.getRasterSize: error ", oEx);
        }

        return new int[] { 0, 0 };
    }

    private String getBoundingBoxFromGdal(String sDatasetPath) {
        try {
            Map<String, Object> oInfoMap = getGdalInfoAsMap(sDatasetPath);

            if (oInfoMap != null && oInfoMap.containsKey("wgs84Extent")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> oExtent = (Map<String, Object>) oInfoMap.get("wgs84Extent");

                if (oExtent != null && oExtent.containsKey("coordinates")) {
                    @SuppressWarnings("unchecked")
                    List<List<List<Number>>> aoCoordinates = (List<List<List<Number>>>) oExtent.get("coordinates");
                    if (aoCoordinates != null && !aoCoordinates.isEmpty() && aoCoordinates.get(0) != null) {
                        double dMinX = Double.MAX_VALUE;
                        double dMaxX = -Double.MAX_VALUE;
                        double dMinY = Double.MAX_VALUE;
                        double dMaxY = -Double.MAX_VALUE;

                        for (List<Number> aoPoint : aoCoordinates.get(0)) {
                            if (aoPoint == null || aoPoint.size() < 2) {
                                continue;
                            }

                            double dX = aoPoint.get(0).doubleValue();
                            double dY = aoPoint.get(1).doubleValue();

                            if (dX < dMinX) dMinX = dX;
                            if (dX > dMaxX) dMaxX = dX;
                            if (dY < dMinY) dMinY = dY;
                            if (dY > dMaxY) dMaxY = dY;
                        }

                        if (dMinX < Double.MAX_VALUE) {
                            return String.format("%f,%f,%f,%f,%f,%f,%f,%f,%f,%f",
                                    (float) dMinY, (float) dMinX,
                                    (float) dMinY, (float) dMaxX,
                                    (float) dMaxY, (float) dMaxX,
                                    (float) dMaxY, (float) dMinX,
                                    (float) dMinY, (float) dMinX);
                        }
                    }
                }
            }
        } catch (Exception oEx) {
            WasdiLog.errorLog("Sentinel1ProductReader.getBoundingBoxFromGdal: error ", oEx);
        }

        return "";
    }

    private String getBoundingBoxFromManifest() {
        String sManifestContent = getManifestXmlContent();
        if (Utils.isNullOrEmpty(sManifestContent)) {
            return "";
        }

        String sPosList = getFirstPosList(sManifestContent);
        if (Utils.isNullOrEmpty(sPosList)) {
            return "";
        }

        return posListToBoundingBox(sPosList);
    }

    private String getManifestXmlContent() {
        try {
            if (isZipProduct()) {
                URI oUri = URI.create("jar:" + m_oProductFile.toURI().toString());
                try (FileSystem oZipFs = FileSystems.newFileSystem(oUri, Map.of())) {
                    Path oManifest = findManifestPath(oZipFs.getPath("/"));
                    if (oManifest != null) {
                        return Files.readString(oManifest, StandardCharsets.UTF_8);
                    }
                }
            } else if (m_oProductFile.isDirectory()) {
                Path oManifest = findManifestPath(m_oProductFile.toPath());
                if (oManifest != null) {
                    return Files.readString(oManifest, StandardCharsets.UTF_8);
                }
            }
        } catch (Exception oEx) {
            WasdiLog.errorLog("Sentinel1ProductReader.getManifestXmlContent: error ", oEx);
        }

        return null;
    }

    private Path findManifestPath(Path oBasePath) throws IOException {
        try (var oPathStream = Files.walk(oBasePath)) {
            return oPathStream
                    .filter(Files::isRegularFile)
                    .filter(oPath -> oPath.getFileName().toString().equalsIgnoreCase("manifest.safe"))
                    .findFirst()
                    .orElse(null);
        }
    }

    private String getFirstPosList(String sManifestContent) {
        Matcher oMatcher = s_oPosListPattern.matcher(sManifestContent);
        if (oMatcher.find()) {
            return oMatcher.group(1).trim();
        }
        return null;
    }

    private String posListToBoundingBox(String sPosList) {
        try {
            String[] asCoordinates = sPosList.trim().split("\\s+");
            if (asCoordinates.length < 4) {
                return "";
            }

            double dMinLat = Double.MAX_VALUE;
            double dMaxLat = -Double.MAX_VALUE;
            double dMinLon = Double.MAX_VALUE;
            double dMaxLon = -Double.MAX_VALUE;

            for (int i = 0; i + 1 < asCoordinates.length; i += 2) {
                double dLat = Double.parseDouble(asCoordinates[i]);
                double dLon = Double.parseDouble(asCoordinates[i + 1]);

                if (dLat < dMinLat) dMinLat = dLat;
                if (dLat > dMaxLat) dMaxLat = dLat;
                if (dLon < dMinLon) dMinLon = dLon;
                if (dLon > dMaxLon) dMaxLon = dLon;
            }

            return String.format("%f,%f,%f,%f,%f,%f,%f,%f,%f,%f",
                    (float) dMinLat, (float) dMinLon,
                    (float) dMinLat, (float) dMaxLon,
                    (float) dMaxLat, (float) dMaxLon,
                    (float) dMaxLat, (float) dMinLon,
                    (float) dMinLat, (float) dMinLon);
        } catch (Exception oEx) {
            WasdiLog.errorLog("Sentinel1ProductReader.posListToBoundingBox: error ", oEx);
            return "";
        }
    }

    private String getCoordinateSystemWktFromGdal(String sDatasetPath) {
        try {
            Map<String, Object> oInfoMap = getGdalInfoAsMap(sDatasetPath);
            if (oInfoMap != null && oInfoMap.containsKey("coordinateSystem")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> oCoordinateSystem = (Map<String, Object>) oInfoMap.get("coordinateSystem");
                if (oCoordinateSystem != null && oCoordinateSystem.containsKey("wkt")) {
                    return (String) oCoordinateSystem.get("wkt");
                }
            }
        } catch (Exception oEx) {
            WasdiLog.errorLog("Sentinel1ProductReader.getCoordinateSystemWktFromGdal: error ", oEx);
        }

        return null;
    }

    private Map<String, Object> getGdalInfoAsMap(String sDatasetPath) {
        try {
            String sGdalCommand = GdalUtils.adjustGdalFolder("gdalinfo");
            ArrayList<String> asArgs = new ArrayList<String>();
            asArgs.add(sGdalCommand);
            asArgs.add("-json");
            asArgs.add(sDatasetPath);
            asArgs.add("-wkt_format");
            asArgs.add("WKT1");

            ShellExecReturn oReturn = RunTimeUtils.shellExec(asArgs, true, true, true, true);
            String sOutput = oReturn.getOperationLogs();

            if (Utils.isNullOrEmpty(sOutput)) {
                return null;
            }

            int iJsonStart = sOutput.indexOf("{");
            if (iJsonStart > 0) {
                sOutput = sOutput.substring(iJsonStart);
            }

            return JsonUtils.jsonToMapOfObjects(sOutput);
        } catch (Exception oEx) {
            WasdiLog.errorLog("Sentinel1ProductReader.getGdalInfoAsMap: error ", oEx);
            return null;
        }
    }

    private String buildVsiZipPath(String sInternalPath) {
        String sZipPath = m_oProductFile.getAbsolutePath().replace('\\', '/');
        return "/vsizip/" + sZipPath + "/" + sInternalPath;
    }

    private boolean isZipProduct() {
        if (m_oProductFile == null) {
            return false;
        }

        return m_oProductFile.isFile() && m_oProductFile.getName().toLowerCase().endsWith(".zip");
    }

    private void addMetadataAttribute(MetadataViewModel oMetadata, String sDescription, String sData) {
        AttributeViewModel oAttribute = new AttributeViewModel();
        oAttribute.setDescription(sDescription);
        oAttribute.setData(sData);
        oMetadata.getAttributes().add(oAttribute);
    }

    private static class MeasurementSource {
        String sDatasetKey;
        String sDatasetPath;
    }
}
