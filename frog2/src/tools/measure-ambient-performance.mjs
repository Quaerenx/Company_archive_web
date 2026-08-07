import { spawn } from 'node:child_process';
import { createServer } from 'node:http';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const SAMPLE_DURATION_MS = 6000;
const fixtureHtml = readFileSync(
    resolve('src/tools/ambient-performance.html')
);
const ambientScript = readFileSync(
    resolve('src/main/webapp/resources/js/ambient-background.js')
);
const fixtureServer = createServer((request, response) => {
    if (request.url?.startsWith('/ambient-performance.html')) {
        response.writeHead(200, { 'Content-Type': 'text/html; charset=UTF-8' });
        response.end(fixtureHtml);
        return;
    }
    if (request.url === '/ambient-background.js') {
        response.writeHead(200, { 'Content-Type': 'text/javascript; charset=UTF-8' });
        response.end(ambientScript);
        return;
    }
    response.writeHead(404);
    response.end();
});
await new Promise((resolveListen) => {
    fixtureServer.listen(0, '127.0.0.1', resolveListen);
});
const fixtureAddress = fixtureServer.address();
if (!fixtureAddress || typeof fixtureAddress === 'string') {
    throw new Error('Unable to resolve the local fixture server address');
}
const fixtureBaseUrl = `http://127.0.0.1:${fixtureAddress.port}`;
const driverPort = await findOpenPort();
const driverUrl = `http://127.0.0.1:${driverPort}`;
const driver = spawn('/snap/bin/geckodriver', ['--port', String(driverPort)], {
    stdio: ['ignore', 'pipe', 'pipe']
});
let sessionId;

driver.stdout.on('data', () => {});
driver.stderr.on('data', () => {});

try {
    await waitForDriver();
    const session = await request('POST', '/session', {
        capabilities: {
            alwaysMatch: {
                acceptInsecureCerts: true,
                browserName: 'firefox',
                'moz:firefoxOptions': {
                    args: ['-headless'],
                    prefs: {
                        'browser.cache.disk.enable': false,
                        'ui.prefersReducedMotion': 0
                    }
                }
            }
        }
    });
    sessionId = session.sessionId;

    await request('POST', `/session/${sessionId}/window/rect`, {
        height: 1000,
        width: 1440,
        x: 0,
        y: 0
    });

    for (const lowPower of [false, true]) {
        const fixture = new URL('/ambient-performance.html', fixtureBaseUrl);
        if (lowPower) {
            fixture.searchParams.set('lowPower', '1');
        }
        await request('POST', `/session/${sessionId}/url`, {
            url: fixture.href
        });
        await delay(SAMPLE_DURATION_MS);
        const metrics = await request(
            'POST',
            `/session/${sessionId}/execute/sync`,
            {
                args: [],
                script: 'return window.readAmbientPerformance();'
            }
        );
        if (!metrics || metrics.callbackCount < 60) {
            throw new Error('Ambient animation did not produce enough samples');
        }
        process.stdout.write(`${JSON.stringify(metrics)}\n`);
    }
} finally {
    if (sessionId) {
        await request('DELETE', `/session/${sessionId}`).catch(() => {});
    }
    driver.kill('SIGTERM');
    fixtureServer.close();
}

async function waitForDriver() {
    const deadline = Date.now() + 10000;
    while (Date.now() < deadline) {
        try {
            await request('GET', '/status');
            return;
        } catch (error) {
            await delay(100);
        }
    }
    throw new Error('geckodriver did not become ready');
}

async function request(method, path, body) {
    const response = await fetch(`${driverUrl}${path}`, {
        body: body === undefined ? undefined : JSON.stringify(body),
        headers: body === undefined ? {} : { 'Content-Type': 'application/json' },
        method
    });
    const payload = await response.json();
    if (!response.ok || payload.value?.error) {
        throw new Error(payload.value?.message || `WebDriver HTTP ${response.status}`);
    }
    return payload.value;
}

function delay(milliseconds) {
    return new Promise((resolveDelay) => setTimeout(resolveDelay, milliseconds));
}

async function findOpenPort() {
    const probe = createServer();
    await new Promise((resolveListen) => {
        probe.listen(0, '127.0.0.1', resolveListen);
    });
    const address = probe.address();
    if (!address || typeof address === 'string') {
        probe.close();
        throw new Error('Unable to allocate a local WebDriver port');
    }
    const port = address.port;
    await new Promise((resolveClose) => probe.close(resolveClose));
    return port;
}
