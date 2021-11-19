function wSetUploadActive(Wasdi, iActive)
%Set the Upload active flag
%Syntax
%wSetUploadActive(Wasdi, iActive)
%
%:param Wasdi: Wasdi object created after the wasdilib call
%:param iActive: true/false
%


  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
  Wasdi.setUploadActive(iActive);

end