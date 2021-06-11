function bVerbose = wGetVerbose(Wasdi)
%
%Gets verbosity flag
%Syntax
%bVerbose = wGetVerbose(Wasdi)
%
%:param Wasdi: Wasdi object created after the wasdilib call
%
%:Returns:
%  :bVerbose: verbosity flag

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not exist')
    return
   end
   
   bVerbose = Wasdi.getVerbose();
   
end