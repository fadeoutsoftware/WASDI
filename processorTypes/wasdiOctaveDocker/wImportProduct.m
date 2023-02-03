function sStatus=wImportProduct(Wasdi, sProductLink)
% Import an EO Image in WASDI
% Syntax
% sStatus=wImportProduct(Wasdi, sProductLink)
% 
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
%	 sProductLink: Product Direct Link as returned by wSearchEOImage
%
% OUTPUT
%   sStatus: End status of the import process
%

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   sStatus = Wasdi.import(sProductLink);
   
	disp(['Process Status ' sStatus]);
   
end
