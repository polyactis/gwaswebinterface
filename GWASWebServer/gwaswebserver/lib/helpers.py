"""Helper functions

Consists of functions to typically be used within templates, but also
available to Controllers. This module is available to both as 'h'.
"""
from webhelpers import *

#2008-12-24 for the forms
from routes import redirect_to
from routes import url_for
from webhelpers.html.tags import *
from pylons import url, request, response
from webhelpers.pylonslib import Flash as _Flash
from pylons.controllers.util import abort
flash = _Flash()

#2009-3-5 store 250k snp dataset sucked in from filesystem
call_method_id2dataset = {}
#2009-3-5 load ecotype_info on demand
ecotype_info = None




def cool_denial_handler(reason):
    # When this handler is called, response.status has two possible values:
    # 401 or 403.
    active_user = user()
    if active_user is None:
        message = 'Oops, you have to login: %s' % reason
        status = 401
    else:
        message = "Come on, %s, you know you can't do that: %s" % (active_user.realname,
                                                                   reason)
        status = 403
    flash(message)
    abort(status, comment=reason)


def user():
    """Return the currently logged-in user object
    or None if not logged in.
    """
    identity = request.environ.get('repoze.who.identity')
    if identity is not None:
        # Get some data associated with the user. (Eg. the user object that was assigned in UserModelPlugin.)
        user = identity.get('user')
    else:
        user = None
    return user

def user_id():
    currentuser = user()
    if currentuser is not None:
        user_id =  currentuser.id
    else:
        user_id = -1
    return user_id
    