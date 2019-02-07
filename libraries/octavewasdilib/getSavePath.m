function sSavePath =getSavePath(Wasdi);
% Get the full local path where to save a product in the active workspace
% Syntax
% sSavePath =getSavePath(Wasdi);
% 
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
%  
%
% OUTPUT
%   sSavePath: the local path to use to save the file, including last /

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   sSavePath = Wasdi.getSavePath();
   
   disp(['SavePath: ' sSavePath]);

endfunction
