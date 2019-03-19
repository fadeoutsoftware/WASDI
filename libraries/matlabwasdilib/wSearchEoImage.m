function [asFileNames, asFileLinks]=wSearchEoImage(Wasdi, sPlatform, sDateFrom, sDateTo, dULLat, dULLon, dLRLat, dLRLon,  sProductType, iOrbitNumber, sSensorOperationalMode, sCloudCoverage)
% Search for EO images
% Syntax
% [asFileNames, asFileLinks]=wSearchEoImage(Wasdi, sPlatform, sDateFrom, sDateTo, dULLat, dULLon, dLRLat, dLRLon,  sProductType, iOrbitNumber, sSensorOperationalMode, sCloudCoverage)
% 
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
%	 sPlatform: Satellite Platform. Accepts "S1","S2"
%	 sDateFrom: Starting date in format "YYYY-MM-DD"
%	 sDateTo: End date in format "YYYY-MM-DD"
%	 dULLat: Upper Left Lat Coordinate. Can be null.
%	 dULLon: Upper Left Lon Coordinate. Can be null.
%	 dLRLat: Lower Right Lat Coordinate. Can be null.
%	 dLRLon: Lower Right Lon Coordinate. Can be null.
%	 sProductType: Product Type. If Platform = "S1" -> Accepts "SLC","GRD", "OCN". If Platform = "S2" -> Accepts "S2MSI1C","S2MSI2Ap","S2MSI2A". Can be null.
%	 iOrbitNumber: Sentinel Orbit Number. Can be null.
%	 sSensorOperationalMode: Sensor Operational Mode. ONLY for S1. Accepts -> "SM", "IW", "EW", "WV". Can be null. Ignored for Platform "S1"
%	 sCloudCoverage: Cloud Coverage. Sample syntax: [0-8.4]
%
% OUTPUT
%   asFileNames: array of strings that are the names of the found files
%   asFileLinks: array of strings that are the direct link of the files
%

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   aoFoundProducts = Wasdi.searchEOImages(sPlatform, sDateFrom, sDateTo, dULLat, dULLon, dLRLat, dLRLon,  sProductType, iOrbitNumber, sSensorOperationalMode, sCloudCoverage);
   
   iTot = aoFoundProducts.size();
   
   disp(['Number of Found Files: ' sprintf("%d",iTot)]);

  % For each Product
  for iProduct = 0:iTot-1
    oProduct = aoFoundProducts.get(iProduct);
    asFileNames{iProduct+1} = oProduct.get("title") + '.zip';
    asFileLinks{iProduct+1} = oProduct.get("link");
  end

   
end
