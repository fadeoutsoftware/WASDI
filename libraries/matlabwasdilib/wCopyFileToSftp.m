function sProcessId=wCopyFileToSftp(Wasdi, sFileName, sRelativePath)
%Copy file to SFTP folder, synchronous version
%Syntax
%wCopyFileToSftp(Wasdi, sFileName, sRelativePath)
%
%:param Wasdi: Wasdi object created after the wasdilib call
%:param sFileName: a string containing the file name
%:param sRelativePath: a string containinng the relative path
%:returns:
%  :sProcessId: process workspace id. It can be used as input to the wWaitProcess method or wGetProcessStatus methods to check the execution.

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not exist')
    return
   end
   
   Wasdi.copyFileToSftp(sFileName, sRelativePath)
end
