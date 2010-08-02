'''
Created on Jun 8, 2010

@author: uemit.seren
'''
import logging

from pylons import request, response, session, tmpl_context as c
from pylons.controllers.util import abort, redirect

from gwaswebserver.lib.base import BaseController, render
from gwaswebserver import model
from pylons.decorators import jsonify
from gwaswebserver.lib.base import h, config
import gviz_api, datetime, re


log = logging.getLogger(__name__)

class MainController(BaseController):
    
    
    def index(self):
        return render('/GWASViewer.html')