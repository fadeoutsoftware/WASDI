function sMyProcessorId =wGetMyProcId(Wasdi)
% Get the process Id of the actual process
% Syntax
% sMyProcessorId =wGetMyProcId(Wasdi);
% 
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
%  
%
% OUTPUT
%   sMyProcessorId: Id of the running process

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   sMyProcessorId = char(Wasdi.getMyProcId());

end
