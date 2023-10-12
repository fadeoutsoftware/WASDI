!PATH = EXPAND_PATH('<IDL_DEFAULT>:+/data/wasdi/processors/edriftlistflood_archive/mpfit')
.r /data/wasdi/processors/edriftlistflood_archive/idlwasdilib.pro
STARTWASDI, '/data/wasdi/processors/edriftlistflood_archive/config.properties'
.r /data/wasdi/processors/edriftlistflood_archive/edriftlistflood_archive.pro
.compile mpcurvefit
.r /data/wasdi/processors/edriftlistflood_archive/wasdi_wrapper.pro
CALLWASDI
exit
