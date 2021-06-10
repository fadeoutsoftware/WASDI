FROM ubuntu:21.04

MAINTAINER Fadeout Software <info@fadeout.it>

# Install Java and common tools
RUN apt-get update && apt-get install -y \
	openjdk-8-jre \
	software-properties-common \
	locate \
	nano

# Install Python 3.x
RUN apt-get update && apt-get install -y \
	python3

RUN apt-get update && apt-get install -y python-dev

RUN mkdir /home/wasdi && chmod o+wrx /home/wasdi

# Go at Home and copy files
WORKDIR "/home/wasdi"

#Install Pip
RUN apt-get -y install python3-pip

RUN pip3 install pip -U

# Install Flask
RUN pip3 install flask

# Install gunicorn
RUN pip3 install gunicorn

# Install Requests
RUN pip3 install requests

# Install Numpy
RUN pip3 install numpy

# Install Pandas
RUN pip3 install pandas

# Install GDAL
RUN apt-get install -y build-essential libssl-dev libffi-dev libxml2-dev libxslt1-dev zlib1g-dev
RUN apt -y install libpq-dev
RUN apt-get install -y gdal-bin 
RUN apt-get install -y libgdal-dev 

#Install Octave
RUN DEBIAN_FRONTEND=noninteractive apt-get install keyboard-configuration
RUN apt-get install -y octave
RUN apt install octave-* -y

# Copy WASDI Server Files
COPY ./ /home/wasdi/

# Install Wasdi
RUN pip3 install wasdi

# Install User packages
RUN apt-get install -y `cat /home/wasdi/packages.txt | tr "\n" " "`; exit 0

# Set working dir
WORKDIR /home/wasdi

# Start the server
RUN chmod 777 ./runServer.sh
CMD ./runServer.sh
