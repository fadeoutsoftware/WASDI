function sProcessPayload = wGetProcessorPayloadAsJSON(Wasdi, sProcessObjId)
%Gets the payload of given processor as a JSON string
%Syntax
%sProcessPayload = wGetProcessorPayloadAsJSON(Wasdi, sProcessObjId)
%
%:param Wasdi: Wasdi object created after the wasdilib call
%:param sProcessObjId: process ID for which the payload must be retrieve
%
%:Returns:
%  sProcessPayload: a JSON formatted string containing the payload

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not exist')
    return
   end
   
   sProcessPayload = char(Wasdi.getProcessorPayloadAsJSON(sProcessObjId));
   
end
