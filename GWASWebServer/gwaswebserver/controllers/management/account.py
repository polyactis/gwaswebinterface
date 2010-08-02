'''
Created on May 27, 2010

@author: uemit.seren
'''

import logging

from pylons import request, response, session, tmpl_context as c, config, url
from pylons.controllers.util import abort, redirect

from gwaswebserver.lib.base import BaseController, render, h
from gwaswebserver import model


CAME_FROM_EXCLUDE = (
    '/management/account/activation',
)



class AccountController(BaseController):
    
    def login(self):
        came_from = request.params.get('came_from', None)
        identity = request.environ.get('repoze.who.identity')
        if identity is not None:
            if not came_from or came_from in CAME_FROM_EXCLUDE:
                redirect(url('/'))
            else:
                redirect(url(str(came_from)))
        #login_counter = request.environ['repoze.who.logins']
        #if login_counter > 0:
            #h.flash('Wrong credentials')
        #c.login_counter = login_counter
        c.came_from = came_from
        return render('management/account/login.html')
    
    