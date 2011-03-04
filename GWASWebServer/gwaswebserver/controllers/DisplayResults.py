import logging

from pylons import request, response, session, tmpl_context as c
from pylons.controllers.util import abort, redirect

from gwaswebserver.lib.base import BaseController, render, config, h, model
#from gwaswebserver import model

log = logging.getLogger(__name__)

from pymodule import PassingData
from variation.src.GeneListRankTest import GeneListRankTest
from formencode import htmlfill
from pylons.decorators import jsonify	#2008-12-30
#from gwaswebserver.lib.base import *
import gviz_api	#2009-2-1 google visualization python api to export data into json-related formats
import numpy, datetime
import simplejson, sys, traceback

from HelpOtherControllers import HelpothercontrollersController as hc
from sqlalchemy.sql import and_, or_, not_

class DisplayresultsController(BaseController):

	def index(self):
		"""
		2010-3-11
			add c.getOneResultRawURL
		2009-11-11
			add c.getPhenotypeURL
		2009-10-8
			first try to get the call_method_id from the URL parameters. if it's null, use the published_call_method_id instead.
		2009-7-2
			update c.displayResultsGeneURL to h.url(controller="DisplayResultsGene", action='showResultsGeneForOnePhenotype')
		"""
		# Return a rendered template
		#   return render('/template.mako')
		# or, Return a response
		c.call_method_id = request.params.get('call_method_id', config['app_conf']['published_call_method_id'])
		c.getPhenotypeCategoryLsURL = h.url(controller="DisplayResults", action="getPhenotypeCategoryLs")
		c.getPhenotypeTableDataURL = h.url(controller="DisplayResults", action="getPhenotypeTableData")
		c.getGWAURL = h.url(controller="DisplayResults", action="showGWA")
		c.getPhenotypeURL = h.url(controller='Phenotype')
		c.displayResultsGeneURL = h.url(controller="DisplayResultsGene", action='showResultsGeneForOnePhenotype')
		# 2010-3-11
		c.getOneResultRawURL = h.url(controller='DisplayResults', action='getOneResultRaw', id=None)
		c.requestCallAndAnalysisMethodURL = h.url(controller='DisplayResults', action='getGWASGivenPhenotypeIDJson', id=None)
		return render("GWASPhenotypes.html")
	
	@jsonify
	def getPhenotypeCategoryLs(self):
		"""
		2010-4-19
			remove the requirement that this phenotype must have association results in table results_method
		2010-4-6
			remove the call_method_id constraint
		2009-4-21
			get categories covering all phenotypes that have association results with published_call_method_id
		"""
		phenotypeCategoryLs = []
		existNullCategoryID = False
		call_method_id = request.params.get('call_method_id', int(config['app_conf']['published_call_method_id']))
		rows = model.db.metadata.bind.execute("select distinct p.biology_category_id from %s p order by biology_category_id"%\
											(model.Stock_250kDB.PhenotypeMethod.table.name,))
		for row in rows:
			if row.biology_category_id is not None:
				biologyCategory = model.Stock_250kDB.BiologyCategory.get(row.biology_category_id)
				if biologyCategory is not None:
					phenotypeCategoryLs.append([str(biologyCategory.id), biologyCategory.short_name])
				else:
					existNullCategoryID = True
			else:
				existNullCategoryID = True
		if existNullCategoryID:
			phenotypeCategoryLs.append(["0", 'Others'])
		return  phenotypeCategoryLs
	
	def getPhenotypeTableData(self):
		"""
		2009-11-17
			"#Accessions" means the number of accessions that were phenotyped.
			add "#Genotyped" column in the output, which means the no of accessions that were phenotyped and genotyped (in certain call method).
		2009-11-17
			getNoOfAccessionsGivenPhenotypeAndCallMethodID() replaces getNoOfAccessionsGivenPhenotypeMethodID()
		2009-7-30
			fetch the number of accessions for each phenotype by calling hc.getNoOfAccessionsGivenPhenotypeMethodID(), rather than static value in table phenotype_method 
		2009-5-24
			add column transformation_description back
		2009-5-13
			remove column transformation_description, citations
		2009-5-11
			add no_of_accessions, growth_condition, phenotype_scoring, citations in the output table
		2009-4-21
			return a google data table containing information of all phenotypes belonging to a category 
		"""
		call_method_id = request.params.get('call_method_id', int(config['app_conf']['published_call_method_id']))
		call_method_id = int(call_method_id)
		biology_category_id = request.params.get('biology_category_id', 1)
		if biology_category_id==0 or biology_category_id=='0':
			biology_category_id=None
		
		#construct the full data and turn it into json
		column_name_type_ls = [("id", ("number", "Phenotype ID")), \
							("short_name", ("string", "Phenotype Name")), \
							("no_of_accessions", ("number","#Accessions")), \
							("growth_condition", ("string","Growth Condition")), \
							("phenotype_scoring", ("string","Phenotype Scoring")), \
							("method_description", ("string","Source")), \
							("data_type", ("string", "Data Type")),\
							("transformation_description", ("string", "Transformation"))]
		#					("association_results", ("string","Association Results")), \	# 2010-3-17 no need. the popup would display this.
		#					("no_of_genotyped_accessions", ("number","#Genotyped")), \	# 2010-3-17 no need. the popup would display this.
		
		rows = model.Stock_250kDB.PhenotypeMethod.getPhenotypesForACL(biology_category_id,h.user())
		#rows = model.Stock_250kDB.PhenotypeMethod.query.filter(model.Stock_250kDB.PhenotypeMethod.biology_category_id==biology_category_id)
		
		return_ls = []
		for row in rows:
			entry = dict()
			for column_name_type in column_name_type_ls:
				column_name = column_name_type[0]
				column_type = column_name_type[1][0]
				
				if column_type=='string':
					default_value = ''
				elif column_type =='number':
					default_value = -1
				elif column_type=='date':
					default_value = datetime.date(2050, 1, 1)
				else:
					default_value = None
				
				if column_name == 'association_results':
					results = model.Stock_250kDB.ResultsMethod.query.filter_by(call_method_id=call_method_id).filter_by(phenotype_method_id=row.id)
					analysis_method_short_name_ls = []
					for result in results:
						analysis_method_short_name_ls.append(result.analysis_method.short_name)
					if analysis_method_short_name_ls:
						column_value = ','.join(analysis_method_short_name_ls)
					else:
						column_value = ""
				elif column_name=='no_of_accessions':
					column_value = hc.getNoOfAccessionsGivenPhenotypeMethodID(row.id)
				elif column_name=='no_of_genotyped_accessions':
					column_value = hc.getNoOfAccessionsGivenPhenotypeAndCallMethodID(row.id, call_method_id)
				else:
					column_value = getattr(row, column_name, default_value)
					if column_value is None:
						column_value = default_value
				entry[column_name] = column_value
			return_ls.append(entry)
		
		description = dict(column_name_type_ls)
		data_table = gviz_api.DataTable(description)
		data_table.LoadData(return_ls)
		column_ls = [row[0] for row in column_name_type_ls]
		json_result = data_table.ToJSon(columns_order=column_ls)	#ToJSonResponse only works with google.visualization.Query
		response.headers['Content-Type'] = 'application/json'
		return json_result
	
	def showGWA(self):
		"""
		2010-3-11
			add functionality to process analysis_method_id
			add c.getOneResultRawURL 
		2009-4-23
		"""
		
		c.phenotype_method_id = int(request.params.get('phenotype_method_id', 1))
		c.call_method_id = int(request.params.get('call_method_id', config['app_conf']['published_call_method_id']))
		# 2010-3-11
		analysis_method_id = request.params.get('analysis_method_id', None)
		transformation_method_id = request.params.get('transformation_method_id', None)
		if analysis_method_id is not None:
			analysis_method_id = int(analysis_method_id)
		
		c.phenotypeSummaryURL = h.url(controller='Phenotype', action='index', phenotype_method_id=c.phenotype_method_id, \
									call_method_id=c.call_method_id,transformation_method_id=transformation_method_id)
		c.GWABaseURL = h.url(controller='DisplayResults', action='fetchOne', \
							phenotype_method_id=c.phenotype_method_id, call_method_id=c.call_method_id,\
							transformation_method_id=transformation_method_id)
		c.SNPBaseURL = h.url(controller='SNP', action='index', \
							phenotype_method_id=c.phenotype_method_id, \
							call_method_id=c.call_method_id,transformation_method_id=transformation_method_id)
		c.getAnalysisMethodLsURL = h.url(controller='DisplayResults', action='getAnalysisMethodLsJson', \
							phenotype_method_id=c.phenotype_method_id, \
							call_method_id=c.call_method_id, analysis_method_id=analysis_method_id,\
							transformation_method_id=transformation_method_id)
		"""
		# 2009-4-25 no way to pass this 2D (int, string) array to the template! 
		analysis_method_ls = self.getAnalysisMethodLs(c.call_method_id, c.phenotype_method_id)
		c.analysis_method_ls = []
		str_func = lambda x: '%s'%x
		for analysis_method_entry in analysis_method_ls:
			analysis_method_entry = map(str_func, analysis_method_entry)
			c.analysis_method_ls.append('[' + ','.join(analysis_method_entry) + ']')
			#c.analysis_method_ls.append(analysis_method_entry)
		#c.analysis_method_ls = simplejson.dumps(c.analysis_method_ls)
		c.analysis_method_ls = '[' + ','.join(c.analysis_method_ls) + ']'
		"""
		pm = model.Stock_250kDB.PhenotypeMethod.get(c.phenotype_method_id)
		
		# check ACL
		if pm.checkACL(h.user()) == False:
			h.cool_denial_handler('No rights')
		
		c.phenotype_method_short_name = pm.short_name
		c.phenotype_method_description = pm.method_description
		
		c.callInfoURL = h.url(controller='DisplayResults', action='fetchCallInfoData', \
							phenotype_method_id=c.phenotype_method_id, call_method_id=c.call_method_id)
		c.phenotypeHistImageURL = h.url(controller='DisplayResults', action='getPhenotypeHistImage', \
										phenotype_method_id=c.phenotype_method_id, call_method_id=c.call_method_id)
		c.callPhenotypeQQImageURL = h.url(controller='DisplayResults', action='getCallPhenotypeQQImage',\
											phenotype_method_id=c.phenotype_method_id, call_method_id=c.call_method_id)
		c.phenotypeHistogramDataURL = h.url(controller='Phenotype', action='getPhenotypeHistogramData', \
										id=c.phenotype_method_id,transformation_method_id=transformation_method_id)
		# 2010-3-11
		c.getOneResultRawURL = h.url(controller='DisplayResults', action='getOneResultRaw', \
										call_method_id=c.call_method_id, phenotype_method_id=c.phenotype_method_id)
		return render('/GWASOnePhenotype.html')
	
	@staticmethod
	def getCallMethodLs():
		"""
		2009-1-30
		"""
		affiliated_table_name = model.Stock_250kDB.ResultsMethod.table.name
		list_info  = hc.getCallMethodInfo(affiliated_table_name=affiliated_table_name)
		call_method_ls = []
		for i in range(len(list_info.id_ls)):
			id = list_info.id_ls[i]
			label = list_info.label_ls[i]
			call_method_ls.append([id, label])
		return call_method_ls
	
	#@jsonify
	def getCallMethodLsJson(cls):
		result = {
				'options': [
						dict(id=value, value=id) for id, value in cls.getCallMethodLs()
						]
				}
		#result['options'].append({'id': u'[At the end]', 'value': u''})
		result['options'].insert(0, {'id': u'Please Choose ...', 'value': 0})
		return simplejson.dumps(result)
	getCallMethodLsJson = classmethod(getCallMethodLsJson)
	
	@staticmethod
	def getPhenotypeMethodLs(call_method_id):
		"""
		2009-1-30
		"""
		affiliated_table_name = model.Stock_250kDB.ResultsMethod.table.name
		extra_condition = 's.call_method_id=%s'%(call_method_id)
		phenotype_info  = hc.getPhenotypeInfo(affiliated_table_name=affiliated_table_name, extra_condition=extra_condition)
		phenotype_method_ls = []
		for i in range(len(phenotype_info.phenotype_method_id_ls)):
			phenotype_method_id = phenotype_info.phenotype_method_id_ls[i]
			phenotype_method_label = phenotype_info.phenotype_method_label_ls[i]
			phenotype_method_ls.append([phenotype_method_id, phenotype_method_label])
		return phenotype_method_ls
	
	@jsonify
	def getPhenotypeMethodLsJson(self):
		result = {
				'options': [
						dict(id=value, value=id) for id, value in self.getPhenotypeMethodLs(request.params.getone('call_method_id'))
						]
				}
		#result['options'].append({'id': u'[At the end]', 'value': u''})
		result['options'].insert(0, {'id': u'Please Choose ...', 'value': 0})
		return result
	
	@staticmethod
	def getAnalysisMethodLs(call_method_id, phenotype_method_id, transformation_method_id=None, analysis_method_id=None):
		"""
		2010-9-21
			deal with the case that transformation_method_id is None
		2010-3-11
			add argument analysis_method_id to narrow down the choice of analysis_method_id
		2009-1-30
		"""
		affiliated_table_name = model.Stock_250kDB.ResultsMethod.table.name	#alias is 's'
		extra_condition = 's.call_method_id=%s and s.phenotype_method_id=%s'%\
				(call_method_id, phenotype_method_id,)
		if transformation_method_id is not None:
			extra_condition += ' and s.transformation_method_id=%s'%transformation_method_id
		
		if analysis_method_id:
			extra_condition += ' and s.analysis_method_id=%s'%analysis_method_id
		list_info = hc.getAnalysisMethodInfo(affiliated_table_name, extra_condition=extra_condition)
		
		analysis_method_ls = []
		
		for i in range(len(list_info.id_ls)):
			id = list_info.id_ls[i]
			label = list_info.label_ls[i]
			description = list_info.description_ls[i]
			pseudoHeritability = list_info.pseudoHeritability_ls[i]
			analysis_method_ls.append([id, label, description,pseudoHeritability])
		return analysis_method_ls
	
	@jsonify
	def getAnalysisMethodLsJson(self):
		call_method_id = request.params.getone('call_method_id')
		phenotype_method_id = request.params.getone('phenotype_method_id')
		analysis_method_id = request.params.get('analysis_method_id', None)
		transformation_method_id = request.params.get('transformation_method_id', None)
		result = {
				'options': [
						dict(id=value, value=id, description=description,pseudoHeritability=pseudoHeritability) \
						for id, value, description,pseudoHeritability in self.getAnalysisMethodLs(call_method_id, phenotype_method_id,\
																			transformation_method_id=transformation_method_id,analysis_method_id=analysis_method_id)
						]
				}
		result['options'].insert(0, {'id': u'Please Choose ...', 'value': 0, 'description': ""})
		return result
	
	def form(self, id=None):
		"""
		2009-1-30
		"""
		if id is None:
			id = request.params.get('id', 1)
		defaults = {'call_method_id': id,
				"remove_old_plots":"on"}
		c.call_method_ls = self.getCallMethodLs()
		c.call_method_ls.insert(0, [0, u'Please Choose ...'])
		html = render('/display_results_form.html')
		return htmlfill.render(html, defaults)
	
	@classmethod
	def getOneResultJsonData(cls, rm, min_MAF, no_of_top_snps):
		"""
		2010-5-3 moved to variation.src.common
		2009-4-24
			refactored out of fetchOne()
			called upon only if its return is not in db.
		"""
		log.info("Getting json_data from result %s ... \n"%rm.id)
		from variation.src.common import getOneResultJsonData
		log.info("Done.\n")
		return getOneResultJsonData(rm, min_MAF, no_of_top_snps)
	
	#@jsonify
	def fetchOne(self, id=None):
		"""
		2009-5-13
			apply min_MAF cutoff according to column min_maf in table analysis_method
			
			difference in float precision between db and python: 0.1 (db) => 0.100000001 (python)
				use filter(ResultsMethodJson.min_MAF<=min_MAF+0.0001).filter(ResultsMethodJson.min_MAF>=min_MAF-0.0001) to solve it
		2009-1-30
			return a dictionary with key as chromosome, value as google visualization data table representing association results.
		"""
		if id is None:
			id = request.params.get('id', None)
		if id is None:
			id = request.params.get('results_id', None)
		
		chromosome = request.params.get('chromosome', None)
		ResultsMethod = model.Stock_250kDB.ResultsMethod
		if id:
			rm = ResultsMethod.get(id)
		else:
			call_method_id = request.params.getone('call_method_id')
			phenotype_method_id = request.params.getone('phenotype_method_id')
			analysis_method_id = request.params.getone('analysis_method_id')
			transformation_method_id = request.params.get('transformation_method_id', None)
			query = ResultsMethod.query.filter_by(call_method_id=call_method_id).\
				filter_by(phenotype_method_id=phenotype_method_id).filter_by(analysis_method_id=analysis_method_id)
			if transformation_method_id is not None:
				query = query.filter_by(transformation_method_id=transformation_method_id)
			rm = query.first()
		if rm is None:	#2010-9-28 this could still be nothing
			return None
		results_id = rm.id
		
		response.headers['Content-Type'] = 'application/json'
		
		if rm.analysis_method.min_maf is not None:
			min_MAF = rm.analysis_method.min_maf
		else:
			min_MAF = 0
		
		json_data = None
		no_of_top_snps = 10000
		ResultsMethodJson = model.Stock_250kDB.ResultsMethodJson
		rm_json = ResultsMethodJson.query.filter_by(results_id=results_id).\
				filter(ResultsMethodJson.min_MAF<=min_MAF+0.0001).filter(ResultsMethodJson.min_MAF>=min_MAF-0.0001).\
				filter_by(no_of_top_snps=no_of_top_snps).first()
		if rm_json:
			json_data =  rm_json.json_data.__str__()
		else:
			json_data = self.getOneResultJsonData(rm, min_MAF, no_of_top_snps)
			try:
				rm_json = ResultsMethodJson(results_id=rm.id, min_MAF=min_MAF, no_of_top_snps=no_of_top_snps)
				rm_json.json_data = json_data
				model.db.session.add(rm_json)
				model.db.session.flush()	#db is in no transaction. automatically commit. 
			except:
				loginfo = "DB Saving Error: json_data of result %s, min_MAF %s, no_of_top_snps %s\n"%(results_id, min_MAF, no_of_top_snps)
				loginfo += repr(sys.exc_info())
				loginfo += repr(traceback.print_exc())
				log.error(loginfo)
		if chromosome != None:
			new_json_data = {}
			json_data_dec = simplejson.loads(json_data)
			new_json_data['chr2data'] = json_data_dec['chr2data'][chromosome]
			new_json_data['chr2length'] = json_data_dec['chr2length'][chromosome]
			new_json_data['max_length'] = json_data_dec['max_length']
			new_json_data['max_value'] = json_data_dec['max_value']
			json_data = simplejson.dumps(new_json_data)
		return json_data
	
	def getCallMethodEigenValue(self):
		"""
		2009-2-2
			get an eigen value given a call_method_id
		"""
		call_method_id = request.params.getone('call_method_id')
		which_eigen_value = request.params.getone('which_eigen_value')
		rows = model.Stock_250kDB.CallMethodEigenValue.query.filter_by(call_method_id=call_method_id).filter_by(which_eigen=which_eigen).all()
	
	def getCallInfoData(self, call_method_id, phenotype_method_id):
		"""
		2009-5-17
			add region & country in the returning data structure
		2009-2-2
			get ecotype id, name, latitude, longitude, pc1, pc2, phenotype for all call_infos in one call_method_id
		"""
		#1st get call_info_id 2 PC12 mapping
		rows = model.Stock_250kDB.CallInfo.query.filter_by(method_id=call_method_id).all()
		call_info_id2pcs = {}
		for row in rows:
			call_info_id2pcs[row.id] = row.pc_value_ls[:2]
		
		
		#2nd check if model.pheno_data exists
		pheno_data = getattr(model, 'pheno_data', None)
		if pheno_data is None:
			from variation.src.OutputPhenotype import OutputPhenotype
			pheno_data = OutputPhenotype.getPhenotypeData(model.db.metadata.bind, phenotype_avg_table=model.Stock_250kDB.PhenotypeAvg.table.name,\
														phenotype_method_table=model.Stock_250kDB.PhenotypeMethod.table.name)
			model.pheno_data = pheno_data
		#3rd finally construct the full data and turn it into json
		column_name_type_ls = [("date", ("date", "Date")), ("ecotypeid", ("number", "Ecotype ID")), ("label", ("string","ID Name Phenotype")), \
							("lat",("number", "Latitude")), ("lon",("number", "Longitude")), ("name", ("string", "Native Name")), \
							("pc1", ("number","PC1")), ("pc2", ("number", "PC2")), ("phenotype", ("number", "Phenotype")),\
							("region", ("string", "Region")), ("country", ("string", "Country"))]
		
		description = dict(column_name_type_ls)
		rows = model.db.metadata.bind.execute("select * from view_call where call_method_id=%s"%(call_method_id))
		return_ls = []
		for row in rows:
			pc12 = call_info_id2pcs.get(row.call_info_id)
			if pc12:
				call_info_pc1, call_info_pc2 = pc12[:2]
				pc1 = call_info_pc1.pc_value
				pc2 = call_info_pc2.pc_value
			else:
				pc1 = pc2 = 0
			pheno_row_index = pheno_data.row_id2row_index.get(row.ecotype_id)
			pheno_col_index = pheno_data.col_id2col_index.get(int(phenotype_method_id))
			if pheno_row_index is not None and pheno_col_index is not None:
				phenotype_value = pheno_data.data_matrix[pheno_row_index][pheno_col_index]
				if phenotype_value == 'NA':
					phenotype_value = None
			else:
				phenotype_value = None	#numpy.nan can't be recognized by ToJSon()
			label = '%s ID:%s Phenotype:%s.'%(row.nativename, row.ecotype_id, phenotype_value)
			#label = 'ID:%s. Name=%s. Phenotype=%s.'%(row.ecotype_id, row.nativename, phenotype_value)
			return_ls.append(dict(ecotypeid=row.ecotype_id, name=row.nativename, lat=row.latitude, lon=row.longitude,\
								pc1=pc1, pc2=pc2, phenotype=phenotype_value, label=label, date=datetime.date(2009,2,3), \
								region=row.region, country=row.country))
		
		data_table = gviz_api.DataTable(description)
		data_table.LoadData(return_ls)
		column_ls = [row[0] for row in column_name_type_ls]
		#column_ls.sort()	#['date', 'ecotypeid', 'label', 'lat', 'lon', 'name', 'pc1', 'pc2', 'phenotype']
		json_result = data_table.ToJSon(columns_order=column_ls)	#ToJSonResponse only works with google.visualization.Query
		return json_result
	
	def fetchCallInfoData(self):
		"""
		2009-2-2
			get a complete catalog of call information given a call_method_id
			including, ecotypeid, name, latitude, longitude, phenotype value, PC1, PC2
		"""
		call_method_id = request.params.getone('call_method_id')
		phenotype_method_id = request.params.getone('phenotype_method_id')
		
		key_tup = (call_method_id, phenotype_method_id)
		CallPhenotypeMethod2call_info_jsdata = getattr(model, 'CallPhenotypeMethod2call_info_data', None)
		if CallPhenotypeMethod2call_info_jsdata is None:
			model.CallPhenotypeMethod2call_info_jsdata = {}
			CallPhenotypeMethod2call_info_jsdata = model.CallPhenotypeMethod2call_info_jsdata
		
		call_info_jsdata = CallPhenotypeMethod2call_info_jsdata.get(key_tup)
		if call_info_jsdata is None:
			call_info_jsdata = self.getCallInfoData(call_method_id, phenotype_method_id)
			CallPhenotypeMethod2call_info_jsdata[key_tup] = call_info_jsdata
		return call_info_jsdata
	
	def getPhenotypeHistImage(self, id=None, img_type='hist_thumb'):
		phenotype_method_id = request.params.get('phenotype_method_id', id)
		img_type = request.params.get('img_type', img_type)
		
		if phenotype_method_id is None:
			response.headers['Content-type'] = 'text/plain'
			return 'No image'
		phenotype_method_id = int(phenotype_method_id)
		
		img_type = request.params.get('img_type', img_type)
		if img_type.find('svg')!=-1:
			response.headers['Content-type'] = 'image/svg'
		else:
			response.headers['Content-type'] = 'image/png'
		
		get_img_data_success = 0
		if phenotype_method_id:
			row = model.Stock_250kDB.PhenotypeHistPlot.query.filter_by(phenotype_method_id=phenotype_method_id).first()
			if row:
				img_data = getattr(row, img_type, None)
				if img_data:
					img_data = img_data.__str__()	#2008-12-26	img_data is a buffer. weird!
					get_img_data_success = 1
				else:
					get_img_data_success = 0
		if not get_img_data_success:
			response.headers['Content-type'] = 'text/plain'
			img_data = 'No Image'
		return img_data
	
	def getCallPhenotypeQQImage(self, id=None, img_type='qq_thumb'):
		phenotype_method_id = request.params.get('phenotype_method_id', id)
		call_method_id = request.params.get('call_method_id', 29)
		img_type = request.params.get('img_type', img_type)
		
		if phenotype_method_id is None:
			response.headers['Content-type'] = 'text/plain'
			return 'No image'
		phenotype_method_id = int(phenotype_method_id)
		call_method_id = int(call_method_id)
		
		img_type = request.params.get('img_type', img_type)
		if img_type.find('svg')!=-1:
			response.headers['Content-type'] = 'image/svg'
		else:
			response.headers['Content-type'] = 'image/png'
		
		get_img_data_success = 0
		if call_method_id and phenotype_method_id:
			row = model.Stock_250kDB.CallPhenotypeQQPlot.query.filter_by(phenotype_method_id=phenotype_method_id).\
						filter_by(phenotype_method_id=phenotype_method_id).first()
			if row:
				img_data = getattr(row, img_type, None)
				if img_data:
					img_data = img_data.__str__()	#2008-12-26	img_data is a buffer. weird!
					get_img_data_success = 1
				else:
					get_img_data_success = 0
		if not get_img_data_success:
			response.headers['Content-type'] = 'text/plain'
			img_data = 'No Image'
		return img_data
	
	def getOneResultRaw(self, id=None):
		"""
		2010-3-10
			return the .tsv association result based on id in database.
		"""
		results_method_id = request.params.get('results_method_id', id)
		if results_method_id:
			rm = model.Stock_250kDB.ResultsMethod.get(results_method_id)
		else:
			call_method_id = request.params.get('call_method_id', config['app_conf']['published_call_method_id'])
			phenotype_method_id = request.params.get('phenotype_method_id', None)
			analysis_method_id = request.params.get('analysis_method_id', None)
			if call_method_id and phenotype_method_id and analysis_method_id:
				analysis_method_id = int(analysis_method_id)
				rm = model.Stock_250kDB.ResultsMethod.query.filter_by(phenotype_method_id=int(phenotype_method_id)).\
						filter_by(call_method_id=int(call_method_id)).filter_by(analysis_method_id=int(analysis_method_id)).first()
			else:
				rm = None
		
		if rm:
			fo = open(rm.filename, "rb")
			content = fo.read();
			# Close opend file
			fo.close()
			response.headers['Content-type'] = 'text/tsv'
			response.headers['Content-disposition'] = 'attachment; filename=gwas_%s_%s.tsv'%(rm.id, rm.short_name)
			return content
		else:
			response.headers['Content-type'] = 'text/plain'
			return 'No Association Result with id=%s, call_method_id=%s, phenotype_method_id=%s, analysis_method_id=%s'%\
					(results_method_id, call_method_id, phenotype_method_id, analysis_method_id)
	
	@jsonify
	def getGWASGivenPhenotypeIDJson(self, id=None):
		"""
		2010-3-16
			given a phenotype_method_id, get call_method_id(s) and analysis_method_id(s) of all available GWASs
			
		"""
		phenotype_method_id = request.params.get('phenotype_method_id', id)
		if phenotype_method_id:
			phenotype_method_id = int(phenotype_method_id)
			call_method_id2analysis_method_id_ls = {}
			call_method_id2label = {}
			analysis_method_id2label = {}
			rows = model.Stock_250kDB.ResultsMethod.query.filter_by(phenotype_method_id=phenotype_method_id)
			for row in rows:
				call_method_id = str(row.call_method_id)
				if row.analysis_method_id:
					analysis_method_id = str(row.analysis_method_id)
					if call_method_id  not in call_method_id2analysis_method_id_ls:
						call_method_id2analysis_method_id_ls[call_method_id] = []
						no_of_phenotyped_and_genotyped = hc.getNoOfAccessionsGivenPhenotypeAndCallMethodID(phenotype_method_id, row.call_method_id)
						call_method_id2label[call_method_id] = '%s genotyped in call %s-%s'%\
									(no_of_phenotyped_and_genotyped, row.call_method_id, row.call_method.short_name)
									
					if analysis_method_id not in call_method_id2analysis_method_id_ls[call_method_id]: 
						call_method_id2analysis_method_id_ls[call_method_id].append(analysis_method_id)
					if analysis_method_id not in analysis_method_id2label:
						analysis_method_id2label[analysis_method_id] = {}
					analysis_method_id2label[analysis_method_id][str(row.transformation_method_id)] = '%s %s'%(row.analysis_method_id, row.analysis_method.short_name if row.transformation_method_id == 1 else row.analysis_method.short_name + ' (%s)' % row.transformation_method.name)

		dc_to_return = {'call_method_id2analysis_method_id_ls': call_method_id2analysis_method_id_ls,\
					'call_method_id2label': call_method_id2label,\
					'analysis_method_id2label': analysis_method_id2label}
		return dc_to_return