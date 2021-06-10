function sProcessObjId=wImportProduct(Wasdi, sProductLink, sBoundingBox='', sProvider='LSA')
% Import an EO Image in WASDI
% Syntax
% sStatus=wImportProduct(Wasdi, sProductLink)
% 
% INPUT
%   Wasdi: Wasdi object created after the wasdilib call
%   sProductLink: Product Direct Link as returned by wSearchEOImage
%   sBoundingBox: product bounding box, optional
%   sProvider: data provider, optional
%
% :Returns:
%   sProcessObjId: Identifier of the import process
%

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not exist')
    return
   end
   
   sProcessObjId = Wasdi.importProduct(sProductLink, sBoundingBox, sProvider);
   
   
end
