function sParametersFilePath = wGetParametersFilePath(Wasdi)
% Gets the parameters file path
% Syntax
% sParametersFilePath = wGetParametersFilPath(Wasdi)
% 
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
%
% :Returns:
%   sParametersFilePath : the path to the parameters file

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not exist')
    return
   end
   
   sParametersFilePath = char(Wasdi.getParametersFilePath());
   
end