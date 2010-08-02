from gwaswebserver.tests import *

class TestMafvsscoreplotController(TestController):

    def test_index(self):
        response = self.app.get(url(controller='MAFVsScorePlot'))
        # Test response...
