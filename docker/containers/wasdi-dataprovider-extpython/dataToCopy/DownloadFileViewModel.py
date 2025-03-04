class DownloadFileViewModel:

    def __init__(self, **kwargs):
        self.url = str()
        self.downloadDirectory = str()
        self.downloadFileName = str()
        self.maxRetry = 1
        self.payload = {}

        self.__dict__.update(kwargs)