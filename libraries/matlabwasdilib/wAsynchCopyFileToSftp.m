function sProcessId=wAsynchCopyFileToSftp(Wasdi, sFileName, sRelativePath)
%Copy file to SFTP folder, asynchronous version
%Syntax
%wAsynchCopyFileToSftp(Wasdi, sFileName, sRelativePath)
%
%:param Wasdi: Wasdi object created after the wasdilib call
%:param sFileName: a string containing the file name
%:param sRelativePath: a string containinng the relative path
%

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not exist')
    return
   end
   
   Wasdi.asynchCopyFileToSftp(sFileName, sRelativePath)
end
