function wLog(Wasdi, sLogRow)
% Add a row to the application logs. Locally, just print on the console if VERBOSE. On the server, it logs on the WASDI interface.
% Syntax
% wLog(Wasdi, sLogRow)
% 
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
%	 sLogRow Text to log
%
% :Returns:
%   None
%

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   Wasdi.wasdiLog(sLogRow);
   
end
