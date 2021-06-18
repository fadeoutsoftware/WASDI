function wasdiLog(Wasdi, sLine)
%Logs a line
%Syntax
%wasdiLog(Wasdi, sLine)
%
%:param Wasdi: Wasdi object created after the wasdilib call
%:param sLine: the string to be logged


  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   char(Wasdi.wasdiLog(sLine));


end