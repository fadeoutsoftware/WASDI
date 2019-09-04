FROM ubuntu:16.04

MAINTAINER Fadeout Software <info@fadeout.it>

# Install Java and common tools
RUN apt-get update && apt-get install -y \
	openjdk-8-jre \
	software-properties-common \
	locate \
	nano \
        python-pip

# Go at Home and copy files
WORKDIR "/root"
COPY ./esa-snap_sentinel_unix_6_0.sh /root/
COPY ./response.varfile /root/

# Install SNAP
RUN chmod 777 ./esa-snap_sentinel_unix_6_0.sh
RUN ./esa-snap_sentinel_unix_6_0.sh -q -varfile "./response.varfile"

# Install Flask
RUN pip install flask

# Install gunicorn
RUN pip install gunicorn

# Install Requests
RUN pip install requests

# Configure SNAPPY
WORKDIR "/usr/local/snap/bin"
RUN ./snappy-conf /usr/bin/python
WORKDIR "/root/.snap/snap-python/snappy"
RUN python setup.py install

# Install Numpy
RUN pip install numpy

#Add Python 3.4 Repo
RUN add-apt-repository -y ppa:ubuntugis/ppa
RUN apt-get -y update 

# Install GDAL
RUN apt-get install -y gdal-bin 
RUN apt-get install -y python-gdal 
#RUN apt-get install -y python3-gdal

# Copy User Files
COPY ./ /root/

# Install Wasdi
RUN pip install wasdi

# Install User packages
RUN apt-get install -y `cat /root/packages.txt | tr "\n" " "`; exit 0

# Install User Python Libs
RUN pip install `cat /root/pip.txt | tr "\n" " "`; exit 0

# Set working dir
WORKDIR /root

# Start the server
#CMD ["gunicorn" , "-w 1", "--max-requests 1" , "-t 3600", "-b 0.0.0.0:5000", "wasdiProcessorServer:app"]
RUN chmod 777 ./runServer.sh
CMD ./runServer.sh
