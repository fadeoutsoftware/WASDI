function sPassword = wGetPassword(Wasdi)
%Gets the password
%Syntax
%sPassword = wGetPassword(Wasdi)
%
%:param Wasdi: Wasdi object created after the wasdilib call
%
%:Returns:
%  :sPassword: WASDI user's password

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not exist')
    return
   end
   
   sPassword = char(Wasdi.getPassword());
   
end