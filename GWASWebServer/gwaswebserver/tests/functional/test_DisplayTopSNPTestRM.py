from gwaswebserver.tests import *

class TestDisplaytopsnptestrmController(TestController):

    def test_index(self):
        response = self.app.get(url(controller='DisplayTopSNPTestRM'))
        # Test response...
