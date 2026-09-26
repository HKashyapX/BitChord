import unittest

from bitchord_desktop.catalog import Track, parse_track


class CatalogTests(unittest.TestCase):
    def test_parses_song(self):
        self.assertEqual(parse_track({
            "videoId": "AbC_123-xyz", "title": "Song",
            "artists": [{"name": "First"}, {"name": "Second"}],
            "album": {"name": "Record"}, "duration": "3:21",
        }), Track("AbC_123-xyz", "Song", "First, Second", "Record", "3:21"))

    def test_rejects_invalid_id(self):
        for value in (None, "", "foo/bar", "foo?bar", "x" * 33):
            self.assertIsNone(parse_track({"videoId": value}))


if __name__ == "__main__":
    unittest.main()
