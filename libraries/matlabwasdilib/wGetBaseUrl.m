function sBaseUrl = wGetBaseUrl(Wasdi)
%Gets the base URL
%Syntax
%sBasePath = wGetBaseUrl(Wasdi)
%
%:param Wasdi: Wasdi object created after the wasdilib call
%
%:Returns:
%  :sBaseUrl: the base URL for WASDI

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not exist')
    return
   end
   
   sBaseUrl = char(Wasdi.getBaseUrl());
   
end