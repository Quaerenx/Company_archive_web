import { spawn } from 'node:child_process';
import { createServer } from 'node:http';
import { readFileSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';

const [baseUrlArgument, profileArgument, manifestArgument, destinationArgument] = process.argv.slice(2);

if (!baseUrlArgument || !profileArgument || !manifestArgument || !destinationArgument) {
    throw new Error('Usage: capture-visual-regression.mjs BASE_URL PROFILE MANIFEST DESTINATION');
}

const baseUrl = new URL(baseUrlArgument);
const allowedHosts = new Set(['127.0.0.1', 'localhost', '[::1]', '192.168.40.70']);
if (baseUrl.protocol !== 'http:' || baseUrl.port !== '18081' || !allowedHosts.has(baseUrl.hostname)) {
    throw new Error(`Only the approved development server on port 18081 is allowed: ${baseUrl.href}`);
}
const basePath = `${baseUrl.pathname.replace(/\/$/, '')}/`;

const profilePath = resolve(profileArgument);
const destinationPath = resolve(destinationArgument);
const routes = readRoutes(resolve(manifestArgument));
const viewports = [
    [360, 900],
    [768, 1024],
    [1024, 900],
    [1440, 1000]
];
const driverPort = await findOpenPort();
const driverUrl = `http://127.0.0.1:${driverPort}`;
const driver = spawn('/snap/bin/geckodriver', ['--port', String(driverPort)], {
    stdio: ['ignore', 'pipe', 'pipe']
});
let driverError = '';
let sessionId;

driver.stdout.on('data', () => {});
driver.stderr.on('data', (chunk) => {
    driverError += chunk.toString();
    if (driverError.length > 4000) {
        driverError = driverError.slice(-4000);
    }
});

try {
    await waitForDriver();
    const session = await request('POST', '/session', {
        capabilities: {
            alwaysMatch: {
                acceptInsecureCerts: true,
                browserName: 'firefox',
                pageLoadStrategy: 'normal',
                'moz:firefoxOptions': {
                    args: ['-headless', '-profile', profilePath]
                }
            }
        }
    });
    sessionId = session.sessionId;
    await request('POST', `/session/${sessionId}/timeouts`, {
        implicit: 0,
        pageLoad: 30000,
        script: 10000
    });

    for (const route of routes) {
        for (const [width, height] of viewports) {
            await request('POST', `/session/${sessionId}/window/rect`, {
                height,
                width,
                x: 0,
                y: 0
            });
            const targetUrl = new URL(route.path, `${baseUrl.href.replace(/\/$/, '')}/`);
            if (targetUrl.origin !== baseUrl.origin || !targetUrl.pathname.startsWith(basePath)) {
                throw new Error(`Route is outside the approved development application: ${route.name}`);
            }
            await request('POST', `/session/${sessionId}/url`, { url: targetUrl.href });
            await delay(350);

            const currentUrl = new URL(await request('GET', `/session/${sessionId}/url`));
            if (
                currentUrl.origin !== baseUrl.origin
                || !currentUrl.pathname.startsWith(basePath)
                || currentUrl.pathname === `${basePath}login`
            ) {
                throw new Error(`Authenticated session is unavailable for route: ${route.name}`);
            }

            const screenshot = await request('GET', `/session/${sessionId}/screenshot`);
            const output = resolve(destinationPath, `${route.name}-${width}x${height}.png`);
            writeFileSync(output, Buffer.from(screenshot, 'base64'), { mode: 0o600 });
        }
    }
} finally {
    if (sessionId) {
        await request('DELETE', `/session/${sessionId}`).catch(() => {});
    }
    driver.kill('SIGTERM');
}

function readRoutes(manifestPath) {
    return readFileSync(manifestPath, 'utf8')
        .split(/\r?\n/)
        .filter((line) => line.trim())
        .map((line) => {
            const [name, path] = line.split('\t');
            if (!name || !path || !/^[a-z0-9-]+$/.test(name)) {
                throw new Error(`Invalid visual regression route: ${line}`);
            }
            return { name, path };
        });
}

async function waitForDriver() {
    const deadline = Date.now() + 10000;
    while (Date.now() < deadline) {
        if (driver.exitCode !== null) {
            throw new Error(`geckodriver exited before startup: ${driverError.trim()}`);
        }
        try {
            await request('GET', '/status');
            return;
        } catch (error) {
            await delay(100);
        }
    }
    throw new Error(`geckodriver did not become ready: ${driverError.trim()}`);
}

async function request(method, path, body) {
    const response = await fetch(`${driverUrl}${path}`, {
        body: body === undefined ? undefined : JSON.stringify(body),
        headers: body === undefined ? {} : { 'Content-Type': 'application/json' },
        method,
        signal: AbortSignal.timeout(45000)
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
