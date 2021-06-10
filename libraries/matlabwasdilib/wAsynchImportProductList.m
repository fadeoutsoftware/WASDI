function asStatuses=wAsynchImportProductList(Wasdi, asProductLinks)
%Import an EO Image in WASDI. This is the asynchronous version
%Syntax
%sProcessObjId=wAsynchImportProductList(Wasdi, asProductLinks)
%
%:param Wasdi: Wasdi object created after the wasdilib call
%:param asProductLinks: collection of Product Direct Link as returned by wSearchEOImage
%
%:Returns:
%  :param asStatuses: list of statuses of the import processes


  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   iTotFound = length(asProductLinks);
   asUrls = javaObject('java.util.ArrayList',iTotFound);
   
   for iProduct = 1:iTotFound
    asUrls.add(asProductLinks{iProduct})
   end
   
   asProcessObjId = Wasdi.asynchImportProductList(asUrls);
   
end
