"""
Waspy - WASDI Python Library

Created on 11 Jun 2018

@author: p.campanella - FadeOut Software - WASDI Sarl
@author: c.nattero - FadeOut Software - WASDI Sarl
"""
import setuptools
import io

with io.open("README.md", "r", encoding="utf8") as oFh:
    sLong_description = oFh.read()

setuptools.setup(
    name="wasdi",
    version="0.8.7.6",
    author="WASDI Sarl",
    author_email="info@wasdi.cloud",
    description="The WASDI Python library",
    long_description=sLong_description,
    long_description_content_type="text/markdown",
    url="https://www.wasdi.net",
    packages=setuptools.find_packages(),
    license='CC-BY License',
    install_requires=[
        'requests',
    ],
    classifiers=[
        "Development Status :: 4 - Beta",
        "Environment :: Console",
        "Intended Audience :: Developers",
        "Intended Audience :: Science/Research",
        "License :: Other/Proprietary License",
        "Operating System :: OS Independent",
        "Programming Language :: Python :: 3",
        "Topic :: Scientific/Engineering",
        "Topic :: Scientific/Engineering :: Atmospheric Science",
        "Topic :: Scientific/Engineering :: GIS",
        "Topic :: Scientific/Engineering :: Image Processing",
        "Topic :: Software Development :: Libraries"
    ],
    project_urls={
        'Source': 'https://github.com/fadeoutsoftware/WASDI/tree/develop/libraries/waspy',
        'Tracker': 'https://github.com/fadeoutsoftware/WASDI/issues'
    }
)
