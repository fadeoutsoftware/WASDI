function [asProductNames, asProductLinks, asProductFootprints]=wSearchEOImages(Wasdi, sPlatform, sDateFrom, sDateTo, dULLat, dULLon, dLRLat, dLRLon, sProductType, iOrbitNumber, sSensorOperationalMode, sCloudCoverage)
%Search EO Images. Returns 3 parallel arrays: one with the names, one with the links and one with the footprints of the found products.
%The links and footprints can be used as input to the wImportProduct function, that imports the product in the active workspace.
%For invokation of the function, a null value is considered an empty string and can be passed with 2 double quotes "".
%In case the "" value is passed the filters on that particular parameter are relaxed, not considering the related constraints.
%Syntax
%[asProductNames, asProductLinks, asProductFootprints] = wSearchEOImages(Wasdi, sPlatform, sDateFrom, sDateTo, dULLat, dULLon, dLRLat, dLRLon, sProductType, iOrbitNumber, sSensorOperationalMode, sCloudCoverage);
%
%:param Wasdi: Wasdi object created after the wasdilib call
%:param sPlatform: Satellite Platform. Accepts "S1","S2"
%:param sDateFrom: Starting date in format "YYYY-MM-DD"
%:param sDateTo: End date in format "YYYY-MM-DD"
%:param dULLat: Upper Left Lat Coordinate. Nullable by passing an empty string "".
%:param dULLon: Upper Left Lon Coordinate. Nullable by passing an empty string "".
%:param dLRLat: Lower Right Lat Coordinate. Nullable by passing an empty string "".
%:param dLRLon: Lower Right Lon Coordinate. Nullable by passing an empty string "".
%:param sProductType: Product Type. If Platform = "S1" -> Accepts "SLC","GRD", "OCN". If Platform = "S2" -> Accepts "S2MSI1C","S2MSI2Ap","S2MSI2A". Nullable by passing an empty string "".
%:param iOrbitNumber: Sentinel Orbit Number. Nullable by passing an empty string "".
%:param sSensorOperationalMode: Sensor Operational Mode. ONLY for S1. Accepts -> "SM", "IW", "EW", "WV". Nullable by passing an empty string "". Ignored for Platform "S2"
%:param sCloudCoverage: sCloudCoverage Cloud Coverage. Sample syntax: [0 TO 9.4]. Nullable by passing an empty string ""
%
%
%:Returns:
%  :asProductNames: array of strings that are the names of the found products
%  :asProductLinks: array of strings that are the links to download the products
%  :asProductFootprints: array of strings that are the footprints of found products in WKT


  if exist("Wasdi") < 1
    disp('Wasdi variable does not existst')
    return
   end

   aoProducts = Wasdi.searchEOImages(sPlatform, sDateFrom, sDateTo,
      double(dULLat), double(dULLon), double(dLRLat), double(dLRLon),
       sProductType, int16(iOrbitNumber), sSensorOperationalMode, sCloudCoverage);

   iTotFound = aoProducts.size();

   disp(['Number of Products found: ' sprintf("%d",iTotFound)]);

  %For each product
  for iProduct = 0:iTotFound-1
    oProduct = aoProducts.get(iProduct);
    asProductNames{iProduct+1} = oProduct.get("title");
    asProductLinks{iProduct+1} = oProduct.get("link");
	asProductFootprints{iProduct+1} = oProduct.get("footprint");
  end


end