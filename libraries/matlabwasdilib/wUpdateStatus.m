function sStatus = wUpdateStatus(wasdi, sStatus, iPerc=[])
%
%updates the status and, optionally, the progress percent
%syntax:
%sStatus = wUpdateStatus(Wasdi, sStatus, iPerc=[])
%
%:param Wasdi: Wasdi object created after the wasdilib call
%:param sStatus: the status to be set
%:param iPerc: optional, the progress percent

  if exist("wasdi") < 1 
    disp('Wasdi variable does not exist')
    return
   end
   
   if isempty(iPerc)
     wasdi.updateStatus(sStatus)
   else
       wasdi.updateStatus(sStatus,iPerc)
   end
end