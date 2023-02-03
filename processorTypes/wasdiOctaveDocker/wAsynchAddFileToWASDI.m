function sStatus =wAsynchAddFileToWASDI(Wasdi, sFileName)
% Ingest a new file in the Active WASDI Workspace WITHOUT waiting for the result
% The method takes a file saved in the workspace root (see getSaveFilePath) not already added to the WS
% If the file is not present in the WASDI cloud workpsace, it will be automatically uploaded if the config AUTOUPLOAD flag is true (default)
% Syntax
% sStatus =wAsynchAddFileToWASDI(Wasdi, sFileName);
% 
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
%    sFileName: Name of the file to add
%  
%
% OUTPUT
%   sProcessId: Process Id of the WASDI Ingest operation on the server. Can be used as input to the wWaitProcess method or wGetProcessStatus methods to check the execution.

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   sStatus = char(Wasdi.asynchAddFileToWASDI(sFileName));
   
   disp(['Ingest Process Status ' sStatus]);

end