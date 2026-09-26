import unittest
from unittest.mock import MagicMock

from bitchord_desktop.app import BitChordWindow
from bitchord_desktop.catalog import Track


class AppFlowTests(unittest.TestCase):
    def test_finished_track_advances_visible_queue(self):
        window = BitChordWindow.__new__(BitChordWindow)
        window.closed = False
        window.current_track = Track("first", "First", "A")
        window.playback_ready = True
        window.queue = [Track("second", "Second", "B")]
        window.player = MagicMock()
        window.player.exit_code.return_value = 0
        window.pause_button = MagicMock()
        window.root = MagicMock()
        window.queue_view = MagicMock()
        played = []
        window.play = played.append

        window._poll_playback()

        self.assertEqual(played, [Track("second", "Second", "B")])
        self.assertEqual(window.queue, [])
        window.queue_view.delete.assert_called_once()
        window.root.after.assert_called_once()

    def test_failed_stream_does_not_discard_queue(self):
        window = BitChordWindow.__new__(BitChordWindow)
        window.closed = False
        window.current_track = Track("first", "First", "A")
        window.playback_ready = True
        window.queue = [Track("second", "Second", "B")]
        window.player = MagicMock()
        window.player.exit_code.return_value = 1
        window.pause_button = MagicMock()
        window.now_playing = MagicMock()
        window.root = MagicMock()
        window.play = MagicMock()

        window._poll_playback()

        self.assertEqual(len(window.queue), 1)
        window.play.assert_not_called()
        self.assertIn("unexpectedly", window.now_playing.set.call_args.args[0])

    def test_stop_invalidates_pending_playback_result(self):
        window = BitChordWindow.__new__(BitChordWindow)
        window.play_request_id = 1
        window.current_track = Track("first", "First", "A")
        window.playback_ready = False
        window.player_pool = MagicMock()
        window.player = MagicMock()
        window.pause_button = MagicMock()
        window.now_playing = MagicMock()

        window.stop()
        window._play_started(1, window.current_track)

        self.assertIsNone(window.current_track)
        self.assertFalse(window.playback_ready)
        window.now_playing.set.assert_called_once_with("Stopped")


if __name__ == "__main__":
    unittest.main()
