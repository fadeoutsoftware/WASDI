function wSetUser(Wasdi, sUser)
% Set the user
% Syntax
% wSetUser(Wasdi, sUser)
% 
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
%    sUser: the user

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   Wasdi.setUser(sUser)
   
end