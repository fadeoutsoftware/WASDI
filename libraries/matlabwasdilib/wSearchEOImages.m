function [asProductNames, asProductLinks, asProductFootprints]=wSearchEOImages(Wasdi, sPlatform, sDateFrom, sDateTo, dULLat, dULLon, dLRLat, dLRLon, sProductType, iOrbitNumber, sSensorOperationalMode, sCloudCoverage)
%Search EO Images. Returns 3 parallel arrays: one with the names, one with the links and one with the footprints of the found products.
%The links and footprints can be used as input to the wImportProduct function, that imports the product in the active workspace
%Syntax
%[asProductNames, asProductLinks, asProductFootprints]=wSearchEOImages(Wasdi, sPlatform, sDateFrom, sDateTo, dULLat, dULLon, dLRLat, dLRLon, sProductType, iOrbitNumber, sSensorOperationalMode, sCloudCoverage);
%
%:param Wasdi: Wasdi object created after the wasdilib call
%:param sPlatform: Satellite Platform. Accepts "S1","S2"
%:param sDateFrom: Starting date in format "YYYY-MM-DD"
%:param sDateTo: End date in format "YYYY-MM-DD"
%:param dULLat: Upper Left Lat Coordinate. Can be null.
%:param dULLon: Upper Left Lon Coordinate. Can be null.
%:param dLRLat: Lower Right Lat Coordinate. Can be null.
%:param dLRLon: Lower Right Lon Coordinate. Can be null.
%:param sProductType: Product Type. If Platform = "S1" -> Accepts "SLC","GRD", "OCN". If Platform = "S2" -> Accepts "S2MSI1C","S2MSI2Ap","S2MSI2A". Can be null.
%:param iOrbitNumber: Sentinel Orbit Number. Can be null.
%:param sSensorOperationalMode: Sensor Operational Mode. ONLY for S1. Accepts -> "SM", "IW", "EW", "WV". Can be null. Ignored for Platform "S2"
%:param sCloudCoverage: sCloudCoverage Cloud Coverage. Sample syntax: [0 TO 9.4]
%
%
%:Returns:
%   :asProductNames: array of strings that are the names of the found products
%   :asProductLinks: array of strings that are the links to download the products
%   :asProductFootprints: array of strings that are the footprints of found products in WKT


  if exist("Wasdi") < 1
    disp('Wasdi variable does not existst')
    return
   end

   aoProducts = Wasdi.searchEOImages(sPlatform, sDateFrom, sDateTo,
      double(dULLat), double(dULLon), double(dLRLat), double(dLRLon),
       sProductType, int16(iOrbitNumber), sSensorOperationalMode, sCloudCoverage);

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