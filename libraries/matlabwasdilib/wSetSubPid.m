function sStatus=wSetSubPid(Wasdi, sProcessId, iSubPid)
% Set the sub pid
% Syntax
% sStatus=wSetSubPid(Wasdi, sProcessId, iSubPid)
% 
% INPUT
%    Wasdi: Wasdi object created after the wasdilib call
%    sProcessId: the process ID
%    iSubPid: the sub pid

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not exist')
    return
   end
   
   Wasdi.setSubPid(sProcessId,iSubPid);
   
end