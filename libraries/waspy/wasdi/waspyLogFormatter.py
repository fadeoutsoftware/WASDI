"""
Custom Log Format inspired by this answer:
https://stackoverflow.com/questions/1343227/can-pythons-logging-format-be-modified-depending-on-the-message-log-level
"""

import logging


# Custom formatter
class WaspyLogFormatter(logging.Formatter):

    __sErrorEmphasizer = ' ------------------------------------------'
    __sDefaultFormat = '%(asctime)s [%(levelname)s] %(name)s - %(message)s'
    __asFormats = {
        logging.DEBUG: '%(asctime)s [%(levelname)s] %(name)s - %(message)s',
        logging.INFO: '%(asctime)s [%(levelname)s] %(name)s - %(message)s',
        logging.WARNING: '%(asctime)s [%(levelname)s] %(name)s - %(message)s',
        logging.ERROR: f'%(asctime)s [%(levelname)s] %(name)s - %(message)s{__sErrorEmphasizer} [%(levelname)s]',
        logging.CRITICAL: f'%(asctime)s [%(levelname)s] %(name)s - %(message)s{__sErrorEmphasizer} [%(levelname)s]'
    }

    def __init__(self, sErrorEmphasizer=None, asFormats=None):
        super().__init__(fmt="%(levelno)d: %(msg)s", datefmt=None, style='%')
        if sErrorEmphasizer is not None:
            WaspyLogFormatter.__sErrorEmphasizer = sErrorEmphasizer
        if asFormats is not None:
            for k, v in asFormats.items():
                if 'errorEmphasizer' in v:
                    asFormats[k] = v.replace('errorEmphasizer', WaspyLogFormatter.__sErrorEmphasizer)
            WaspyLogFormatter.__asFormats.update(asFormats)

    def format(self, record):

        # Replace the original format with one customized by logging level
        try:
            self._style._fmt = WaspyLogFormatter.__asFormats[record.levelno]
        except KeyError:
            self._style._fmt = WaspyLogFormatter.__sDefaultFormat

        # Call the original formatter class to do the grunt work

        sResult = logging.Formatter.format(self, record)

        return sResult
