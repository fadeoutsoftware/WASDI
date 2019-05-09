#from snappy import ProductIO
import wasdi 
import eDriftDeleteImages

wasdi.setUser('paolo')
wasdi.setPassword('password')
wasdi.openWorkspaceById("7d05edb2-bb7a-48fb-8c65-1939e1663afc")
wasdi.addParameter('workspace', 'MYANMAR')
wasdi.addParameter('enddate', '2017-07-15') 

wasdi.init()

eDriftDeleteImages.run(wasdi.getParametersDict(), '')
