function sOutputStatus =wUpdateProgress(Wasdi, iPerc)
% Updates the progress of the processor
% Syntax
% sStatus =wUpdateProgress(Wasdi, iPerc);
% 
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
%    iPerc: progress percentage of the own process
%
% :Returns:
%   sOutputStatus: Process Status as a String: CREATED,  RUNNING,  STOPPED,  DONE,  ERROR

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   sOutputStatus = char(Wasdi.updateProgressPerc(iPerc));
   
   ;disp(['Process Status ' sStatus]);

end