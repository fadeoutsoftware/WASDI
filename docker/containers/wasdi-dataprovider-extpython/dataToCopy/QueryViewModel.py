class QueryViewModel:

    def __init__(self, **kwargs):
        self.offset = -1
        self.limit = -1
        self.north = float()
        self.south = float()
        self.east = float()
        self.west = float()
        self.startFromDate = str()
        self.startToDate = str()
        self.endFromDate = str()
        self.endToDate = str()
        self.platformName = str()
        self.productType = str()
        self.productLevel = str()
        self.relativeOrbit = -1
        self.absoluteOrbit = -1
        self.cloudCoverageFrom = float()
        self.cloudCoverageTo = float()
        self.sensorMode = str()
        self.productName = str()
        self.timeliness = str()
        self.polarisation = str()
        self.platformSerialIdentifier = str()
        self.instrument = str()
        self.filters = {}

        self.__dict__.update(kwargs)
