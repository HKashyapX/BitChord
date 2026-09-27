import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

from bitchord_desktop.downloads import saved_audio


class DownloadTests(unittest.TestCase):
    def test_only_complete_audio_files_are_reused(self):
        with tempfile.TemporaryDirectory() as directory, patch.dict(os.environ, {"XDG_DATA_HOME": directory}):
            target = Path(directory) / "bitchord" / "downloads"
            target.mkdir(parents=True)
            (target / "abc.webm.part").write_bytes(b"partial")
            self.assertIsNone(saved_audio("abc"))
            (target / "abc.webm").write_bytes(b"audio")
            self.assertEqual(saved_audio("abc"), target / "abc.webm")
            self.assertIsNone(saved_audio("../abc"))


if __name__ == "__main__":
    unittest.main()
