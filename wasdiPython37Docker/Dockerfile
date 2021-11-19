FROM ubuntu:20.04

MAINTAINER Fadeout Software <info@fadeout.it>

ARG USR_NAME
ARG USR_ID
ARG GRP_NAME
ARG GRP_ID

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

# Install GDAL
RUN apt-get install -y build-essential libssl-dev libffi-dev libxml2-dev libxslt1-dev zlib1g-dev
RUN apt -y install libpq-dev
RUN apt-get install -y gdal-bin
RUN apt-get install -y libgdal-dev
RUN apt-get update && apt install -y python3-gdal

RUN mkdir /home/wasdi && chmod o+wrx /home/wasdi

# Install venv
RUN apt-get install -y python3-venv

# Activate venv
ENV VIRTUAL_ENV=/home/wasdi/venv
RUN python3 -m venv $VIRTUAL_ENV
ENV PATH="$VIRTUAL_ENV/bin:$PATH"

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

# Install Rasterio
RUN pip3 install rasterio

# Install python gdal
RUN export CPLUS_INCLUDE_PATH=/usr/include/gdal
RUN export C_INCLUDE_PATH=/usr/include/gdal

RUN pip3 install --global-option=build_ext --global-option="-I/usr/include/gdal" GDAL==`gdal-config --version`

# Add the tomcat user
RUN groupadd -g $GRP_ID $GRP_NAME
RUN chmod 777 /home
RUN useradd -u $USR_ID -g $GRP_ID $USR_NAME -d /home/$USR_NAME

# Copy WASDI Server Files
COPY ./ /home/wasdi/

# Install Wasdi
RUN pip3 install wasdi

# Install User packages
RUN apt-get install -y `cat /home/wasdi/packages.txt | tr "\n" " "`; exit 0

# Install User Python Libs
RUN pip3 install `cat /home/wasdi/pip.txt | tr "\n" " "`; exit 0

# Set working dir
WORKDIR /home/wasdi

# Start the server
RUN chmod 777 ./runServer.sh
CMD ./runServer.sh
