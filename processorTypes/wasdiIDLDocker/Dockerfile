FROM ubuntu:18.04


LABEL org.opencontainers.image.authors="WASDI SARL"


# From now, all commands executed are executed with the user 'root'
USER root


# This environment variable is setted to avoid packages to ask a confirmation
ENV DEBIAN_FRONTEND="noninteractive"

# This variable is setted to make Python stop creating files '.pyc'
ENV PYTHONDONTWRITEBYTECODE="False"

# This variable is setted to force the 'print' method of Python to flush message directly
ENV PYTHONUNBUFFERED="True"

# This variable is setted to use binaries from our virtual environment
ENV PATH="{{ sWasdiSystemUserHome }}/venv/bin:${PATH}"


# RUN generic to create user, group, directories, etc
RUN groupadd --gid {{ sWasdiSystemGroupId }} --non-unique {{ sWasdiSystemGroupName }} && \
    useradd --create-home --home-dir {{ sWasdiSystemUserHome }} --uid {{ sWasdiSystemUserId }} --gid {{ sWasdiSystemGroupId }} --non-unique --shell /bin/bash {{ sWasdiSystemUserName }}


# Update the system and install packages
RUN apt-get update && \
    apt-get install --assume-yes software-properties-common && \
    add-apt-repository ppa:nextgis/ppa && \
    apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y \
        build-essential \
        dos2unix \
        curl \
        gdal-bin \
        libffi-dev \
        libgdal-dev \
        libpq-dev \
        libssl-dev \
        libxml2-dev \
        libxslt1-dev \
        locate \
        nano \
        openjdk-8-jre \
        python3 \
        python3-dev \
        python3-gdal \
        python3-pip \
        python3-venv \
        wget \
        zlib1g-dev

# Specific for IDL:
# - install additional packages
# - create directories
# - extract the archive in a temporary directory
# - install the license server
# - clean temporary files
RUN apt-get update && \
    apt-get install \
        libxpm4 \
        libxmu6 && \
mkdir --parents \
        /.idl \
        /tmp/install && \
    chown {{ sWasdiSystemUserName }}:{{ sWasdiSystemGroupName }} \
        /.idl && \
    chmod 750 \
        /.idl && \
    rm --recursive --force /var/lib/apt/lists/* && \
    find / -xdev -type d -name __pycache__ -exec rm --recursive --force {} +

ADD envi552-linux.tar /tmp/install/

RUN bash /tmp/install/install-wasdi.sh && \
    touch /usr/local/harris/license/o_licenseserverurl.txt && \
    chown {{ sWasdiSoftwareRootDirectoryOwner }}:{{ sWasdiSoftwareRootDirectoryGroup }} /usr/local/harris/license/o_licenseserverurl.txt && \
    chmod 640 /usr/local/harris/license/o_licenseserverurl.txt && \
    rm --recursive --force \
        /usr/local/harris/envi55/idl/examples \
        /usr/local/harris/envi55/idl/help \
        /tmp/install


# From now, all commands executed are executed as {{ sWasdiSystemUserName }}
USER {{ sWasdiSystemUserName }}


# Create the application directory
# Create the virtual environment
RUN mkdir --parents \
        {{ sWasdiSystemUserHome }}/application


# Set the workdir to {{ sWasdiSystemUserHome }}/application
WORKDIR {{ sWasdiSystemUserHome }}/application


## FROM HERE, THERE IS NO MORE CACHE ##
# Copy the needed files
COPY --chown={{ sWasdiSoftwareRootDirectoryOwner }}:{{ sWasdiSoftwareRootDirectoryGroup }} ./ {{ sWasdiSystemUserHome }}/application/


# Create the virtual environment
# Install the common PIP libraries
RUN python3 -m venv --system-site-packages {{ sWasdiSystemUserHome }}/venv && \
    pip3 install --no-cache-dir --no-compile \
      flask \
      gunicorn \
      requests \
      numpy \
      pandas \
      rasterio \
      wheel && \
    pip3 install --no-cache-dir --no-compile {{ sPythonPipLibraryWasdiRepository }} \
      wasdi && \
    find {{ sWasdiSystemUserHome }} -xdev -type d -name __pycache__ -exec rm --recursive --force {} +

# [IN OUR VENV] Install needed Python GDAL library
RUN export CPLUS_INCLUDE_PATH=/usr/include/gdal && \
    export C_INCLUDE_PATH=/usr/include/gdal && \
    pip3 install --global-option=build_ext --global-option="-I/usr/include/gdal" GDAL==`gdal-config --version`

# From now, all commands executed are executed with the user 'root'
USER root


# Install packages only if {{ sWasdiSystemUserHome }}/application/packages.txt exists
RUN bash {{ sWasdiSystemUserHome }}/application/installUserPackage.sh \
        --application-directory {{ sWasdiSystemUserHome }}/application \
        --home-directory {{ sWasdiSystemUserHome }} \
        --package-apt


# From now, all commands executed are executed as {{ sWasdiSystemUserName }}
USER {{ sWasdiSystemUserName }}


# Install libraries only if {{ sWasdiSystemUserHome }}/application/pip.txt exists
RUN bash {{ sWasdiSystemUserHome }}/application/installUserPackage.sh \
        --application-directory {{ sWasdiSystemUserHome }}/application \
        --home-directory {{ sWasdiSystemUserHome }} \
        --package-pip


# Start the container
CMD [ \
    "/bin/bash", \
    "{{ sWasdiSystemUserHome }}/application/docker-entrypoint.sh" \
]
