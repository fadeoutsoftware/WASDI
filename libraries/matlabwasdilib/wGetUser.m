function sUser = wGetUser(Wasdi)
%
% Gets the user
% Syntax
% sUser = wGetUser(Wasdi)
% 
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
%
%:Returns:
%   sUser: the username on wasdi

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not exist')
    return
   end
   
   sUser = Wasdi.getUser();
   
end