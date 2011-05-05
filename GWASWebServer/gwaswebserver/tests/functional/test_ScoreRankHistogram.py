from gwaswebserver.tests import *

class TestScorerankhistogramController(TestController):

    def test_index(self):
        response = self.app.get(url(controller='ScoreRankHistogram'))
        # Test response...
