import logging

from gwaswebserver.lib.base import *
#from pylons import app_globals

log = logging.getLogger(__name__)


class HelloController(BaseController):

	def index(self):
		"""
		2009-6-16
			assign values to c.greeting, c.name
		2009-5-4
			choose the homepage depending on the config['app_conf']['site_public'] flag
		"""
		# Return a rendered template
		#   return render('/some/template.mako')
		# or, Return a response
		#return 'Hello World'
		#response.headers['Content-Type'] = 'text/plain'
		#return 'Hello from the index() action!'
		
		c.greeting = request.params.get('greeting', 'Welcome')
		c.name = request.params.get('name', 'Visitor')
		
		site_public = config['app_conf']['site_public']
		if site_public=='true' or site_public=='True':
			return render('/public.html')
		return render('/index.html')
	
	def public(self):
		"""
		2009-6-16
			assign values to c.greeting, c.name
		2009-5-4
			display the public page
		"""
		c.greeting = request.params.get('greeting', 'Welcome')
		c.name = request.params.get('name', 'Visitor')
		return render('/public.html')
	
	"""
	# 2009-6-22 comment it out
	def serverinfo(self):
		import cgi
		import pprint
		c.pretty_environ = cgi.escape(pprint.pformat(request.environ))
		c.name = 'The Black Knight'
		session['name'] = 'mighty'
		session.save()
		return render('/serverinfo.mako')
	"""
	
	def app_globals_test(self):
		if g.message == 'Hello':
			content = g.message
			g.message = 'Hello World!'
			#return content
		else:
			pass
			#return g.message
		g.visits += 1
		return "You are visitor number %s." % g.visits
	
	"""
	# 2009-6-22 comment it out
	def environ(self):
		result = '<html><body><h1>Environ</h1>'
		for key, value in request.environ.items():
			result += '%s: %r <br />'%(key, value)
		result += '</body></html>'
		return result
	"""
	
	def navigation(self):
		return render('/navigation.html')