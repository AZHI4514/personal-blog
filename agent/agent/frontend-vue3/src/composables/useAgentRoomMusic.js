import { computed, onBeforeUnmount, ref } from 'vue';

function formatTime(seconds) {
  if (!Number.isFinite(seconds) || seconds < 0) return '0:00';
  const minutes = Math.floor(seconds / 60);
  const rest = Math.floor(seconds % 60).toString().padStart(2, '0');
  return `${minutes}:${rest}`;
}

export function useAgentRoomMusic(tracks = []) {
  const audio = new Audio();
  const trackIndex = ref(0);
  const playing = ref(false);
  const currentTime = ref(0);
  const duration = ref(0);
  const volume = ref(0.72);

  const currentTrack = computed(() => tracks[trackIndex.value] || null);
  const currentLabel = computed(() => formatTime(currentTime.value));
  const durationLabel = computed(() => formatTime(duration.value));

  function loadTrack(index, options = {}) {
    if (!tracks.length) return;
    const nextIndex = ((index % tracks.length) + tracks.length) % tracks.length;
    trackIndex.value = nextIndex;
    audio.src = tracks[nextIndex].url;
    audio.preload = 'metadata';
    if (options.play) audio.play().catch(() => {});
  }

  function togglePlay() {
    if (audio.paused) audio.play().catch(() => {});
    else audio.pause();
  }

  function next() {
    loadTrack(trackIndex.value + 1, { play: playing.value });
  }

  function prev() {
    loadTrack(trackIndex.value - 1, { play: playing.value });
  }

  function setVolume(value) {
    volume.value = Math.max(0, Math.min(1, Number(value)));
    audio.volume = volume.value;
  }

  function destroy() {
    audio.pause();
    audio.removeAttribute('src');
    audio.load();
  }

  audio.volume = volume.value;
  audio.addEventListener('loadedmetadata', () => {
    duration.value = Number.isFinite(audio.duration) ? audio.duration : 0;
  });
  audio.addEventListener('timeupdate', () => {
    currentTime.value = Number.isFinite(audio.currentTime) ? audio.currentTime : 0;
  });
  audio.addEventListener('play', () => {
    playing.value = true;
  });
  audio.addEventListener('pause', () => {
    playing.value = false;
  });
  audio.addEventListener('ended', next);
  onBeforeUnmount(destroy);

  return {
    trackIndex,
    currentTrack,
    playing,
    currentTime,
    duration,
    currentLabel,
    durationLabel,
    loadTrack,
    togglePlay,
    next,
    prev,
    setVolume,
    destroy
  };
}

