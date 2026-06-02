const express = require('express');
const roomMemory = require('../services/room-memory');

const router = express.Router();

router.get('/world', async (req, res) => {
  const now = new Date();
  const hour = now.getHours();
  const month = now.getMonth() + 1;
  const season = month >= 3 && month <= 5 ? 'spring' : month >= 6 && month <= 8 ? 'summer' : month >= 9 && month <= 11 ? 'autumn' : 'winter';
  const timePhase = hour >= 5 && hour < 8 ? 'dawn' : hour >= 8 && hour < 17 ? 'day' : hour >= 17 && hour < 20 ? 'dusk' : 'night';

  res.json({
    success: true,
    data: {
      weather: 'clear',
      weatherCode: 0,
      temperature: 26,
      windSpeed: 3,
      timePhase,
      season,
      city: 'Room',
      locationSource: 'template',
      updatedAt: new Date().toISOString()
    }
  });
});

router.get('/memory', (req, res) => {
  const userId = String(req.headers['x-user-id'] || 'guest');
  const query = String(req.query.q || '').trim();
  const limit = req.query.limit || 20;
  const data = query
    ? roomMemory.searchMemories(userId, query, limit)
    : roomMemory.listMemories(userId, { limit, type: req.query.type });
  res.json({ success: true, data });
});

router.post('/memory', async (req, res) => {
  const userId = String(req.headers['x-user-id'] || 'guest');
  const result = await roomMemory.recordMemory(userId, req.body || {});
  res.json({ success: true, data: result });
});

router.patch('/memory/:id', (req, res) => {
  const userId = String(req.headers['x-user-id'] || 'guest');
  const data = roomMemory.updateMemory(userId, String(req.params.id || ''), req.body || {});
  res.json({ success: true, data });
});

router.delete('/memory/:id', (req, res) => {
  const userId = String(req.headers['x-user-id'] || 'guest');
  const count = roomMemory.deleteMemory(userId, String(req.params.id || ''));
  res.json({ success: true, data: { count } });
});

router.post('/mcp/call', async (req, res) => {
  const { name, args } = req.body || {};
  const allowed = new Set(['understand_image', 'web_search']);
  if (!allowed.has(String(name || ''))) {
    return res.status(403).json({ success: false, message: 'tool not allowed' });
  }
  res.json({
    success: true,
    data: {
      text: `template mcp result for ${name}`,
      raw: { name, args }
    }
  });
});

module.exports = router;

