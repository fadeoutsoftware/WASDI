package wasdi.io;

import java.awt.Dimension;
import java.awt.image.RenderedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.geotiff.GeoTIFF;
import org.esa.snap.core.util.geotiff.GeoTIFFMetadata;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.referencing.CRS;

import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.utils.MissionUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.ZipFileUtils;
import wasdi.shared.utils.gis.GdalBandInfo;
import wasdi.shared.utils.gis.GdalInfoResult;
import wasdi.shared.utils.gis.GdalUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.products.AttributeViewModel;
import wasdi.shared.viewmodels.products.BandViewModel;
import wasdi.shared.viewmodels.products.MetadataViewModel;
import wasdi.shared.viewmodels.products.NodeGroupViewModel;
import wasdi.shared.viewmodels.products.ProductViewModel;

public class SnapProductReader extends WasdiProductReader {

	public SnapProductReader(File oProductFile) {
		super(oProductFile);
	}
	
	public ProductViewModel getProductViewModel() {
		
		WasdiLog.debugLog("SnapProductReader.getProductViewModel: start");
		
		// Create View Model
		ProductViewModel oViewModel = new ProductViewModel();

        // Get Bands
        this.getSnapProductBandsViewModel(oViewModel, getSnapProduct());
        
        // Set name and path
        if (m_oProduct != null) oViewModel.setName(m_oProduct.getName());
        if (m_oProductFile != null) oViewModel.setFileName(m_oProductFile.getName());
        
        // fix name, if it is null
        if (!Utils.isNullOrEmpty(oViewModel.getFileName()) && Utils.isNullOrEmpty(oViewModel.getName())) {
        	oViewModel.setName(oViewModel.getFileName());
        }
        

        // Snap set the name of geotiff files as geotiff: let replace with the file name
        if (oViewModel.getName() != null && oViewModel.getName().toLowerCase().equals("geotiff")) {
        	oViewModel.setName(oViewModel.getFileName());
        }

        WasdiLog.debugLog("SnapProductReader.getProductViewModel: done");
		return oViewModel;
	}

