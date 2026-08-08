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
let bidiClient;
let browsingContextId;

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
                webSocketUrl: true,
                'moz:firefoxOptions': {
                    args: ['-headless', '-profile', profilePath]
                }
            }
        }
    });
    sessionId = session.sessionId;
    bidiClient = await connectBidi(session.capabilities.webSocketUrl);
    const contextTree = await bidiClient.command('browsingContext.getTree', {});
    browsingContextId = contextTree.contexts[0]?.context;
    if (!browsingContextId) {
        throw new Error('Firefox did not provide a browsing context for viewport emulation');
    }
    await request('POST', `/session/${sessionId}/timeouts`, {
        implicit: 0,
        pageLoad: 30000,
        script: 10000
    });

    for (const route of routes) {
        for (const [width, height] of viewports) {
            await setViewport(width, height);
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
    bidiClient?.close();
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

async function setViewport(width, height) {
    await bidiClient.command('browsingContext.setViewport', {
        context: browsingContextId,
        devicePixelRatio: 1,
        viewport: { height, width }
    });
    const metrics = await viewportMetrics();
    if (metrics.width !== width || metrics.height !== height) {
        throw new Error(
            `Requested viewport ${width}x${height}, but Firefox provided `
            + `${metrics.width}x${metrics.height}`
        );
    }
    if (metrics.scrollWidth > metrics.width) {
        throw new Error(
            `Horizontal overflow at ${width}x${height}: `
            + `${metrics.scrollWidth}px content in ${metrics.width}px viewport`
        );
    }
}

function viewportMetrics() {
    return request('POST', `/session/${sessionId}/execute/sync`, {
        script: `return {
            width: window.innerWidth,
            height: window.innerHeight,
            scrollWidth: document.documentElement.scrollWidth
        };`,
        args: []
    });
}

function connectBidi(url) {
    if (!url) {
        throw new Error('Firefox did not provide a WebDriver BiDi URL');
    }
    return new Promise((resolveConnect, rejectConnect) => {
        const socket = new WebSocket(url);
        const timeout = setTimeout(() => {
            socket.close();
            rejectConnect(new Error('WebDriver BiDi connection timed out'));
        }, 10000);
        socket.addEventListener('open', () => {
            clearTimeout(timeout);
            resolveConnect(createBidiClient(socket));
        }, { once: true });
        socket.addEventListener('error', () => {
            clearTimeout(timeout);
            rejectConnect(new Error('WebDriver BiDi connection failed'));
        }, { once: true });
    });
}

function createBidiClient(socket) {
    let nextId = 1;
    const pending = new Map();
    socket.addEventListener('message', (event) => {
        const message = JSON.parse(String(event.data));
        const pendingCommand = pending.get(message.id);
        if (!pendingCommand) {
            return;
        }
        pending.delete(message.id);
        if (message.type === 'success') {
            pendingCommand.resolve(message.result);
        } else {
            pendingCommand.reject(new Error(
                message.message || message.error || 'WebDriver BiDi command failed'
            ));
        }
    });
    return {
        command(method, params) {
            const id = nextId++;
            return new Promise((resolveCommand, rejectCommand) => {
                pending.set(id, {
                    reject: rejectCommand,
                    resolve: resolveCommand
                });
                socket.send(JSON.stringify({ id, method, params }));
            });
        },
        close() {
            socket.close();
        }
    };
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
