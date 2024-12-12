FROM ubuntu:22.04


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
ENV PATH="/home/wasdi/venv/bin:${PATH}"


# RUN generic to create user, group, directories, etc
RUN groupadd --gid {{ sWasdiSystemGroupId }} --non-unique {{ sWasdiSystemGroupName }} && \
    useradd --create-home --home-dir {{ sWasdiSystemUserHome }} --uid {{ sWasdiSystemUserId }} --gid {{ sWasdiSystemGroupId }} --non-unique --shell /bin/bash {{ sWasdiSystemUserName }} && \
    mkdir --parent /home/wasdi && \
    chown {{ sWasdiSystemUserName }}:{{ sWasdiSystemGroupName }} /home/wasdi


# RUN generic to update the system and install common packages
RUN echo "APT::Install-Suggests \"0\";" > /etc/apt/apt.conf.d/99local && \
    echo "APT::Install-Recommends \"0\";" >> /etc/apt/apt.conf.d/99local && \
    echo "APT::Get::Assume-Yes \"true\";" >> /etc/apt/apt.conf.d/99local && \
    echo "APT::Keep-Downloaded-Packages \"false\";" >> /etc/apt/apt.conf.d/99local && \
    echo "Dpkg::Options::--force-confold;" >> /etc/apt/apt.conf.d/99local && \
    apt-get update && \
    apt-get install \
        ca-certificates && \
    echo "deb https://mirror.ubuntu.ikoula.com/ jammy main restricted universe multiverse"           > /etc/apt/sources.list && \
    echo "deb https://mirror.ubuntu.ikoula.com/ jammy-updates main restricted universe multiverse"  >> /etc/apt/sources.list && \
    echo "deb https://mirror.ubuntu.ikoula.com/ jammy-security main restricted universe multiverse" >> /etc/apt/sources.list && \
    apt-get update && \
    apt-get dist-upgrade && \
    apt-get install \
        curl \
        dos2unix \
        gdal-bin \
        git \
        gunicorn \
        nano \
        python3 \
        python3-flask \
        python3-gdal \
        python3-requests \
        python3-pip \
        python3-venv \
        python3-wheel \
        wget && \
    rm --recursive --force /var/lib/apt/lists/* && \
    find / -xdev -type d -name __pycache__ -exec rm --recursive --force {} +


# From now, all commands executed are executed as {{ sWasdiSystemUserName }}
USER {{ sWasdiSystemUserName }}


# Set the workdir to /home/wasdi
WORKDIR /home/wasdi


## FROM HERE, THERE IS NO MORE CACHE ##
# Copy the needed files
COPY --chown={{ sWasdiSoftwareRootDirectoryOwner }}:{{ sWasdiSoftwareRootDirectoryGroup }} ./ /home/wasdi/


# Create the virtual environment
# Install the common PIP libraries
RUN python3 -m venv --system-site-packages /home/wasdi/venv && \
    pip3 install --no-cache-dir --no-compile \
        numpy && \
    pip3 install --no-cache-dir --no-compile {{ sPythonPipLibraryWasdiRepository }} \
        wasdi && \
    find /home/wasdi/ -xdev -type d -name __pycache__ -exec rm --recursive --force {} +


# From now, all commands executed are executed with the user 'root'
USER root


# Install packages only if /home/wasdi/packages.txt exists
RUN bash /home/wasdi/installUserPackage.sh \
        --application-directory /home/wasdi \
        --home-directory /home/wasdi \
        --package-apt


# From now, all commands executed are executed as {{ sWasdiSystemUserName }}
USER {{ sWasdiSystemUserName }}


# Install libraries only if /home/wasdi/pip.txt exists
RUN bash /home/wasdi/installUserPackage.sh \
        --application-directory /home/wasdi \
        --home-directory /home/wasdi \
        --package-pip


# Start the container
CMD [ \
    "/home/wasdi/venv/bin/python3", \
    "/usr/bin/gunicorn", \
    "--workers", \
    "1", \
    "--max-requests", \
    "1", \
    "--timeout", \
    "3600", \
    "--bind", \
    "0.0.0.0:5000", \
    "wasdiProcessorServer:app" \
]
