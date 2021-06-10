function sSessionId = wGetSessionId(Wasdi)
% Get the session ID
% Syntax
% sSessionId = wGetSessionId(Wasdi)
% 
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
%  
%
% :Returns:
%   sSessionId: the current session

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not exist')
    return
   end
   
   sSessionId = char(Wasdi.getSessionId());
end
