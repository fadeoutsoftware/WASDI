function wSetIsOnServer(Wasdi, bIsOnServer)
%Set is on server flag
%Syntax
%wSetVerbose(Wasdi, bIsOnServer)
%
%:param Wasdi: Wasdi object created after the wasdilib call
%:param bIsOnServer: true/false

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   Wasdi.setIsOnServer(bIsOnServer);
   
end