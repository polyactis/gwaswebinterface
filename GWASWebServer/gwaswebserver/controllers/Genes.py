import math

from pylons import request, response, session, tmpl_context as c
from pylons.controllers.util import abort, redirect
from gwaswebserver.lib.base import BaseController, render, config, h, model
import simplejson
from gwaswebserver.lib.JBrowseDataSource import DataSource as JBrowseDataSource
from pylons.decorators import jsonify


class GenesController(BaseController):


    @jsonify
    def getGenes(self):
        try:
            if  not hasattr(self, '__datasource') or self.__datasource == None:
                self.__datasource = JBrowseDataSource('/var/www/jbrowse','mRNA2')
            genes = []
            chromosome = request.params['chromosome']
            start = request.params['start']
            end = request.params['end']
            isFeatures= request.params['isFeatures']
            genes = self.__datasource.getGenes(chromosome, int(start), int(end), bool(isFeatures))
            retval = {'status': 'OK','genes':genes}
        except Exception,err:
            retval =  {"status":"ERROR","statustext":"%s" %str(err)}
        return retval
    
