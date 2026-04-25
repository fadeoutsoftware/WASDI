package wasdi.io;

import java.io.File;
import java.util.ArrayList;

import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.gis.GdalBandInfo;
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
 * SNAP-free reader for GeoTIFF files (single-band, multi-band, COG, BigTIFF,
 * any CRS / data type).  All information is obtained via gdalinfo.
 */
public class GeoTiffProductReader extends WasdiProductReader {

    public GeoTiffProductReader(File oProductFile) {
        super(oProductFile);
    }

    @Override
    public ProductViewModel getProductViewModel() {
        ProductViewModel oViewModel = new ProductViewModel();
        oViewModel.setFileName(m_oProductFile.getName());
        oViewModel.setName(WasdiFileUtils.getFileNameWithoutLastExtension(m_oProductFile.getName()));

        NodeGroupViewModel oBandsGroup = new NodeGroupViewModel("Bands");
        oBandsGroup.setBands(new ArrayList<BandViewModel>());

        GdalInfoResult oInfo = GdalUtils.getGdalInfoResult(m_oProductFile);
        if (oInfo != null) {
            int iWidth  = (oInfo.size != null && oInfo.size.size() >= 2) ? oInfo.size.get(0) : 0;
            int iHeight = (oInfo.size != null && oInfo.size.size() >= 2) ? oInfo.size.get(1) : 0;

            if (oInfo.bands != null) {
                int iBandIndex = 1;
                for (GdalBandInfo oGdalBand : oInfo.bands) {
                    String sBandName = (!Utils.isNullOrEmpty(oGdalBand.description))
                            ? oGdalBand.description
                            : "Band_" + iBandIndex;
                    BandViewModel oBand = new BandViewModel(sBandName);
                    oBand.setWidth(iWidth);
                    oBand.setHeight(iHeight);
                    oBandsGroup.getBands().add(oBand);
                    iBandIndex++;
                }
            }
        }

        oViewModel.setBandsGroups(oBandsGroup);
        return oViewModel;
    }

    @Override
    public String getProductBoundingBox() {
        try {
            GdalInfoResult oInfo = GdalUtils.getGdalInfoResult(m_oProductFile);
            if (oInfo != null && oInfo.wgs84East != 0.0) {
                return String.format("%f,%f,%f,%f,%f,%f,%f,%f,%f,%f",
                        (float) oInfo.wgs84South, (float) oInfo.wgs84West,
                        (float) oInfo.wgs84South, (float) oInfo.wgs84East,
                        (float) oInfo.wgs84North, (float) oInfo.wgs84East,
                        (float) oInfo.wgs84North, (float) oInfo.wgs84West,
                        (float) oInfo.wgs84South, (float) oInfo.wgs84West);
            }
        } catch (Exception oEx) {
            WasdiLog.errorLog("GeoTiffProductReader.getProductBoundingBox: error ", oEx);
        }
        return "";
    }

    @Override
    public MetadataViewModel getProductMetadataViewModel() {
        MetadataViewModel oMetadata = new MetadataViewModel("Metadata");
        oMetadata.setAttributes(new ArrayList<AttributeViewModel>());

        GdalInfoResult oInfo = GdalUtils.getGdalInfoResult(m_oProductFile);
        if (oInfo != null) {
            addAttribute(oMetadata, "Driver",       oInfo.driverLongName);
            addAttribute(oMetadata, "File",         m_oProductFile.getName());
            addAttribute(oMetadata, "Description",  oInfo.description);

            if (oInfo.size != null && oInfo.size.size() >= 2) {
                addAttribute(oMetadata, "Width",  String.valueOf(oInfo.size.get(0)));
                addAttribute(oMetadata, "Height", String.valueOf(oInfo.size.get(1)));
            }

            if (oInfo.bands != null) {
                addAttribute(oMetadata, "Band Count", String.valueOf(oInfo.bands.size()));
                int iBandIndex = 1;
                for (GdalBandInfo oGdalBand : oInfo.bands) {
                    String sLabel = "Band " + iBandIndex;
                    String sValue = oGdalBand.colorInterpretation;
                    if (!Utils.isNullOrEmpty(oGdalBand.description)) sValue += " - " + oGdalBand.description;
                    if (!Utils.isNullOrEmpty(oGdalBand.type))        sValue += " (" + oGdalBand.type + ")";
                    addAttribute(oMetadata, sLabel, sValue);
                    iBandIndex++;
                }
            }

            if (!Utils.isNullOrEmpty(oInfo.coordinateSystemWKT)) {
                addAttribute(oMetadata, "CRS", oInfo.coordinateSystemWKT);
            }
        }

        return oMetadata;
    }

    @Override
    public String adjustFileAfterDownload(String sDownloadedFileFullPath, String sFileNameFromProvider, String sPlatform) {
        return sDownloadedFileFullPath;
    }

