import unittest

from bitchord_desktop.bridge import unique_tracks
from bitchord_desktop.catalog import Track


class BridgeTests(unittest.TestCase):
    def test_duplicate_provider_ids_are_emitted_once_in_order(self):
        tracks = [
            Track("same", "First", "Artist"),
            Track("different", "Second", "Artist"),
            Track("same", "Duplicate", "Artist"),
        ]
        self.assertEqual([tracks[0], tracks[1]], list(unique_tracks(tracks)))
