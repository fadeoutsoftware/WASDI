function sStatus =wWaitProcess(Wasdi, sProcessId)
% Wait for the end of a process
% Syntax
% sStatus =wWaitProcess(Wasdi, sProcessId);
% 
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
%    sProcessId: Id of the process to wait 
%  
%
% :Returns:
%   sStatus: exit status of the process: CREATED,  RUNNING,  STOPPED,  DONE,  ERROR

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   sStatus = char(Wasdi.waitProcess(sProcessId));
   
   disp(['Output Status ' sStatus]);

end