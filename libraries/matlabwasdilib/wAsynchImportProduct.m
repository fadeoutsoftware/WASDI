function sProcessObjId=wAsynchImportProduct(Wasdi, sProductLink, sName, sBoundingBox='', sProvider='AUTO')
%Import an EO Image in WASDI. This is the asynchronous version
%Syntax
%sStatus=wImportProduct(Wasdi, sProductLink, sName)
%
%:param Wasdi: Wasdi object created after the wasdilib call
%:param sProductLink: Product Direct Link as returned by wSearchEOImage
%:param sName: Product Name as returned by wSearchEOImage
%:param sBoundingBox: product bounding box, optional
%:param sProvider: data provider, optional
%
%:Returns:
%  :param sProcessObjId: Identifier of the import process


  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   sProcessObjId = Wasdi.asynchImportProduct(sProductLink, sName, sBoundingBox, sProvider);
   
   
end