    /**
     * Fills Band View Models
     * @param oProductViewModel
     * @param oProduct
     */
    protected void getSnapProductBandsViewModel2(ProductViewModel oProductViewModel, Product oProduct)
    {
        if (oProductViewModel == null) {
            WasdiLog.infoLog("SnapProductReader.getSnapProductBandsViewModel: ViewModel null, return");
            return;
        }

        if (oProduct == null) {
            WasdiLog.infoLog("SnapProductReader.getSnapProductBandsViewModel: Product null, check if it is a tiff to try backup reader");
            
            if (m_oProductFile.getName().toUpperCase().endsWith(".TIF")|| m_oProductFile.getName().toUpperCase().endsWith(".TIFF")) {
            	
            	System.setProperty("org.geotools.imageio.disable", "true");
            	
            	GridCoverage2DReader oTiffReader = null;
            	
            	try {
            		WasdiLog.infoLog("SnapProductReader.getSnapProductBandsViewModel: the file is a tiff, try to read with geotools instead");
            		
                    // Detect the format (GeoTIFF in this case)
                    oTiffReader = new GeoTiffReader(m_oProductFile);

                    // Read the coverage
                    GridCoverage2D oCoverage = oTiffReader.read(null);
                    RenderedImage oImage = oCoverage.getRenderedImage();

                    // GET NUMBER OF BANDS
                    int iNumBands = oCoverage.getNumSampleDimensions();
                    
                    // Initialize the band group
                    if (oProductViewModel.getBandsGroups() == null) oProductViewModel.setBandsGroups(new NodeGroupViewModel("Bands"));
                    
                    if (oProductViewModel.getBandsGroups().getBands() == null) {
                    	oProductViewModel.getBandsGroups().setBands(new ArrayList<BandViewModel>());
                    }
                        
                    // For Each band, add it to our group
                    for (int i = 0; i < iNumBands; i++) {
                    	
                        String sBandName = oCoverage.getSampleDimension(i).getDescription().toString();
                        
                        BandViewModel oViewModel = new BandViewModel(sBandName);
                        oViewModel.setWidth(oImage.getWidth());
                        oViewModel.setHeight(oImage.getHeight());
                        oProductViewModel.getBandsGroups().getBands().add(oViewModel);
                    }

                    oTiffReader.dispose();            		
            	}
            	catch (Throwable oEx) {
					WasdiLog.errorLog("SnapProductReader.getSnapProductBandsViewModel: error trying to recover the tiff reading " +  oEx.toString());
					if (oTiffReader!=null)
						try {
							oTiffReader.dispose();
						} catch (IOException oInnerEx) {
							WasdiLog.errorLog("SnapProductReader.getSnapProductBandsViewModel: error trying to recover the tiff reading " +  oInnerEx.toString());
						}
				}
            }
        }
        else {
            if (oProductViewModel.getBandsGroups() == null) oProductViewModel.setBandsGroups(new NodeGroupViewModel("Bands"));

            WasdiLog.debugLog("SnapProductReader.getSnapProductBandsViewModel: add bands");
            
            for (Band oBand : oProduct.getBands()) {

                if (oProductViewModel.getBandsGroups().getBands() == null)
                    oProductViewModel.getBandsGroups().setBands(new ArrayList<BandViewModel>());

                BandViewModel oViewModel = new BandViewModel(oBand.getName());
                oViewModel.setWidth(oBand.getRasterWidth());
                oViewModel.setHeight(oBand.getRasterHeight());
                oProductViewModel.getBandsGroups().getBands().add(oViewModel);
            }        	
        }
    }
    
    
    protected void getSnapProductBandsViewModel(ProductViewModel oProductViewModel, Product oProduct) {
        if (oProductViewModel == null) {
            WasdiLog.infoLog("SnapProductReader.getSnapProductBandsViewModel: ViewModel null, return");
            return;
        }

        // Initialize band group
        if (oProductViewModel.getBandsGroups() == null)
            oProductViewModel.setBandsGroups(new NodeGroupViewModel("Bands"));
        
        if (oProductViewModel.getBandsGroups().getBands() == null)
            oProductViewModel.getBandsGroups().setBands(new ArrayList<>());
        

        // CASE 1: SNAP successfully opened the product
        if (oProduct != null) {

            WasdiLog.debugLog("SnapProductReader.getSnapProductBandsViewModel: add bands from SNAP");

            for (Band oBand : oProduct.getBands()) {
            	
                BandViewModel oViewModel = new BandViewModel(oBand.getName());
                oViewModel.setWidth(oBand.getRasterWidth());
                oViewModel.setHeight(oBand.getRasterHeight());
                oProductViewModel.getBandsGroups().getBands().add(oViewModel);
            }

            return;
        }

        // CASE 2: SNAP failed → fallback to TIFF reader
        WasdiLog.debugLog("SnapProductReader.getSnapProductBandsViewModel: SNAP product null, checking TIFF fallback");

        if (!m_oProductFile.getName().toUpperCase().endsWith(".TIF") &&
            !m_oProductFile.getName().toUpperCase().endsWith(".TIFF")) {
            WasdiLog.debugLog("SnapProductReader.getSnapProductBandsViewModel: Not a TIFF, cannot fallback");
            return;
        }

        WasdiLog.infoLog("SnapProductReader.getSnapProductBandsViewModel: Using Apache Commons Imaging fallback for tiff");

        try {
        	/*
            // Read TIFF metadata
            TiffImageMetadata oMetadata = (TiffImageMetadata) Imaging.getMetadata(m_oProductFile);

            if (oMetadata == null) {
                WasdiLog.errorLog("SnapProductReader.getSnapProductBandsViewModel: TIFF metadata null");
                return;
            }

            // Most GeoTIFFs have a single directory
            Directory oDir = (Directory) oMetadata.getDirectories().get(0);
            oDir.getAllFields().get

            int iWidth = oDir.getSingleFieldValue(TiffTagConstants.TIFF_TAG_IMAGE_WIDTH);
            int iHeight = oDir.getSingleFieldValue(TiffTagConstants.TIFF_TAG_IMAGE_LENGTH);
            int iNumBands = oDir.getFieldValue(TiffTagConstants.TIFF_TAG_SAMPLES_PER_PIXEL);

            WasdiLog.debugLog("SnapProductReader.getSnapProductBandsViewModel: TIFF width=" + iWidth + " height=" + iHeight + " bands=" + iNumBands);


            // Add bands using WASDI naming convention
            for (int i = 0; i < iNumBands; i++) {
                String sBandName = "Band_" + (i + 1); // WASDI naming convention

                BandViewModel oViewModel = new BandViewModel(sBandName);
                oViewModel.setWidth(iWidth);
                oViewModel.setHeight(iHeight);

                oProductViewModel.getBandsGroups().getBands().add(oViewModel);
            }
            
            WasdiLog.debugLog("SnapProductReader.getSnapProductBandsViewModel: backup tiff read done");
            */
        	
        	
        	GdalInfoResult oGdalInfo = GdalUtils.getGdalInfoResult(m_oProductFile);
        	if (oGdalInfo != null) {
            	int iWidth = 0;
            	int iHeight = 0;
        		
        		if (oGdalInfo.size != null) {
        			if (oGdalInfo.size.size()>=2) {
            			iWidth= oGdalInfo.size.get(0);
            			iHeight= oGdalInfo.size.get(1);        				
        			}
        		}
        		
        		if (oGdalInfo.bands != null) {
        			int iBandCount = 0;
        			for (GdalBandInfo oGdalBand : oGdalInfo.bands) {
        				String sBandName = "Band_" + (iBandCount + 1); 
        				iBandCount ++;
        				if (!Utils.isNullOrEmpty(oGdalBand.description)) {
        					sBandName = oGdalBand.description;
        				}

                        BandViewModel oViewModel = new BandViewModel(sBandName);
                        oViewModel.setWidth(iWidth);
                        oViewModel.setHeight(iHeight);

                        oProductViewModel.getBandsGroups().getBands().add(oViewModel);
					}
        		}
        		
        	}
        	
        	

        } catch (Exception oEx) {
            WasdiLog.errorLog("SnapProductReader.getSnapProductBandsViewModel: TIFF fallback error " + oEx.toString());
        }
    }
    
