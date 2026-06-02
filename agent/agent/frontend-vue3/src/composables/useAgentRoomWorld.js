import { computed, ref } from 'vue';

const WEATHER_LABELS = {
  clear: '晴朗',
  cloudy: '多云',
  fog: '雾',
  rain: '雨',
  snow: '雪',
  storm: '雷暴'
};

const WEATHER_ICONS = {
  clear: '☀',
  cloudy: '☁',
  fog: '〰',
  rain: '☂',
  snow: '❄',
  storm: '⚡'
};

function worldDetail(world) {
  return `${world.timePhase || 'day'} / ${world.season || 'summer'} / ${world.locationSource || 'unknown'}`;
}

export function useAgentRoomWorld() {
  const rawWorld = ref(null);
  const loading = ref(false);
  const error = ref('');

  async function loadWorld(params = {}) {
    loading.value = true;
    error.value = '';
    try {
      const query = new URLSearchParams();
      Object.entries(params).forEach(([key, value]) => {
        if (value !== '' && value != null) query.set(key, String(value));
      });
      const response = await fetch(`/api/room-agent/world?${query.toString()}`, {
        credentials: 'include',
        cache: 'no-store'
      });
      const result = await response.json().catch(() => ({}));
      if (!response.ok || !result.success) throw new Error(result.message || `HTTP ${response.status}`);
      rawWorld.value = result.data || null;
    } catch (err) {
      error.value = err.message || 'load world failed';
    } finally {
      loading.value = false;
    }
  }

  const weatherCard = computed(() => {
    const world = rawWorld.value || {};
    const weather = world.weather || 'clear';
    const temperature = Number.isFinite(Number(world.temperature)) ? `${Math.round(Number(world.temperature))}°C` : '--';
    const wind = Number.isFinite(Number(world.windSpeed)) ? `${Math.round(Number(world.windSpeed))} km/h` : '--';
    return {
      icon: WEATHER_ICONS[weather] || '☁',
      label: WEATHER_LABELS[weather] || '未知天气',
      city: world.city || '房间',
      temperature,
      wind,
      detail: worldDetail(world),
      raw: world
    };
  });

  return {
    rawWorld,
    weatherCard,
    loading,
    error,
    loadWorld
  };
}

