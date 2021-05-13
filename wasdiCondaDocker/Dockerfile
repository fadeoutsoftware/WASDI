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
	nano \
	wget

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

# Go at Home and copy files
WORKDIR "/home/wasdi"

# Add the tomcat user
RUN groupadd -g $GRP_ID $GRP_NAME
RUN chmod 777 /home
RUN useradd -u $USR_ID -g $GRP_ID $USR_NAME -d /home/$USR_NAME

USER $USR_NAME
#
# MiniConda installing
RUN wget https://repo.anaconda.com/miniconda/Miniconda3-py39_4.9.2-Linux-x86_64.sh
RUN bash Miniconda3-py39_4.9.2-Linux-x86_64.sh -b -p /home/$USR_NAME/miniconda
RUN rm Miniconda3-py39_4.9.2-Linux-x86_64.sh

#RUN wget https://repo.anaconda.com/archive/Anaconda3-2021.04-Linux-x86_64.sh
#RUN bash Anaconda3-2021.04-Linux-x86_64.sh -b -p /home/$USR_NAME/miniconda
#RUN rm Anaconda3-2021.04-Linux-x86_64.sh


## Updating Anaconda packages
ENV PATH /home/$USR_NAME/miniconda/bin:$PATH
RUN /home/$USR_NAME/miniconda/bin/conda update -y --all

# Install fast ai
RUN /home/$USR_NAME/miniconda/bin/conda install -y -c fastchan fastai
# Install OpenCV
RUN /home/$USR_NAME/miniconda/bin/conda install -y -c conda-forge opencv

RUN /home/$USR_NAME/miniconda/bin/conda install -y -c anaconda pip
RUN pip3 install wasdi
RUN pip3 install gunicorn
RUN pip3 install flask
RUN pip3 install requests

# Copy WASDI Server Files
COPY ./ /home/wasdi/

# Install specific app requirements
#RUN /home/$USR_NAME/miniconda/bin/conda install `cat /home/wasdi/requirements.txt | tr "\n" " "`; exit 0
RUN pip3 install `cat /home/wasdi/requirements.txt | tr "\n" " "`; exit 0
#RUN /home/$USR_NAME/miniconda/bin/conda env update --file /home/wasdi/env.yml

# Set working dir
WORKDIR /home/wasdi

# Start the server
USER root
RUN chmod 777 ./runServer.sh
USER $USR_NAME
CMD ./runServer.sh
