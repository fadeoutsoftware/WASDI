function sOutputStatus =wUpdateStatus(Wasdi, sStatus)
% Updates the status of the actual Process
% Syntax
% sStatus =wUpdateStatus(Wasdi, sStatus);
% 
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
%    sStatus: updated status. Must be CREATED,  RUNNING,  STOPPED,  DONE,  ERROR
%
% OUTPUT
%   sOutputStatus: Process Status Updated as a String: CREATED,  RUNNING,  STOPPED,  DONE,  ERROR

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   sOutputStatus = char(Wasdi.updateStatus(sStatus));
   
   %disp(['Process Status ', sStatus]);

end