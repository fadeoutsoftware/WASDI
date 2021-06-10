function sProcessorPath = wGetProcessorPath(Wasdi)
%Gets the parameters file path
%Syntax
%sProcessorPath = wGetProcessorPath(Wasdi)
%
%:param Wasdi: Wasdi object created after the wasdilib call
%
%:Returns:
%  sProcessorPath : the path to current processor

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not exist')
    return
   end
   
   sProcessorPath = char(Wasdi.getProcessorPath());
   
end