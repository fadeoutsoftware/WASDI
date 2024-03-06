function sOutputStatus =updateProcessStatus(Wasdi, sProcessId, sStatus, iPerc)
% Updates the status of a Process
% Syntax
% sStatus =updateProcessStatus(Wasdi, sProcessId, sStatus, iPerc);
% 
% :param Wasdi: Wasdi object created after the wasdilib call
% :param sProcessId: Id of the process to update 
% :param sStatus: updated status. Must be CREATED,  RUNNING,  STOPPED,  DONE,  ERROR
% :param iPerc: progress percentage of the process
%
% :returns: ``sOutputStatus`` Process Status Updated as a String: CREATED,  RUNNING,  STOPPED,  DONE,  ERROR
;
  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   sOutputStatus = Wasdi.updateProcessStatus(sProcessId,sStatus,iPerc);
   
   disp(['Process Status ' sStatus]);

endfunction