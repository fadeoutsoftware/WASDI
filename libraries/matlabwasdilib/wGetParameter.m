function sParameter = wGetParameter(Wasdi, sKey)
% Get the value of a parameter identified by sKey in the parameters file
% Syntax
% sParameter = wGetParameter(Wasdi, sKey)
% 
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
%    sKey: The KEY of the parameter in the paramteres file
%
% :Returns:
%   sParameter: The value of the parameter. If it does not exists the function returns ""

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   sParameter = char(Wasdi.getParam(sKey));
   
   disp(['Parameter ' sParameter]);

end