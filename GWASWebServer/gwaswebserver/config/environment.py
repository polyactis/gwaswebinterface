"""Pylons environment configuration"""
import os, sys

from mako.lookup import TemplateLookup
from pylons.configuration import PylonsConfig
from pylons.error import handle_mako_error

import gwaswebserver.lib.app_globals as app_globals
import gwaswebserver.lib.helpers
from gwaswebserver.config.routing import make_map

import gwaswebserver.model as model


def load_environment(global_conf, app_conf):
	"""Configure the Pylons environment via the ``pylons.config``
	object
	"""
	config = PylonsConfig()
	
	# Pylons paths
	root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
	paths = dict(root=root,
				 controllers=os.path.join(root, 'controllers'),
				 static_files=os.path.join(root, 'public'),
				 templates=[os.path.join(root, 'templates')])

	# Initialize config with the basic options
	config.init_app(global_conf, app_conf, package='gwaswebserver', paths=paths)

	config['routes.map'] = make_map(config)
	config['pylons.app_globals'] = app_globals.Globals(config)
	config['pylons.h'] = gwaswebserver.lib.helpers

	# Setup cache object as early as possible
	import pylons
	pylons.cache._push_object(config['pylons.app_globals'].cache)
	

	# Create the Mako TemplateLookup, with the default auto-escaping
	config['pylons.app_globals'].mako_lookup = TemplateLookup(
		directories=paths['templates'],
		error_handler=handle_mako_error,
		module_directory=os.path.join(app_conf['cache_dir'], 'templates'),
		input_encoding='utf-8', default_filters=['escape'],
		imports=['from webhelpers.html import escape'])

	# CONFIGURATION OPTIONS HERE (note: all config options will override
	# any Pylons config options)

	

	#2008-10-05 setup the database connection
	drivername = config['app_conf']['drivername']
	hostname = config['app_conf']['hostname']
	dbname = config['app_conf']['dbname']
	schema = config['app_conf']['schema']
	db_user = config['app_conf']['db_user']
	db_passwd = config['app_conf']['db_passwd']
	pool_recycle = int(config['app_conf']['pool_recycle'])
	sql_echo = False
	if ('sql_echo' in config['app_conf'] and config['app_conf']['sql_echo'] == 'True'):
		sql_echo = True
	if config['app_conf'].get('echo_pool', False)=='True':	#watch:  bool('False')= True
		echo_pool = True	#2010-9-20 to enable monitoring of the pool
	else:
		echo_pool = False
	#model.setup()
	
	"""
	#2010-9-19 Set up a specific logger with our desired output level
	import logging
	import logging.handlers
	#logging.basicConfig()
	LOG_FILENAME = '/tmp/sqlalchemy_pool_log.out'
	my_logger = logging.getLogger('sqlalchemy.pool')
	
	# Add the log message handler to the logger
	handler = logging.handlers.RotatingFileHandler(LOG_FILENAME, maxBytes=1000000, backupCount=5)
	# create formatter
	formatter = logging.Formatter("%(asctime)s - %(name)s - %(levelname)s - %(message)s")
	# add formatter to handler
	handler.setFormatter(formatter)
	my_logger.addHandler(handler)
	my_logger.setLevel(logging.DEBUG)
	"""
	
	model.db = model.Stock_250kDB.Stock_250kDB(drivername=drivername, username=db_user, password=db_passwd, \
						hostname=hostname, database=dbname, schema=schema, pool_recycle=pool_recycle, \
						sql_echo=sql_echo, echo_pool= echo_pool)
	
	model.db.setup(create_tables=False)

	model.genome_db = model.GenomeDB.GenomeDatabase(drivername=drivername, username=db_user, password=db_passwd, \
				hostname=hostname, database='genome_tair10', schema=schema, pool_recycle=pool_recycle)

	#from variation.src import dbsnp
	#snp_db = dbsnp.DBSNP(drivername=drivername, username=db_user, password=db_passwd, \
	#				hostname=hostname, database='dbsnp', schema=schema, pool_recycle=pool_recycle)

	#from variation.src import StockDB
	model.stock_db = model.StockDB.StockDB(drivername=drivername, username=db_user, password=db_passwd, \
				hostname=hostname, database='stock', schema=schema, pool_recycle=pool_recycle)

	model.at_db = model.AtDB.AtDB(drivername=drivername, username=db_user, password=db_passwd, \
				hostname=hostname, database='at', schema=schema, pool_recycle=pool_recycle)
	"""
	for entity in entities:
		if entity.__module__==db.__module__:	#entity in the same module
			entity.metadata = metadata
			#using_table_options_handler(entity, schema=self.schema)
	"""
	model.genome_db.setup(create_tables=False)
	#snp_db.setup(create_tables=False)
	model.stock_db.setup(create_tables=False)
	model.at_db.setup(create_tables=False)
	config['pylons.strict_tmpl_context'] = False
	from variation.src.DrawSNPRegion import DrawSNPRegion
	def dealWithGeneAnnotation():
		gene_annotation_picklef = '/Network/Data/250k/tmp-yh/at_gene_model_pickelf'
		DrawSNPRegion_ins = DrawSNPRegion(db_user=db_user, db_passwd=db_passwd, hostname=hostname, database=dbname,\
									input_fname='/tmp/dumb', output_dir='/tmp', debug=0)
		gene_annotation = DrawSNPRegion_ins.dealWithGeneAnnotation(gene_annotation_picklef, cls_with_db_args=DrawSNPRegion_ins)
		return gene_annotation
	model.gene_annotation = dealWithGeneAnnotation()
	return config
	
	#2008-11-05 a dictionary to link two type-tables in order to cross-link pages of DisplayTopSNPTestRM and ScoreRankHistogram/DisplayResultsGene
	sys.stderr.write("Getting a map between CandidateGeneTopSNPTestRMType id and ScoreRankHistogramType id ... ")
	model.CandidateGeneTopSNPTestRMType_id_min_distance2ScoreRankHistogramType_id = {}
	ScoreRankHistogramType = model.Stock_250kDB.ScoreRankHistogramType
	CandidateGeneTopSNPTestRMType = model.Stock_250kDB.CandidateGeneTopSNPTestRMType
	rows = model.db.metadata.bind.execute("select s.id as sid, s.min_distance, s.call_method_id, c.id as cid from %s s, %s c where \
				s.get_closest=c.get_closest and s.min_MAF=c.min_MAF and \
				s.allow_two_sample_overlapping=c.allow_two_sample_overlapping and \
				s.null_distribution_type_id=1 and s.results_type=1"%\
				(ScoreRankHistogramType.table.name, CandidateGeneTopSNPTestRMType.table.name))
				# 2008-1-8 temporarily set call_method_id=17 cuz CandidateGeneTopSNPTestRMType doesn't include call_method_id
				# 2010-2-25 remove "s.null_distribution_type_id=c.null_distribution_type_id and  s.results_type=c.results_type"
				#	and set s.null_distribution_type_id=1 and s.results_type=1
				#	because null_distribution_type_id=2/3 or results_type=3 (2 means different but deprecated) in 
				#	CandidateGeneTopSNPTestRMType doesn't matter	for DisplayResultsGene.
	for row in rows:
		key_tuple = (row.cid, row.min_distance, row.call_method_id)
		model.CandidateGeneTopSNPTestRMType_id_min_distance2ScoreRankHistogramType_id[key_tuple] = row.sid
	sys.stderr.write("%s pairs. Done.\n"%(len(model.CandidateGeneTopSNPTestRMType_id_min_distance2ScoreRankHistogramType_id)))

	# 2009-4-10 takes too long in individual request, put here. used in Accession.py
	from variation.src.common import map_perlegen_ecotype_name2accession_id, fillInPhenotypeMethodID2ecotype_id_set
	model.ecotype_name2accession_id = map_perlegen_ecotype_name2accession_id(model.db.metadata.bind)
	# 2009-11-17
	model.PhenotypeMethodID2ecotype_id_set = fillInPhenotypeMethodID2ecotype_id_set(model.Stock_250kDB.PhenotypeAvg)
	# 2009-11-17
	from variation.src.common import fillInCallMethodID2ecotype_id_set

	model.CallMethodID2ecotype_id_set = fillInCallMethodID2ecotype_id_set(model.Stock_250kDB.CallInfo)
	
	return config
