import unittest
from unittest.mock import patch

from bitchord_desktop.lyrics import fetch_lyrics, parse_lrc


class LyricsTests(unittest.TestCase):
    def test_parses_and_sorts_line_timing(self):
        self.assertEqual(parse_lrc("[ar:Artist]\n[00:02.34]Second\n[00:01.005]First"),
                         [(1005, "First"), (2340, "Second")])

    @patch("bitchord_desktop.lyrics._get")
    def test_prefers_exact_then_near_duration(self, get):
        get.side_effect = [None, [
            {"duration": 300, "syncedLyrics": "[00:01.00]Wrong"},
            {"duration": 201, "syncedLyrics": "[00:01.00]Right"},
        ]]
        self.assertEqual(fetch_lyrics("Song", "Artist", 200), [(1000, "Right")])


if __name__ == "__main__":
    unittest.main()
