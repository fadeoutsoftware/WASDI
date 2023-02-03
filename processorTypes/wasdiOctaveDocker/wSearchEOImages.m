function [asProductNames, asProductLinks, asProductFootprints]=wSearchEOImages(Wasdi, sPlatform, sDateFrom, sDateTo, dULLat, dULLon, dLRLat, dLRLon, sProductType, iOrbitNumber, sSensorOperationalMode, sCloudCoverage)
% Search EO Images. Returns 3 parallel arrays: one with the names, one with the links and one with the footprints of the found products.
% The links and footprints can be used as input to the wImportProduct function, that imports the product in the active workspace
% Syntax
% [asWorkspaceNames, asWorkspaceIds]=wSearchEOImages(Wasdi, sPlatform, sDateFrom, sDateTo, dULLat, dULLon, dLRLat, dLRLon, sProductType, iOrbitNumber, sSensorOperationalMode, sCloudCoverage);
%
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
% 	 sPlatform Satellite Platform. Accepts "S1","S2"
%	 sDateFrom Starting date in format "YYYY-MM-DD"
% 	 sDateTo End date in format "YYYY-MM-DD"
%	 dULLat Upper Left Lat Coordinate. Can be null.
%	 dULLon Upper Left Lon Coordinate. Can be null.
%	 dLRLat Lower Right Lat Coordinate. Can be null.
%	 dLRLon Lower Right Lon Coordinate. Can be null.
%	 sProductType Product Type. If Platform = "S1" -> Accepts "SLC","GRD", "OCN". If Platform = "S2" -> Accepts "S2MSI1C","S2MSI2Ap","S2MSI2A". Can be null.
%	 iOrbitNumber Sentinel Orbit Number. Can be null.
%	 sSensorOperationalMode Sensor Operational Mode. ONLY for S1. Accepts -> "SM", "IW", "EW", "WV". Can be null. Ignored for Platform "S2"
%	 sCloudCoverage sCloudCoverage Cloud Coverage. Sample syntax: [0 TO 9.4]
%
%
% OUTPUT
%   asProductNames: array of strings that are the names of the found products
%   asProductLinks: array of strings that are the links to download the products
%   asProductFootprints: array of strings that are the footprints of found products in WKT


  if exist("Wasdi") < 1
    disp('Wasdi variable does not existst')
    return
   end

   aoProducts = Wasdi.searchEOImages(sPlatform, sDateFrom, sDateTo, ...
       java.lang.Double(dULLat), java.lang.Double(dULLon), ...
       java.lang.Double(dLRLat), java.lang.Double(dLRLon), ...
       sProductType, iOrbitNumber, sSensorOperationalMode, sCloudCoverage);

   iTotFound = aoProducts.size();

   disp(['Number of Products found: ' sprintf("%d",iTotFound)]);

  % For each product
  for iProduct = 0:iTotFound-1
    oProduct = aoProducts.get(iProduct);
    asProductNames{iProduct+1} = oProduct.get("title");
    asProductLinks{iProduct+1} = oProduct.get("link");
	asProductFootprints{iProduct+1} = oProduct.get("footprint");
  end


end