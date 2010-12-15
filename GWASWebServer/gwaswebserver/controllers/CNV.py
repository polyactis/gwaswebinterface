import logging

from pylons import request, response, session, tmpl_context as c, url
from pylons.controllers.util import abort, redirect

from gwaswebserver.lib.base import BaseController, render, config, h, model
from pylons.decorators import jsonify
import simplejson, sys, traceback
from pymodule import algorithm

log = logging.getLogger(__name__)


class CnvController(BaseController):

	def index(self):
		"""
		2010-9-17
		"""
		# Return a rendered template
		#return render('/CNV.mako')
		# or, return a string
		c.cnvCallCNVMethodLsJson = self.getCNVMethodLsJson(table_name=model.Stock_250kDB.CNVCall.table.name)
		c.cnvFrequencyCNVMethodLsJson = self.getCNVMethodLsJson(table_name=model.Stock_250kDB.CNV.table.name)
		
		c.getChromsomeLsJsonFromTableCNVCallURL = h.url(controller="CNV", action='getChromsomeLsJsonFromTableCNVCall')
		c.getChromsomeLsJsonFromTableCNVURL = h.url(controller="CNV", action='getChromsomeLsJsonFromTableCNV')
		
		c.getFrequencyOverviewDataJsonFromTableCNVURL = h.url(controller="CNV", action='getFrequencyOverviewDataJsonFromTableCNV')
		c.getGeneModelDataJsonURL = h.url(controller="CNV", action='getGeneModelDataJson')
		c.getHaplotypeDataJsonFromTableCNVCallURL = h.url(controller="CNV", action='getHaplotypeDataJsonFromTableCNVCall')
		
		smooth_type_id_name_ls = [(1, 'Top 1500'), (2, 'median over a window'), (3, 'maximum over a window'), \
								(4, 'weighted average (for segments)')]
		c.smoothTypeLsJson = self.getPredefinedOptionListJson(option_id_name_ls=smooth_type_id_name_ls)
		plot_type_id_name_ls = [(1, 'As a new overview'), (2, 'As a child to the last overview'), (3, 'Overlay on the last overview')]
		c.plotTypeLsJson = self.getPredefinedOptionListJson(option_id_name_ls=plot_type_id_name_ls)
		return render('/CNV.html')
	
	@jsonify
	def getCNVMethodLsJson(self, table_name):
		"""
		2010-9-21
			add argument table_name
		"""
		cnvMethodLs = []
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
	
	@jsonify
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
		from pymodule.CNV import CNVCompare, CNVSegmentBinarySearchTreeKey
		from pymodule.RBTree import RBDict
		
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
			if gene_model.gene_commentaries:
				from variation.src.DrawSNPRegion import DrawSNPRegion
				gene_desc_names = ['gene_symbol', 'type_of_gene', 'description', 'protein_label', 'protein_comment', 'protein_text', ]
				gene_desc_ls = DrawSNPRegion.returnGeneDescLs(gene_desc_names, gene_model, \
														gene_commentary=None, cutoff_length=200, replaceNoneElemWithEmptyStr=1)
				import string
				local_gene_desc_names = map(string.upper, gene_desc_names)
				description = '.  '.join([': '.join(entry) for entry in zip(local_gene_desc_names, gene_desc_ls)])
			else:
				description = getattr(gene_model, 'description', '')
			geneModelDataLs.append(dict(gene_id=gene_id, chromosome=gene_model.chromosome, start=gene_model.start, stop=gene_model.stop, \
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
		return {'data':geneModelDataLs, 'overviewData':[]}
	
	@jsonify
	def getHaplotypeDataJsonFromTableCNVCall(self, ):
		cnv_method_id = request.params.get('cnv_method_id')
		chromosome = request.params.get('chromosome')
		start = request.params.get('start')
		stop = request.params.get('stop')
		deletionData = []
		rowInfoData = {"rowInfoLs":[],
							"row_id2yValue": {}}
		
		array_id2row_index = {}
		rows = model.db.metadata.bind.execute("select * from %s p where p.cnv_method_id=%s and p.chromosome=%s \
				and p.stop>=%s and p.start<=%s order by chromosome, start"%\
				(model.Stock_250kDB.CNVCall.table.name, cnv_method_id, chromosome, start, stop))
		for row in rows:
			array_id = row.array_id
			if array_id not in rowInfoData["row_id2yValue"]:
				rowInfoData["row_id2yValue"][array_id] = len(rowInfoData["row_id2yValue"])
				rowInfoData["rowInfoLs"].append(dict(description='array %s'%array_id))
			deletionData.append(dict(chromosome=row.chromosome, \
					start=row.start, stop=row.stop, \
					row_id=row.array_id, probability=row.probability, no_of_probes_covered=row.no_of_probes_covered,\
					median_intensity=row.median_intensity))
		
		result = {'data':deletionData, 'rowInfoData':rowInfoData, 'overviewData':[]}
		return result
	
	@jsonify
	def getFrequencyOverviewDataJsonFromTableCNV(self, cnv_method_id=None, chromosome=None, smooth_type_id=3, no_of_overview_points=1500):
		"""
		2010-9-23
			smooth_type_id: different ways to summarize data in fullData 50% overlapping windows.
					window size is calculated to make data points <= no_of_overview_points.
				1. top no_of_overview_points
				2. median over a window
				3. maximum over a window
				4. weighted average = \sum(yStart*(stop-start+1))/window_size
		"""
		cnv_method_id = request.params.get('cnv_method_id', cnv_method_id)
		chromosome = request.params.get('chromosome', chromosome)
		smooth_type_id = request.params.get('smooth_type_id', None)
		start = request.params.get('start', None)
		stop = request.params.get('stop', None)
		sys.stderr.write("Getting frequency data from cnv_method %s, chromosome %s, smooth type %s, start %s, stop %s ... \n"%\
						(cnv_method_id, chromosome, smooth_type_id, start, stop))
		array_id2row_index = {}
		where_sql = 'where p.cnv_method_id=%s and p.chromosome=%s '%(cnv_method_id, chromosome)
		if start:
			where_sql += ' and p.start>=%s'%(start)
		if stop:
			where_sql += ' and p.stop>=%s'%(stop)
		rows = model.db.metadata.bind.execute("select p.* from %s p %s order by chromosome, start"%\
											(model.Stock_250kDB.CNV.table.name, where_sql))
		fullData = []
		for row in rows:
			fullData.append(dict(start=row.start, yStart=row.frequency, stop=row.stop, chromosome=row.chromosome, \
					description='score: %s, no_of_probes: %s, size: %s'%(row.score, row.no_of_probes_covered, row.size_affected),))
		
		if smooth_type_id is not None:
			overviewData = algorithm.smoothFullData(fullData, smooth_type_id=int(smooth_type_id), \
												no_of_overview_points = int(no_of_overview_points))
		else:
			overviewData = []
		sys.stderr.write("Done.\n")
		return {"overviewData": overviewData, "data": fullData}
	
	@classmethod
	def getPredefinedOptionListJson(cls, option_id_name_ls=[]):
		"""
		2010-9-24
			option_id_name_ls is a list of (id, name) tuples
		"""
		smoothTypeLs = []
		for type_id, type_name in option_id_name_ls:
			smoothTypeLs.append({'value': type_id, 'id':"%s: %s"%(type_id , type_name)})
		result = {'options': smoothTypeLs}
		#result['options'].insert(0, {'id': u'Please Choose ...', 'value': 0})	# not necessary
		response.content_type = 'text/html'	#otherwise, the page rendered in index() will be regarded as "application/json"
		response.charset = 'utf-8'
		return simplejson.dumps(result)
	