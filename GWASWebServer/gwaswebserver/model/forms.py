import formencode

class EmailForm(formencode.Schema):
    allow_extra_fields = True
    name = formencode.validators.String(not_empty=True)
    email = formencode.validators.Email(not_empty=True)
    organisation = formencode.validators.String(not_empty=True)


