import doctest
import unittest
import sys
import json
import urllib2


class Endpoint(object):
    def __init__(self, base='http://localhost:9200'):
        self.base = base

    def _req(self, path):
        url = self.base + path
        return urllib2.Request(url)


    def get(self, path):
        req = self._req(path)
        print urllib2.urlopen(req).read()

    def post(self, path, data=''):
        req = self._req(path)
        body = json.dumps(data)
        req.add_data(body)
        print urllib2.urlopen(req).read()


def setUp(test):
    ep = Endpoint()
    test.globs['get'] = ep.get
    test.globs['post'] = ep.post


def test_suite(fname):
    s = unittest.TestSuite((
        doctest.DocFileSuite(
            fname, setUp=setUp,
            optionflags=doctest.NORMALIZE_WHITESPACE | doctest.ELLIPSIS)
    ))
    return s


if __name__ == '__main__':
    runner = unittest.TextTestRunner(failfast=True)
    res = runner.run(test_suite(sys.argv[1]))
    if res.failures or res.errors:
        sys.exit(1)

