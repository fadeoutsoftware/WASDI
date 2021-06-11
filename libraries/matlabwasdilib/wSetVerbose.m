function wSetVerbose(Wasdi, bVerbose)
%Set verbose flag
%Syntax
%wSetVerbose(Wasdi, bVerbose)
%
%:param Wasdi: Wasdi object created after the wasdilib call
%:param bVerbose: true/false

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   Wasdi.setVerbose(bVerbose)
   
end