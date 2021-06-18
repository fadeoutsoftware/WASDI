FROM ubuntu:18.04

MAINTAINER Fadeout Software <info@fadeout.it>

# Install Java and common tools
RUN apt-get update && apt-get install -y \
	openjdk-8-jre \
	software-properties-common \
	locate \
	nano

#Add GDAL Next GIS Repo
RUN add-apt-repository ppa:nextgis/ppa

# Install Python 3.x
RUN apt-get update && apt-get install -y \
	python3

RUN apt-get update && apt-get install -y python-dev

RUN mkdir /home/wasdi && chmod o+wrx /home/wasdi

# Go at Home and copy files
WORKDIR "/home/wasdi"

# Install PIP
RUN apt-get -y install python3-pip

RUN pip3 install pip -U

# Install Flask
RUN pip3 install flask

# Install gunicorn
RUN pip3 install gunicorn

# Install Requests
RUN pip3 install requests

# Install GDAL
RUN apt-get install -y gdal-bin

#Copy envi installation pack
COPY ./ /home/wasdi/

# Set working dir
WORKDIR /home/wasdi

# Unpack the tar
RUN tar -xf envi552-linux.tar

# Overwrite install script
COPY ./install.sh /home/wasdi/

# Make it executable
RUN chmod +x /home/wasdi/install.sh

# Run the installation
RUN /home/wasdi/install.sh s

# Set the license server url
COPY ./o_licenseserverurl.txt /usr/local/harris/license/o_licenseserverurl.txt

# clean envi installation files 1
CMD rm -rf ./install
CMD rm -rf ./silent

# Copy WASDI Server Files
#COPY ./ /home/wasdi/

# clean envi installation files 2
CMD rm -rf  /home/wasdi/envi552-linux.tar
CMD rm -rf  /home/wasdi/o_licenseserverurl.txt
CMD rm -rf  /home/wasdi/install.sh

# Make the folder for idl and let others write
RUN mkdir /.idl && chmod o+wrx /.idl

# Install GDAL
#RUN apt-get install -y gdal-bin

# Install Wasdi
RUN pip3 install wasdi

# Install User packages
RUN apt-get install -y `cat /home/wasdi/packages.txt | tr "\n" " "`; exit 0

# Install User Python Libs
#RUN pip install `cat /home/wasdi/pip.txt | tr "\n" " "`; exit 0

# Set working dir
WORKDIR /home/wasdi

# Start the server
RUN chmod -R o+wrx /home/wasdi/*
RUN chmod 777 ./runServer.sh
CMD ./runServer.sh
