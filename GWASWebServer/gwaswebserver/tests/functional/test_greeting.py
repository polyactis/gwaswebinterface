from gwaswebserver.tests import *

class TestGreetingController(TestController):

    def test_index(self):
        response = self.app.get(url(controller='greeting'))
        # Test response...
