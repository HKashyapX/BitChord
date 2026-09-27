import unittest

from bitchord_desktop.catalog import Track, parse_track


class CatalogTests(unittest.TestCase):
    def test_parses_song(self):
        self.assertEqual(parse_track({
            "videoId": "AbC_123-xyz", "title": "Song",
            "artists": [{"name": "First"}, {"name": "Second"}],
            "album": {"name": "Record"}, "duration": "3:21",
            "thumbnails": [{"url": "https://i.ytimg.com/vi/abc/default.jpg"}],
        }), Track("AbC_123-xyz", "Song", "First, Second", "Record", "3:21",
                  "https://i.ytimg.com/vi/abc/default.jpg"))

    def test_ignores_untrusted_thumbnail_host(self):
        track = parse_track({"videoId": "abc", "thumbnails": [{"url": "https://bad.example/art"}]})
        self.assertEqual(track.artwork_url, "")

    def test_rejects_invalid_id(self):
        for value in (None, "", "foo/bar", "foo?bar", "x" * 33):
            self.assertIsNone(parse_track({"videoId": value}))


if __name__ == "__main__":
    unittest.main()
