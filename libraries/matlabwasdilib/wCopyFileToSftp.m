function sProcessId=wCopyFileToSftp(Wasdi, sFileName, sRelativePath)
% Copy file to SFTP folder, synchronous version
% Syntax
% wCopyFileToSftp(Wasdi, sFileName, sRelativePath)
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
   
   Wasdi.copyFileToSftp(sFileName, sRelativePath)
end
