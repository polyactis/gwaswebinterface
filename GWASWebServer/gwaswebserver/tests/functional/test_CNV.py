from gwaswebserver.tests import *

class TestCnvController(TestController):

    def test_index(self):
        response = self.app.get(url(controller='CNV', action='index'))
        # Test response...
