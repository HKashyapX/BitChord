import unittest
from unittest.mock import MagicMock, patch

from bitchord_desktop.catalog import Track
from bitchord_desktop.player import Player


class PlayerTests(unittest.TestCase):
    @patch("bitchord_desktop.player.subprocess.Popen")
    @patch("bitchord_desktop.player.resolve_stream", return_value=("https://example.com/audio", {"User-Agent": "Test"}))
    @patch("bitchord_desktop.player.shutil.which", return_value="/usr/bin/ffplay")
    def test_launches_without_shell(self, _which, _resolve, popen):
        player = Player()
        player.play(Track("abc", "Song", "Artist"))
        command = popen.call_args.args[0]
        self.assertEqual(command[0], "ffplay")
        self.assertEqual(command[-1], "https://example.com/audio")
        self.assertNotIn("shell", popen.call_args.kwargs)

    def test_stops_live_process(self):
        player = Player()
        player.process = MagicMock()
        player.process.poll.return_value = None
        process = player.process
        player.stop()
        process.terminate.assert_called_once()
        self.assertIsNone(player.process)


if __name__ == "__main__":
    unittest.main()
