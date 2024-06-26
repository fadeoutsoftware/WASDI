function sPath = wGetPath(Wasdi, sFileName)
%Gets the path of given product
%Syntax
%sPath = wGetPath(Wasdi, sFileName)
%
%:param Wasdi: Wasdi object created after the wasdilib call
%
%:Returns:
%  :sPath: wasdi local path for given product

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not exist')
    return
   end
   
   sPath = char(Wasdi.getPath(sFileName));
   
end