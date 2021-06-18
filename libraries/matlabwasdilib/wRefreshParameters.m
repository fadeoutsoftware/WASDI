function wRefreshParameters(Wasdi)
%Read again the parameters from the configured file
%Syntax
%sParameter = wRefreshParameters(Wasdi, sKey)
%
%:param Wasdi: Wasdi object created after the wasdilib call

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   Wasdi.refreshParameters();
   
   disp(['Parameters read from file']);

end