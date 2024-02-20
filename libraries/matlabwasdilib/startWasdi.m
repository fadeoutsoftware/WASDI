function Wasdi = startWasdi(config_path)
%Initialize the Wasdi object 
%
%:param config_path: The path to be used to import the configuration. 
%
%:Returns:
%  :Wasdi: The object to be used to invoke all the following call to Wasdi services

%find out what the library path is:
sFileName = 'startWasdi.m';
sDir = which(sFileName);
sDir = sDir(1:end-length(sFileName));

%add the jsonlab path too
addpath(fullfile(sDir,'jsonlab'));


%try to set JAVA_HOME automatically
sJavaHome = getenv('JAVA_HOME');
if exist(sJavaHome)<1
  disp('WASDI: Warning: could not find JAVA_HOME, please define it');
else
  sLower=lower(sJavaHome);
  %are we on windows?
  %C:\Program Files\Java\jdk1.8.0_211\jre\bin\server
  if(sLower(2:9)==':\progra')
  disp('WASDI: Windows detected, checking environment variables...')
    %then the code is probably running on windows
    sSubstr = substr(sJavaHome,-6,-1);
    if strcmp(sSubstr, 'erve')<1
      %turn this
      %C:\Program Files\Java\jdk1.8.0_211\
      %into this
      %C:\Program Files\Java\jdk1.8.0_211\jre\bin\server
      sOldJavaHome=sJavaHome;
	  if length(strfind(sJavaHome,"8.0"))>0
		sJavaHome= [sJavaHome, '/jre/bin/server'];
	  else
		sJavaHome= [sJavaHome, '/bin/server'];
	  end
      setenv('JAVA_HOME',sJavaHome);
      disp(['WASDI: Changed JAVA_HOME from ', sOldJavaHome, ' to ', sJavaHome]);
      disp('WASDI: (This is temporary and will not affect the operating system)')
    end
  end
end

%Check if there is the wasdipath variable
iPathExists = exist("wasdipath");

%Initialize path as folder containing this file
sPath = fileparts(mfilename('fullpath'));

%Check if we need to use the user supplied one
if iPathExists > 0
   sPath = wasdipath;
end

%add internal path
%addpath(genpath(sPath))
%add WASDI Library Jar
wasdilib = 'jwasdilib-0.7.4.jar';
if isdeployed
    javaaddpath(['./', wasdilib])
else
    javaaddpath(fullfile(sPath, wasdilib))
end


%Create the Wasdi Object
Wasdi = javaObject ("wasdi.jwasdilib.WasdiLib");

if nargin == 0
    Wasdi.init();
else
    Wasdi.init(config_path);
end

%Log
disp('WASDI: WASDI MATLAB LIB INITIALIZED (v.0.07.01). Welcome to Space')
