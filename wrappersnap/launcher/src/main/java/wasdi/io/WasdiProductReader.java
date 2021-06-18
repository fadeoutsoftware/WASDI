package wasdi.io;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import wasdi.LauncherMain;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.AttributeViewModel;
import wasdi.shared.viewmodels.BandViewModel;
import wasdi.shared.viewmodels.GeorefProductViewModel;
import wasdi.shared.viewmodels.MetadataViewModel;
import wasdi.shared.viewmodels.NodeGroupViewModel;
import wasdi.shared.viewmodels.ProductViewModel;

/**
 * Read SNAP Product utility class
 * Created by s.adamo on 18/05/2016.
 * Refactoring of 21/10/2019 (p.campanella):
 * Changed class name
 * Added support to different file types (starting from shape files)
 **/
public class WasdiProductReader {
	
	Product m_oProduct;
	File m_oProductFile;
	
	public WasdiProductReader() {
		
	}
	
	public WasdiProductReader(File oProductFile) {
		if (oProductFile!=null) {
			if (oProductFile.exists()) {
				m_oProductFile = oProductFile;
				m_oProduct = readSnapProduct(m_oProductFile, null);
			}
		}
	}

	/**
	 * Get the SNAP product (or null)
	 * @return
	 */
	public Product getSnapProduct() {
		return m_oProduct;
	}
	
	/**
	 * Get the product File Java Object
	 * @return
	 */
	public File getProductFile() {
		return m_oProductFile;
	}
	
