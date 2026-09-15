FROM ubuntu:24.04


LABEL org.opencontainers.image.authors="WASDI SARL"


# From now, all commands executed are executed with the user 'root'
USER root


# This environment variable is setted to avoid packages to ask a confirmation
ENV DEBIAN_FRONTEND="noninteractive"

# This variable is setted to make Python stop creating files '.pyc'
ENV PYTHONDONTWRITEBYTECODE="False"

# This variable is setted to force the 'print' method of Python to flush message directly
ENV PYTHONUNBUFFERED="True"

# This variable identifies the Conda base environment used by the processor.
ENV CONDA_PREFIX="{{ sWasdiSystemUserHome }}/venv"

# This variable is setted to use binaries from our Conda environment
ENV PATH="${CONDA_PREFIX}/bin:${PATH}"


# RUN generic to create user, group, directories, etc
RUN groupadd --gid {{ sWasdiSystemGroupId }} --non-unique {{ sWasdiSystemGroupName }} && \
    useradd --create-home --home-dir {{ sWasdiSystemUserHome }} --uid {{ sWasdiSystemUserId }} --gid {{ sWasdiSystemGroupId }} --non-unique --shell /bin/bash {{ sWasdiSystemUserName }}


# RUN generic to update the system and install common packages
RUN echo "APT::Install-Suggests \"0\";" > /etc/apt/apt.conf.d/99local && \
    echo "APT::Install-Recommends \"0\";" >> /etc/apt/apt.conf.d/99local && \
    echo "APT::Get::Assume-Yes \"true\";" >> /etc/apt/apt.conf.d/99local && \
    echo "APT::Keep-Downloaded-Packages \"false\";" >> /etc/apt/apt.conf.d/99local && \
    echo "Dpkg::Options::--force-confold;" >> /etc/apt/apt.conf.d/99local && \
    apt-get update && \
    apt-get install \
        ca-certificates && \
    echo "deb https://mirror.ubuntu.ikoula.com/ noble main restricted universe multiverse"           > /etc/apt/sources.list && \
    echo "deb https://mirror.ubuntu.ikoula.com/ noble-updates main restricted universe multiverse"  >> /etc/apt/sources.list && \
    echo "deb https://mirror.ubuntu.ikoula.com/ noble-security main restricted universe multiverse" >> /etc/apt/sources.list && \
    apt-get update && \
    apt-get dist-upgrade && \
    apt-get install \
        curl \
        dos2unix \
        gdal-bin \
        git \
        nano \
        wget && \
    rm --recursive --force /var/lib/apt/lists/* && \
    find / -xdev -type d -name __pycache__ -exec rm --recursive --force {} +


# From now, all commands executed are executed as {{ sWasdiSystemUserName }}
USER {{ sWasdiSystemUserName }}


# Create the application directory
# Create the virtual environment
RUN mkdir --parents \
        {{ sWasdiSystemUserHome }}/application


# Install Miniconda with Python 3.12 in the processor Conda prefix
RUN wget --quiet --output-document=/tmp/miniconda.sh https://repo.anaconda.com/miniconda/Miniconda3-py312_25.1.1-2-Linux-x86_64.sh && \
    bash /tmp/miniconda.sh -b -p ${CONDA_PREFIX} && \
    rm --force /tmp/miniconda.sh && \
    conda install --yes \
        flask  \
        gunicorn && \
    conda clean --all --force-pkgs-dirs --yes


# Set the workdir to {{ sWasdiSystemUserHome }}/application
WORKDIR {{ sWasdiSystemUserHome }}/application


## FROM HERE, THERE IS NO MORE CACHE ##
# Copy the needed files
COPY --chown={{ sWasdiSoftwareRootDirectoryOwner }}:{{ sWasdiSoftwareRootDirectoryGroup }} ./ {{ sWasdiSystemUserHome }}/application/


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
        --package-conda


# Install the common PIP libraries in the environment updated from env.yml
RUN ${CONDA_PREFIX}/bin/python -m pip install --no-cache-dir --no-compile \
        numpy && \
    ${CONDA_PREFIX}/bin/python -m pip install --no-cache-dir --no-compile {{ sPythonPipLibraryWasdiRepository }} \
        wasdi && \
    find {{ sWasdiSystemUserHome }} -xdev -type d -name __pycache__ -exec rm --recursive --force {} +


# Start the container
CMD [ \
    "{{ sWasdiSystemUserHome }}/venv/bin/python3", \
    "{{ sWasdiSystemUserHome }}/venv/bin/gunicorn", \
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
