function sStatus =wAddFileToWASDI(Wasdi, sFileName)
%Ingest a new file in the Active WASDI Workspace waiting for the result
%The method takes a file saved in the workspace root (see getSaveFilePath) not already added to the WS
%o work be sure that the file is on the server
%Syntax
%sStatus =wAddFileToWASDI(Wasdi, sFileName);
% 
%
%:param Wasdi: Wasdi object created after the wasdilib call
%:param sFileName: Name of the file to add
%  
%
%:Returns:
%   :sStatus: Status of the Ingest Process as a String: CREATED,  RUNNING,  STOPPED,  DONE,  ERROR

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   sStatus = char(Wasdi.addFileToWASDI(sFileName));
   
   disp(['Ingest Process Status ' sStatus]);

end