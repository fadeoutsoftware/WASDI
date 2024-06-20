# Configuration file for the Sphinx documentation builder.
#
# This file only contains a selection of the most common options. For a full
# list see the documentation:
# https://www.sphinx-doc.org/en/master/usage/configuration.html

# -- Path setup --------------------------------------------------------------

# If extensions (or modules to document with autodoc) are in another directory,
# add these directories to sys.path here. If the directory is relative to the
# documentation root, use os.path.abspath to make it absolute, like shown here.
#
import os
import sys
sys.path.insert(0, os.path.abspath('.'))


# -- Project information -----------------------------------------------------

project = 'WASDI documentation center'
copyright = '2024, WASDI Sàrl'
author = ''


# -- General configuration ---------------------------------------------------

# Add any Sphinx extension module names here, as strings. They can be
# extensions coming with Sphinx (named 'sphinx.ext.*') or your custom
# ones.
extensions = ['javasphinx',
'sphinxemoji.sphinxemoji',
'sphinx.ext.autodoc',
'sphinxcontrib.matlab',
'sphinx_js',
'sphinxcontrib.youtube']

#js_language = 'javascript'
js_source_path = '../libraries/jswasdilib/src/index.js'
#jsdoc_config_path = '../libraries/jswasdilib/src/tsconfig.json'
# Add any paths that contain templates here, relative to this directory.
templates_path = ['_templates']

# List of patterns, relative to source directory, that match files and
# directories to ignore when looking for source files.
# This pattern also affects html_static_path and html_extra_path.
exclude_patterns = ['_build', 'Thumbs.db', '.DS_Store']


# -- Options for HTML output -------------------------------------------------

# The theme to use for HTML and HTML Help pages.  See the documentation for
# a list of builtin themes.
#

html_theme = 'sphinx_rtd_theme'
html_css_files = ["custom.css"]

# Add any paths that contain custom static files (such as style sheets) here,
# relative to this directory. They are copied after the builtin static files,
# so a file named "default.css" will overwrite the builtin "default.css".
html_static_path = ['_static']
html_favicon = 'favicon.ico'

html_logo = '_static/logowasdi.svg'


import os
import sys
#print(sys.path)
# Import Waspy library
sys.path.insert(0, os.path.abspath('../libraries/waspy'))
# Import Octave/Matlab library
matlab_src_dir = os.path.abspath('../libraries/')

print("Python source folders " + sys.path[0] +"\n")
print("Matlab/Octave source folder " + matlab_src_dir +"\n")


os.system("echo --- HOT FIX for Javasphinx library ---")
os.system("echo --- overwriting domain.py with the fixed version ---")
os.system("mv domain.py /home/docs/checkouts/readthedocs.org/user_builds/wasdi/envs/latest/lib/python3.12/site-packages/javasphinx/domain.py")

os.system("npm list -g;")

os.system("echo --- Installation of JsDoc---")
os.system("cd ~ ;npm install -g jsdoc@3.6.10;")

#os.system("echo --- FIX for npm version ---")
#os.system("cd ~ ;npm install -g typedoc@0.22.18;")

os.system("echo --- Python version ---")
os.system("python --version")

os.system("echo --- pip version ---")
os.system("pip --version")