package wasdi.io;

import java.awt.Dimension;
import java.awt.image.ColorModel;
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
import org.geotools.referencing.CRS;

import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.utils.MissionUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.ZipFileUtils;
import wasdi.shared.utils.gis.GdalInfoResult;
import wasdi.shared.utils.gis.GdalUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.products.AttributeViewModel;
import wasdi.shared.viewmodels.products.BandViewModel;
import wasdi.shared.viewmodels.products.MetadataViewModel;
import wasdi.shared.viewmodels.products.NodeGroupViewModel;
import wasdi.shared.viewmodels.products.ProductViewModel;
import wasdi.snapopearations.BandImageManager;

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
    protected void getSnapProductBandsViewModel(ProductViewModel oProductViewModel, Product oProduct)
    {
        if (oProductViewModel == null) {
            WasdiLog.debugLog("SnapProductReader.FillBandsViewModel: ViewModel null, return");
            return;
        }

        if (oProduct == null) {
            WasdiLog.debugLog("SnapProductReader.FillBandsViewModel: Product null, return");
            return;
        }

        if (oProductViewModel.getBandsGroups() == null) oProductViewModel.setBandsGroups(new NodeGroupViewModel("Bands"));

        WasdiLog.debugLog("SnapProductReader.FillBandsViewModel: add bands");
        
        for (Band oBand : oProduct.getBands()) {

            if (oProductViewModel.getBandsGroups().getBands() == null)
                oProductViewModel.getBandsGroups().setBands(new ArrayList<BandViewModel>());

            BandViewModel oViewModel = new BandViewModel(oBand.getName());
            oViewModel.setWidth(oBand.getRasterWidth());
            oViewModel.setHeight(oBand.getRasterHeight());
            oProductViewModel.getBandsGroups().getBands().add(oViewModel);
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
	public String adjustFileAfterDownload(String sDownloadedFileFullPath, String sFileNameFromProvider) {
		
		String sFileName = sDownloadedFileFullPath;
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
	        else if(MissionUtils.getPlatformFromSatelliteImageFileName(sFileNameFromProvider).equals(Platforms.LANDSAT5)
	        		&& sFileNameFromProvider.endsWith(".zip")) {
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
	public File getFileForPublishBand(String sBand, String sLayerId) {
		
		m_oProduct = getSnapProduct();
		
		String sBaseDir = m_oProductFile.getParentFile().getPath();
		if (!sBaseDir.endsWith("/")) sBaseDir += "/";
		
		String sPlatform = MissionUtils.getPlatformFromSatelliteImageFileName(m_oProductFile.getName());
		
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
	            Product oGeotiffProduct = new Product(sBand, "GEOTIFF");
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

	            // Check if the Colour Model is present
	            ColorModel oColorModel = oBandImage.getColorModel();

	            // Tested for Copernicus Marine - netcdf files
	            if (oColorModel == null) {

	                // Colour Model not present: try a different way to get the Image
	                BandImageManager oImgManager = new BandImageManager(m_oProduct);

	                // Create full dimension and View port
	                Dimension oOutputImageSize = new Dimension(oBand.getRasterWidth(), oBand.getRasterHeight());

	                // Render the image
	                oBandImage = oImgManager.buildImageWithMasks(oBand, oOutputImageSize, null, false, true);
	            }

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
