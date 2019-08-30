function sStatus =wImportProduct(Wasdi, sFileUrl, sBoundingBox)
% Import a product in the active WASDI workspace.
%
% Syntax
% sStatus =wImportProduct(Wasdi,sFileUrl, sBoundingBox);
% 
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
%    sFileUrl: File URL as returned by wSearchEOImages
%	 sBoundingBox: Footprint as returned by wSearchEOImages
%  
%
% OUTPUT
%   sStatus: Process Status as a String: CREATED,  RUNNING,  STOPPED,  DONE,  ERROR

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   sStatus = char(Wasdi.importProduct(sFileUrl, sBoundingBox));
   
   disp(['Import Product Status ' sStatus]);

end