function wasdiHello(Wasdi)
% Hello world in WASDI. Useful for testing the setup
% Syntax:
% wasdiHello(Wasdi)
% 
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
%
  if exist("Wasdi") < 1 
    disp('Wasdi variable does not exist')
    return
   end
   
  Wasdi.hello()
   
end
