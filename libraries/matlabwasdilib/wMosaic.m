function sStatus=wMosaic(Wasdi, asInputFileNames, sOutputFile, sNoDataValue, sInputIgnoreValue)
%Mosaic input images in the output file
%Syntax
%sStatus=wMosaic(Wasdi, asInputFileNames, sOutputFile, sNoDataValue, sInputIgnoreValue)
%
%:param Wasdi: Wasdi object created after the wasdilib call
%	 asInputFileNames: Array of input file names
%	 sOutputFile: Name of the output file
%	 sNoDataValue: value to use as no data in the output file
%	 sInputIgnoreValue: value used as no data in the input file
%
%:Returns:
%  sStatus: end status of the mosaic operation
%

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   sStatus = Wasdi.mosaic(asInputFileNames, sOutputFile, sNoDataValue, sInputIgnoreValue);
   
   disp(['Mosaic Status: ' sStatus]);
   
end
