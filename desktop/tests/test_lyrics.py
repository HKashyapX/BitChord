import unittest
from unittest.mock import patch

from bitchord_desktop.lyrics import fetch_from_provider, fetch_lyrics, parse_lrc, parse_ttml


class LyricsTests(unittest.TestCase):
    def test_parses_and_sorts_line_timing(self):
        self.assertEqual(parse_lrc("[ar:Artist]\n[00:02.34]Second\n[00:01.005]First"),
                         [(1005, "First"), (2340, "Second")])

    def test_parses_android_ttml_provider_format(self):
        source = '<tt xmlns="http://www.w3.org/ns/ttml"><body><div>' \
                 '<p begin="1.250" end="2.0"><span>First</span> line</p>' \
                 '<p begin="00:03.5">Second</p></div></body></tt>'
        self.assertEqual(parse_ttml(source), [(1250, "First line"), (3500, "Second")])

    @patch("bitchord_desktop.lyrics._lrclib", return_value=[(1000, "Fallback")])
    @patch("bitchord_desktop.lyrics._better_lyrics", return_value=[])
    def test_automatic_provider_falls_back_in_android_order(self, _better, _lrclib):
        self.assertEqual(fetch_from_provider("Song", "Artist", 200),
                         ("LRCLIB", [(1000, "Fallback")]))
        self.assertEqual(fetch_lyrics("Song", "Artist", 200), [(1000, "Fallback")])

    @patch("bitchord_desktop.lyrics._better_lyrics", return_value=[(1000, "Line")])
    def test_provider_query_uses_android_title_and_artist_cleanup(self, better):
        fetch_from_provider("Song (feat. Guest) [Official Video]", "Artist - Topic", 200,
                            provider="BetterLyrics")
        self.assertEqual(better.call_args.args[:3], ("Song", "Artist", 200))


if __name__ == "__main__":
    unittest.main()
