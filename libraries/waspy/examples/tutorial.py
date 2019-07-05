import myProcessor
import wasdi

bInitResult = wasdi.init('config.json')
if bInitResult:
    myProcessor.run(wasdi.getParametersDict(), '')
