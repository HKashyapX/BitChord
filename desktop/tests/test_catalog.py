import unittest

from unittest.mock import patch
from unittest.mock import MagicMock
import sys

from bitchord_desktop.catalog import (Track, build_radio, find_video_version, lookup_artwork,
                                      lookup_metadata, parse_track, radio, same_recording)


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

    def test_selects_largest_thumbnail(self):
        track = parse_track({"videoId": "abc", "thumbnails": [
            {"url": "https://i.ytimg.com/small.jpg", "width": 60, "height": 60},
            {"url": "https://i.ytimg.com/large.jpg", "width": 544, "height": 544},
            {"url": "https://i.ytimg.com/medium.jpg", "width": 120, "height": 120},
        ]})
        self.assertEqual(track.artwork_url, "https://i.ytimg.com/large.jpg")

    @patch("bitchord_desktop.catalog.search")
    def test_lookup_requires_exact_video_id(self, search):
        search.return_value = [Track("other", "Song", "Artist", artwork_url="https://i.ytimg.com/wrong.jpg"),
                               Track("right", "Song", "Artist", artwork_url="https://i.ytimg.com/right.jpg")]
        self.assertEqual(lookup_artwork("right", "Song", "Artist"), "https://i.ytimg.com/right.jpg")
        self.assertEqual(lookup_metadata("right", "Song", "Artist").artwork_url,
                         "https://i.ytimg.com/right.jpg")

    def test_radio_parses_watch_playlist(self):
        api = MagicMock()
        api.YTMusic.return_value.get_watch_playlist.return_value = {
            "tracks": [{"videoId": "abc", "title": "Song", "artists": [{"name": "Artist"}]}]
        }
        with patch.dict(sys.modules, {"ytmusicapi": api}):
            self.assertEqual(radio("abc"), [Track("abc", "Song", "Artist")])
        api.YTMusic.return_value.get_watch_playlist.assert_called_once_with(videoId="abc", limit=25, radio=True)

    def test_rejects_invalid_id(self):
        for value in (None, "", "foo/bar", "foo?bar", "x" * 33):
            self.assertIsNone(parse_track({"videoId": value}))

    def test_radio_deduplicates_audio_and_video_and_caps_other_artists(self):
        seed = Track("seed", "Kesariya", "Arijit Singh")
        candidates = [
            Track("video", "Kesariya (Official Video)", "Arijit Singh, Pritam"),
            Track("a1", "One", "Another Artist"),
            Track("a2", "Two", "Another Artist"),
            Track("a3", "Three", "Another Artist"),
            Track("x", "Different Song", "Third Artist"),
        ]
        self.assertTrue(same_recording(seed, candidates[0]))
        self.assertEqual([track.video_id for track in build_radio(seed, candidates)],
                         ["a1", "a2", "x"])

    def test_video_version_uses_matching_recording_instead_of_first_result(self):
        api = MagicMock()
        api.YTMusic.return_value.search.return_value = [
            {"videoId": "wrong", "title": "Different", "artists": [{"name": "Artist"}],
             "resultType": "video", "duration": "3:00"},
            {"videoId": "right", "title": "Song (Official Video)",
             "artists": [{"name": "Artist"}], "resultType": "video", "duration": "3:32"},
        ]
        with patch.dict(sys.modules, {"ytmusicapi": api}):
            found = find_video_version(Track("audio", "Song", "Artist", duration="3:30"))
        self.assertEqual(found.video_id, "right")


if __name__ == "__main__":
    unittest.main()
