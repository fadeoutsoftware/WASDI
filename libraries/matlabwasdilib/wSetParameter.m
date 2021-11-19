function sParameter = wSetParameter(Wasdi, sKey, sValue)
%Set the value of a parameter
%Syntax
%sParameter = wSetParameter(Wasdi, sKey, sValue)
%
%:param Wasdi: Wasdi object created after the wasdilib call
%:param sKey: The KEY of the parameter to add or update
%:param sValue: The the value of the parameter
%
%:Returns:
%  :sParameter: The value (same as sValue in input)

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   Wasdi.addParam(sKey, sValue);
   sParameter = sValue;
   
   disp(['Parameter ' sParameter]);

end