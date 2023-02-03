function Wasdi = startWasdi(config_path)

% Check if there is the wasdipath variable
iPathExists = exist("wasdipath");

% Initialize path as folder containing this file
sPath = fileparts(mfilename('fullpath'));

% Check if we need to use the user supplied one
if iPathExists > 0
   sPath = wasdipath;
end

% add internal path
addpath(genpath(sPath))
% add WASDI Library Jar
wasdilib = 'jwasdilib-0.7.0.jar';
if isdeployed
    javaaddpath(['./', wasdilib])
else
    javaaddpath(fullfile(sPath, wasdilib))
end

% Log
disp('WASDI MATLAB LIB INITIALIZED (v.0.07.00)')

% Create the Wasdi Object
Wasdi = javaObject ("wasdi.jwasdilib.WasdiLib");

if nargin == 0
    Wasdi.init();
else
    Wasdi.init(config_path);
end