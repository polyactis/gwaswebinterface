import logging

from pylons import request, response, session, tmpl_context as c
from pylons.controllers.util import abort, redirect

from gwaswebserver.lib.base import *	#BaseController, render, h, config, os , etc.
from gwaswebserver import model

from DisplayResults import DisplayresultsController
from DisplayResultsGene import DisplayresultsgeneController
from variation.src.common import getEcotypeInfo
from variation.src.GeneListRankTest import GeneListRankTest
from variation.src.DrawSNPRegion import DrawSNPRegion, SNPPassingData
from HelpOtherControllers import HelpothercontrollersController as hc
from DisplayTopSNPTestRM import DisplaytopsnptestrmController as DTSTR

log = logging.getLogger(__name__)

class HaplotypeviewController(BaseController):

	def index(self):
		#c.call_method_ls = DisplayresultsController.getCallMethodLsJson()		
		c.callMethodLsURL = h.url(controller="DisplayResults", action="getCallMethodLsJson")
		#c.gene_list_ls = DisplayresultsgeneController.getGeneListTypeLsGivenTypeAndPhenotypeMethodAndAnalysisMethodJson()
		c.geneListLsURL = h.url(controller="DisplayResultsGene", action="getGeneListTypeLsGivenTypeAndPhenotypeMethodAndAnalysisMethodJson")
		c.callMethodOnChangeURL = h.url(controller="DisplayResults", action="getPhenotypeMethodLsJson")
		c.haplotypeImgURL = h.url(controller='HaplotypeView', action='getPlot')
		return render('/HaplotypeView.html')
	
	def getPlot(self):
		"""
		2010-4-14
			output the figure to a file, rather than to StringIO.
				The old way through cache in a dictionary affiliated with "model" doesn't pan out well.
					A 2nd request from the user for the same haplotype (save image as, or view image, etc.)
					invokes one more getPlot() call.
			 	The file-way would just check if the file exists or not first.
		2010-1-22
			add argument call_method_id when calling DrawSNPRegion.drawRegionAroundThisSNP()
		2009-4-30
		"""
		chromosome = int(request.params.get('chromosome', 1))
		start = int(request.params.get('start', 1))
		stop = int(request.params.get('stop', 10))
		if start>stop:
			start, stop = stop, start
		snps_id = '%s_%s'%(chromosome, start)
		
		call_method_id = int(request.params.get('call_method_id'))
		phenotype_method_id = int(request.params.get('phenotype_method_id'))
		list_type_id = int(request.params.get('list_type_id', None))
		
		plot_key = (chromosome, start, stop, call_method_id, phenotype_method_id, list_type_id)
		
		#2010-4-14
		plot_key_str = map(str, plot_key)
		haplotype_plot_fname_prefix = 'Haplotype_%s'%('_'.join(plot_key_str))
		output_fname_prefix = str(os.path.join(config['app_conf']['plots_store'], haplotype_plot_fname_prefix))
		#str() to avoid "TypeError: cannot return std::string from Unicode object"
		output_fname = '%s.png'%output_fname_prefix
		if os.path.isfile(output_fname):	#2010-4-14 check if the plot is there already.
			return DTSTR.getImage(output_fname)
		
		if not getattr(model, 'key2analysis_method_id2gwr', None):
			model.key2analysis_method_id2gwr = {}
		key = (call_method_id, phenotype_method_id)
		analysis_method_id2gwr = model.key2analysis_method_id2gwr.get(key)
		if not analysis_method_id2gwr:
			analysis_method_id2gwr = DrawSNPRegion.getSimilarGWResultsGivenResultsByGene(phenotype_method_id, call_method_id)
			model.key2analysis_method_id2gwr[key] = analysis_method_id2gwr
		
		if list_type_id>0:	#2009-2-22
			candidate_gene_set = GeneListRankTest.dealWithCandidateGeneList(list_type_id, return_set=True)
		else:
			candidate_gene_set = set()
		gene_annotation = model.gene_annotation
		
		snpData = hc.getSNPDataGivenCallMethodID(call_method_id)
		pheno_data = hc.getPhenotypeDataInSNPDataOrder(snpData)
		
		if h.ecotype_info is None:	#2009-3-6 not used right now
			h.ecotype_info = getEcotypeInfo(model.db)
		
		snp_info = getattr(model, 'snp_info', None)
		if snp_info is None:
			snp_info = DrawSNPRegion.getSNPInfo(model.db)
			model.snp_info = snp_info

		LD_info = None
		this_snp = SNPPassingData(chromosome=chromosome, position=start, stop=stop, snps_id='%s_%s'%(chromosome, start))
		
		DrawSNPRegion.construct_chr_pos2index_forSNPData(snpData)	#prerequisite
		
		# 2010-4-14 manually set the font size small to 6
		from pymodule import yh_matplotlib
		yh_matplotlib.setFontAndLabelSize(6)
		
		after_plot_data = DrawSNPRegion.drawRegionAroundThisSNP(phenotype_method_id, this_snp, candidate_gene_set, \
													gene_annotation, snp_info, \
								analysis_method_id2gwr, LD_info, output_dir=output_fname_prefix, which_LD_statistic=1, \
								min_distance=20000, list_type_id=list_type_id,
								label_gene=True, \
								draw_LD_relative_to_center_SNP=False,\
								commit=False, snpData=snpData, phenData=pheno_data, \
								ecotype_info=h.ecotype_info, snpData_before_impute=None,\
								snp_matrix_data_type=1, call_method_id=call_method_id)
		yh_matplotlib.restoreMatplotlibRCDefaults()
		return DTSTR.getImage(output_fname)