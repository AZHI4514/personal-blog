const { spawn } = require('child_process');

function requestOverStdio({ command, args = [], method, params = {}, timeoutMs = 30000 }) {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, { stdio: ['pipe', 'pipe', 'pipe'] });
    let buffer = '';
    let settled = false;
    const id = 1;

    const timer = setTimeout(() => {
      if (!settled) {
        settled = true;
        child.kill();
        reject(new Error(`mcp timeout after ${timeoutMs}ms`));
      }
    }, timeoutMs);

    child.stdout.on('data', (chunk) => {
      buffer += chunk.toString('utf8');
      const lines = buffer.split('\n');
      buffer = lines.pop() || '';
      for (const line of lines) {
        const text = line.trim();
        if (!text) continue;
        try {
          const message = JSON.parse(text);
          if (message.id === id && !settled) {
            settled = true;
            clearTimeout(timer);
            child.kill();
            if (message.error) reject(new Error(message.error.message || 'mcp error'));
            else resolve(message.result);
          }
        } catch (_) {
        }
      }
    });

    child.on('error', (error) => {
      if (settled) return;
      settled = true;
      clearTimeout(timer);
      reject(error);
    });

    child.stdin.write(`${JSON.stringify({ jsonrpc: '2.0', id, method, params })}\n`);
  });
}

module.exports = {
  requestOverStdio
};

