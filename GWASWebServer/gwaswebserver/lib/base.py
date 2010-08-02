"""The base Controller API

Provides the BaseController class for subclassing, and other objects
utilized by Controllers.
"""
from pylons.controllers import WSGIController
from pylons.templating import render_mako as render
from pylons import tmpl_context as c, cache, config, app_globals, request, response, session
from pylons.controllers.util import abort, etag_cache, redirect
from pylons.decorators import jsonify, validate
from pylons.i18n import _, ungettext, N_


import gwaswebserver.lib.helpers as h
import gwaswebserver.model as model
import os, sys	#2008-10-16
sys.path.insert(0, os.path.join(os.path.expanduser('~/script')))
from pymodule import PassingData
from sets import Set
import numpy

class BaseController(WSGIController):

	def __call__(self, environ, start_response):
		"""Invoke the Controller"""
		# WSGIController.__call__ dispatches to the Controller method
		# the request is routed to. This routing information is
		# available in environ['pylons.routes_dict']
		try:
			return WSGIController.__call__(self, environ, start_response)
		finally:
			model.db.session.remove()

# Include the '_' function in the public names
__all__ = [__name for __name in locals().keys() if not __name.startswith('_') \
		   or __name == '_']
