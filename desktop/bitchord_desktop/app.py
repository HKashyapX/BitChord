"""A lightweight desktop shell for search, a play queue, and audio playback."""

from concurrent.futures import ThreadPoolExecutor
import tkinter as tk
from tkinter import ttk

from .catalog import Track, search
from .player import Player


class BitChordWindow:
    def __init__(self, root: tk.Tk) -> None:
        self.root = root
        self.root.title("BitChord · Linux preview")
        self.root.geometry("900x620")
        self.root.minsize(620, 400)
        self.search_pool = ThreadPoolExecutor(max_workers=2)
        self.player_pool = ThreadPoolExecutor(max_workers=1)
        self.player = Player()
        self.tracks: list[Track] = []
        self.queue: list[Track] = []
        self.current_track: Track | None = None
        self.playback_ready = False
        self.play_request_id = 0
        self.request_id = 0
        self.closed = False

        style = ttk.Style()
        if "clam" in style.theme_names():
            style.theme_use("clam")
        frame = ttk.Frame(root, padding=18)
        frame.pack(fill="both", expand=True)
        ttk.Label(frame, text="BitChord", font=("Sans", 24, "bold")).pack(anchor="w")
        ttk.Label(frame, text="YouTube Music · Linux preview").pack(anchor="w", pady=(0, 16))
        search_row = ttk.Frame(frame)
        search_row.pack(fill="x")
        self.query = tk.StringVar()
        entry = ttk.Entry(search_row, textvariable=self.query)
        entry.pack(side="left", fill="x", expand=True)
        entry.bind("<Return>", lambda _: self.search())
        ttk.Button(search_row, text="Search", command=self.search).pack(side="left", padx=(8, 0))
        self.status = tk.StringVar(value="Search for a song to begin")
        ttk.Label(frame, textvariable=self.status).pack(anchor="w", pady=10)

        content = ttk.Panedwindow(frame, orient="horizontal")
        content.pack(fill="both", expand=True)
        self.results = ttk.Treeview(content, columns=("artist", "album", "duration"), show="tree headings")
        self.results.heading("#0", text="Song")
        for col, label, width in (("artist", "Artist", 190), ("album", "Album", 190), ("duration", "Time", 65)):
            self.results.heading(col, text=label)
            self.results.column(col, width=width, minwidth=50)
        content.add(self.results, weight=4)
        self.results.bind("<Double-1>", lambda _: self.play_selected())
        queue_frame = ttk.LabelFrame(content, text="Up next")
        self.queue_view = tk.Listbox(queue_frame, width=27, exportselection=False)
        self.queue_view.pack(fill="both", expand=True, padx=6, pady=6)
        content.add(queue_frame, weight=1)
        controls = ttk.Frame(frame)
        controls.pack(fill="x", pady=(12, 0))
        first_row = ttk.Frame(controls)
        first_row.pack(fill="x")
        second_row = ttk.Frame(controls)
        second_row.pack(fill="x", pady=(6, 0))
        ttk.Button(first_row, text="Play selected", command=self.play_selected).pack(side="left")
        ttk.Button(first_row, text="Add to queue", command=self.enqueue_selected).pack(side="left", padx=8)
        ttk.Button(first_row, text="Next", command=self.next).pack(side="left")
        ttk.Button(second_row, text="Remove queued", command=self.remove_queued).pack(side="left")
        self.pause_button = ttk.Button(second_row, text="Pause", command=self.toggle_pause)
        self.pause_button.pack(side="left", padx=8)
        ttk.Button(second_row, text="Stop", command=self.stop).pack(side="left")
        self.now_playing = tk.StringVar(value="Nothing playing")
        ttk.Label(frame, textvariable=self.now_playing).pack(anchor="w", pady=(12, 0))
        self.root.protocol("WM_DELETE_WINDOW", self.close)
        self.root.after(700, self._poll_playback)
        entry.focus_set()

    def _on_ui(self, callback) -> None:
        if not self.closed:
            self.root.after(0, lambda: None if self.closed else callback())

    def search(self) -> None:
        query = self.query.get().strip()
        if not query:
            return
        self.request_id += 1
        request_id = self.request_id
        self.status.set("Searching YouTube Music…")
        def work():
            try:
                tracks = search(query)
            except Exception as exc:
                self._on_ui(lambda: self._search_error(request_id, str(exc)))
            else:
                self._on_ui(lambda: self._show_results(request_id, tracks))
        self.search_pool.submit(work)

    def _search_error(self, request_id: int, error: str) -> None:
        if request_id == self.request_id:
            self.status.set(f"Search failed: {error}")

    def _show_results(self, request_id: int, tracks: list[Track]) -> None:
        if request_id != self.request_id:
            return
        self.tracks = tracks
        self.results.delete(*self.results.get_children())
        for index, track in enumerate(tracks):
            self.results.insert("", "end", iid=str(index), text=track.title,
                                values=(track.artist, track.album, track.duration))
        self.status.set(f"{len(tracks)} songs found" if tracks else "No songs found")

    def selected(self) -> Track | None:
        selection = self.results.selection()
        return self.tracks[int(selection[0])] if selection else None

    def play_selected(self) -> None:
        track = self.selected()
        if track:
            self.play(track)

    def enqueue_selected(self) -> None:
        track = self.selected()
        if track:
            self.queue.append(track)
            self._refresh_queue()
            self.status.set(f"Queued {track.title} · {len(self.queue)} waiting")

    def _refresh_queue(self) -> None:
        self.queue_view.delete(0, tk.END)
        for track in self.queue:
            self.queue_view.insert(tk.END, f"{track.title} — {track.artist}")

    def remove_queued(self) -> None:
        selection = self.queue_view.curselection()
        if selection:
            self.queue.pop(selection[0])
            self._refresh_queue()
            self.status.set(f"{len(self.queue)} songs waiting")

    def play(self, track: Track) -> None:
        self.play_request_id += 1
        request_id = self.play_request_id
        self.current_track = track
        self.playback_ready = False
        self.now_playing.set(f"Loading: {track.title} — {track.artist}")
        def work():
            try:
                self.player.play(track)
            except Exception as exc:
                self._on_ui(lambda: self._play_failed(request_id, str(exc)))
            else:
                self._on_ui(lambda: self._play_started(request_id, track))
        self.player_pool.submit(work)

    def _play_failed(self, request_id: int, error: str) -> None:
        if request_id == self.play_request_id:
            self.current_track = None
            self.now_playing.set(f"Playback failed: {error}")

    def _play_started(self, request_id: int, track: Track) -> None:
        if request_id == self.play_request_id:
            self.playback_ready = True
            self.pause_button.configure(text="Pause")
            self.now_playing.set(f"Playing: {track.title} — {track.artist}")

    def next(self) -> None:
        if self.queue:
            track = self.queue.pop(0)
            self._refresh_queue()
            self.play(track)

    def toggle_pause(self) -> None:
        if not self.playback_ready:
            return
        paused = self.player.toggle_pause()
        if paused is not None and self.current_track:
            self.pause_button.configure(text="Resume" if paused else "Pause")
            state = "Paused" if paused else "Playing"
            self.now_playing.set(f"{state}: {self.current_track.title} — {self.current_track.artist}")

    def _poll_playback(self) -> None:
        if self.closed:
            return
        exit_code = self.player.exit_code() if self.current_track and self.playback_ready else None
        if exit_code is not None:
            self.current_track = None
            self.playback_ready = False
            self.pause_button.configure(text="Pause")
            if exit_code != 0:
                self.now_playing.set("Playback stopped unexpectedly. Check your connection or try another song.")
            elif self.queue:
                self.next()
            else:
                self.now_playing.set("Playback ended")
        self.root.after(700, self._poll_playback)

    def stop(self) -> None:
        self.play_request_id += 1
        self.current_track = None
        self.playback_ready = False
        self.player_pool.submit(self.player.stop)
        self.pause_button.configure(text="Pause")
        self.now_playing.set("Stopped")

    def close(self) -> None:
        self.closed = True
        self.player.close()
        self.search_pool.shutdown(wait=False, cancel_futures=True)
        self.player_pool.shutdown(wait=False, cancel_futures=True)
        self.root.destroy()


def main() -> None:
    root = tk.Tk()
    BitChordWindow(root)
    root.mainloop()


if __name__ == "__main__":
    main()
