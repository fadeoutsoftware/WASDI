function sStatus=wAsynchMosaic(Wasdi, asInputFileNames, sOutputFile, sNoDataValue, sInputIgnoreValue)
%Mosaic input images in the output file
%Syntax
%sProcessId=wAsynchMosaic(Wasdi, asInputFileNames, sOutputFile, sNoDataValue, sInputIgnoreValue)
%
%:param Wasdi: Wasdi object created after the wasdilib call
%:param asInputFileNames: Array of input file names
%:param sOutputFile: Name of the output file
%:param sNoDataValue: value to use as no data in the output file
%:param sInputIgnoreValue: value used as no data in the input file
%
%:Returns:
%  :sProcessId: Id of the mosaic process on WASDI. Can be used as input to the wWaitProcess method or wGetProcessStatus methods to check the execution.


  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   sProcessId = Wasdi.asynchMosaic(asInputFileNames, sOutputFile, sNoDataValue, sInputIgnoreValue);
   
   disp(['Process id: ' sProcessId]);
   
end
