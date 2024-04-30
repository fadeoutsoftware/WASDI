import boto3
import botocore
import os

from WasdiLogging import WasdiLogging




'''
This class is used to
support the object storage S3 protocol.
'''
class WasdiS3():
    def __init__(self, **kwargs):
        self.oLogging = oLogging = WasdiLogging(
            sLoggerName = kwargs['sLoggerName']
        )
        self.sAccessKey   = kwargs['sAccessKey']
        self.sBucketName  = kwargs['sBucketName']
        self.sEndpointUrl = kwargs['sEndpointUrl']
        self.sRegionName  = kwargs['sRegionName']
        self.sSecretKey   = kwargs['sSecretKey']

        self.oClient = boto3.client(
            service_name          = 's3',
            aws_access_key_id     = self.sAccessKey,
            aws_secret_access_key = self.sSecretKey,
            endpoint_url          = self.sEndpointUrl,
            region_name           = self.sRegionName
        )


    '''
    Delete an element from a bucket.

    :param sKey:
        Name (with full path) of the object
        to delete

    :return:
        True if the deletion worked successfully
        False else
    '''
    def deleteObject(self, sKey):
        try:
            self.oClient.delete_object(
                Bucket=self.sBucketName,
                Key=sKey
            )
        except:
            return False

        return True


    '''
    Get the content of elements present
    in the bucket (no matter if it is a
    file or a directory).

    :param bOnlyName:
        (optional) If True, we keep only the name
        of the file. Else, we keep all metadata.
    :param sDirectory:
        (optional) If setted, return only the content
        of the directory specified.

    :return:
        if bOnlyName is True: a list which contain
        all objects present in the bucket (directories +
        files)
        if bOnlyName is False: a list of dict which
        contain all metadata for all objects present
        in the bucket (directories + files)
    '''
    def getContent(self, **kwargs):
        aoConfiguration = {
            'Bucket': self.sBucketName
        }

        if 'sDirectory' in kwargs and type(kwargs['sDirectory']) == str and kwargs['sDirectory'].strip() != '':
            aoConfiguration['Prefix'] = kwargs['sDirectory'].strip()

        bOnlyName = True

        if 'bOnlyName' in kwargs and type(kwargs['bOnlyName']) == bool:
            bOnlyName = kwargs['bOnlyName']

        if bOnlyName is True:
            aElements = []

            for aoCurrentFile in self.oClient.list_objects(**aoConfiguration)['Contents']:
                aElements.append(aoCurrentFile['Key'])

            return aElements

        return self.oClient.list_objects(**aoConfiguration)['Contents']


    '''
    Get the list of files present in a bucket.

    :return:
        a list which contain the list of files
        present in the bucket
    '''
    def getFiles(self, **kwargs):
        kwargs['bOnlyName'] = True

        aFiles = []

        for sCurrentElement in self.getContent(**kwargs):
            if not sCurrentElement.endswith('/'):
                aFiles.append(sCurrentElement)

        return aFiles


    '''
    Upload a file to an S3 bucket

    :param sFileToUpload: File to upload
    :param (optional) kwargs['sBucketName]': Bucket to upload to
    :param sObjectName: S3 object name. If not specified then the basename of sFileToUpload is used
    :return: True if file was uploaded, else False
    '''
    def uploadFile(self, sFileToUpload, **kwargs):
        # If S3 sObjectName was not specified, use the basename of sFileToUpload
        if 'sObjectName' in kwargs:
            sObjectName = kwargs['sObjectName']
        else:
            sObjectName = os.path.basename(sFileToUpload)

        self.oLogging.info(
            'Upload \'%(sFileToUpload)s\' in the bucket \'%(sBucketName)s\'...'
            %{
                'sBucketName': self.sBucketName,
                'sFileToUpload': sFileToUpload
            }
        )

        # Upload the file
        try:
            self.oClient.upload_file(sFileToUpload, self.sBucketName, sObjectName)
            self.oLogging.info('OK')
        except Exception as oException:
            self.oLogging.error(oException)
            return False

        return True
