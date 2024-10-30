FROM python:3.10

COPY requirements.txt /home/appwasdi/requirements.txt
WORKDIR /home/appwasdi
# Installs requirements from pip freeze
RUN pip install -r requirements.txt
COPY ./meteo_ocean_server.py /home/appwasdi/meteo_ocean_server.py
# Starts FLASK server
ENTRYPOINT [ "python", "/home/appwasdi/meteo_ocean_server.py" ]
