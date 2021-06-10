function sStatus =wSetProcessPayload(Wasdi, sProcessId, sData)
% Writes a Payload in a process
% Syntax
% sStatus =wSetProcessPayload(Wasdi, sProcessId, sData);
% 
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
%    sProcessId: Id of the process to update 
%    sData: Data to write as payloar
%
%:Returns:
%   sStatus: Process Status as a String: CREATED,  RUNNING,  STOPPED,  DONE,  ERROR

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   sStatus = char(Wasdi.setProcessPayload(sProcessId,sData));
   
   disp(['Process Status ' sStatus]);

end