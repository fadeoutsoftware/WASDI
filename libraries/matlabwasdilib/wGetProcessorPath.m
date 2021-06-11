function sProcessorPath = wGetProcessorPath(Wasdi)
% Gets the parameters file path
% Syntax
% sProcessorPath = wGetProcessorPath(Wasdi)
% 
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
%
% OUTPUT
%   sProcessorPath : the path to current processor

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not exist')
    return
   end
   
   sProcessorPath = char(Wasdi.getProcessorPath());
   
end