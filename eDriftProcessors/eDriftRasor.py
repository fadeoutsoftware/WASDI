import wasdi
import gdal
import numpy
import json
import gdalconst

def run(parameters, processId): 
    
    wasdi.wasdiLog(' PROC ID = ' + processId)
    ws_name = parameters.get('workspace', "Workshop")
    scenario_file_name = parameters.get('scenario_file', "-.tif") 
    pop_file_name = parameters.get('pop_file', "-.tif")
    
    wasdi.updateProcessStatus(processId, "RUNNING", 10)
    
    wasdi.wasdiLog('RasorProcessor: workspace = ' + ws_name)
    wasdi.wasdiLog('RasorProcessor: scenario file = ' + scenario_file_name)
    wasdi.wasdiLog('RasorProcessor: pop file = ' + pop_file_name)
    
    wasdi.openWorkspace(ws_name)
    
    geotiff_scenario_path = wasdi.getFullProductPath(scenario_file_name);    
    geotiff_pop_path = wasdi.getFullProductPath(pop_file_name);
#      geotiff_scenario_path = '/home/doy/tmp/wasdi/rasor_docker/Mappa_CSKS4_SCS_B_HI_11_HH_RD_SF_20161126171433.tif'
#     geotiff_pop_path = '/home/doy/tmp/dds/lauro/gpw_v4_population_count_2015.geotiff'
    
    wasdi.updateProcessStatus(processId, "RUNNING", 20)
    
    #regrid the population raster in the scenario grid    
    scenario_ds = gdal.Open(geotiff_scenario_path)
    scenario_proj = scenario_ds.GetProjection()
    scenario_gt = scenario_ds.GetGeoTransform()
    
    wasdi.wasdiLog('regrid done')

    wasdi.updateProcessStatus(processId, "RUNNING", 30)
    
    pop_ds = gdal.Open(geotiff_pop_path)
    pop_proj = pop_ds.GetProjection()
    pop_gt = pop_ds.GetGeoTransform()
    
    wasdi.wasdiLog('open pop done')

    wasdi.updateProcessStatus(processId, "RUNNING", 40)
    
    sOutPath=wasdi.getSavePath()

    driver_mem = gdal.GetDriverByName('GTiff')
    mem_ds = driver_mem.Create(sOutPath+'/pippo.tif', scenario_ds.RasterXSize, scenario_ds.RasterYSize, 1, gdal.GDT_Float32)    
    mem_ds.SetGeoTransform(scenario_gt)
    mem_ds.SetProjection(scenario_proj)    
    
    wasdi.wasdiLog('output file created')

    wasdi.updateProcessStatus(processId, "RUNNING", 50)
    
    gdal.ReprojectImage(pop_ds, mem_ds, pop_proj, scenario_proj, gdalconst.GRA_Average)
    
    wasdi.wasdiLog('reprojecton done')
    
    wasdi.updateProcessStatus(processId, "RUNNING", 60)
    
    #read the scenario regridded data
    pop_data = mem_ds.ReadAsArray()
    numpy.nan_to_num(pop_data, copy=False)
    mem_ds = None
    
    wasdi.wasdiLog('scenario read done')
    
    wasdi.updateProcessStatus(processId, "RUNNING", 70)
    
    #read the population data
    scenario_data = scenario_ds.ReadAsArray()
    scenario_ds = None
    
    wasdi.wasdiLog('pop read done')
        
    wasdi.updateProcessStatus(processId, "RUNNING", 80)
    
    #computing the total polutation affected
    affected_pop = numpy.sum(pop_data[scenario_data==1])
    
    wasdi.wasdiLog('sum done')
    
    scale_factor = pop_gt[1]/scenario_gt[1] * pop_gt[5]/scenario_gt[5]
    wasdi.wasdiLog('affected scale factor ' + str(scale_factor))
    affected_pop /= scale_factor
    
    wasdi.wasdiLog('******affected pop done ' + str(affected_pop))
    
    scenario_data = None
    pop_data = None
    
    wasdi.wasdiLog('affected data set to null done')
    
    wasdi.updateProcessStatus(processId, "RUNNING", 90)
    
    wasdi.wasdiLog('updated to 90')
    
    payload = {'pop': float(affected_pop)}
    
    wasdi.wasdiLog('payload set')
    
    wasdi.setProcessPayload(processId, payload)
    
    wasdi.wasdiLog('set process payload done ' + json.dumps(payload))
    
    #with open('/data/wasdi/wasdi_rasor_docker.log', 'a') as f:
    #    f.write('------------------\n')
    #    f.write(json.dumps(payload) + '\n')
    #    f.write('------------------\n')
    
    #wasdi.wasdiLog('RasorProcessor: Affected Population: %s'%affected_pop)    
    
    wasdi.updateProcessStatus(processId, "DONE", 100)
    
    wasdi.wasdiLog('RasorProcessor: Done Bye bye')


def WasdiHelp():
    sHelp = "Rasor APP: this processor takes in input a Raster flood Map and a Raster Population Layer and computes the number of people affected by the flood.\n"
    sHelp = sHelp + "Both products must be in .tif format\n"
    sHelp = sHelp + "The Parameters are\n"
    sHelp = sHelp + "\tworkspace: name of the Wasdi workspace\n"
    sHelp = sHelp + "\tscenario_file: name of the flood map file\n"
    sHelp = sHelp + "\tpop_file: name of the population layer file\n"
    sHelp = sHelp + "\n"
    sHelp = sHelp + 'SAMPLE: {"workspace":"test WS", "scenario_file":"floodMap.tif", "pop_file":"pop.tif"}\n'
    
    return sHelp