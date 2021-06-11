function sProcessId=wAsynchCopyFileToSftp(Wasdi, sFileName, sRelativePath)
% Copy file to SFTP folder, asynchronous version
% Syntax
% wAsynchCopyFileToSftp(Wasdi, sFileName, sRelativePath)
% 
% INPUT
%  Wasdi: Wasdi object created after the wasdilib call
%	 sFileName: a string containing the file name
%	 sRelativePath: a string containinng the relative path
%

  if exist("Wasdi") < 1 
    disp('Wasdi variable does not exist')
    return
   end
   
   Wasdi.asynchCopyFileToSftp(sFileName, sRelativePath)
end
