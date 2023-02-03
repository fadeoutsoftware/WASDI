function sStatus=wExecuteProcessor(Wasdi, sProcessorName, asParams)
% Execute a WASDI processor
% Syntax
% sStatus=wExecuteProcessor(Wasdi, sProcessorName, asParams)
% 
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
%	 sProcessorName: Processor Name
%	 asParams: Processor parameters, as a key/value dictionary
%
% OUTPUT
%   sStatus: Status of the process
%

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not existst')
    return
   end
   
   %sStatus = Wasdi.subset(sInputFile, sOutputFile, dLatN, dLonW, dLatS, dLonE);
   
   disp(['TODO: ' ]);
   
end
