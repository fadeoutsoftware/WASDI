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
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import wasdi.LauncherMain;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.utils.BandImageManager;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.ZipFileUtils;
import wasdi.shared.utils.gis.GdalInfoResult;
import wasdi.shared.utils.gis.GdalUtils;
import wasdi.shared.viewmodels.products.*;

public class SnapProductReader extends WasdiProductReader {

	public SnapProductReader(File oProductFile) {
		super(oProductFile);
	}
	
	public ProductViewModel getProductViewModel() {
		
		LauncherMain.s_oLogger.debug("SnapProductReader.getProductViewModel: start");
		
		// Create View Model
		ProductViewModel oViewModel = new ProductViewModel();

        // Get Bands
        this.getSnapProductBandsViewModel(oViewModel, getSnapProduct());
        
        // Set name and path
        if (m_oProduct != null) oViewModel.setName(m_oProduct.getName());
        if (m_oProductFile!=null) oViewModel.setFileName(m_oProductFile.getName());
        

        // Snap set the name of geotiff files as geotiff: let replace with the file name
        if (oViewModel.getName().toLowerCase().equals("geotiff")) {
        	oViewModel.setName(oViewModel.getFileName());
        }

        LauncherMain.s_oLogger.debug("SnapProductReader.getProductViewModel: done");
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
            LauncherMain.s_oLogger.debug("SnapProductReader.FillBandsViewModel: ViewModel null, return");
            return;
        }

        if (oProduct == null) {
            LauncherMain.s_oLogger.debug("SnapProductReader.FillBandsViewModel: Product null, return");
            return;
        }

        if (oProductViewModel.getBandsGroups() == null) oProductViewModel.setBandsGroups(new NodeGroupViewModel("Bands"));

        LauncherMain.s_oLogger.debug("SnapProductReader.FillBandsViewModel: add bands");
        
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
			LauncherMain.s_oLogger.info("SnapProductReader.getProductBoundingBox: product is null return empty ");
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
			LauncherMain.s_oLogger.error("SnapProductReader.getProductBoundingBox: Exception " + e.getMessage());
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
	        	LauncherMain.s_oLogger.debug("SnapProductReader.adjustFileAfterDownload: File is a Sentinel 3 image, start unzip");
	            ZipFileUtils oZipExtractor = new ZipFileUtils();
	            oZipExtractor.unzip(sDownloadPath + File.separator + sFileNameFromProvider, sDownloadPath);
	            String sFolderName = sDownloadPath + sFileNameFromProvider.replace(".zip", ".SEN3");
	            LauncherMain.s_oLogger.debug("SnapProductReader.adjustFileAfterDownload: Unzip done, folder name: " + sFolderName);
	            sFileName = sFolderName + "/" + "xfdumanifest.xml";
	            LauncherMain.s_oLogger.debug("SnapProductReader.adjustFileAfterDownload: File Name changed in: " + sFileName);
	        }
		}
		catch (Exception oEx) {
			LauncherMain.s_oLogger.error("SnapProductReader.adjustFileAfterDownload: error ", oEx);
		}
		
		return sFileName;
	}
	
	@Override
	public File getFileForPublishBand(String sBand, String sLayerId) {
		
		m_oProduct = getSnapProduct();
		
		String sBaseDir = m_oProductFile.getParentFile().getPath();
		if (!sBaseDir.endsWith("/")) sBaseDir += "/";
		
		String sPlatform = WasdiFileUtils.getPlatformFromSatelliteImageFileName(m_oProductFile.getName());
		
		if (m_oProductFile.getName().toLowerCase().endsWith(".tif") || m_oProductFile.getName().toLowerCase().endsWith(".tiff")) {
			
			LauncherMain.s_oLogger.debug("SnapProductReader.getFileForPublishBand: this is a geotiff file");
			
			addPrjToMollweidTiffFiles();
			
			return m_oProductFile;
		}
		else if (sPlatform!=null) {
	        // Check if it is a S2
	        if (sPlatform.equals(Platforms.SENTINEL2)) {

	        	LauncherMain.s_oLogger.debug("SnapProductReader.getFileForPublishBand:  Managing S2 Product Band " + sBand);

	            Band oBand = m_oProduct.getBand(sBand);
	            Product oGeotiffProduct = new Product(sBand, "GEOTIFF");
	            oGeotiffProduct.addBand(oBand);
	            String sOutputFilePath = sBaseDir + sLayerId + ".tif";
				try {
					sOutputFilePath = new WasdiProductWriter(null, null).WriteGeoTiff(oGeotiffProduct, sBaseDir, sLayerId+".tif");
				} catch (Exception oEx) {
					LauncherMain.s_oLogger.debug("SnapProductReader.getFileForPublishBand: Exception converting S2 to geotiff " + oEx.toString() );
				}
	            File oOutputFile = new File(sOutputFilePath);
	            LauncherMain.s_oLogger.debug("SnapProductReader.getFileForPublishBand:  Geotiff File Created" + sOutputFilePath);
	            
	            return oOutputFile;

	        } else {
	        	
	        	String sOutputFilePath = "";
	        	File oOutputFile = new File(sOutputFilePath);

	        	LauncherMain.s_oLogger.debug("SnapProductReader.getFileForPublishBand:  Managing NON S2 Product Band " + sBand);

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
						LauncherMain.s_oLogger.debug("SnapProductReader.getFileForPublishBand:  GeoTiff File Created: " + sOutputFilePath);
					}
					else {
						LauncherMain.s_oLogger.debug("SnapProductReader.getFileForPublishBand:  Impossible to create: " + sOutputFilePath);
					}
					
				} catch (IOException oEx) {
					LauncherMain.s_oLogger.debug("Exception converting S1 to geotiff " + oEx.toString() );
				}
	            
	            return oOutputFile;
	        }			
		}
		
		LauncherMain.s_oLogger.debug("SnapProductReader.getFileForPublishBand: we did not find any useful way, return null");
		return null;
	}
	
	@Override
    public String getEPSG() {
		try {
            String sEPSG = CRS.lookupIdentifier(getSnapProduct().getSceneCRS(), true);
			return sEPSG;
		}
		catch (Exception oEx) {
			LauncherMain.s_oLogger.error("WasdiProductReader.getEPSG(): exception " + oEx.toString());
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
					LauncherMain.s_oLogger.debug("SnapProductReader.addPrjToMollweidTiffFiles: this is a Mollweide file, try to convert");
					
					String sExtension = Utils.GetFileNameExtension(m_oProductFile.getName());
					String sOutputFile = m_oProductFile.getAbsolutePath().replace("." +sExtension, ".prj");
					
		            File oPrjFile = new File(sOutputFile);
		            
		            if (oPrjFile.exists()==false) {
			            try (BufferedWriter oPrjWriter = new BufferedWriter(new FileWriter(oPrjFile))) {
			                // Fill the script file
			                if (oPrjWriter != null) {
			                    LauncherMain.s_oLogger.debug("SnapProductReader.addPrjToMollweidTiffFiles: " + sOutputFile + " file");
			                    oPrjWriter.write(GdalUtils.getMollweideProjectionDescription());
			                    oPrjWriter.flush();
			                    oPrjWriter.close();
			                }
			            } catch (IOException oEx) {
			            	LauncherMain.s_oLogger.debug("SnapProductReader.addPrjToMollweidTiffFiles: Exception converting Generating prj file " + oEx.toString() );
						}
		            }
				}
			}			
		}
		catch (Exception oEx) {
			
		}
	
	}

}
