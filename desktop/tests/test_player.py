import unittest
from unittest.mock import MagicMock, patch
import signal
import json
import os
import tempfile

from bitchord_desktop.catalog import Track
from bitchord_desktop.player import Player


class PlayerTests(unittest.TestCase):
    @patch("bitchord_desktop.player.subprocess.Popen")
    @patch("bitchord_desktop.player.resolve_stream", return_value=("https://example.com/audio", {"User-Agent": "Test"}))
    @patch("bitchord_desktop.player.shutil.which", side_effect=lambda name: None if name == "mpv" else "/usr/bin/ffplay")
    def test_launches_without_shell(self, _which, _resolve, popen):
        player = Player()
        player.play(Track("abc", "Song", "Artist"))
        command = popen.call_args.args[0]
        self.assertEqual(command[0], "ffplay")
        self.assertEqual(command[-1], "https://example.com/audio")
        self.assertNotIn("shell", popen.call_args.kwargs)

    @patch("bitchord_desktop.player.subprocess.Popen")
    @patch("bitchord_desktop.player.resolve_stream", return_value=("https://example.com/audio", {"user-agent": "Test"}))
    @patch("bitchord_desktop.player.shutil.which", return_value="/usr/bin/mpv")
    def test_prefers_mpv_with_control_socket(self, _which, _resolve, popen):
        player = Player()
        player.play(Track("abc", "Song", "Artist"))
        command = popen.call_args.args[0]
        self.assertEqual(command[0], "mpv")
        self.assertIn("--user-agent=Test", command)
        self.assertTrue(any(arg.startswith("--input-ipc-server=") for arg in command))
        self.assertTrue(player.supports_seek)
        player.stop()

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

    @patch("bitchord_desktop.player.socket.socket")
    @patch("bitchord_desktop.player.os.path.exists", return_value=True)
    def test_mpv_position_seek_and_pause_over_ipc(self, _exists, socket_factory):
        player = Player()
        player.backend = "mpv"
        player.process = MagicMock()
        player.process.poll.return_value = None
        with tempfile.TemporaryDirectory() as directory:
            player._ipc_dir = directory
            connection = socket_factory.return_value.__enter__.return_value
            responses = iter([42.5, None, None])
            connection.makefile.return_value.__enter__.return_value.__iter__.side_effect = lambda: iter([
                json.dumps({"request_id": 1, "error": "success", "data": next(responses)})
            ])
            self.assertEqual(player.position(), 42.5)
            self.assertTrue(player.seek(75.0))
            self.assertTrue(player.toggle_pause())
            commands = [json.loads(call.args[0])['command'] for call in connection.sendall.call_args_list]
            self.assertEqual(commands, [["get_property", "time-pos"],
                                        ["seek", 75.0, "absolute"],
                                        ["set_property", "pause", True]])
            _exists.return_value = False
            player.stop()

    def test_mpv_output_controls_validate_device(self):
        player = Player()
        player.backend = "mpv"
        player.process = MagicMock()
        devices = [{"name": "auto", "description": "Default"},
                   {"name": "pulse/sink", "description": "Speakers"}]
        with patch.object(player, "_command_locked", side_effect=[devices, "auto", 72.0, devices, None, None]) as command:
            self.assertEqual(player.output_info(),
                             ([("auto", "Default"), ("pulse/sink", "Speakers")], "auto", 72.0))
            player.set_output("pulse/sink")
            player.set_volume(50)
            self.assertEqual(command.call_args_list[-1].args, ("set_property", "volume", 50))
        with patch.object(player, "_command_locked", return_value=devices):
            with self.assertRaises(ValueError):
                player.set_output("unknown")


if __name__ == "__main__":
    unittest.main()
