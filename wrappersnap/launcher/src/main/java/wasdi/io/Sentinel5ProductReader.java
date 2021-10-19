package wasdi.io;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import wasdi.LauncherMain;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.BandViewModel;
import wasdi.shared.viewmodels.GeorefProductViewModel;
import wasdi.shared.viewmodels.MetadataViewModel;
import wasdi.shared.viewmodels.NodeGroupViewModel;
import wasdi.shared.viewmodels.ProductViewModel;

public class Sentinel5ProductReader extends WasdiProductReader {

	public Sentinel5ProductReader(File oProductFile) {
		super(oProductFile);
	}

	@Override
	public ProductViewModel getProductViewModel() {

		if (m_oProductFile == null) return null;

    	// Create the return value
    	GeorefProductViewModel oRetViewModel = null;

		try {
			NetcdfFile oFile = NetcdfFiles.open(m_oProductFile.getAbsolutePath());

	    	// Create the Product View Model
	    	oRetViewModel = new GeorefProductViewModel();

        	// Set name values
        	oRetViewModel.setFileName(m_oProductFile.getName());
        	oRetViewModel.setName(Utils.getFileNameWithoutLastExtension(m_oProductFile.getName()));
        	oRetViewModel.setProductFriendlyName(oRetViewModel.getName());

	    	NodeGroupViewModel oNodeGroupViewModel = new NodeGroupViewModel();
        	oNodeGroupViewModel.setNodeName("Bands");

    		Set<String> excludedVariableSet = new HashSet<>(Arrays.asList("scanline", "ground_pixel", "time", "corner",
    				"latitude", "longitude", "delta_time", "time_utc", "qa_value", "layer"));

    		Group rootGroup = oFile.getRootGroup();
    		List<Group> rootGroupGroups = rootGroup.getGroups();

        	ArrayList<BandViewModel> oBands = new ArrayList<BandViewModel>();

    		for (Group g : rootGroupGroups) {
    			if (g.getShortName().equalsIgnoreCase("PRODUCT")) {

    				List<Variable> variableList = g.getVariables();
    				for (Variable v : variableList) {
    					String variableShortName = v.getShortName();
    					if (!excludedVariableSet.contains(variableShortName)) {
    						int iWidth = 0;
    						int iHeight = 0;
    						// [time = 1;, scanline = 358;, ground_pixel = 450;]
    						int[] shapeArray = v.getShape();

    						if (shapeArray == null) {
    							continue;
    						}

							if (shapeArray.length < 3) {
								continue;
							}

							iWidth = shapeArray[1];
							iHeight = shapeArray[2];
    			        	
    			        	// Create the single band representing the shape
    			        	BandViewModel oBandViewModel = new BandViewModel();
    			        	oBandViewModel.setPublished(false);
    			        	oBandViewModel.setGeoserverBoundingBox("");
    			        	oBandViewModel.setHeight(iHeight);
    			        	oBandViewModel.setWidth(iWidth);
    			        	oBandViewModel.setPublished(false);
    			        	oBandViewModel.setName(variableShortName);

    			        	oBands.add(oBandViewModel);
    					}
    				}
    			}	
    		}

        	oNodeGroupViewModel.setBands(oBands);
	    	oRetViewModel.setBandsGroups(oNodeGroupViewModel);
		} catch (Exception e) {
    		LauncherMain.s_oLogger.debug("Sentinel5ProductReader.getProductViewModel: exception reading the shape file: " + e.toString());
		}
		
    	return oRetViewModel;
	}

	@Override
	public String getProductBoundingBox() {

		if (m_oProductFile == null) return null;
		
		return "getProductBoundingBox";
	}

	@Override
	public MetadataViewModel getProductMetadataViewModel() {
		
		return new MetadataViewModel("Metadata");
	}

}
