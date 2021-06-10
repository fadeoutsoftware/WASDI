function sProcessId=wExecuteProcessor(Wasdi, sProcessorName, asParams)
%Execute a WASDI processor asynchronously
%Syntax
%sStatus=wExecuteProcessor(Wasdi, sProcessorName, asParams)
%
%:param Wasdi: Wasdi object created after the wasdilib call
%:param sProcessorName: Processor Name
%:param asParams: Processor parameters, as a key/value dictionary
%
%:Returns:
%   :sProcessId: process workspace id. It can be used as input to the wWaitProcess method or wGetProcessStatus methods to check the execution.


  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   sJsonParams = wUrlEncode(savejson('',asParams,'Compact',1))
   
   sProcessId = char(Wasdi.executeProcessor(sProcessorName, sJsonParams));
   
   disp(['sProcessId: ', sProcessId ]);
   
end
