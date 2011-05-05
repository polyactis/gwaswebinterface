from gwaswebserver.tests import *

class TestDisplayresultsgeneController(TestController):

    def test_index(self):
        response = self.app.get(url(controller='DisplayResultsGene'))
        # Test response...
