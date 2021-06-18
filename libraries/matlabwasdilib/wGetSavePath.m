function sSavePath =wGetSavePath(Wasdi)
%Get the full local path where to save a product in the active workspace
%Syntax
%sSavePath =wGetSavePath(Wasdi);
%
%:param Wasdi: Wasdi object created after the wasdilib call
%
%
%:Returns:
%  :sSavePath: the local path to use to save the file, including last /

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   sSavePath = char(Wasdi.getSavePath());
   
   disp(['SavePath: ' sSavePath]);

end
