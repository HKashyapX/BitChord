import unittest
from unittest.mock import MagicMock, patch
import signal

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

    @patch("bitchord_desktop.player.os.kill")
    def test_pause_resume_then_stop(self, kill):
        player = Player()
        player.process = MagicMock(pid=1234)
        player.process.poll.return_value = None
        self.assertTrue(player.toggle_pause())
        self.assertFalse(player.toggle_pause())
        self.assertEqual(kill.call_args_list[0].args, (1234, signal.SIGSTOP))
        self.assertEqual(kill.call_args_list[1].args, (1234, signal.SIGCONT))
        player.stop()
        player.process = MagicMock(pid=1234)
        player.process.poll.return_value = None
        player.toggle_pause()
        player.stop()
        self.assertEqual(kill.call_args_list[-1].args, (1234, signal.SIGCONT))

    @patch("bitchord_desktop.player.os.kill")
    def test_closed_player_does_not_start_after_resolution(self, _kill):
        player = Player()
        player.close()
        with patch("bitchord_desktop.player.shutil.which", return_value="ffplay"), \
             patch("bitchord_desktop.player.resolve_stream", return_value=("https://example.com/audio", {})), \
             patch("bitchord_desktop.player.subprocess.Popen") as popen:
            player.play(Track("abc", "Song", "Artist"))
        popen.assert_not_called()


if __name__ == "__main__":
    unittest.main()
