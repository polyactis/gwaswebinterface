from gwaswebserver.tests import *

class TestFormtestController(TestController):

    def test_index(self):
        response = self.app.get(url(controller='formtest'))
        # Test response...