	@Override
	public String getProductBoundingBox() {
		
		Product oProduct = getSnapProduct();
		
		if (oProduct == null) {
			WasdiLog.infoLog("SnapProductReader.getProductBoundingBox: product is null return empty ");
			return "";
		}
		
		if (m_oProductFile.getName().toLowerCase().endsWith(".tif") || m_oProductFile.getName().toLowerCase().endsWith(".tiff")) {
        	addPrjToMollweidTiffFiles();
        }
		
		String sBB = "";
		try {
			
			// Snap Bounding Box
			GeoCoding oGeocoding = oProduct.getSceneGeoCoding();
			if (oGeocoding!=null) {
				Dimension oDim = oProduct.getSceneRasterSize();		
				GeoPos oMin = oGeocoding.getGeoPos(new PixelPos(0,0), null);
				GeoPos oMax = oGeocoding.getGeoPos(new PixelPos(oDim.getWidth(), oDim.getHeight()), null);
				float fMinX = (float) Math.min(oMin.lon, oMax.lon);
				float fMinY = (float) Math.min(oMin.lat, oMax.lat);
				float fMaxX = (float) Math.max(oMin.lon, oMax.lon);
				float fMaxY = (float) Math.max(oMin.lat, oMax.lat);
				
				sBB = String.format("%f,%f,%f,%f,%f,%f,%f,%f,%f,%f", fMinY, fMinX, fMinY, fMaxX, fMaxY, fMaxX, fMaxY, fMinX, fMinY, fMinX);
			}

			return sBB;
		} catch (Exception e) {
			WasdiLog.errorLog("SnapProductReader.getProductBoundingBox: Exception " + e.getMessage());
		}
		
		return "";
	}
	@Override
	public MetadataViewModel getProductMetadataViewModel() {
		m_oProduct = getSnapProduct();
		if (m_oProduct == null) return null;
		return  getSnapProductMetadataViewModel(m_oProduct.getMetadataRoot(), new MetadataViewModel("Metadata"));
	}
	
