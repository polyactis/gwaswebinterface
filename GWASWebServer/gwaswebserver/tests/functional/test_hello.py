from gwaswebserver.tests import *

class TestHelloController(TestController):

    def test_index(self):
        response = self.app.get(url(controller='hello'))
        # Test response...
