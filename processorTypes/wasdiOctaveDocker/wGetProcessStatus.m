function sStatus =wGetProcessStatus(Wasdi, sProcessId)
% Get the status of a Process
% Syntax
% sStatus =wGetProcessStatus(Wasdi, sProcessId);
%
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
%    sProcessId: Id of the process to query
%
%
% OUTPUT
%   sStatus: Process Status as a String: CREATED,  RUNNING,  STOPPED,  DONE,  ERROR

  if exist("Wasdi") < 1
    disp('Wasdi variable does not existst')
    return
   end

   sStatus = char(Wasdi.getProcessStatus(sProcessId));

   disp(['Process Status ' sStatus]);

end