    /**
     * Recursive Metadata Explorer Function
     * @param oElement
     * @param oSourceViewModel
     * @return
     */
    private MetadataViewModel getSnapProductMetadataViewModel(MetadataElement oElement, MetadataViewModel oSourceViewModel) {

    	// For Each Attribute
        for (MetadataAttribute oMetadataAttribute : oElement.getAttributes()) {
        	
        	// Data Exists ?
            if (oMetadataAttribute.getData() != null) {
            	
            	// Create Attribute View Model
                AttributeViewModel oAttributeViewModel = new AttributeViewModel();
                
                // Leave the name: this is a code...
                //oAttributeViewModel.setName(oMetadataAttribute.getName());
                
                if (!Utils.isNullOrEmpty(oMetadataAttribute.getDescription())) {
                	oAttributeViewModel.setDescription(oMetadataAttribute.getDescription());
                }
                else if (!Utils.isNullOrEmpty(oMetadataAttribute.getName())) {
                	oAttributeViewModel.setDescription(oMetadataAttribute.getName());
                }
                
            	oAttributeViewModel.setData(oMetadataAttribute.getData().toString());
            	
                if (oSourceViewModel.getAttributes() == null) oSourceViewModel.setAttributes(new ArrayList<AttributeViewModel>());
                
                oSourceViewModel.getAttributes().add(oAttributeViewModel);
            }
        }


        for (MetadataElement oMetadataElement : oElement.getElements()) {
            MetadataViewModel oElementViewModel = new MetadataViewModel(oMetadataElement.getName());
            
            if (oSourceViewModel.getElements() == null) {
            	oSourceViewModel.setElements(new ArrayList<MetadataViewModel>());
            }
            
            oSourceViewModel.getElements().add(getSnapProductMetadataViewModel(oMetadataElement, oElementViewModel));
        }

        return  oSourceViewModel;
    }
    
	@Override
	public String adjustFileAfterDownload(String sDownloadedFileFullPath, String sFileNameFromProvider, String sPlatform) {
		
		String sFileName = sDownloadedFileFullPath;
		
		if (Utils.isNullOrEmpty(sPlatform)) {
			sPlatform = MissionUtils.getPlatformFromSatelliteImageFileName(sFileNameFromProvider);
			if (sPlatform==null) {
				sPlatform = "";
			}
		}
		
		try {

	        if (sFileNameFromProvider.startsWith("S3") && sFileNameFromProvider.toLowerCase().endsWith(".zip")) {
	        	String sDownloadPath = new File(sDownloadedFileFullPath).getParentFile().getPath();
	        	WasdiLog.debugLog("SnapProductReader.adjustFileAfterDownload: File is a Sentinel 3 image, start unzip");
	            ZipFileUtils oZipExtractor = new ZipFileUtils();
	            oZipExtractor.unzip(sDownloadPath + File.separator + sFileNameFromProvider, sDownloadPath);
	            String sFolderName = sDownloadPath + sFileNameFromProvider.replace(".zip", ".SEN3");
	            WasdiLog.debugLog("SnapProductReader.adjustFileAfterDownload: Unzip done, folder name: " + sFolderName);
	            sFileName = sFolderName + "/" + "xfdumanifest.xml";
	            WasdiLog.debugLog("SnapProductReader.adjustFileAfterDownload: File Name changed in: " + sFileName);
	        } 
	        else if(sPlatform.equals(Platforms.LANDSAT5) && sFileNameFromProvider.endsWith(".zip")) {
	        	
	        	WasdiLog.debugLog("SnapProductReader.adjustFileAfterDownload: File is a Landsat-5 product, start unzip");
	        	String sDownloadFolderPath = new File(sDownloadedFileFullPath).getParentFile().getPath();
	        	ZipFileUtils oZipExtractor = new ZipFileUtils();
	        	oZipExtractor.unzip(sDownloadFolderPath + File.separator + sFileNameFromProvider, sDownloadFolderPath);
	        	
	        	String sLandsat5UnzippedFolderPath = sDownloadFolderPath + File.separator + sFileNameFromProvider.replace(".zip", "");
	        	File oLandsat5UnzippedFolder = new File(sLandsat5UnzippedFolderPath);
	        
	        	if (!oLandsat5UnzippedFolder.exists() || !oLandsat5UnzippedFolder.isDirectory()) {
	        		WasdiLog.warnLog("SnapProductReader.adjustFileAfterDownload: file does not exists or is not a folder " + sLandsat5UnzippedFolderPath);
	        		return sFileName;
	        	}
	        	
	        	// now we need to look for the ".TIFF" folder
	        	File oTIFFolder = null;
	        	for (File oFile : oLandsat5UnzippedFolder.listFiles()) {
	        		if (oFile.isDirectory() && oFile.getName().endsWith(".TIFF")) {
	        			oTIFFolder = oFile;
	        			break;
	        		}
	        	}
	        	
	        	if (oTIFFolder == null) {
	        		WasdiLog.warnLog("SnapProductReader.adjustFileAfterDownload: TIFF folder with Landsat-5 files not found");
	        		return sFileName;
	        	}
	        	
	        	// if we found the TIF folder, then we can access the "MTL" file
	        	File oMTLFile = null;
	        	for (File oFile : oTIFFolder.listFiles()) {
	        		if (oFile.getName().endsWith("_MTL.txt")) {
	        			oMTLFile = oFile;
	        			break;
	        		}
 	        	}
	        	
	        	if (oMTLFile == null) {
	        		WasdiLog.warnLog("SnapProductReader.adjustFileAfterDownload: no MTL file that can be read by SNAP");
	        		return sFileName;
	        	}
	        	
	        	sFileName = oMTLFile.getAbsolutePath();
	        	m_oProductFile = oMTLFile;
	        	WasdiLog.debugLog("SnapProductReader.adjustFileAfterDownload: MTL file found " + sFileName);
	        }
 		}
		catch (Exception oEx) {
			WasdiLog.errorLog("SnapProductReader.adjustFileAfterDownload: error ", oEx);
		}
		
		return sFileName;
	}
	
