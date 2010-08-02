from gwaswebserver.tests import *

class TestCallinfoController(TestController):

    def test_index(self):
        response = self.app.get(url(controller='CallInfo'))
        # Test response...
