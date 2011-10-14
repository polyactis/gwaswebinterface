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
                self.__datasource = JBrowseDataSource('/srv/jbrowse','TAIR10')
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
    
    @jsonify
    def getGeneFromName(self):
        try:
            query = request.params['query']
            if  not hasattr(self, '__datasource') or self.__datasource == None:
                self.__datasource = JBrowseDataSource('/srv/jbrowse','TAIR10')
            gene = self.__datasource.getGeneFromName(query)
            retval = {'status': 'OK','gene':gene}
        except Exception,err:
            retval =  {"status":"ERROR","statustext":"%s" %str(err)}
        return retval
    
    @jsonify
    def getGenesFromQuery(self):
        try:
            query = request.params['query']
            if  not hasattr(self, '__datasource') or self.__datasource == None:
                self.__datasource = JBrowseDataSource('/srv/jbrowse','TAIR10')
            genes = []
            genes = self.__datasource.getGenesFromQuery(query)
            isMore = False
            count=0
            if len(genes) > 20:
                count = len(genes)
                genes = genes[0:20]
                isMore = True
            retval = {'status': 'OK','isMore':isMore,'count':count,'genes':genes}
        except Exception,err:
            retval =  {"status":"ERROR","statustext":"%s" %str(err)}
        return retval
    
    @jsonify
    def getGeneDescription(self):
        from sqlalchemy import desc,asc
        try:
            
            gene = request.params['gene']
            gene_parts = gene.split('.')
            gene_obj = model.GenomeDB.GeneCommentary.query.filter_by(gene_id = model.GenomeDB.Gene_symbol2id.get_by(gene_symbol=gene_parts[0]).gene_id).order_by(desc('gene_commentary_type_id')).first()
            if gene_obj != None:
                description = gene_obj.comment;
            else: 
                description = 'No Description found'
            retval = {'status': 'OK','description':description}
        except Exception,err:
            retval =  {"status":"ERROR","statustext":"%s" %str(err)}
        return retval