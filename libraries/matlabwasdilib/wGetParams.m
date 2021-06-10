function asParams = wGetParams(Wasdi)
%
% Gets processor parameters
% Syntax
% asParams = wGetParams(Wasdi)
% 
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
%
% :Returns:
%   asParams: a map containing the parameters

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not exist')
    return
   end
   
   asParams = Wasdi.getParamsAsJsonString();
   
end