import logging
import logging.handlers
import os
import sys




'''
This class permits to create a singleton object.
We use singleton for the logging abstraction object.
'''
class Singleton(type):
    _instances = {}

    def __call__(self, *args, **kwargs):
        if self not in self._instances:
            self._instances[self] = super(Singleton, self).__call__(*args, **kwargs)

        return self._instances[self]




'''
This class is an abstraction for logging.
It is used to manage all log messages.
'''
class WasdiLogging(metaclass = Singleton):

    def __init__(self, **kwargs):
        # Define the default format of the message
        self.sFormatterDefault = '[%(process)s] %(asctime)s | %(levelname)-8s | %(message)s'

        if 'sLogFormat' in kwargs:
            self.sFormatterDefault = kwargs['sLogFormat']

        # Create the logging instance
        self.oLogging = logging.getLogger(kwargs['sLoggerName'])

        # Set the level of logging
        # Here we must have the lowest level
        # And we can set a higher level in each Logging Handler
        self.oLogging.setLevel(1)

        # Parameters to add a default prefix
        # and/or a default suffix.
        # If we provide another prefix/suffix
        # in a message function, the temporary
        # prefix/suffix will replace these
        # default prefix/suffix
        self.sPrefixDefault = kwargs['sPrefixDefault'] if 'sPrefixDefault' in kwargs else ''
        self.sSuffixDefault = kwargs['sSuffixDefault'] if 'sSuffixDefault' in kwargs else ''

        # Configure log on screen
        if 'aoLogOnScreen' in kwargs:
            if not self.enableLogOnScreen(kwargs['aoLogOnScreen']):
                print(
                    '[%(fileName)s] Unable to enable log on screen'
                    %{
                        'fileName': os.path.basename(__file__)
                    },
                    file = sys.stderr
                )

        # Configure log with syslog
        if 'aoLogWithSyslog' in kwargs:
            if not self.enableLogWithSyslog(kwargs['aoLogWithSyslog']):
                print(
                    '[%(fileName)s] Unable to enable log with syslog'
                    %{
                        'fileName': os.path.basename(__file__)
                    },
                    file = sys.stderr
                )


    '''
    Display a debug message
    The message is only displayed if the level
    is adapted.
    '''
    def debug(self, message, **kwargs):
        self.oLogging.debug(
            self.getMessage(message, kwargs),
            exc_info = self.isExceptionDisplayed(kwargs)
        )


    '''
    Enable log on screen

    Default value:
        - enable: False
    '''
    def enableLogOnScreen(self, aoConfiguration):
        if 'bEnable' not in aoConfiguration:
            aoConfiguration['bEnable'] = False

        if aoConfiguration['bEnable']:
            self.oStreamHandlerTty = logging.StreamHandler(sys.stdout)

            if 'bVerbose' in aoConfiguration and aoConfiguration['bVerbose']:
                self.oStreamHandlerTty.setLevel(logging.DEBUG)
            else:
                self.oStreamHandlerTty.setLevel(logging.INFO)

            self.oStreamHandlerTty.setFormatter(
                logging.Formatter(self.sFormatterDefault)
            )

            self.oLogging.addHandler(self.oStreamHandlerTty)
            return True

        return False


    '''
    Enable log with syslog

    Default value:
        - enable: False
        - server address: localhost
        - server port: 514
        - syslog facility: local0
    '''
    def enableLogWithSyslog(self, aoConfiguration):
        if 'bEnable' not in aoConfiguration:
            aoConfiguration['bEnable'] = False

        if 'sSyslogServerAddress' not in aoConfiguration:
            aoConfiguration['sSyslogServerAddress'] = 'localhost'

        if 'iSyslogServerPort' not in aoConfiguration:
            aoConfiguration['iSyslogServerPort'] = 514

        if 'sSyslogFacility' not in aoConfiguration:
            aoConfiguration['sSyslogFacility'] = 'local0'

        if aoConfiguration['bEnable']:
            self.oStreamHandlerSyslog = logging.handlers.SysLogHandler(
                (
                    aoConfiguration['sSyslogServerAddress'],
                    aoConfiguration['iSyslogServerPort'],
                ),
                facility = aoConfiguration['sSyslogFacility']
            )

            self.oStreamHandlerSyslog.setLevel(logging.DEBUG)
            self.oStreamHandlerSyslog.setFormatter(
                logging.Formatter(fmt=aoConfiguration['sSyslogProgramName'] + self.sFormatterDefault)
            )

            self.oLogging.addHandler(self.oStreamHandlerSyslog)
            return True

        return False


    '''
    Verbose mode:
        - we display DEBUG messages on screen
    '''
    def enableVerbose(self):
        self.oStreamHandlerTty.setLevel(logging.DEBUG)


    '''
    Display an error message
    '''
    def error(self, message, **kwargs):
        self.oLogging.error(
            self.getMessage(message, kwargs),
            exc_info = self.isExceptionDisplayed(kwargs)
        )


    '''
    This method is used to update the log format
    to another value.
    With this function, we can add:
        - a prefix
        - a suffix
        - both
    '''
    def getMessage(self, message, aoFormatterData):
        if 'prefix' in aoFormatterData:
            message = aoFormatterData['prefix'].strip() + ' ' + message
        elif type(self.sPrefixDefault) == str and self.sPrefixDefault.strip() != '':
            message = self.sPrefixDefault.strip() + ' ' + message

        if 'suffix' in aoFormatterData:
            message = message.strip() + ' ' + aoFormatterData['suffix']
        elif type(self.sSuffixDefault) == str and self.sSuffixDefault.strip() != '':
            message = message.strip() + ' ' + self.sSuffixDefault

        return message


    '''
    This method returns:
      - the value of aoConfiguration['bDisplayException'] if:
        - aoConfiguration['bDisplayException'] exists
        - aoConfiguration['bDisplayException'] is a boolean
      - False in all other cases

    Input:
      - aoConfiguration: a aoionnary which should contain
        a boolean key 'bDisplayException'
    '''
    def isExceptionDisplayed(self, aoConfiguration):
        if 'bDisplayException' in aoConfiguration:
            if type(aoConfiguration['bDisplayException']) == bool:
                return aoConfiguration['bDisplayException']

        return False


    '''
    Display an info message
    '''
    def info(self, message, **kwargs):
        self.oLogging.info(
            self.getMessage(message, kwargs)
        )


    '''
    Used to change the level
    '''
    def setLevel(self, level):
        self.oLogging.setLevel(level)
