"""
Waspy - WASDI Python Library

Created on 11 Jun 2018

@author: p.campanella - FadeOut Software
@author: c.nattero - FadeOut Software
"""
import setuptools
import io

with io.open("README.md", "r", encoding="utf8") as oFh:
    sLong_description = oFh.read()


setuptools.setup(
    name="wasdi",
    version="0.6.3",
    author="FadeOut Software",
    author_email="info@fadeout.biz",
    description="The Wasdi Python library",
    long_description=sLong_description,
    long_description_content_type="text/markdown",
    url="https://www.wasdi.net",
    packages=setuptools.find_packages(),
    install_requires = [
        'requests', 
    ],
    classifiers=[
        "Programming Language :: Python :: 3",
        "Programming Language :: Python :: 2",
        "License :: OSI Approved :: MIT License",
        "Operating System :: OS Independent",
        "Development Status :: 3 - Alpha",
        "Intended Audience :: Science/Research",
        "Topic :: Scientific/Engineering",
        "Topic :: Scientific/Engineering :: GIS",
        "Topic :: Software Development :: Libraries"
    ],
    project_urls={
        'Source': 'https://github.com/fadeoutsoftware/WASDI/tree/develop/libraries/waspy',
        'Tracker': 'https://github.com/fadeoutsoftware/WASDI/issues'
    }
)