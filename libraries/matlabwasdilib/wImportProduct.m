function sProcessObjId=wImportProduct(Wasdi, sProductLink, sProductName, sBoundingBox='', sProvider='AUTO')
%Import an EO Image in WASDI
%Syntax
%sStatus=wImportProduct(Wasdi, sProductLink, sProductName)
%
%:param Wasdi: Wasdi object created after the wasdilib call
%:param sProductLink: Product Direct Link as returned by wSearchEOImage
%:param sProductName: Product Name as returned by wSearchEOImage
%:param sBoundingBox: product bounding box, optional
%:param sProvider: data provider, optional
%
%:Returns:
%  :sProcessObjId: Identifier of the import process
%

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not exist')
    return
   end
   
   sProcessObjId = Wasdi.importProduct(sProductLink, sProductName, sBoundingBox, sProvider);
   
   
end
