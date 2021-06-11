function sBasePath = wGetBasePath(Wasdi)
%Gets the base path
%Syntax
%sBasePath = wGetBasePath(Wasdi)
%
%:param Wasdi: Wasdi object created after the wasdilib call
%
%:Returns:
%    :sBasePath: the base path in use

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not exist')
    return
   end
   
   sBasePath = char(Wasdi.getBasePath());
   
end