import logging

from gwaswebserver.lib.base import *

log = logging.getLogger(__name__)
import gwaswebserver.model as model
from pymodule import PassingData

class CallinfoController(BaseController):

	def index(self):
		# Return a rendered template
		#   return render('/some/template.mako')
		# or, Return a response
		c.call_methods = model.Stock_250kDB.CallMethod.query.all()
		return render('/call_method.html')
	
	def call_method(self, id=None, qc_simplified='0'):
		"""
		2010-9-21
			use pymodule.PassingData to create a new object to get all associated data of a row fetched from db.
			Can't set arbitrary attributes on the row fetched through model.db.metadata.bind.execute() any more.
		"""
		view_call_query = "select * from view_call"
		view_qc_query = "select * from view_qc"
		if id:
			view_call_query += ' where call_method_id=%s'%id
			view_qc_query += ' where call_method_id=%s'%id
		view_call_query += ' order by nativename, stockparent'
		rows = model.db.metadata.bind.execute(view_call_query)
		
		c.call_info_ls = []
		qc_simplified = request.params.get('qc_simplified', qc_simplified)
		c.qc_simplified = qc_simplified
		i = 0
		for row in rows:
			passingObject = PassingData()
			for key in row.keys():
				setattr(passingObject, key, getattr(row, key, None))
			#2008-10-09 find QC for calls
			if id:
				qc_rows = model.db.metadata.bind.execute('%s and call_info_id=%s'%(view_qc_query, row.call_info_id))
			else:
				qc_rows = model.db.metadata.bind.execute('%s where call_info_id=%s'%(view_qc_query, row.call_info_id))
			for qc_row in qc_rows:
				if not hasattr(passingObject, 'call_NA_rate'):
					setattr(passingObject, 'call_NA_rate', qc_row.call_NA_rate)
				if not hasattr(passingObject, 'array_created'):
					setattr(passingObject, 'array_created', qc_row.array_created)
				
				if qc_simplified=='1' or qc_simplified==1:
					qc_data = '%.4f'%qc_row.mismatch_rate
				else:
					qc_data = '%.4f(%s/%s)'%(qc_row.mismatch_rate, qc_row.no_of_mismatches, qc_row.no_of_non_NA_pairs)
				
				setattr(passingObject, qc_row.QC_method_name, qc_data)
				
			i += 1
			setattr(passingObject, 'no', i)
			c.call_info_ls.append(passingObject)
		return render('/call_info.html')
