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
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.gis.GdalInfoResult;
import wasdi.shared.utils.gis.GdalUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.runtime.RunTimeUtils;
import wasdi.shared.utils.runtime.ShellExecReturn;
import wasdi.shared.viewmodels.products.AttributeViewModel;
import wasdi.shared.viewmodels.products.BandViewModel;
import wasdi.shared.viewmodels.products.MetadataViewModel;
import wasdi.shared.viewmodels.products.NodeGroupViewModel;
import wasdi.shared.viewmodels.products.ProductViewModel;

/**
 * SNAP-free reader for Sentinel-2 products (L1C and L2A).
 * Works directly on ZIP (via /vsizip) or unpacked .SAFE directory.
 * Band names are derived from the JP2 filename suffix (B01, B02 … B8A, TCI, SCL, etc.).
 */
public class Sentinel2ProductReader extends WasdiProductReader {

    // Matches the band token at the end of S2 JP2 filenames, e.g. _B02.jp2, _TCI.jp2, _B8A.jp2
    private static final Pattern s_oBandTokenPattern =
            Pattern.compile("_([A-Z0-9]{2,4})\\.jp2$", Pattern.CASE_INSENSITIVE);

    // Matches geographic bounding box tags in MTD XML
    private static final Pattern s_oWestPattern  = Pattern.compile("<West_longitude>([^<]+)</West_longitude>",   Pattern.CASE_INSENSITIVE);
    private static final Pattern s_oEastPattern  = Pattern.compile("<East_longitude>([^<]+)</East_longitude>",   Pattern.CASE_INSENSITIVE);
    private static final Pattern s_oNorthPattern = Pattern.compile("<North_latitude>([^<]+)</North_latitude>",   Pattern.CASE_INSENSITIVE);
    private static final Pattern s_oSouthPattern = Pattern.compile("<South_latitude>([^<]+)</South_latitude>",   Pattern.CASE_INSENSITIVE);