	@Override
	public File getFileForPublishBand(String sBand, String sLayerId, String sPlatform) {
		
		m_oProduct = getSnapProduct();
		
		String sBaseDir = m_oProductFile.getParentFile().getPath();
		
		if (!sBaseDir.endsWith("/")) sBaseDir += "/";
		
		if (m_oProductFile.getName().toLowerCase().endsWith(".tif") || m_oProductFile.getName().toLowerCase().endsWith(".tiff")) {
			
			WasdiLog.debugLog("SnapProductReader.getFileForPublishBand: this is a geotiff file");
			
			addPrjToMollweidTiffFiles();
			
			return m_oProductFile;
		}
		else if (sPlatform!=null) {
			
			if (sPlatform.equals(Platforms.ERS)) {
				WasdiLog.debugLog("SnapProductReader.getFileForPublishBand: publishing bands for ERS products is not yet supported");
				return null;
			}
			
			if (sPlatform.equals(Platforms.VIIRS) 
					&& (m_oProductFile.getName().startsWith("VNP21A1D") 
							|| m_oProductFile.getName().startsWith("VNP21A1N")
							|| m_oProductFile.getName().startsWith("VNP15A2H"))) {
				WasdiLog.debugLog("SnapProductReader.getFileForPublishBand: publishing bands for this VIIRS products is not yet supported");
				return null;
			}
			
	        // Check if it is a S2
	        if (sPlatform.equals(Platforms.SENTINEL2)) {

	        	WasdiLog.debugLog("SnapProductReader.getFileForPublishBand:  Managing S2 Product Band " + sBand);

	            Band oBand = m_oProduct.getBand(sBand);
	            Product oGeotiffProduct = new Product(sBand, "GeoTIFF", m_oProduct.getSceneRasterWidth(), m_oProduct.getSceneRasterHeight());
	            oGeotiffProduct.addBand(oBand);
	            String sOutputFilePath = sBaseDir + sLayerId + ".tif";
				try {
					sOutputFilePath = new WasdiProductWriter(null, null).WriteGeoTiff(oGeotiffProduct, sBaseDir, sLayerId+".tif");
				} catch (Exception oEx) {
					WasdiLog.debugLog("SnapProductReader.getFileForPublishBand: Exception converting S2 to geotiff " + oEx.toString() );
				}
	            File oOutputFile = new File(sOutputFilePath);
	            WasdiLog.debugLog("SnapProductReader.getFileForPublishBand:  Geotiff File Created" + sOutputFilePath);
	            
	            return oOutputFile;

	        } else {
	        	
	        	String sOutputFilePath = sBaseDir + sLayerId + ".tif";
	        	File oOutputFile = new File(sOutputFilePath);

	        	WasdiLog.debugLog("SnapProductReader.getFileForPublishBand:  Managing NON S2 Product Band " + sBand);

	            // Get the Band
	            Band oBand = m_oProduct.getBand(sBand);
	            // Get Image
	            // MultiLevelImage oBandImage = oBand.getSourceImage();
	            RenderedImage oBandImage = oBand.getSourceImage();

	            // Get TIFF Metadata
	            GeoTIFFMetadata oMetadata = ProductUtils.createGeoTIFFMetadata(m_oProduct);
	            
	            try {
					if (GeoTIFF.writeImage(oBandImage, oOutputFile, oMetadata)) {
						WasdiLog.debugLog("SnapProductReader.getFileForPublishBand:  GeoTiff File Created: " + sOutputFilePath);
					}
					else {
						WasdiLog.debugLog("SnapProductReader.getFileForPublishBand:  Impossible to create: " + sOutputFilePath);
					}
					
				} catch (IOException oEx) {
					WasdiLog.debugLog("Exception converting S1 to geotiff " + oEx.toString() );
				}
	            
	            return oOutputFile;
	        }			
		}
		
		WasdiLog.debugLog("SnapProductReader.getFileForPublishBand: we did not find any useful way, return null");
		return null;
	}
	
