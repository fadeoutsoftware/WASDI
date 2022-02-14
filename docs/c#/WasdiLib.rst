C# WasdiLib
==============

.. c#:solution:: C#
   :noindex:

.. java:type:: public class Wasdi

Fields
------
m_sUser
^^^^^^^^^

.. java:field:: private string m_sUser
   :outertype: WasdiLib

Constructors
------------
Wasdi
^^^^^^^^

.. java:constructor:: public Wasdi()
   :outertype: Wasdi

   Self constructor. If there is a config file initializes the class members

Methods
-------
AddFileToWASDI
^^^^^^^^^^^^^^

.. java:method:: public String AddFileToWASDI(System.String sFileName)
   :outertype: System.String

   Adds a generated file to current open workspace in a synchronous way.

   :param sFileName: the name of the file to be added to the open workspace
   :return: the process Id or empty string in case of any issues

AddParam
^^^^^^^^

.. java:method:: public void AddParam(System.String sKey, System.String sParam)
   :outertype: 

   Add a parameter to the parameters dictionary.

   :param sKey: the new key
   :param sParam: the new value