    // Fallback: EXT_POS_LIST (space-separated lat lon pairs, like S1 posList)
    private static final Pattern s_oPosListPattern =
            Pattern.compile("<EXT_POS_LIST>([^<]+)</EXT_POS_LIST>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private List<MeasurementSource> m_aoMeasurementSources = null;

    public Sentinel2ProductReader(File oProductFile) {
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

        for (MeasurementSource oSource : aoSources) {
            BandViewModel oBand = new BandViewModel(oSource.sBandName);

            GdalInfoResult oInfo = GdalUtils.getGdalInfoResult(oSource.sDatasetPath, false);
            if (oInfo != null && oInfo.size != null && oInfo.size.size() >= 2) {
                oBand.setWidth(oInfo.size.get(0));
                oBand.setHeight(oInfo.size.get(1));
            }

            oBandsGroup.getBands().add(oBand);
        }

        oViewModel.setBandsGroups(oBandsGroup);

        return oViewModel;
    }

    @Override
    public String getProductBoundingBox() {
        try {
            // First try the MTD XML bounding box (fast, no raster access)
            String sBB = getBoundingBoxFromMtd();
            if (!Utils.isNullOrEmpty(sBB)) {
                return sBB;
            }

            // Fallback: run gdalinfo on the first granule
            List<MeasurementSource> aoSources = getMeasurementSources();
            if (!aoSources.isEmpty()) {
                GdalInfoResult oInfo = GdalUtils.getGdalInfoResult(aoSources.get(0).sDatasetPath, false);
                if (oInfo != null && oInfo.wgs84East != 0.0) {
                    return String.format("%f,%f,%f,%f,%f,%f,%f,%f,%f,%f",
                            (float) oInfo.wgs84South, (float) oInfo.wgs84West,
                            (float) oInfo.wgs84South, (float) oInfo.wgs84East,
                            (float) oInfo.wgs84North, (float) oInfo.wgs84East,
                            (float) oInfo.wgs84North, (float) oInfo.wgs84West,
                            (float) oInfo.wgs84South, (float) oInfo.wgs84West);
                }
            }
        } catch (Exception oEx) {
            WasdiLog.errorLog("Sentinel2ProductReader.getProductBoundingBox: error ", oEx);
        }

        return "";
    }

    @Override
    public MetadataViewModel getProductMetadataViewModel() {
        MetadataViewModel oMetadata = new MetadataViewModel("Metadata");
        oMetadata.setAttributes(new ArrayList<AttributeViewModel>());

        addMetadataAttribute(oMetadata, "Product Name", m_oProductFile.getName());
        addMetadataAttribute(oMetadata, "Zip Product", String.valueOf(isZipProduct()));

        List<MeasurementSource> aoSources = getMeasurementSources();
        addMetadataAttribute(oMetadata, "Granule Count", String.valueOf(aoSources.size()));

        for (int iIndex = 0; iIndex < aoSources.size(); iIndex++) {
            MeasurementSource oSource = aoSources.get(iIndex);
            addMetadataAttribute(oMetadata, "Band " + (iIndex + 1), oSource.sBandName + " -> " + oSource.sDatasetKey);
        }

        return oMetadata;
    }

    @Override
    public String adjustFileAfterDownload(String sDownloadedFileFullPath, String sFileNameFromProvider, String sPlatform) {
        // Reader works directly on ZIP via /vsizip — no extraction needed.
        return sDownloadedFileFullPath;
    }

    @Override
    public File getFileForPublishBand(String sBand, String sLayerId, String sPlatform) {
        try {
            if (Utils.isNullOrEmpty(sBand)) {
                WasdiLog.warnLog("Sentinel2ProductReader.getFileForPublishBand: band is null or empty");
                return null;
            }

            MeasurementSource oSource = findSourceForBand(sBand);
            if (oSource == null) {
                WasdiLog.warnLog("Sentinel2ProductReader.getFileForPublishBand: no source found for band " + sBand);
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
            WasdiLog.debugLog("Sentinel2ProductReader.getFileForPublishBand [gdal_translate]: " + oTranslateReturn.getOperationLogs());

            File oOutputFile = new File(sOutputPath);
            if (oOutputFile.exists()) {
                return oOutputFile;
            }

            WasdiLog.errorLog("Sentinel2ProductReader.getFileForPublishBand: output file does not exist " + sOutputPath);
            return null;
        } catch (Exception oEx) {
            WasdiLog.errorLog("Sentinel2ProductReader.getFileForPublishBand: error ", oEx);
            return null;
        }
    }

    @Override
    public String getEPSG() {
        try {
            List<MeasurementSource> aoSources = getMeasurementSources();
            if (!aoSources.isEmpty()) {
                GdalInfoResult oInfo = GdalUtils.getGdalInfoResult(aoSources.get(0).sDatasetPath, false);
                if (oInfo != null && !Utils.isNullOrEmpty(oInfo.coordinateSystemWKT)) {
                    CoordinateReferenceSystem oCRS = CRS.parseWKT(oInfo.coordinateSystemWKT);
                    String sEPSG = CRS.lookupIdentifier(oCRS, true);
                    if (!Utils.isNullOrEmpty(sEPSG)) {
                        return sEPSG;
                    }
                }
            }
        } catch (Exception oEx) {
            WasdiLog.errorLog("Sentinel2ProductReader.getEPSG: exception ", oEx);
        }

        return super.getEPSG();
    }

    // -------------------------------------------------------------------------
    // Measurement source discovery
    // -------------------------------------------------------------------------

    private List<MeasurementSource> getMeasurementSources() {
        if (m_aoMeasurementSources != null) return m_aoMeasurementSources;

        List<MeasurementSource> aoSources = new ArrayList<MeasurementSource>();

        try {
            if (isZipProduct()) {
                URI oUri = URI.create("jar:" + m_oProductFile.toURI().toString());
                try (FileSystem oZipFs = FileSystems.newFileSystem(oUri, Map.of())) {
                    addGranuleSourcesFromPath(oZipFs.getPath("/"), true, aoSources);
                }
            } else if (m_oProductFile.isDirectory()) {
                addGranuleSourcesFromPath(m_oProductFile.toPath(), false, aoSources);
            }
        } catch (Exception oEx) {
            WasdiLog.errorLog("Sentinel2ProductReader.getMeasurementSources: error ", oEx);
        }

        // Sort: by resolution folder (R10m before R20m before R60m), then band name
        aoSources.sort(Comparator.comparing((MeasurementSource oS) -> resolutionSortKey(oS.sDatasetKey))
                .thenComparing(oS -> oS.sBandName));

        m_aoMeasurementSources = aoSources;
        return m_aoMeasurementSources;
    }

    private void addGranuleSourcesFromPath(Path oBasePath, boolean bZipPath, List<MeasurementSource> aoSources) throws IOException {
        try (var oStream = Files.walk(oBasePath)) {
            oStream.filter(Files::isRegularFile)
                    .filter(oPath -> oPath.getFileName().toString().toLowerCase().endsWith(".jp2"))
                    .filter(oPath -> isInsideImgData(oPath.toString()))
                    .forEach(oPath -> {
                        String sFileName = oPath.getFileName().toString();
                        String sBandName = inferBandName(sFileName);
                        if (sBandName == null) {
                            return;
                        }

                        MeasurementSource oSource = new MeasurementSource();
                        oSource.sBandName = sBandName;

                        if (bZipPath) {
                            String sInternalPath = oPath.toString().replace('\\', '/');
                            if (sInternalPath.startsWith("/")) {
                                sInternalPath = sInternalPath.substring(1);
                            }
                            oSource.sDatasetKey = sInternalPath;
                            oSource.sDatasetPath = buildVsiZipPath(sInternalPath);
                        } else {
                            oSource.sDatasetKey = oPath.toFile().getAbsolutePath();
                            oSource.sDatasetPath = oPath.toFile().getAbsolutePath();
                        }

                        aoSources.add(oSource);
                    });
        }
    }

    /**
     * Returns true if the path is inside an IMG_DATA folder (any depth).
     * Excludes preview/QI folders.
     */
    private boolean isInsideImgData(String sPath) {
        String sLower = sPath.toLowerCase().replace('\\', '/');
        return sLower.contains("/img_data/");
    }

    /**
     * Extracts the band token from a JP2 filename, e.g.:
     *   T32TNT_20230101T100001_B02.jp2       -> B02
     *   T32TNT_20230101T100001_B8A_10m.jp2   -> B8A
     *   T32TNT_20230101T100001_TCI_10m.jp2   -> TCI
     * Returns null if no recognisable band token found.
     */
    private String inferBandName(String sFileName) {
        // Strip resolution suffix if present (_10m, _20m, _60m) before matching
        String sNormalized = sFileName.replaceAll("(?i)_(10m|20m|60m)\\.jp2$", ".jp2");
        Matcher oMatcher = s_oBandTokenPattern.matcher(sNormalized);
        if (oMatcher.find()) {
            return oMatcher.group(1).toUpperCase();
        }
        return null;
    }

    private MeasurementSource findSourceForBand(String sBand) {
        List<MeasurementSource> aoSources = getMeasurementSources();
        for (MeasurementSource oSource : aoSources) {
            if (oSource.sBandName.equalsIgnoreCase(sBand)) {
                return oSource;
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Bounding box from MTD XML
    // -------------------------------------------------------------------------

    private String getBoundingBoxFromMtd() {
        String sMtdContent = getMtdXmlContent();
        if (Utils.isNullOrEmpty(sMtdContent)) {
            return "";
        }

        // Try geographic bounding box tags first (present in both L1C and L2A)
        try {
            double dWest  = parseFirstDouble(s_oWestPattern,  sMtdContent, Double.NaN);
            double dEast  = parseFirstDouble(s_oEastPattern,  sMtdContent, Double.NaN);
            double dNorth = parseFirstDouble(s_oNorthPattern, sMtdContent, Double.NaN);
            double dSouth = parseFirstDouble(s_oSouthPattern, sMtdContent, Double.NaN);

            if (!Double.isNaN(dWest) && !Double.isNaN(dEast) && !Double.isNaN(dNorth) && !Double.isNaN(dSouth)) {
                return String.format("%f,%f,%f,%f,%f,%f,%f,%f,%f,%f",
                        (float) dSouth, (float) dWest,
                        (float) dSouth, (float) dEast,
                        (float) dNorth, (float) dEast,
                        (float) dNorth, (float) dWest,
                        (float) dSouth, (float) dWest);
            }
        } catch (Exception oEx) {
            WasdiLog.warnLog("Sentinel2ProductReader.getBoundingBoxFromMtd: error parsing bbox tags: " + oEx.getMessage());
        }

        // Fallback: EXT_POS_LIST (lat lon pairs)
        Matcher oPosListMatcher = s_oPosListPattern.matcher(sMtdContent);
        if (oPosListMatcher.find()) {
            return posListToBoundingBox(oPosListMatcher.group(1).trim());
        }

        return "";
    }

    private String getMtdXmlContent() {
        try {
            if (isZipProduct()) {
                URI oUri = URI.create("jar:" + m_oProductFile.toURI().toString());
                try (FileSystem oZipFs = FileSystems.newFileSystem(oUri, Map.of())) {
                    Path oMtd = findMtdPath(oZipFs.getPath("/"));
                    if (oMtd != null) {
                        return Files.readString(oMtd, StandardCharsets.UTF_8);
                    }
                }
            } else if (m_oProductFile.isDirectory()) {
                Path oMtd = findMtdPath(m_oProductFile.toPath());
                if (oMtd != null) {
                    return Files.readString(oMtd, StandardCharsets.UTF_8);
                }
            }
        } catch (Exception oEx) {
            WasdiLog.errorLog("Sentinel2ProductReader.getMtdXmlContent: error ", oEx);
        }
        return null;
    }

    private Path findMtdPath(Path oBasePath) throws IOException {
        try (var oStream = Files.walk(oBasePath, 3)) {
            // MTD_MSIL1C.xml or MTD_MSIL2A.xml sit at the product root level
            return oStream
                    .filter(Files::isRegularFile)
                    .filter(oPath -> {
                        String sName = oPath.getFileName().toString().toUpperCase();
                        return sName.startsWith("MTD_MSI") && sName.endsWith(".XML");
                    })
                    .findFirst()
                    .orElse(null);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private double parseFirstDouble(Pattern oPattern, String sContent, double dDefault) {
        Matcher oMatcher = oPattern.matcher(sContent);
        if (oMatcher.find()) {
            try {
                return Double.parseDouble(oMatcher.group(1).trim());
            } catch (NumberFormatException oEx) {
                // fall through
            }
        }
        return dDefault;
    }

    private String posListToBoundingBox(String sPosList) {
        try {
            String[] asCoords = sPosList.trim().split("\\s+");
            if (asCoords.length < 4) return "";

            double dMinLat = Double.MAX_VALUE, dMaxLat = -Double.MAX_VALUE;
            double dMinLon = Double.MAX_VALUE, dMaxLon = -Double.MAX_VALUE;

            for (int i = 0; i + 1 < asCoords.length; i += 2) {
                double dLat = Double.parseDouble(asCoords[i]);
                double dLon = Double.parseDouble(asCoords[i + 1]);
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
            WasdiLog.errorLog("Sentinel2ProductReader.posListToBoundingBox: error ", oEx);
            return "";
        }
    }

    /**
     * Sort key for resolution folders: R10m < R20m < R60m < everything else.
     * Bands without a resolution folder (L1C flat IMG_DATA) sort first.
     */
    private int resolutionSortKey(String sDatasetKey) {
        String sLower = sDatasetKey.toLowerCase();
        if (sLower.contains("/r10m/")) return 0;
        if (sLower.contains("/r20m/")) return 1;
        if (sLower.contains("/r60m/")) return 2;
        return -1; // L1C flat
    }

    private String buildVsiZipPath(String sInternalPath) {
        String sZipPath = m_oProductFile.getAbsolutePath().replace('\\', '/');
        return "/vsizip/" + sZipPath + "/" + sInternalPath;
    }

    private boolean isZipProduct() {
        return m_oProductFile != null
                && m_oProductFile.isFile()
                && m_oProductFile.getName().toLowerCase().endsWith(".zip");
    }

    private void addMetadataAttribute(MetadataViewModel oMetadata, String sDescription, String sData) {
        AttributeViewModel oAttribute = new AttributeViewModel();
        oAttribute.setDescription(sDescription);
        oAttribute.setData(sData);
        oMetadata.getAttributes().add(oAttribute);
    }

    private static class MeasurementSource {
        String sBandName;    // e.g. "B02", "TCI", "SCL"
        String sDatasetKey;  // internal ZIP path or absolute path (for sorting/display)
        String sDatasetPath; // path passed to GDAL (/vsizip/... or absolute)
    }
}
