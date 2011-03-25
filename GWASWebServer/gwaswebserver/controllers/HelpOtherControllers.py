import logging

from pylons import request, response, session, tmpl_context as c, config
from pylons.controllers.util import abort, redirect

from gwaswebserver.lib.base import BaseController, render, h
from gwaswebserver import model

import os,sys
sys.path.insert(0, os.path.join(os.path.expanduser('~/script')))

from variation.src.DrawSNPRegion import DrawSNPRegion
from variation.src.GeneListRankTest import GeneListRankTest
from variation.src.Kruskal_Wallis import Kruskal_Wallis
from pymodule import PassingData, SNPData
from sets import Set
import simplejson

log = logging.getLogger(__name__)


#2009-3-5 path to get ecotype X SNP matrix
SNPDatasetPath = '/Network/Data/250k/db/dataset/'

class HelpothercontrollersController(BaseController):
	"""
	2009-5-4
		controller that provides helper functions for other controllers
	"""
	def index(self):
		# Return a rendered template
		#   return render('/template.mako')
		# or, Return a response
		return 'Hello World'
	
	@classmethod
	def returnGeneDescLs(cls, gene_annotation, gene_id_ls=[]):
		"""
		2009-6-22
			fix a bug. db_user and other variables are now accessible from config['app_conf'], not model.xxx
		"""
		DrawSNPRegion_ins = DrawSNPRegion(db_user=config['app_conf']['db_user'], db_passwd=config['app_conf']['db_passwd'], hostname=config['app_conf']['hostname'],\
										database=config['app_conf']['dbname'],\
										input_fname='/tmp/dumb', output_dir='/tmp', debug=0)
		
		matrix_of_gene_descriptions = []
		for gene_id in gene_id_ls:
			gene_model = gene_annotation.gene_id2model.get(gene_id)
			if gene_model:
				if len(gene_model.gene_commentaries)==0:
					gene_commentaries = [gene_model]	#fake one here
				else:
					gene_commentaries = gene_model.gene_commentaries
				for gene_commentary in gene_commentaries:
					gene_desc_ls = DrawSNPRegion_ins.returnGeneDescLs(DrawSNPRegion_ins.gene_desc_names, gene_model, gene_commentary)
					matrix_of_gene_descriptions.append(gene_desc_ls)
		return matrix_of_gene_descriptions
	
	@classmethod
	def getCallMethodInfo(cls, affiliated_table_name, extra_condition=None, extra_tables=None):
		"""
		2009-1-30
			similar to getPhenotypeInfo, getListTypeInfo, getAnalysisMethodInfo
		"""
		table_str = '%s s, %s p'%(affiliated_table_name, model.Stock_250kDB.CallMethod.table.name)
		if extra_tables:
			table_str += ', %s'%extra_tables
		where_condition = 'p.id=s.call_method_id'
		if extra_condition:
			where_condition += ' and %s'%extra_condition
		rows = model.db.metadata.bind.execute("select distinct p.id, p.short_name from %s \
			where %s order by p.id"\
			%(table_str, where_condition))
		id_ls = []
		id2index = {}
		label_ls = []
		prev_biology_category_id = -1
		no_of_separators = 0
		for row in rows:
			id2index[row.id] = len(id_ls)
			id_ls.append(row.id)
			label_ls.append('%s %s'%(row.id, row.short_name))
		list_info = PassingData()
		list_info.id2index = id2index
		list_info.id_ls = id_ls
		list_info.label_ls = label_ls
		return list_info
	
	@classmethod
	def getPhenotypeInfo(cls, affiliated_table_name=None, extra_condition=None, extra_tables=None, with_category_separator=True):
		"""
		2009-12-1
			add argument with_category_separator
		2008-10-30
			affiliated_table_name becomes optional
		2008-10-19
			add option extra_tables
		2008-10-16
			sort phenotype by biology_category_id and return other info as well
		"""
		if affiliated_table_name:
			table_str = '%s s, %s p'%(affiliated_table_name, model.Stock_250kDB.PhenotypeMethod.table.name)
			where_condition = ['p.id=s.phenotype_method_id']
		else:
			table_str = '%s p'%(model.Stock_250kDB.PhenotypeMethod.table.name)
			where_condition = []
		
		if extra_tables:
			table_str += ', %s'%extra_tables
		if extra_condition:
			where_condition.append(extra_condition)
		
		if where_condition:	#2009-3-9
			where_condition = 'where ' + ' and '.join(where_condition)
		else:
			where_condition = ''
		
		rows = model.db.metadata.bind.execute("select distinct p.id, p.biology_category_id, p.short_name from %s\
				%s order by p.biology_category_id, p.id"%\
				(table_str, where_condition))
		phenotype_method_id_ls = []
		phenotype_method_id2index = {}
		phenotype_method_label_ls = []
		prev_biology_category_id = -1
		no_of_separators = 0
		for row in rows:
			if prev_biology_category_id == -1:
				prev_biology_category_id = row.biology_category_id
			elif with_category_separator and row.biology_category_id!=prev_biology_category_id:
				prev_biology_category_id = row.biology_category_id
				#add a blank phenotype id as separator
				no_of_separators += 1
				phenotype_method_id2index[-no_of_separators] = len(phenotype_method_id_ls)
				phenotype_method_id_ls.append(-no_of_separators)
				phenotype_method_label_ls.append('=====')
			phenotype_method_id2index[row.id] = len(phenotype_method_id_ls)
			phenotype_method_id_ls.append(row.id)
			phenotype_method_label_ls.append('%s %s'%(row.id, row.short_name))
		phenotype_info = PassingData()
		phenotype_info.phenotype_method_id2index = phenotype_method_id2index
		phenotype_info.phenotype_method_id_ls = phenotype_method_id_ls
		phenotype_info.phenotype_method_label_ls = phenotype_method_label_ls
		return phenotype_info
	
	@classmethod
	def getPhenotypeData(cls):
		"""
		2009-4-30
			get data from all the phenotypes into one matrix (accession by phenotype) 
		"""
		phenoData = getattr(model, 'phenoData', None)
		if phenoData is None:
			from variation.src.OutputPhenotype import OutputPhenotype
			phenoData = OutputPhenotype.getPhenotypeData(model.db.metadata.bind, phenotype_avg_table=model.Stock_250kDB.PhenotypeAvg.table.name,\
														phenotype_method_table=model.Stock_250kDB.PhenotypeMethod.table.name)
			model.phenoData = phenoData
		return phenoData
	
	@classmethod
	def getPhenotypeDataInSNPDataOrder(cls, snpData):
		"""
		2010-9-20
			fix a bug here.
			"phenoData_inSNPDataOrder" has to change according to snpData.row_id_ls.
			so add model.snpDataRowKey2phenoData_inSNPDataOrder.
		2009-4-30
			get data from all the phenotypes into one matrix (accession by phenotype) 
		"""
		if getattr(model, 'snpDataRowKey2phenoData_inSNPDataOrder', None) is None:
			model.snpDataRowKey2phenoData_inSNPDataOrder = {}
		
		key = tuple(snpData.row_id_ls)
		if key not in model.snpDataRowKey2phenoData_inSNPDataOrder:
			phenoData = cls.getPhenotypeData()
			phenoData_inSNPDataOrder = SNPData(col_id_ls = phenoData.col_id_ls, strain_acc_list=snpData.row_id_ls, \
											data_matrix=phenoData.data_matrix) #row label is that of the SNP matrix
			phenoData_inSNPDataOrder.col_label_ls = phenoData.col_label_ls
			phenotype_row_id_ls = map(str, phenoData.row_id_ls)	# phenoData.row_id_ls is a list of integer ecotype ids, need to convert
			phenoData_inSNPDataOrder.data_matrix = Kruskal_Wallis.get_phenotype_matrix_in_data_matrix_order(snpData.row_id_ls, \
																phenotype_row_id_ls, phenoData_inSNPDataOrder.data_matrix)
			model.snpDataRowKey2phenoData_inSNPDataOrder[key] = phenoData_inSNPDataOrder
		return model.snpDataRowKey2phenoData_inSNPDataOrder[key]
	
	@classmethod
	def getListTypeInfo(cls, affiliated_table_name=None, extra_condition=None, extra_tables=None):
		"""
		2009-3-9
			handle the case in which there is no the where_condition at all.
		2008-10-30
			affiliated_table_name becomes optional
		2008-10-19
			add option extra_tables
		2008-10-16
			sort gene list type by biology_category_id and return other info as well
			add -1 as a separator into list_type_id_ls
		"""
		if affiliated_table_name:
			table_str = '%s s, %s p'%(affiliated_table_name, model.Stock_250kDB.GeneListType.table.name)
			where_condition = ['p.id=s.list_type_id']
		else:
			table_str = '%s p'%(model.Stock_250kDB.GeneListType.table.name)
			where_condition = []
		
		if extra_tables:
			table_str += ', %s'%extra_tables
		
		if extra_condition:
			where_condition.append(extra_condition)
		
		if where_condition:	#2009-3-9
			where_condition = 'where ' + ' and '.join(where_condition)
		else:
			where_condition = ''
		rows = model.db.metadata.bind.execute("select distinct p.id, p.biology_category_id, p.short_name from %s \
			%s order by p.biology_category_id, p.id"\
			%(table_str, where_condition))
		list_type_id_ls = []
		list_type_id2index = {}
		list_type_label_ls = []
		prev_biology_category_id = -1
		no_of_separators = 0
		for row in rows:
			if prev_biology_category_id == -1:
				prev_biology_category_id = row.biology_category_id
			elif row.biology_category_id!=prev_biology_category_id:
				prev_biology_category_id = row.biology_category_id
				no_of_separators += 1
				list_type_id2index[-no_of_separators] = len(list_type_id_ls)
				list_type_id_ls.append(-no_of_separators)
				list_type_label_ls.append('====\n====')
			list_type_id2index[row.id] = len(list_type_id_ls)
			list_type_id_ls.append(row.id)
			list_type_label_ls.append('%s %s'%(row.id, row.short_name))
		list_info = PassingData()
		list_info.list_type_id2index = list_type_id2index
		list_info.list_type_id_ls = list_type_id_ls
		list_info.list_type_label_ls = list_type_label_ls
		return list_info
	
	@classmethod
	def getAnalysisMethodInfo(cls, affiliated_table_name, extra_condition=None, extra_tables=None):
		"""
		2011-3-21
			a new way of handling transformation_method_id, which doesn't require every ResultsMethod entry
				having a non-null transformation_method_id.
		2009-5-20
			label no longer includes db id
		2009-4-26
			add description_ls in return
		2008-10-19
		"""
		table_str = '%s s, %s p'%(affiliated_table_name, model.Stock_250kDB.AnalysisMethod.table.name)
			#,model.Stock_250kDB.TransformationMethod.table.name)
		if extra_tables:
			table_str += ', %s'%extra_tables
		where_condition = ' p.id=s.analysis_method_id'
		if extra_condition:
			where_condition += ' and %s'%extra_condition
		
		rows = model.db.metadata.bind.execute("select distinct p.id, p.short_name, p.method_description, \
			s.transformation_method_id, s.pseudo_heritability from %s \
			where %s order by p.id"\
			%(table_str, where_condition))
		id_ls = []
		id2index = {}
		label_ls = []
		description_ls = []
		pseudoHeritability_ls = []
		prev_biology_category_id = -1
		no_of_separators = 0
		for row in rows:
			id2index[row.id] = len(id_ls)
			id_ls.append(row.id)
			if row.transformation_method_id:
				tm = model.Stock_250kDB.TransformationMethod.get(row.transformation_method_id)
			else:
				tm = None
			if tm and tm.id != 1:
				tm_description = ' (%s)' % tm.name
			else:
				tm_description = ''
			label_ls.append('%s'%(row.short_name + tm_description))
			description_ls.append(row.method_description)
			pseudoHeritability_ls.append(row.pseudo_heritability)
			
		list_info = PassingData()
		list_info.id2index = id2index
		list_info.id_ls = id_ls
		list_info.label_ls = label_ls
		list_info.description_ls = description_ls
		list_info.pseudoHeritability_ls = pseudoHeritability_ls
		return list_info
	
	@classmethod
	def getAssociationOverlappingTypeInfo(cls, affiliated_table_name=None, extra_condition=None, extra_tables=None):
		"""
		2009-11-30
		"""
		if affiliated_table_name:
			table_str = '%s s, %s p'%(affiliated_table_name, model.Stock_250kDB.AssociationOverlappingType.table.name)
			where_condition = ['p.id=s.overlapping_type_id']
		else:
			table_str = '%s p'%(model.Stock_250kDB.AssociationOverlappingType.table.name)
			where_condition = []
		
		if extra_tables:
			table_str += ', %s'%extra_tables
		
		if extra_condition:
			where_condition.append(extra_condition)
		
		if where_condition:	#2009-3-9
			where_condition = 'where ' + ' and '.join(where_condition)
		else:
			where_condition = ''
		rows = model.db.metadata.bind.execute("select distinct p.id, p.short_name, p.description from %s \
			%s order by p.no_of_methods, p.short_name"\
			%(table_str, where_condition))
		list_type_id_ls = []
		list_type_id2index = {}
		list_type_label_ls = []
		no_of_separators = 0
		for row in rows:
			list_type_id2index[row.id] = len(list_type_id_ls)
			list_type_id_ls.append(row.id)
			list_type_label_ls.append('%s %s'%(row.short_name, row.description))
		list_info = PassingData()
		list_info.list_type_id2index = list_type_id2index
		list_info.list_type_id_ls = list_type_id_ls
		list_info.list_type_label_ls = list_type_label_ls
		return list_info
	
	@classmethod
	def getSNPDataGivenCallMethodID(cls, call_method_id):
		"""
		2009-4-30
			
		2009-3-5
			read a 250k dataset from SNPDatasetPath
		"""
		call_method_id = int(call_method_id)
		if getattr(model, 'call_method_id2dataset', None) is None:
			model.call_method_id2dataset = {}
		if call_method_id not in model.call_method_id2dataset:
			#datasetPath = os.path.join(SNPDatasetPath, 'call_method_%s.tsv'%call_method_id)
			cm = model.Stock_250kDB.CallMethod.get(call_method_id)
			snpData = SNPData(input_fname=cm.filename, turn_into_array=1, ignore_2nd_column=1)	#use 1st column (ecotype id) as main ID
			model.call_method_id2dataset[call_method_id] = snpData
		return model.call_method_id2dataset[call_method_id]
	
	@classmethod
	def getNoOfAccessionsGivenPhenotypeMethodID(cls, phenotype_method_id):
		"""
		2009-11-17
			updated to use model.PhenotypeMethodID2ecotype_id_set
		2009-7-30
			return the number of accessions affliated with one phenotype method id
		"""
		
		"""
		# 2009-11-17 commented out
		if phenotype_method_id not in model.PhenotypeMethodID2no_of_accessions:			
			rows = model.db.metadata.bind.execute("select method_id, count(distinct ecotype_id) as cnt from %s where method_id=%s group by method_id"%\
												(model.Stock_250kDB.PhenotypeAvg.table.name, phenotype_method_id))
			for row in rows:
				model.PhenotypeMethodID2no_of_accessions[row.method_id] = row.cnt
		"""
		if phenotype_method_id not in model.PhenotypeMethodID2ecotype_id_set:	# no ecotype_id_set for this phenotype cuz it doesn't have phenotype data
			model.PhenotypeMethodID2ecotype_id_set[phenotype_method_id] = set()
		return len(model.PhenotypeMethodID2ecotype_id_set[phenotype_method_id])
	
	@classmethod
	def getNoOfAccessionsGivenPhenotypeAndCallMethodID(cls, phenotype_method_id, call_method_id):
		"""
		2009-11-17
			return the number of accessions intersected by a given call method and phenotype method
		"""
		dict_key = (phenotype_method_id, call_method_id)
		if dict_key not in model.PhenotypeAndCallMethodID2ecotype_id_set:
			if phenotype_method_id not in model.PhenotypeMethodID2ecotype_id_set:	# no ecotype_id_set for this phenotype cuz it doesn't have phenotype data
				model.PhenotypeMethodID2ecotype_id_set[phenotype_method_id] = set()
			if call_method_id not in model.CallMethodID2ecotype_id_set:
				model.CallMethodID2ecotype_id_set[call_method_id] = set()
			model.PhenotypeAndCallMethodID2ecotype_id_set[dict_key] = model.CallMethodID2ecotype_id_set[call_method_id] & \
					model.PhenotypeMethodID2ecotype_id_set[phenotype_method_id]
		return len(model.PhenotypeAndCallMethodID2ecotype_id_set[dict_key])
	
	
	@classmethod
	def getTemporaryGWASResults(cls):
		user = h.user()
		#if user is None: 
			#return None
		dirname = "/Network/Data/250k/tmp-data/gwas"
		return cls.getFilesFromDirectory(dirname)
	
	@classmethod
	def	getHeatMapFiles(cls):
		dirname = "/Network/Data/250k/tmp-data/heatmap"
		files =  cls.getFilesFromDirectory(dirname)
		filtered_files = []
		for file in files['files']:
			try:
				parts = file.split("_")
				if parts[1] !=  '':
					phenotype = model.Stock_250kDB.PhenotypeMethod.get_by(id=parts[1])
					if phenotype.checkACL(h.user()):
						filtered_files.append(file)
			except Exception:
				filtered_files.append(file)
		files['files'] = filtered_files
		return files
		
		
	
	@classmethod 
	def getFilesFromDirectory(cls,dirname):
		files =[]
		files = [f for f in os.listdir(dirname)  if os.path.isfile(os.path.join(dirname, f))]
		result = {"path":dirname,"files":files}
		return result
	
	@classmethod
	def getPredefinedOptionListJson(cls, option_id_name_ls=[], withIDInLabel=True, addZeroOption=False):
		"""
		2010-10-19
			add argument withIDInLabel, 
		2010-9-24
			option_id_name_ls is a list of (id, name) tuples
		"""
		smoothTypeLs = []
		for type_id, type_name in option_id_name_ls:
			if withIDInLabel:
				type_label = "%s: %s"%(type_id , type_name)
			else:
				type_label = type_name
			smoothTypeLs.append({'value': type_id, 'id':type_label})
		if addZeroOption:
			smoothTypeLs.insert(0, {'id': u'Please Choose ...', 'value': 0})
		result = {'options': smoothTypeLs}
		#result['options'].insert(0, {'id': u'Please Choose ...', 'value': 0})	# not necessary
		response.content_type = 'text/html'	#otherwise, the page rendered in index() will be regarded as "application/json"
		response.charset = 'utf-8'
		return simplejson.dumps(result)
	
	@classmethod
	def getGWASResultsMethodGivenRequest(cls, request, id=None):
		"""
		2011-3-21
			add code to deal with CNV association, which are tagged by cnv_method_id
		2010-11-8
			fix a bug: transformation_method_id could be 'None' (a string).
		2010-10-26
		"""
		if id is None:
			id = request.params.get('id', None)
		if id is None:
			id = request.params.get('results_id', None)
		
		ResultsMethod = model.Stock_250kDB.ResultsMethod
		if id:
			rm = ResultsMethod.get(id)
		else:
			cnv_method_id = request.params.getone('cnv_method_id')
			call_method_id = request.params.getone('call_method_id')
			phenotype_method_id = request.params.getone('phenotype_method_id')
			analysis_method_id = request.params.getone('analysis_method_id')
			transformation_method_id = request.params.get('transformation_method_id', None)
			query = ResultsMethod.query.filter_by(phenotype_method_id=phenotype_method_id).\
						filter_by(analysis_method_id=analysis_method_id)
			if call_method_id and call_method_id!='None' and call_method_id!='0':	#2011-3-21
				query = query.filter_by(call_method_id=call_method_id)
			elif cnv_method_id and cnv_method_id!='None' and cnv_method_id!='0':	#2011-3-21
				query = query.filter_by(cnv_method_id=cnv_method_id)
			
			if transformation_method_id and transformation_method_id!='None' and transformation_method_id!='0':	#2011-3-21
				query = query.filter_by(transformation_method_id=transformation_method_id)
			rm = query.first()
		return rm