	@Override
    public String getEPSG() {
		try {
            String sEPSG = CRS.lookupIdentifier(getSnapProduct().getSceneCRS(), true);
            
            if (Utils.isNullOrEmpty(sEPSG)) {
            	WasdiLog.errorLog("SnapProductReader.getEPSG(): sEPSG is null, try with gdal");
            	sEPSG = super.getEPSG();
            }
			return sEPSG;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("SnapProductReader.getEPSG(): exception ", oEx);
		}
		return null;    	
    }
	
	/**
	 * Mollweid projected files are not read by geotools if the do not have the .prj file.
	 * This methods checks and if it is missing it creates it
	 */
	protected void addPrjToMollweidTiffFiles() {
		
		try {
			GdalInfoResult oGdalInfoResult = GdalUtils.getGdalInfoResult(m_oProductFile);
			if (oGdalInfoResult != null) {
				if (oGdalInfoResult.coordinateSystemWKT.contains("Mollweide")) {
					WasdiLog.debugLog("SnapProductReader.addPrjToMollweidTiffFiles: this is a Mollweide file, try to convert");
					
					String sExtension = WasdiFileUtils.getFileNameExtension(m_oProductFile.getName());
					String sOutputFile = m_oProductFile.getAbsolutePath().replace("." +sExtension, ".prj");
					
		            File oPrjFile = new File(sOutputFile);
		            
		            if (oPrjFile.exists()==false) {
			            try (BufferedWriter oPrjWriter = new BufferedWriter(new FileWriter(oPrjFile))) {
			                // Fill the script file
			                if (oPrjWriter != null) {
			                    WasdiLog.debugLog("SnapProductReader.addPrjToMollweidTiffFiles: " + sOutputFile + " file");
			                    oPrjWriter.write(GdalUtils.getMollweideProjectionDescription());
			                    oPrjWriter.flush();
			                    oPrjWriter.close();
			                }
			            } catch (IOException oEx) {
			            	WasdiLog.debugLog("SnapProductReader.addPrjToMollweidTiffFiles: Exception converting Generating prj file " + oEx.toString() );
						}
		            }
				}
			}			
		}
		catch (Exception oEx) {
        	WasdiLog.errorLog("SnapProductReader.addPrjToMollweidTiffFiles: error ", oEx);
		}
	
	}

}
