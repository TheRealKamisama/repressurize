// Download test dependencies through the required local HTTP CONNECT proxy.
const http = require('node:http');
const https = require('node:https');
const tls = require('node:tls');
const fs = require('node:fs');
const agent = new https.Agent();
agent.createConnection = (options, callback) => {
  const tunnel = http.request({host: 'localhost', port: 7897, method: 'CONNECT', path: `${options.host}:443`});
  tunnel.on('connect', (response, socket) => {
    if (response.statusCode !== 200) { socket.destroy(); callback(new Error(`Proxy returned ${response.statusCode}`)); return; }
    const secure = tls.connect({socket, servername: options.host});
    secure.once('secureConnect', () => callback(null, secure));
    secure.once('error', callback);
  });
  tunnel.once('error', callback);
  tunnel.end();
};
function download(url, destination, redirects = 0) {
  https.get(url, {agent, headers: {'User-Agent': 'PressurePipesMigrationQA/1.0'}}, response => {
    if (response.statusCode >= 300 && response.statusCode < 400 && response.headers.location && redirects < 5) {
      response.resume(); download(new URL(response.headers.location, url), destination, redirects + 1); return;
    }
    if (response.statusCode !== 200) throw new Error(`Download returned ${response.statusCode}: ${url}`);
    const output = fs.createWriteStream(destination);
    response.pipe(output);
    output.on('finish', () => { output.close(); agent.destroy(); console.log(destination); });
  }).on('error', error => { console.error(error.message); process.exitCode = 1; agent.destroy(); });
}
download(process.argv[2], process.argv[3]);
