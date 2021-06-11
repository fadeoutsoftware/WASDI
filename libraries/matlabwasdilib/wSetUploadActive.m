function wSetUploadActive(Wasdi, iActive)
% Set the Upload active flag
% Syntax
% wSetUploadActive(Wasdi, iActive)
% 
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
%    iActive: true/false
%


  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
  Wasdi.setUploadActive(iActive);

end