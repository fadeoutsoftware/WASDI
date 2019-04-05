FROM ubuntu:16.04

MAINTAINER Fadeout Software <info@fadeout.it>

# Install Java and common tools
RUN apt-get update && apt-get install -y \
	openjdk-8-jre \
	software-properties-common \
	locate \
	nano

#Add Python 3.4 Repo
RUN add-apt-repository -y ppa:deadsnakes/ppa

# Install Python 3.4
RUN apt-get update && apt-get install -y \
	python3.4

# Go at Home and copy files
WORKDIR "/root"
COPY ./esa-snap_sentinel_unix_6_0.sh /root/
COPY ./response.varfile /root/
COPY ./get-pip.py /root/

# Install PIP
RUN /usr/bin/python3.4 ./get-pip.py

# Install SNAP
RUN chmod 777 ./esa-snap_sentinel_unix_6_0.sh
RUN ./esa-snap_sentinel_unix_6_0.sh -q -varfile "./response.varfile"

# Install Flask
RUN /usr/local/bin/pip3.4 install flask

# Install gunicorn
RUN /usr/local/bin/pip3.4 install gunicorn

# Install Requests
RUN /usr/local/bin/pip3.4 install requests

# Configure SNAPPY
WORKDIR "/usr/local/snap/bin"
RUN ./snappy-conf /usr/bin/python3.4
WORKDIR "/root/.snap/snap-python/snappy"
RUN /usr/bin/python3.4 setup.py install

# Copy User Files
COPY ./ /root/

# Install User packages
RUN apt-get install -y `cat packages.txt | tr "\n" " "`; exit 0

# Install User Python Libs
RUN /usr/local/bin/pip3.4 install `cat pip.txt | tr "\n" " "`; exit 0

# Set working dir
WORKDIR /root

# Start the server
#CMD ["gunicorn" , "-w 1", "--max-requests 1" , "-t 3600", "-b 0.0.0.0:5000", "wasdiProcessorServer:app"]
RUN chmod 777 ./runServer.sh
CMD ./runServer.sh