    @Override
    public File getFileForPublishBand(String sBand, String sLayerId, String sPlatform) {
        try {
            if (Utils.isNullOrEmpty(sBand)) {
                WasdiLog.warnLog("GeoTiffProductReader.getFileForPublishBand: band is null/empty");
                return null;
            }

            GdalInfoResult oInfo = GdalUtils.getGdalInfoResult(m_oProductFile);
            boolean bSingleBand = (oInfo != null && oInfo.bands != null && oInfo.bands.size() == 1);
            boolean bAlready4326 = isEpsg4326();

            // Single-band and already EPSG:4326 — return the original file directly
            if (bSingleBand && bAlready4326) {
                WasdiLog.debugLog("GeoTiffProductReader.getFileForPublishBand: single-band EPSG:4326, using file as-is");
                return m_oProductFile;
            }

            String sOutputPath = m_oProductFile.getParentFile().getAbsolutePath()
                    + File.separator + sLayerId + ".tif";

            // Single-band but not EPSG:4326 — warp directly
            if (bSingleBand) {
                return warpToEpsg4326(m_oProductFile.getAbsolutePath(), sOutputPath);
            }

            // Multi-band: find the requested band index first
            int iBandIndex = findBandIndex(sBand);
            if (iBandIndex < 1) {
                WasdiLog.warnLog("GeoTiffProductReader.getFileForPublishBand: band not found: " + sBand);
                return null;
            }

            if (bAlready4326) {
                // Already in EPSG:4326 — just extract the band, no warp needed
                WasdiLog.debugLog("GeoTiffProductReader.getFileForPublishBand: multi-band EPSG:4326, extracting band " + iBandIndex);
                return extractBand(iBandIndex, sOutputPath);
            }

            // Multi-band, non-4326: extract band then warp
            String sTempPath = m_oProductFile.getParentFile().getAbsolutePath()
                    + File.separator + sLayerId + "_band.tif";

            File oTemp = extractBand(iBandIndex, sTempPath);
            if (oTemp == null) {
                WasdiLog.errorLog("GeoTiffProductReader.getFileForPublishBand: band extraction failed");
                return null;
            }

            File oOutput = warpToEpsg4326(sTempPath, sOutputPath);
            oTemp.delete();

            return oOutput;

        } catch (Exception oEx) {
            WasdiLog.errorLog("GeoTiffProductReader.getFileForPublishBand: error ", oEx);
            return null;
        }
    }

    @Override
    public String getEPSG() {
        try {
            GdalInfoResult oInfo = GdalUtils.getGdalInfoResult(m_oProductFile);
            if (oInfo != null && !Utils.isNullOrEmpty(oInfo.coordinateSystemWKT)) {
                CoordinateReferenceSystem oCRS = CRS.parseWKT(oInfo.coordinateSystemWKT);
                String sEPSG = CRS.lookupIdentifier(oCRS, true);
                if (!Utils.isNullOrEmpty(sEPSG)) {
                    return sEPSG;
                }
            }
        } catch (Exception oEx) {
            WasdiLog.errorLog("GeoTiffProductReader.getEPSG: exception ", oEx);
        }
        return super.getEPSG();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the 1-based GDAL band index for the given band name.
     * Falls back to parsing "Band_N" names.
     */
    private int findBandIndex(String sBand) {
        GdalInfoResult oInfo = GdalUtils.getGdalInfoResult(m_oProductFile);
        if (oInfo == null || oInfo.bands == null) return -1;

        int iBandIndex = 1;
        for (GdalBandInfo oGdalBand : oInfo.bands) {
            String sStoredName = (!Utils.isNullOrEmpty(oGdalBand.description))
                    ? oGdalBand.description
                    : "Band_" + iBandIndex;
            if (sStoredName.equalsIgnoreCase(sBand)) {
                return iBandIndex;
            }
            iBandIndex++;
        }
        return -1;
    }

    private boolean isEpsg4326() {
        String sEpsg = getEPSG();
        return !Utils.isNullOrEmpty(sEpsg) && sEpsg.contains("4326");
    }

    private File extractBand(int iBandIndex, String sOutputPath) {
        String sGdalTranslate = GdalUtils.adjustGdalFolder("gdal_translate");
        ArrayList<String> asArgs = new ArrayList<>();
        asArgs.add(sGdalTranslate);
        asArgs.add("-b");
        asArgs.add(String.valueOf(iBandIndex));
        asArgs.add("-of");
        asArgs.add("GTiff");
        asArgs.add(m_oProductFile.getAbsolutePath());
        asArgs.add(sOutputPath);

        ShellExecReturn oReturn = RunTimeUtils.shellExec(asArgs, true, true, true, true);
        WasdiLog.debugLog("GeoTiffProductReader.extractBand [gdal_translate]: " + oReturn.getOperationLogs());

        File oOutput = new File(sOutputPath);
        if (oOutput.exists()) return oOutput;

        WasdiLog.errorLog("GeoTiffProductReader.extractBand: output not created at " + sOutputPath);
        return null;
    }

    private File warpToEpsg4326(String sInputPath, String sOutputPath) {
                    ? oGdalBand.description
                    : "Band_" + iBandIndex;
            if (sStoredName.equalsIgnoreCase(sBand)) {
                return iBandIndex;
            }
            iBandIndex++;
        }
        return -1;
    }

    private File warpToEpsg4326(String sInputPath, String sOutputPath) {
        String sGdalWarp = GdalUtils.adjustGdalFolder("gdalwarp");
        ArrayList<String> asWarpArgs = new ArrayList<>();
        asWarpArgs.add(sGdalWarp);
        asWarpArgs.add("-t_srs");
        asWarpArgs.add("EPSG:4326");
        asWarpArgs.add("-of");
        asWarpArgs.add("GTiff");
        asWarpArgs.add(sInputPath);
        asWarpArgs.add(sOutputPath);

        ShellExecReturn oReturn = RunTimeUtils.shellExec(asWarpArgs, true, true, true, true);
        WasdiLog.debugLog("GeoTiffProductReader.warpToEpsg4326 [gdalwarp]: " + oReturn.getOperationLogs());

        File oOutput = new File(sOutputPath);
        if (oOutput.exists()) return oOutput;

        WasdiLog.errorLog("GeoTiffProductReader.warpToEpsg4326: output not created at " + sOutputPath);
        return null;
    }

    private void addAttribute(MetadataViewModel oMetadata, String sDescription, String sData) {
        if (Utils.isNullOrEmpty(sData)) return;
        AttributeViewModel oAttr = new AttributeViewModel();
        oAttr.setDescription(sDescription);
        oAttr.setData(sData);
        oMetadata.getAttributes().add(oAttr);
    }
}
