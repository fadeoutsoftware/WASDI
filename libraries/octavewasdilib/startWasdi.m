
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
javaaddpath([sPath "./jwasdilib-0.1.1.jar"])

% Log
disp('WASDI MATLAB LIB INITIALIZED (v.0.01.01)')

% Create the Wasdi Object
Wasdi =  javaObject ("wasdi.jwasdilib.WasdiLib") 

Wasdi.init()