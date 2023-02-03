function sStatus=wAsynchMosaic(Wasdi, asInputFileNames, sOutputFile, sNoDataValue, sInputIgnoreValue)
% Mosaic input images in the output file
% Syntax
% sProcessId=wAsynchMosaic(Wasdi, asInputFileNames, sOutputFile, sNoDataValue, sInputIgnoreValue)
% 
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
%	 asInputFileNames: Array of input file names
%	 sOutputFile: Name of the output file
%	 sNoDataValue: value to use as no data in the output file
%	 sInputIgnoreValue: value used as no data in the input file
%
% OUTPUT
%   sProcessId: Id of the mosaic process on WASDI. Can be used as input to the wWaitProcess method or wGetProcessStatus methods to check the execution.
%

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   sProcessId = Wasdi.asynchMosaic(asInputFileNames, sOutputFile, sNoDataValue, sInputIgnoreValue);
   
   disp(['Process id: ' sProcessId]);
   
end
