function sOutputStatus =wUpdateProcessStatus(Wasdi, sProcessId, sStatus, iPerc)
% Updates the status of a Process
% Syntax
% sStatus =wUpdateProcessStatus(Wasdi, sProcessId, sStatus, iPerc);
% 
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
%    sProcessId: Id of the process to update 
%    sStatus: updated status. Must be CREATED,  RUNNING,  STOPPED,  DONE,  ERROR
%    iPerc: progress percentage of the process
%
%:Returns:
%   sOutputStatus: Process Status Updated as a String: CREATED,  RUNNING,  STOPPED,  DONE,  ERROR

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   sOutputStatus = char(Wasdi.updateProcessStatus(sProcessId,sStatus,iPerc));
   
   disp(['Process Status ' sStatus]);

end