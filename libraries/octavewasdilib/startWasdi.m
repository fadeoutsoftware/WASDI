%Initialize the Wasdi object 
%
%:param config_path: The path to be used to import the configuration. 
%
%:returns: ``Wasdi`` The object to be used to invoke all the following call to Wasdi services  

% Check if there is the wasdipath varialbe
iPathExists = exist("wasdipath");

% Initialize path as actual folder
sPath = "./";

% Check if we need to use the user supplied one
if iPathExists > 0
   sPath = wasdipath
end

% add internal path 
addpath(genpath(sPath))
% add WASDI Library Jar
javaaddpath([sPath "./jwasdilib.jar"])

% Log
disp('WASDI MATLAB LIB INITIALIZED (v.0.7.0)')

% Create the Wasdi Object
Wasdi =  javaObject ("wasdi.jwasdilib.WasdiLib") 

Wasdi.init()