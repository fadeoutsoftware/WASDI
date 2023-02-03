function sFullPath = wGetFullProductPath(Wasdi, sFileName)
% Get the full local path of a product. If it is not present on the local PC 
% and DownloadActive flag is true the product will be downloaded
% Syntax
% sFullPath =wGetFullProductPath(Wasdi, sFileName);
% 
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
%    sFileName: Name of the file 
%  
%
% OUTPUT
%   sFullPath: full local path 

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   sJavaFullPath = Wasdi.getFullProductPath(sFileName);
   sFullPath = char(sJavaFullPath);
   
   
   disp(['Full Path: ' sFullPath]);

end