    /**
     * Read a WASDI Product 
     * @param oFile File to open
     * @param sFormatName Format, if known.
     * @return Product object
     */
    public Product readSnapProduct(File oFile, String sFormatName) {
    	
        Product oProduct = null;

        // P.Campanella 2019/04/16: deleted the static cache. There is a new instance of this class every time is used
        // so the cache was useless and could have memory problems
        
        if (oFile == null) {
        	LauncherMain.s_oLogger.debug("WasdiProductReader.ReadProduct: file to read is null, return null ");
        	return null;
        }
        
        try {

        	long lStartTime = System.currentTimeMillis();
            LauncherMain.s_oLogger.debug("WasdiProductReader.ReadProduct: begin read " + oFile.getAbsolutePath());

            if (sFormatName != null) {
                oProduct = ProductIO.readProduct(oFile, sFormatName);
            } 
            else {
                oProduct = ProductIO.readProduct(oFile);                
            }
                
            LauncherMain.s_oLogger.debug("WasdiProductReader.ReadProduct: read done in " + (System.currentTimeMillis() - lStartTime) + "ms");
            
            return oProduct;
            
        } catch (Exception oEx) {
            oEx.printStackTrace();
            LauncherMain.s_oLogger.debug("WasdiProductReader.ReadProduct: excetpion: " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
        }

        return null;
    }

    /**
     * discover the format of the product contained in oFile
     * @param oFile
     * @return the format name if the reader plugin manage one and only one format. null otherwise
     */
    public String getProductFormat(File oFile) {
        ProductReader oProductReader = ProductIO.getProductReaderForInput(oFile);
        ProductReaderPlugIn oPlugin = oProductReader.getReaderPlugIn();
        String[] asFormats = oPlugin.getFormatNames();    	        
        if (asFormats==null || asFormats.length != 1) return null;        
        return asFormats[0];
    }
    
    /**
     * Get Product View Model from a Shape File
     * @param oFile File to read
     * @return Product View Model representing the shape file
     */
    public ProductViewModel getShapeFileProductViewModel(File oFile) {
    	
    	// Create the return value
    	GeorefProductViewModel oRetViewModel = null;
    	
    	try {
    		
    		// Try to read the shape
            ShapefileDataStore oShapefileDataStore = new ShapefileDataStore(oFile.toURI().toURL());
            
            // Got it?
            if (oShapefileDataStore!=null) {
            	
            	// Create the Product View Model
            	oRetViewModel = new GeorefProductViewModel();
            	
            	// Set name values
            	oRetViewModel.setFileName(oFile.getName());
            	oRetViewModel.setName(Utils.getFileNameWithoutLastExtension(oFile.getName()));
            	oRetViewModel.setProductFriendlyName(oRetViewModel.getName());
            	
            	// Create the sub folder
            	NodeGroupViewModel oNodeGroupViewModel = new NodeGroupViewModel();
            	oNodeGroupViewModel.setNodeName("ShapeFile");
            	
            	// Create the single band representing the shape
            	BandViewModel oBandViewModel = new BandViewModel();
            	oBandViewModel.setPublished(false);
            	oBandViewModel.setGeoserverBoundingBox("");
            	oBandViewModel.setHeight(0);
            	oBandViewModel.setWidth(0);
            	oBandViewModel.setPublished(false);
            	oBandViewModel.setName(oRetViewModel.getName());
            	
            	ArrayList<BandViewModel> oBands = new ArrayList<BandViewModel>();
            	oBands.add(oBandViewModel);
            	
            	oNodeGroupViewModel.setBands(oBands);
            	
            	oRetViewModel.setBandsGroups(oNodeGroupViewModel);
            	
            	
            	// Bounding Box
            	oRetViewModel.setBbox("");
            	
            }
            /*
            // Sample code to read the features
            SimpleFeatureIterator features = oShapefileDataStore.getFeatureSource().getFeatures().features();

            while (features.hasNext()) {
            	SimpleFeature shp = features.next();
            	String name = (String)shp.getAttribute("");
            	MultiPolygon geom = (MultiPolygon) shp.getDefaultGeometry();
            }
            
            features.close();
            */
            
            // Clean
            oShapefileDataStore.dispose();    	
            
            String sBbox = getShapeFileBoundingBox(oFile);
            oRetViewModel.setBbox(sBbox);
    	}
    	catch (Exception oEx) {
    		LauncherMain.s_oLogger.debug("WasdiProductReader.getShapeFileProduct: exception reading the shape file");
		}
    	
    	return oRetViewModel;

    }
    
   public ProductViewModel getVrtFileProductViewModel(File oFile) {
    	
    	// Create the return value
    	GeorefProductViewModel oRetViewModel = null;
    	
    	try {
    		
        	// Create the Product View Model
        	oRetViewModel = new GeorefProductViewModel();
        	
        	// Set name values
        	oRetViewModel.setFileName(oFile.getName());
        	oRetViewModel.setName(Utils.getFileNameWithoutLastExtension(oFile.getName()));
        	oRetViewModel.setProductFriendlyName(oRetViewModel.getName());
        	
        	// Create the sub folder
        	NodeGroupViewModel oNodeGroupViewModel = new NodeGroupViewModel();
        	oNodeGroupViewModel.setNodeName("VRT");
        	
        	// Create the single band representing the shape
        	BandViewModel oBandViewModel = new BandViewModel();
        	oBandViewModel.setPublished(false);
        	oBandViewModel.setGeoserverBoundingBox("");
        	oBandViewModel.setHeight(0);
        	oBandViewModel.setWidth(0);
        	oBandViewModel.setPublished(false);
        	oBandViewModel.setName("VRT Fake Band");
        	
        	ArrayList<BandViewModel> oBands = new ArrayList<BandViewModel>();
        	oBands.add(oBandViewModel);
        	
        	oNodeGroupViewModel.setBands(oBands);
        	
        	oRetViewModel.setBandsGroups(oNodeGroupViewModel);
    	}
    	catch (Exception oEx) {
    		LauncherMain.s_oLogger.debug("WasdiProductReader.getShapeFileProduct: exception reading the shape file");
		}
    	
    	return oRetViewModel;

    }
    /**
     * Converts a product in a View Model
     * @param oFile
     * @return
     * @throws IOException
     */
    public ProductViewModel getProductViewModel(File oFile) throws IOException
    {
        LauncherMain.s_oLogger.debug("WasdiProductReader.getProductViewModel: start");

        Product oExportProduct = null;
        
        try {
            oExportProduct =readSnapProduct(oFile, null);

            if (oExportProduct == null) {
            	
            	if (oFile.getName().toLowerCase().endsWith("shp")) {
                    LauncherMain.s_oLogger.debug("WasdiProductReader.getProductViewModel: this is a shape file");
                    ProductViewModel oRetValue = getShapeFileProductViewModel(oFile);
                    
                    return oRetValue;            		
            	}
            	else if (oFile.getName().toLowerCase().endsWith("vrt")) {
                    LauncherMain.s_oLogger.debug("WasdiProductReader.getProductViewModel: this is a vrt file");
                    ProductViewModel oRetValue = getVrtFileProductViewModel(oFile);
                    
                    return oRetValue;            		
            	}
            	else {
            		LauncherMain.s_oLogger.debug("WasdiProductReader.getProductViewModel: unsupported file");
            		return null;
            	}
            }

            ProductViewModel oViewModel = getProductViewModel(oExportProduct, oFile);

            //LauncherMain.s_oLogger.debug("WasdiProductReader.getProductViewModel: done");
            return  oViewModel;        	
        }
        finally {
        	if (oExportProduct != null) {
        		oExportProduct.dispose();
        	}
        }
    }

    /**
     * Converts a product in a View Model
     * @param oFile
     * @return
     * @throws IOException
     */
    public ProductViewModel getProductViewModel() throws IOException
    {        
        if (m_oProduct == null) {
        	
        	if (m_oProductFile != null) {
        		return getProductViewModel(m_oProductFile);
        	}
        	else {
            	LauncherMain.s_oLogger.debug("WasdiProductReader.getProductViewModel: member product and file are null, return null");
            	return null;
        	}        	
        }

        ProductViewModel oViewModel = getProductViewModel(m_oProduct, m_oProductFile);

        return  oViewModel;
    }
    
    /**
     * Converts a product in a View Model
     * @param oExportProduct
     * @return
     */
	public ProductViewModel getProductViewModel(Product oExportProduct, File oFile) {
		
		LauncherMain.s_oLogger.debug("WasdiProductReader.getProductViewModel: start");
		
		// Create View Model
		ProductViewModel oViewModel = new ProductViewModel();

        // Get Bands
        this.FillBandsViewModel(oViewModel, oExportProduct);
        
        // Set name and path
        if (oExportProduct != null) oViewModel.setName(oExportProduct.getName());
        if (oFile!=null) oViewModel.setFileName(oFile.getName());

        LauncherMain.s_oLogger.debug("WasdiProductReader.getProductViewModel: done");
		return oViewModel;
	}

	/**
	 * Get Product Bounding Box from a File
	 * @param oProductFile
	 * @return "%f,%f,%f,%f,%f,%f,%f,%f,%f,%f", minY, minX, minY, maxX, maxY, maxX, maxY, minX, minY, minX
	 */
	public String getProductBoundingBox(File oProductFile) {
		
		if (oProductFile == null) {
			LauncherMain.s_oLogger.info("WasdiProductReader.getProductBoundingBox: file is null return empty ");
			return "";
		}
		
		Product oProduct = null;
		try {
			oProduct = ProductIO.readProduct(oProductFile);
			
			if (oProduct == null) {
				// Not a SNAP product. Try shape file
				return getShapeFileBoundingBox(oProductFile);
			}
			else {
				// Snap Bounding Box
				GeoCoding geocoding = oProduct.getSceneGeoCoding();
				if (geocoding!=null) {
					Dimension dim = oProduct.getSceneRasterSize();		
					GeoPos min = geocoding.getGeoPos(new PixelPos(0,0), null);
					GeoPos max = geocoding.getGeoPos(new PixelPos(dim.getWidth(), dim.getHeight()), null);
					float minX = (float) Math.min(min.lon, max.lon);
					float minY = (float) Math.min(min.lat, max.lat);
					float maxX = (float) Math.max(min.lon, max.lon);
					float maxY = (float) Math.max(min.lat, max.lat);
					
					return String.format("%f,%f,%f,%f,%f,%f,%f,%f,%f,%f", minY, minX, minY, maxX, maxY, maxX, maxY, minX, minY, minX);
				}				
			}
			
			
		} catch (Exception e) {
			LauncherMain.s_oLogger.error("WasdiProductReader.getProductBoundingBox: Exception " + e.getMessage());
		}
		finally {
			if (oProduct != null) {
				oProduct.dispose();
			}
		}
		
		return "";
	}
	
	/**
	 * Get Product Bounding Box from a File
	 * @param oProductFile
	 * @return "%f,%f,%f,%f,%f,%f,%f,%f,%f,%f", minY, minX, minY, maxX, maxY, maxX, maxY, minX, minY, minX
	 */
	public String getProductBoundingBox(Product oProduct) {
		
		if (oProduct == null) {
			LauncherMain.s_oLogger.info("WasdiProductReader.getProductBoundingBox: product is null return empty ");
			return "";
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
			if(Utils.isNullOrEmpty(sBB)) {
				//todo try reading BB from a shapefile
			}
			return sBB;
		} catch (Exception e) {
			LauncherMain.s_oLogger.error("WasdiProductReader.getProductBoundingBox: Exception " + e.getMessage());
		}
		
		return "";
	}
	
	/**
	 * Get Product Bounding Box 
	 * @return "%f,%f,%f,%f,%f,%f,%f,%f,%f,%f", minY, minX, minY, maxX, maxY, maxX, maxY, minX, minY, minX
	 */
	public String getProductBoundingBox() {
		
		return getProductBoundingBox(m_oProduct);
	}
	
	/**
	 * Get the bounding box of a shape file
	 * @param oShapeFile
	 * @return
	 */
	public String getShapeFileBoundingBox(File oShapeFile) {
		
		String sBbox = "";
		ShapefileDataStore oShpFileDataStore = null;
		
		try {
			// Open the data store
			oShpFileDataStore = new ShapefileDataStore(oShapeFile.toURI().toURL());
			SimpleFeatureCollection oFeatColl = oShpFileDataStore.getFeatureSource().getFeatures();
			
			// Check the coordinate system
			CoordinateReferenceSystem oCrs = oFeatColl.getSchema().getGeometryDescriptor().getCoordinateReferenceSystem();
		    if (oCrs == null) {
		        oCrs = DefaultGeographicCRS.WGS84;
		    }
		    
		    // Get the envelope
			ReferencedEnvelope oBbox = oFeatColl.getBounds();
			double dMinY = oBbox.getMinY();
			double dMinX = oBbox.getMinX();
			double dMaxY = oBbox.getMaxY();
			double dMaxX = oBbox.getMaxX();
			
			// Write the bounding box
			sBbox = String.format("%f,%f,%f,%f,%f,%f,%f,%f,%f,%f", dMinY, dMinX, dMinY, dMaxX, dMaxY, dMaxX, dMaxY, dMinX, dMinY, dMinX);
			
		} 
		catch (IOException e) {
			LauncherMain.s_oLogger.error("WasdiProductReader.getProductBoundingBox: Exception " + e.getMessage());
		}
		finally {
			if (oShpFileDataStore != null) {
				oShpFileDataStore.dispose();
			}
		}
		
		return sBbox;
	}
	

	
	
	/**
	 * Get the metadata View Model of a Product
	 * @return
	 * @throws IOException
	 */
    public MetadataViewModel getProductMetadataViewModel() throws IOException
    {
        return  GetMetadataViewModel(m_oProduct.getMetadataRoot(), new MetadataViewModel("Metadata"));
    }
    
	/**
	 * Get the metadata View Model of a Product
	 * @param oFile
	 * @return
	 * @throws IOException
	 */
    public MetadataViewModel getProductMetadataViewModel(File oFile) throws IOException
    {
        Product exportProduct = readSnapProduct(oFile, null);

        if (exportProduct == null) return null;

        return  GetMetadataViewModel(exportProduct.getMetadataRoot(), new MetadataViewModel("Metadata"));
    }

    /**
     * Recursive Metadata Explorer Function
     * @param oElement
     * @param oSourceViewModel
     * @return
     */
    private MetadataViewModel GetMetadataViewModel(MetadataElement oElement, MetadataViewModel oSourceViewModel) {

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
            
            oSourceViewModel.getElements().add(GetMetadataViewModel(oMetadataElement, oElementViewModel));
        }

        return  oSourceViewModel;
    }

    /**
     * Fills Band View Models
     * @param oProductViewModel
     * @param oProduct
     */
    private void FillBandsViewModel(ProductViewModel oProductViewModel, Product oProduct)
    {
        if (oProductViewModel == null) {
            LauncherMain.s_oLogger.debug("WasdiProductReader.FillBandsViewModel: ViewModel null return");
            return;
        }

        if (oProduct == null) {
            LauncherMain.s_oLogger.debug("WasdiProductReader.FillBandsViewModel: Product null");
            return;
        }

        if (oProductViewModel.getBandsGroups() == null) oProductViewModel.setBandsGroups(new NodeGroupViewModel("Bands"));

        LauncherMain.s_oLogger.debug("WasdiProductReader.FillBandsViewModel: add bands");
        
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
