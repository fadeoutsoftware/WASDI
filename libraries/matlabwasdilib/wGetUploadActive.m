function bUploadActive = wGetUploadActive(Wasdi)
%
% Gets whether Upload is active or not
% Syntax
% bUploadActive = wGetUploadActive(Wasdi)
% 
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
%
% OUTPUT
%   bUploadActive: true if Upload is active, false otherwise

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not exist')
    return
   end
   
   bUploadActive = Wasdi.getUploadActive();
   
end