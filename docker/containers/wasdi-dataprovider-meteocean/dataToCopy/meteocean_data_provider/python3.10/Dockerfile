FROM python:3.10

# From now, all commands executed are executed with the user 'root'
#FROM ubuntu:22.04
#RUN apk add --no-cache bash
COPY requirements.txt /home/appwasdi/requirements.txt
WORKDIR /home/appwasdi
RUN pip install -r requirements.txt
COPY ./meteo_ocean_server.py /home/appwasdi/meteo_ocean_server.py

ENTRYPOINT [ "python", "/home/appwasdi/meteo_ocean_server.py" ]
