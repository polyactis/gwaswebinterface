from gwaswebserver.tests import *

class TestSnpregionplotController(TestController):

    def test_index(self):
        response = self.app.get(url(controller='SNPRegionPlot'))
        # Test response...
