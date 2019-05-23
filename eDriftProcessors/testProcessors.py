#from snappy import ProductIO
import wasdi 
import eDriftDeleteImages
import eDriftCheckImages
import eDriftRasor
import eDriftGetTileCoordinate
import eDriftArchiveGenerator

if (wasdi.init("C:\Codice\Progetti\WASDI\Codice\eDriftProcessors\config.properties")):
    #eDriftCheckImages.run(wasdi.getParametersDict(), '35888d57-21c1-4fa2-94bc-daf2beda37d4')
    #eDriftRasor.run(wasdi.getParametersDict(), '')
    #eDriftGetTileCoordinate.run(wasdi.getParametersDict(), '')
    eDriftArchiveGenerator.run(wasdi.getParametersDict(), '')
