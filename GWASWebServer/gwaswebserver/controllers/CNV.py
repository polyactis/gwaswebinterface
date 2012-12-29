import logging

from pylons import request, response, session, tmpl_context as c, url
from pylons.controllers.util import abort, redirect

from gwaswebserver.lib.base import BaseController, render, config, h, model
from pylons.decorators import jsonify
import simplejson, sys, traceback
from pymodule import algorithm, PassingData
from pymodule import SNP
from HelpOtherControllers import HelpothercontrollersController as hc
from variation.src.common import getEcotypeInfo
from DisplayResults import DisplayresultsController
from variation.src.plot.DrawSNPRegion import DrawSNPRegion
from pymodule import CNVCompare, CNVSegmentBinarySearchTreeKey
from pymodule import RBDict

log = logging.getLogger(__name__)

class CnvController(BaseController):

	def index(self):
		"""
		2010-9-17
		"""
		# Return a rendered template
		#return render('/CNV.mako')
		# or, return a string
		
		# cnv frequency etc.
		data_type_id_name_ls = [(1, 'Frequency'), (2, 'fractionNotCoveredByLyrata'), ]
		c.dataTypeOptionLsJson = hc.getPredefinedOptionListJson(option_id_name_ls=data_type_id_name_ls)
		c.cnvFrequencyCNVMethodLsJson = self.getCNVMethodLsJson(table_name=model.Stock_250kDB.CNV.table.name)
		c.getChromsomeLsJsonFromTableCNVURL = h.url(controller="CNV", action='getChromsomeLsJsonFromTableCNV')
		c.getFrequencyOverviewDataJsonFromTableCNVURL = h.url(controller="CNV", action='getFrequencyOverviewDataJsonFromTableCNV')
		
		c.getChromsomeLsJsonFromTableCNVCallURL = h.url(controller="CNV", action='getChromsomeLsJsonFromTableCNVCall')
		
		c.getGeneModelDataJsonURL = h.url(controller="CNV", action='getGeneModelDataJson')
		
		# haplotype data
		haplotypeDataTypeList = [(1, 'cnv_call'), (2, 'cnv_qc_call'), (3, "cnv_array_call")]
		c.haplotypeDataTypeListJson = hc.getPredefinedOptionListJson(option_id_name_ls=haplotypeDataTypeList, \
																	withIDInLabel=False, addZeroOption=True)
		c.cnvCallCNVMethodLsJson = self.getCNVMethodLsJson(table_name=model.Stock_250kDB.CNVCall.table.name)
		c.getCNVMethodLsJsonURL = h.url(controller="CNV", action="getCNVMethodLsJson")
		c.getCNVTypeLsJsonURL = h.url(controller="CNV", action="getCNVTypeLsJson")
		c.getHaplotypeDataJsonURL = h.url(controller="CNV", action='getHaplotypeDataJson')
		
		# probe position
		probeTypeList = [(1, 'Tiling Probe'), (2, "Good SNP Probe"), (3, "All SNP Probe")]
		c.probeTypeListJson = hc.getPredefinedOptionListJson(option_id_name_ls=probeTypeList, withIDInLabel=False, ) 
		c.getTilingProbeDataJsonURL = h.url(controller="CNV", action='getTilingProbeDataJson')
		
		# gwas
		c.gwasCNVMethodLsJson = self.getCNVMethodLsJson(table_name='results_method')
		c.callMethodLsJson = DisplayresultsController.getCallMethodLsJson()
		c.getPhenotypeMethodLsJsonURL = h.url(controller='DisplayResults', action='getPhenotypeMethodLsJson')
		c.getAnalysisMethodLsJsonURL = h.url(controller='DisplayResults', action='getAnalysisMethodLsJson')
		c.getGWASDataJsonURL = h.url(controller='CNV', action='getGWASDataJson')
		
		# common stuff
		smooth_type_id_name_ls = [(1, 'Top 1500'), (2, 'median over a window'), (3, 'maximum over a window'), \
								(4, 'weighted average (for segments)')]
		c.smoothTypeLsJson = hc.getPredefinedOptionListJson(option_id_name_ls=smooth_type_id_name_ls)
		plot_type_id_name_ls = [(1, 'As a new overview'), (2, 'As a child to the last overview'), (3, 'Overlay on the last overview')]
		c.plotTypeLsJson = hc.getPredefinedOptionListJson(option_id_name_ls=plot_type_id_name_ls)
		chromosome_id_name_ls = [(1, '1'), (2, '2'), (3, "3"), (4, "4"), (5, "5") ]
		c.chromosomeOptionLsJson = hc.getPredefinedOptionListJson(option_id_name_ls=chromosome_id_name_ls)
		
		return render('/CNV.html')
	
	@jsonify
	def getCNVMethodLsJson(self, table_name=None):
		"""
		2010-10-18
			get table_name from request.params in case it's called from the client
		2010-9-21
			add argument table_name, from which the cnv_method_id column is used to identify which rows of CNVMethod to pull.
		"""
		if table_name is None:
			table_name = request.params.get('table_name', model.Stock_250kDB.CNV.table.name)
		cnvMethodLs = []
		if table_name.find('Please')==-1:
			rows = model.db.metadata.bind.execute("select distinct p.cnv_method_id from %s p order by cnv_method_id"%\
												(table_name,))
			for row in rows:
				if row.cnv_method_id is not None:
					cnv_method = model.Stock_250kDB.CNVMethod.get(row.cnv_method_id)
					cnvMethodLs.append({'value': cnv_method.id, 'id':"%s: %s"%(cnv_method.id , cnv_method.short_name)})
		result = {'options': cnvMethodLs}
		result['options'].insert(0, {'id': u'Please Choose ...', 'value': 0})
		
		response.content_type = 'text/html'	#otherwise, the page rendered in index() will be regarded as "application/json"
		response.charset = 'utf-8'
		"""
		#2010-9-16
		this kind of error would happen if response is not set as above. pylons 1.0.
		
			Error - <type 'exceptions.AttributeError'>: You cannot access Response.unicode_body unless charset is set
		
		"""
		return result
	
	def getCNVTypeLsJson(self, table_name=None):
		"""
		2010-10-18
			get table_name from request.params in case it's called from the client
		"""
		if table_name is None:
			table_name = request.params.get('table_name', model.Stock_250kDB.CNVCall.table.name)
		cnv_method_id = request.params.get('cnv_method_id', None)
		
		if table_name =='cnv_call':
			CNVTableClass=model.Stock_250kDB.CNVCall
		elif table_name == 'cnv_qc_call':
			CNVTableClass=model.Stock_250kDB.CNVQCCall
		elif table_name == 'cnv_array_call':
			CNVTableClass=model.Stock_250kDB.CNV
		else:
			CNVTableClass=model.Stock_250kDB.CNVCall
		if cnv_method_id:
			extra_condition="s.cnv_method_id=%s"%cnv_method_id
		else:
			extra_condition = None
		
		type_info = model.db.getCNVMethodOrTypeInfoDataInCNVCallOrQC(TableClass=model.Stock_250kDB.CNVType, \
									CNVTableClass=CNVTableClass, \
									extra_condition=extra_condition)
		cnvTypeLs = []
		no_of_types = len(type_info.list_id_ls)
		for i in xrange(no_of_types):
			list_id = type_info.list_id_ls[i]
			list_label = type_info.list_label_ls[i]
			cnvTypeLs.append({'value': list_id, 'id':list_label})
		result = {'options': cnvTypeLs}
		result['options'].insert(0, {'id': u'Please Choose ...', 'value': 0})
		return simplejson.dumps(result)
	
	@classmethod
	def getChromsomeLs(cls, field_name='cnv_method_id', field_value=None, table_name=None):
		chromsomeLs = []
		if table_name is None:
			table_name = model.Stock_250kDB.CNVCall.table.name
		rows = model.db.metadata.bind.execute("select distinct p.chromosome from %s p where p.%s=%s order by chromosome"%\
											(table_name, field_name, field_value))
		for row in rows:
			if row.chromosome is not None:
				chromsomeLs.append([str(row.chromosome), row.chromosome])	#id (string), value (integer) 
		return chromsomeLs
	
	@jsonify
	def getChromsomeLsJsonFromTableCNVCall(self, ):
		cnv_method_id = request.params.get('cnv_method_id')
		result = {
				'options': [
						dict(id=id, value=value) for id, value in self.getChromsomeLs(field_value=cnv_method_id)
						]
				}
		#result['options'].append({'id': u'[At the end]', 'value': u''})
		result['options'].insert(0, {'id': u'Please Choose ...', 'value': 0})
		return result
	
	@jsonify
	def getChromsomeLsJsonFromTableCNV(self, ):
		cnv_method_id = request.params.get('cnv_method_id')
		result = {
				'options': [
						dict(id=id, value=value) for id, value in self.getChromsomeLs(field_value=cnv_method_id,
															table_name = model.Stock_250kDB.CNV.table.name)
						]
				}
		#result['options'].append({'id': u'[At the end]', 'value': u''})
		result['options'].insert(0, {'id': u'Please Choose ...', 'value': 0})
		return result
	
	#@jsonify
	def getGeneModelDataJson(self, ):
		"""
		2010-9-17
		"""
		chromosome = request.params.get('chromosome')
		start = request.params.get('start')
		stop = request.params.get('stop')
		geneModelDataLs = []
		if chromosome is None or start is None or stop is None:
			return geneModelDataLs
		chromosome = str(chromosome)	#model.gene_annotation stores chromosome in str format.
		start = float(start)	#int('103.4324') will throw ValueError: invalid literal for int() with base 10: 
		stop = float(stop)
		gene_id2model = model.gene_annotation.gene_id2model
		sys.stderr.write("Getting gene model data from chromosome %s, start %s, stop %s ... \n"%\
						(chromosome, start, stop))
		if not hasattr(model.gene_annotation, 'geneSpanRBDict'):
			sys.stderr.write("Constructing model.gene_annotation.geneSpanRBDict ...\n")
			model.gene_annotation.geneSpanRBDict = RBDict()
			geneSpanRBDict = model.gene_annotation.geneSpanRBDict
			for gene_id, gene_model in gene_id2model.iteritems():
				segmentKey = CNVSegmentBinarySearchTreeKey(chromosome=gene_model.chromosome, \
							span_ls=[gene_model.start, gene_model.stop], \
							min_reciprocal_overlap=1, strand=gene_model.strand, gene_id=gene_model.gene_id,\
							gene_start=gene_model.start, gene_stop=gene_model.stop)	#2010-8-17 any overlap is tolerated.
				if segmentKey in geneSpanRBDict:
					identical_position_gene_id = geneSpanRBDict[segmentKey][0]
					sys.stderr.write("gene %s has identical chr, start, stop (%s, %s, %s) with this gene %s.\n"%
									(identical_position_gene_id, gene_model.chromosome, gene_model.start, gene_model.stop, gene_id))
				else:
					geneSpanRBDict[segmentKey] = []
				geneSpanRBDict[segmentKey].append(gene_id)
			sys.stderr.write("model.gene_annotation.geneSpanRBDict construction done.\n")
		
		geneSpanRBDict = model.gene_annotation.geneSpanRBDict
		compareIns = CNVCompare(min_reciprocal_overlap=0.0000001)	#any overlap is an overlap
		segmentKey = CNVSegmentBinarySearchTreeKey(chromosome=chromosome, \
							span_ls=[start, stop], \
							min_reciprocal_overlap=0.0000001, )	#min_reciprocal_overlap doesn't matter here.
		# it's decided by compareIns.
		node_ls = []
		geneSpanRBDict.findNodes(segmentKey, node_ls=node_ls, compareIns=compareIns)
		start_stop_gene_id_ls = []	# a list to be sorted later
		for node in node_ls:
			geneSegKey = node.key
			gene_id_ls = node.value
			for gene_id in gene_id_ls:
				gene_model = gene_id2model.get(gene_id)
				start_stop_gene_id_ls.append((gene_model.start, gene_model.stop, gene_id))
		
		# 2010-9-26 sort it in chromosomal order
		start_stop_gene_id_ls.sort()
		for data in start_stop_gene_id_ls:
			gene_id = data[2]
			gene_model = gene_id2model.get(gene_id)
			if len(gene_model.gene_commentaries)==0:
				gene_commentaries = [gene_model]	#fake one here
			else:
				gene_commentaries = gene_model.gene_commentaries
			for gene_commentary in gene_commentaries:	#multiple commentary
				gene_desc_names = ['gene_symbol', 'type_of_gene', 'description', 'protein_label', 'protein_comment', 'protein_text', ]
				gene_desc_ls = DrawSNPRegion.returnGeneDescLs(gene_desc_names, gene_model, \
														gene_commentary=gene_commentary, cutoff_length=200, replaceNoneElemWithEmptyStr=1)
				import string
				local_gene_desc_names = map(string.upper, gene_desc_names)
				description = '.  '.join([': '.join(entry) for entry in zip(local_gene_desc_names, gene_desc_ls)])
				
				if getattr(gene_commentary, 'box_ls', None):
					box_ls = gene_commentary.box_ls
				elif gene_commentary.start and gene_commentary.stop:	#no box_ls, just use start, stop
					box_ls = [(gene_commentary.start, gene_commentary.stop, 'exon', 0)]	# last 0 denotes is_translated.
				elif gene_model.start and gene_model.stop:	#use gene_model's coordinate
					box_ls = [(gene_model.start, gene_model.stop, 'exon', 0)]
				else:
					continue	#ignore this
				
				geneModelDataLs.append(dict(gene_id=gene_id, chromosome=gene_model.chromosome, start=box_ls[0][0], \
						stop=box_ls[-1][1], box_ls=box_ls, \
						name=gene_model.gene_symbol, locustag=gene_model.locustag, \
						description=description, type_of_gene=gene_model.type_of_gene, strand=gene_model.strand))
			
		"""
		for chr, gene_id_ls in model.gene_annotation.chr_id2gene_id_ls.iteritems():
			if chr is None:
				continue
			if chr not in geneModelDataLs:
				geneModelDataLs[chr] = []
			for gene_id in gene_id_ls:
				gene_model = gene_id2model.get(gene_id)
				if gene_model.start is not None and gene_model.stop is not None: 
					if gene_model.gene_commentaries:
						description = gene_model.gene_commentaries[0].protein_label
					else:
						description = getattr(gene_model, 'description', '')
					geneModelDataLs[chr].append(dict(gene_id=gene_id, chromosome=chr, start=gene_model.start, stop=gene_model.stop, \
											name=gene_model.gene_symbol, locustag=gene_model.locustag, description=description, type_of_gene=gene_model.type_of_gene))
				GeneModel(gene_id=gene_id, chromosome=chromosome, gene_symbol=row.gene.gene_symbol,\
														locustag=row.gene.locustag, map_location=row.gene.map_location,\
														type_of_gene=row.entrezgene_type.type, type_id=row.entrezgene_type_id,\
														start=row.start, stop=row.stop, strand=row.strand, tax_id=row.tax_id,\
														description =row.gene.description)
				"""
		sys.stderr.write("%s gene models. Done.\n"%(len(geneModelDataLs),))
		return simplejson.dumps({'data':geneModelDataLs, 'overviewData':[]}, encoding='latin1')
	
	def getHaplotypeDataJson(self, data_type="CNVCall"):
		"""
		2010-10-18
			central controller for haplotype data
		"""
		table_name = request.params.get('table_name', model.Stock_250kDB.CNVCall.table.name)
		cnv_method_id = request.params.get('cnv_method_id')
		cnv_type_id = request.params.get('cnv_type_id')
		chromosome = request.params.get('chromosome')
		start = request.params.get('start')
		stop = request.params.get('stop')
		sys.stderr.write("Getting deletion haplotype data from cnv-method %s, cnv-type %s, chromosome %s, start %s, stop %s ... \n"%\
						(cnv_method_id, cnv_type_id, chromosome, start, stop))
		if table_name=='cnv_call':
			return self.getHaplotypeDataJsonFromTableCNVCall(cnv_method_id, cnv_type_id, chromosome, start, stop)
		elif table_name=='cnv_qc_call':
			return self.getHaplotypeDataJsonFromTableCNVQCCall(cnv_method_id, cnv_type_id, chromosome, start, stop)
		elif table_name=='cnv_array_call':
			return self.getHaplotypeDataJsonFromTableCNVArrayCall(cnv_method_id, cnv_type_id, chromosome, start, stop)
	
	@classmethod
	def getHaplotypeRowInfoDict(cls, ecotype_info, ecotype_id, row_name=None, row_id=None):
		"""
		2010-10-18
		"""
		ecotype_obj = ecotype_info.ecotype_id2ecotype_obj.get(ecotype_id)
		if ecotype_obj is None:
			latitude = None
			longitude = None
			description='%s %s'%(row_name, row_id)
		else:
			latitude = ecotype_obj.latitude
			longitude = ecotype_obj.longitude
			description='%s %s, e-id %s, %s, region %s %s'%(row_name, row_id, ecotype_obj.ecotype_id, ecotype_obj.nativename,\
												ecotype_obj.region, ecotype_obj.country)
		return dict(description=description, latitude=latitude, longitude=longitude)
	
	def getHaplotypeDataJsonFromTableCNVCall(self, cnv_method_id, cnv_type_id, chromosome, start, stop):
		"""
		2010-10-18
			deal with parameter cnv_type_id
		"""
		haplotypeData = []
		rowInfoData = {"rowInfoLs":[],
							"row_id2yValue": {}}
		if not getattr(h, 'ecotype_info', None):	#2009-3-6 not used right now
			h.ecotype_info = getEcotypeInfo(model.db)
		
		rows = model.db.metadata.bind.execute("select * from %s p where p.cnv_method_id=%s and p.cnv_type_id=%s \
				and p.chromosome=%s and p.stop>=%s and p.start<=%s order by chromosome, start"%\
				(model.Stock_250kDB.CNVCall.table.name, cnv_method_id, cnv_type_id, chromosome, start, stop))
		
		for row in rows:
			row_id = row.array_id
			if row_id not in rowInfoData["row_id2yValue"]:
				rowInfoData["row_id2yValue"][row_id] = len(rowInfoData["row_id2yValue"])
				
				array = model.Stock_250kDB.ArrayInfo.get(row_id)
				oneRowInfoDict = self.getHaplotypeRowInfoDict(h.ecotype_info, array.maternal_ecotype_id, row_name='array', row_id=row_id)
				rowInfoData["rowInfoLs"].append(oneRowInfoDict)
			size = row.stop - row.start + 1
			description = 'prob %s size %s no-of-probes %s amplitude %s fractionDeletedInPECoverageData %s'%(row.probability, size, \
															row.no_of_probes_covered, row.amplitude, row.fractionDeletedInPECoverageData)
			haplotypeData.append(dict(chromosome=row.chromosome, \
					start=row.start, stop=row.stop, \
					row_id=row_id, description = description))
		sys.stderr.write("%s deletions among %s arrays. Done.\n"%(len(haplotypeData), len(rowInfoData["rowInfoLs"])))
		result = {'data':haplotypeData, 'rowInfoData':rowInfoData, 'overviewData':[]}
		return simplejson.dumps(result, encoding='latin1')
	
	def getHaplotypeDataJsonFromTableCNVQCCall(self, cnv_method_id, cnv_type_id, chromosome, start, stop):
		"""
		2010-10-18
		"""
		haplotypeData = []
		rowInfoData = {"rowInfoLs":[],
							"row_id2yValue": {}}
		if not getattr(h, 'ecotype_info', None):	#2009-3-6 not used right now
			h.ecotype_info = getEcotypeInfo(model.db)
		
		rows = model.db.metadata.bind.execute("select * from %s p where p.cnv_method_id=%s and p.cnv_type_id=%s and p.chromosome=%s \
				 and (p.chromosome=p.stop_chromosome or p.stop_chromosome is null) and p.stop>=%s and p.start<=%s order by chromosome, start"%\
				(model.Stock_250kDB.CNVQCCall.table.name, cnv_method_id, cnv_type_id, chromosome, start, stop))
		
		for row in rows:
			row_id = row.accession_id
			if row_id not in rowInfoData["row_id2yValue"]:
				rowInfoData["row_id2yValue"][row_id] = len(rowInfoData["row_id2yValue"])
				
				db_obj = model.Stock_250kDB.CNVQCAccession.get(row_id)
				oneRowInfoDict = self.getHaplotypeRowInfoDict(h.ecotype_info, db_obj.ecotype_id, \
															row_name='accession', row_id=row_id)
				rowInfoData["rowInfoLs"].append(oneRowInfoDict)
			size = row.stop - row.start + 1
			description = 'score %s size %s no-of-probes %s'%(row.score, size, \
													row.no_of_probes_covered)
			haplotypeData.append(dict(chromosome=row.chromosome, \
					start=row.start, stop=row.stop, \
					row_id=row_id, description = description))
		sys.stderr.write("%s deletions among %s arrays. Done.\n"%(len(haplotypeData), len(rowInfoData["rowInfoLs"])))
		result = {'data':haplotypeData, 'rowInfoData':rowInfoData, 'overviewData':[]}
		return simplejson.dumps(result, encoding='latin1')
	
	def getHaplotypeDataJsonFromTableCNVArrayCall(self, cnv_method_id, cnv_type_id, chromosome, start, stop):
		"""
		2010-10-18
		"""
		haplotypeData = []
		rowInfoData = {"rowInfoLs":[],
					"row_id2yValue": {}}
		if not getattr(h, 'ecotype_info', None):	#2009-3-6 not used right now
			h.ecotype_info = getEcotypeInfo(model.db)
		
		Table = model.Stock_250kDB.CNV
		query = Table.query.filter_by(cnv_method_id=cnv_method_id).filter_by(cnv_type_id=cnv_type_id).\
			filter_by(chromosome=chromosome).filter(Table.start<=stop).filter(Table.stop>=start).\
			order_by(Table.chromosome).order_by(Table.start)
		#rows = model.db.metadata.bind.execute("select * from %s p where p.cnv_method_id=%s and p.cnv_type_id=%s \
		#		and p.chromosome=%s and p.stop>=%s and p.start<=%s order by chromosome, start"%\
		#		(model.Stock_250kDB.CNV.table.name, cnv_method_id, cnv_type_id, chromosome, start, stop))
		
		for row in query:
			for cnv_array_call in row.cnv_array_call_ls:
				row_id = cnv_array_call.array_id
				if row_id not in rowInfoData["row_id2yValue"]:
					rowInfoData["row_id2yValue"][row_id] = len(rowInfoData["row_id2yValue"])
					array = model.Stock_250kDB.ArrayInfo.get(row_id)
					oneRowInfoDict = self.getHaplotypeRowInfoDict(h.ecotype_info, array.maternal_ecotype_id, row_name='array', row_id=row_id)
					rowInfoData["rowInfoLs"].append(oneRowInfoDict)
				
				size = row.stop - row.start + 1
				description = 'score %s size %s no-of-probes %s frequency %s fractionNotCoveredByLyrata %s fractionDeletedInPECoverageData %s'%\
					(cnv_array_call.score, size, row.no_of_probes_covered, row.frequency, row.fractionNotCoveredByLyrata, \
					cnv_array_call.fractionDeletedInPECoverageData)
				haplotypeData.append(dict(chromosome=row.chromosome, \
					start=row.start, stop=row.stop, \
					row_id=row_id, description = description))
		sys.stderr.write("%s deletions among %s arrays. Done.\n"%(len(haplotypeData), len(rowInfoData["rowInfoLs"])))
		result = {'data':haplotypeData, 'rowInfoData':rowInfoData, 'overviewData':[]}
		return simplejson.dumps(result, encoding='latin1')
	
	@jsonify
	def getFrequencyOverviewDataJsonFromTableCNV(self, cnv_method_id=None, chromosome=None, smooth_type_id=3, no_of_overview_points=1500,\
												data_type_id=1):
		"""
		2010-9-23
			smooth_type_id: different ways to summarize data in fullData 50% overlapping windows.
					window size is calculated to make data points <= no_of_overview_points.
				1. top no_of_overview_points
				2. median over a window
				3. maximum over a window
				4. weighted average = \sum(yStart*(stop-start+1))/window_size
			
			data_type_id:
				1: frequency
				2: fractionNotCoveredByLyrata
		"""
		cnv_method_id = request.params.get('cnv_method_id', cnv_method_id)
		chromosome = request.params.get('chromosome', chromosome)
		smooth_type_id = request.params.get('smooth_type_id', None)
		data_type_id = request.params.get('data_type_id', 1)
		data_type_id = int(data_type_id)
		start = request.params.get('start', None)
		stop = request.params.get('stop', None)
		sys.stderr.write("Getting frequency data from cnv_method %s, chromosome %s, smooth type %s, data_type %s, start %s, stop %s ... \n"%\
						(cnv_method_id, chromosome, smooth_type_id, data_type_id, start, stop))
		array_id2row_index = {}
		where_sql = 'where p.cnv_method_id=%s and p.chromosome=%s '%(cnv_method_id, chromosome)
		if start:
			where_sql += ' and p.stop>=%s'%(start)
		if stop:
			where_sql += ' and p.start<=%s'%(stop)
		rows = model.db.metadata.bind.execute("select p.* from %s p %s order by chromosome, start"%\
											(model.Stock_250kDB.CNV.table.name, where_sql))
		fullData = []
		for row in rows:
			if data_type_id==2:
				yStart = row.fractionNotCoveredByLyrata
			else:
				yStart = row.frequency
			fullData.append(dict(start=row.start, yStart=yStart, stop=row.stop, chromosome=row.chromosome, \
				description='score: %s, no_of_probes: %s, size: %s'%(row.score, row.no_of_probes_covered, row.size_affected),))
		
		if smooth_type_id is not None:
			overviewData = algorithm.smoothFullData(fullData, smooth_type_id=int(smooth_type_id), \
												no_of_overview_points = int(no_of_overview_points))
		else:
			overviewData = []
		sys.stderr.write("%s fullData %s overviewData. Done.\n"%(len(fullData), len(overviewData)))
		return {"overviewData": overviewData, "data": fullData}
	
	@jsonify
	def getTilingProbeDataJson(self, chromosome=None, smooth_type_id=3, no_of_overview_points=1500, data_type=1):
		"""
		2010-10-17
			data_type:
				1: tiling probes
				2: SNP probes, ones included after QC
				3: all SNP probes
		
			smooth_type_id: different ways to summarize data in fullData 50% overlapping windows.
					window size is calculated to make data points <= no_of_overview_points.
				1. top no_of_overview_points
				2. median over a window
				3. maximum over a window
				4. weighted average = \sum(yStart*(stop-start+1))/window_size
			
		"""
		data_type = request.params.get('data_type', data_type)
		if data_type is None:
			data_type = 1
		else:
			data_type = int(data_type)
		chromosome = request.params.get('chromosome', chromosome)
		smooth_type_id = request.params.get('smooth_type_id', None)
		start = request.params.get('start', None)
		stop = request.params.get('stop', None)
		sys.stderr.write("Getting tiling probe position data from chromosome %s, smooth type %s, start %s, stop %s ... \n"%\
						(chromosome, smooth_type_id, start, stop))
		
		if data_type==2 or data_type==3:	# SNP probes
			where_sql = 'where p.chromosome is not null and p.position is not null and p.end_position is null '
			where_sql += ' and p.chromosome=%s '%(chromosome)
			if data_type==2:
				where_sql += ' and p.include_after_qc=1'
		else:
			where_sql = 'where p.snps_id is null and p.chromosome=%s and p.direction is not null and p.Tair9Copy=1 '%(chromosome)
		if start:
			where_sql += ' and p.position>=%s'%(start)
		if stop:
			where_sql += ' and p.position<=%s'%(stop)
		
		if data_type==2 or data_type==3:
			sql_sentence = "select distinct p.chromosome, p.position, p.tair8_chromosome, p.tair8_position \
				from %s p %s order by chromosome, position"%(model.Stock_250kDB.Snps.table.name, where_sql)
		else:
			sql_sentence = "select p.id, p.chromosome, p.position, p.tair8_chromosome, p.tair8_position \
				from %s p %s order by chromosome, position"%(model.Stock_250kDB.Probes.table.name, where_sql)
		rows = model.db.metadata.bind.execute(sql_sentence)
		fullData = []
		for row in rows:
			fullData.append(dict(start=row.position, yStart=1, stop=row.position, chromosome=row.chromosome, \
				description='tair8 chr, pos: %s, %s'%(row.tair8_chromosome,\
													row.tair8_position),))
		
		if smooth_type_id is not None:
			overviewData = algorithm.smoothFullData(fullData, smooth_type_id=int(smooth_type_id), \
												no_of_overview_points = int(no_of_overview_points))
		else:
			overviewData = []
		sys.stderr.write("%s fullData %s overviewData. Done.\n"%(len(fullData), len(overviewData)))
		return {"overviewData": overviewData, "data": fullData}
	
	@jsonify
	def getGWASDataJson(self, no_of_top_loci=10000, no_of_overview_points=1500,):
		"""
		2011-5-4
			bugfix: model.db.chr_pos2snp_id => model.db.snp_id2chr_pos
		2011-3-21
			finished. can handle ResultsMethod that have either call_method_id or cnv_method_id.
		2010-10-26
			unfinished
		"""
		rm = hc.getGWASResultsMethodGivenRequest(request)
		chromosome = request.params.get('chromosome', None)
		smooth_type_id = request.params.get('smooth_type_id', None)
		start = request.params.get('start', None)
		stop = request.params.get('stop', None)
		sys.stderr.write("Getting gwas data for result %s chromosome %s, smooth type %s, start %s, stop %s ... \n"%\
						(rm.id, chromosome, smooth_type_id, start, stop))
		
		#genomeRBDict = None
		pd = PassingData(min_MAF=0.1,\
					no_of_top_loci=no_of_top_loci, \
					starting_rank=0, \
					need_chr_pos_ls=0,\
					need_candidate_association=False,\
					chromosome=chromosome,\
					start=start,\
					stop=stop)
		
		if rm.call_method_id:
			pd.db_id2chr_pos = model.db.snp_id2chr_pos	#2011-5-4
		elif rm.cnv_method_id:
			if model.db._cnv_method_id!=rm.cnv_method_id:
				model.db.cnv_id2chr_pos = rm.cnv_method_id
			pd.db_id2chr_pos = model.db.cnv_id2chr_pos
		else:
			return "Error: ResultsMethod %s have neither call_method_id nor cnv_method_id.\n"%(rm.id)
		
		gwr = model.db.getResultMethodContent(rm.id, pdata=pd)
		
		data_obj_ls = []
		for i in range(min(no_of_top_loci, len(gwr))):	#2011-3-21 less than the max number it contains
			data_obj = gwr.get_data_obj_at_given_rank(i+1)
			data_obj_ls.append(data_obj)
		
		data_obj_ls.sort(cmp=SNP.cmpDataObjByChrPos)
		
		fullData = []
		for data_obj in data_obj_ls:
			fullData.append(dict(start=data_obj.position, yStart=data_obj.value, stop=data_obj.stop_position, \
					chromosome=data_obj.chromosome, \
					description='MAF: %s, MAC: %s'%(data_obj.maf, data_obj.mac),))
		
		if smooth_type_id is not None:
			overviewData = algorithm.smoothFullData(fullData, smooth_type_id=int(smooth_type_id), \
												no_of_overview_points = int(no_of_overview_points))
		else:
			overviewData = []
		sys.stderr.write("%s fullData %s overviewData. Done.\n"%(len(fullData), len(overviewData)))
		return {"overviewData": overviewData, "data": fullData}