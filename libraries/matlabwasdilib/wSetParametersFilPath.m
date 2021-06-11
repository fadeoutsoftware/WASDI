function sVoid = wSetParametersFilePath(Wasdi, sFilePath)
% Sets the parameters file path
% Syntax
% sVoid = wSetParametersFilPath(Wasdi)
% 
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
%
% OUTPUT
%   sParametersFilePath : the path to the parameters file

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not exist')
    return
   end
   
   sBasePath = char(Wasdi.setParametersFilePath(sFilePath));
   
end