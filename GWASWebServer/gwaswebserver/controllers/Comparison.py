import logging
import math

from pylons import request, response, session, tmpl_context as c
from pylons.controllers.util import abort, redirect

from gwaswebserver.lib.base import BaseController, render, config, h, model
#from gwaswebserver import model
from pymodule import figureOutDelimiter
import datetime, StringIO, csv, gviz_api, traceback, sys
from DisplayResults import DisplayresultsController
from HelpOtherControllers import HelpothercontrollersController
from formencode import htmlfill
import simplejson
from variation.src.Stock_250kDB import Stock_250kDB, CallMethod
from variation.src.Stock_250kDB import ResultsMethod,AnalysisMethod,CallMethod,PhenotypeMethod
from pylons.decorators import jsonify
from variation.src import analyzeSNPResult


log = logging.getLogger(__name__)

class ComparisonController(BaseController):

    def index(self):
        # Return a rendered template
        #   return render('/template.mako')
        # or, Return a response
        c.call_method_ls_json =  DisplayresultsController.getCallMethodLsJson()
        analysis_method_ls = [[0,"please choose"]]
        for row in model.Stock_250kDB.AnalysisMethod.query.all():
            analysis_method_ls.append([row.id, row.short_name])
        c.analysis_method_ls_json = simplejson.dumps(analysis_method_ls)
        
        gwas_files = HelpothercontrollersController.getTemporaryGWASResults()
        gwas_files['files'].insert(0,'Select Temporary GWAS File')
        heatmap_files = HelpothercontrollersController.getHeatMapFiles()
        heatmap_files['files'].insert(0,'Select Temporary GWAS File')
        c.heatmap_files = simplejson.dumps(heatmap_files)
        c.gwas_files = simplejson.dumps(gwas_files)
        return render('/Comparison.html')
    
    @jsonify
    def getResultMethodByArgs(self):
        phenotype_method_id = request.params.get('phenotype_method_id')
        analysis_method_id = request.params.get('analysis_method_id')
        call_method_id = request.params.get('call_method_id')
        first_entry = request.params.get('first_entry')
        results_method_ls = {'options': []}
        if first_entry is None:
            results_method_ls['options'].append({"value":0,"id":"please choose"})
        #.filter(ResultsMethod.analysis_method.get_by(id=analysis_method_id)))
        #CallMethod.get_by(id=call_method_id)).filter(PhenotypeMethod.get_by(id=phenotype_method_id)).filter(AnalysisMethod.get_by(id=analysis_method_id)):
        #analysis_method_id=1,phenotype_method_id=1
        query = ResultsMethod.query
        query = query.filter_by(call_method_id=call_method_id)
        query =  query.filter_by(phenotype_method_id = phenotype_method_id)
        for row in query:
            results_method_ls['options'].append({"value":row.id, "id":row.short_name})
        return results_method_ls;
    
    @jsonify
    def getGWASForComparision(self):
        x_results_method_id = request.params.get('x_results_method_id',None)
        y_results_method_id = request.params.get('y_results_method_id',None)
        path = "/Network/Data/250k/tmp-data/gwas/";
        x_filename = request.params.get('x_results_file',None)
        y_filename = request.params.get('y_results_file',None)
        min_MAF_x = None
        min_MAF_y = None
        
        rm_x = ResultsMethod.get(x_results_method_id)
        
        if x_filename is None:
            x_filename = rm_x.filename
            if rm_x.analysis_method.min_maf is not None:
                min_MAF_x = rm_x.analysis_method.min_maf
        else:
            x_filename = path+x_filename
            
        rm_y = ResultsMethod.get(y_results_method_id)
        if y_filename is None:
            y_filename = rm_y.filename
            if rm_y.analysis_method.min_maf is not None:
                min_MAF_y = rm_y.analysis_method.min_maf
        else:
            y_filename = path+y_filename  
        #0.002
        #return '[ {"x_value":5.4948500216800937,"y_value":3.6986661045512061,"chr":1,"pos":30204930},{"x_value":2.4948500216800937,"y_value":6.6986661045512061,"chr":2,"pos":50204930}]';
        json_data = analyzeSNPResult.get_comparison_lists(result_file_1=x_filename, result_file_2=y_filename, type='fraction', top_fraction=1,min_MAF_x=min_MAF_x ,min_MAF_y=min_MAF_y)
        del(json_data['results'])
        #json_data = self.getGWASForComparisionJsonData(rm_x,rm_y)
        json_data['overlap_count'] = 1
        if rm_x is not None:
            json_data['x_analysis_method_id'] = rm_x.analysis_method_id
        if rm_y is not None:
            json_data['y_analysis_method_id'] = rm_y.analysis_method_id
        return json_data
    
    @classmethod
    def getGWASForComparisionJsonData(cls, rm_x, rm_y):
        log.info("Getting Comparison Data from result %s and %s... \n"%(rm_x.id,rm_y.id))
        from variation.src.common import getResultForComparison
        return getResultForComparison(rm_x, rm_y)
        log.info("Done.\n")
    
    @jsonify
    def getGeneOccurencesData(self):
        result_method_ids = [int(x) for x in request.params.get('result_method_ids').split(',')];
        count = int(request.params.get('count'))
        if count is None:
            count = 100
        files = []
        for id in result_method_ids:
            files.append(ResultsMethod.get(id).filename)
        data = analyzeSNPResult.get_gene_occurences(result_files=files,num_genes=count)
        retval = {}
        retval['genes_data'] = self.getDataArrayFromOccurencesData(data)
        
        return retval
        
    
    @jsonify
    def getSNPOccurencesData(self):
        result_method_ids = [int(x) for x in request.params.get('result_method_ids').split(',')];
        count = int(request.params.get('count'))
        if count is None:
            count = 1000
        files = []
        for id in result_method_ids:
            files.append(ResultsMethod.get(id).filename)
        data = analyzeSNPResult.get_snp_occurence(result_files=files,num_snps=count)
        retval = {}
        retval['snps_data'] = self.getDataArrayFromOccurencesData(data)
        return retval
    
    @jsonify
    def getResultOccurencesData(self):
        result_method_ids = [int(x) for x in request.params.get('result_method_ids').split(',')];
        snps_count = int(request.params.get('snps_count'))
        genes_count = int(request.params.get('genes_count'))
        if snps_count is None:
            snps_count = 1000
        if genes_count is None:
            genes_count = 100
        files = []
        for id in result_method_ids:
            files.append(ResultsMethod.get(id).filename)
        data = analyzeSNPResult.get_result_occurences(result_files=files,num_snps=snps_count,num_genes=genes_count)
        
        snps_result = data['snps_result']
        genes_result = data['genes_result']
        retval = {}
        retval['snps_data'] = self.getDataArrayFromOccurencesData(snps_result)
        retval['genes_data'] = self.getDataArrayFromOccurencesData(genes_result)
        return retval
    
    @jsonify
    def getBioHeatMapData(self):
        heatmap_file = request.params.get('heatmap_file',None)
        path = "/Network/Data/250k/tmp-data/heatmap/";
        heatmap_file = path + heatmap_file
        f = open(heatmap_file)
        
        data = []
        header = f.next().replace('"','')
        header = header.split("\t")[1:]
        description = (map(lambda snp: (snp,"number"),header))
        description.insert(0,("snps","string"))
        line_lists = []
        i = 0
        for line in f:
            
            line_splits = line.split("\t")
            values = [-math.log(float(v),10) for v in line_splits[1:]]
            values.insert(0,line_splits[0].replace('"',''))
            #[-math.log(v,10) for v in line_splits[1:19]]
            data.append(values)
            i=i+1
            #if i == 31: break
        
        dataTable = gviz_api.DataTable(description)
        dataTable.LoadData(data)
        return dataTable.ToJSon()
    
    
    @classmethod
    def getDataArrayFromOccurencesData(cls,data):
        bar_chart_data = []
        table_data = []
        table_json_data = []
        for i,set_info in enumerate(data[0]):
            bar_chart_data.append({"count":i+1,"amount":len(set_info)})
            for info in set_info:
                if len(info) == 4:
                    table_data.append({"chromosome":info[0],"position":info[1],"start_pos":info[1]-10000,"end_pos":info[1]+10000})
                else:
                    table_data.append({"chromosome":info[0],"position":info[1],"start_pos":info[1]-10000,"end_pos":info[1]+10000})
            dataTable = gviz_api.DataTable({"chromosome":("number","Chromosome"),"position":("number","Position"),"start_pos":("number","start pos"),"end_pos":("number","end pos")})
            dataTable.LoadData(table_data)
            table_json_data.append(dataTable.ToJSon(["chromosome","position","start_pos","end_pos"]))
            table_data = []
            dataTable = None
            
        dataTable = gviz_api.DataTable({"count":("string","Count"),"amount":("number","Amount")})
        dataTable.LoadData(bar_chart_data)
        retval = {}
        retval['bar_chart_data'] = dataTable.ToJSon(["count","amount"])
        retval['table_data'] = table_json_data;
        return retval
            