function wSetUser(Wasdi, sUser)
%Set the user
%Syntax
%wSetUser(Wasdi, sUser)
%
%:param Wasdi: Wasdi object created after the wasdilib call
%:param sUser: the user

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   Wasdi.setUser(sUser)
   
end