function sStatus =waitProcess(Wasdi, sProcessId)
% Wait for the end of a process
% Syntax
% sStatus =waitProcess(Wasdi, sProcessId);
% 
% :param Wasdi: Wasdi object created after the wasdilib call
% :param sProcessId: Id of the process to wait 
%  
%  :returns: ``sStatus`` exit status of the process: CREATED,  RUNNING,  STOPPED,  DONE,  ERROR
;
  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   sStatus = Wasdi.waitProcess(sProcessId);
   
   disp(['Output Status ' sStatus]);

endfunction