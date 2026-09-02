const eventLog = document.getElementById('eventLog');
const bridgeLog = document.getElementById('bridgeLog');

function log(box, msg) {
    const el = document.createElement('div');
    el.textContent = `[${new Date().toLocaleTimeString()}] ${msg}`;
    box.appendChild(el);
    box.scrollTop = box.scrollHeight;
}

// ── 1. RUNTIME INFO ──
window.addEventListener('load', () => {
    document.getElementById('runtimeInfo').textContent =
        `v${AnyLogic.runtime.version} (${AnyLogic.runtime.platform})`;

    // ── 2. SHARED STATE: Subscribe ──
    AnyLogic.state.subscribe('globalTarget', (newVal) => {
        document.getElementById('stateTargetVal').textContent = newVal || '(empty)';
        log(eventLog, `State 'globalTarget' changed to: ${newVal}`);
    });

    // Start listening to the continuous feed
    startFeed();

    // Notify Java that the UI is ready
    AnyLogic.call('__ready__').catch(console.warn);
});

// ── 2. SHARED STATE: Get & Set ──
async function setSharedState() {
    const val = document.getElementById('inputTarget').value;
    await AnyLogic.state.set('globalTarget', val);
    document.getElementById('inputTarget').value = '';
}

async function readSharedState() {
    const val = await AnyLogic.state.get('globalTarget');
    document.getElementById('readResult').textContent = `Read: ${val}`;
}

// ── 3. DIALOG CONTROL ──
function updateTitle() {
    const title = document.getElementById('inputTitle').value;
    if (title) AnyLogic.dialog.setTitle(title);
}

function closeDialog() {
    if (confirm('Do you want to close the simulation window?')) {
        AnyLogic.dialog.close();
    }
}

// ── 4. EVENT VARIANTS (once & off) ──
function listenOnce() {
    log(eventLog, "Listening to 'secretToken' with .once()...");
    AnyLogic.events.once('secretToken', (data) => {
        log(eventLog, `Token received (single execution)!: ${data.token}`);
    });
}

function requestSecret() {
    AnyLogic.call('requestToken');
}

// Feed with toggle using .on() and .off()
let feedActive = false;
const feedHandler = (data) => {
    log(eventLog, `Live Feed: tick=${data.tick}`);
};

function startFeed() {
    if (!feedActive) {
        AnyLogic.events.on('feedTick', feedHandler);
        feedActive = true;
        document.getElementById('btnToggleFeed').textContent = "Stop Feed (off)";
        document.getElementById('feedStatus').textContent = "Feed: Active";
    }
}

function stopFeed() {
    if (feedActive) {
        AnyLogic.events.off('feedTick', feedHandler);
        feedActive = false;
        document.getElementById('btnToggleFeed').textContent = "Resume Feed (on)";
        document.getElementById('feedStatus').textContent = "Feed: Paused";
        log(eventLog, "Listener removed with .off()");
    }
}

function toggleFeed() {
    if (feedActive) stopFeed(); else startFeed();
}

// ── 5. FILE SYSTEM TEXT MODE (isBase64 = false) ──
async function saveTextFile() {
    const text = document.getElementById('fileContent').value;
    const path = await AnyLogic.files.saveDialog("Save text file", "notes.txt");
    if (path) {
        // Save as UTF-8 plain text (isBase64 = false)
        await AnyLogic.files.write(path, text, false);
        document.getElementById('filePathInfo').textContent = `Saved to: ${path}`;
    }
}

async function openTextFile() {
    const path = await AnyLogic.files.openDialog("Open text file", "*.txt");
    if (path) {
        // Read as UTF-8 plain text (isBase64 = false)
        const text = await AnyLogic.files.read(path, false);
        document.getElementById('fileContent').value = text;
        document.getElementById('filePathInfo').textContent = `Read from: ${path}`;
    }
}

// ── 6. JAVA BRIDGE COMMANDS ──
async function callJavaCustom(action) {
    try {
        log(bridgeLog, `Calling command: ${action}`);
        await AnyLogic.call(action, { timestamp: Date.now() });
        log(bridgeLog, `Command ${action} completed.`);
    } catch (err) {
        log(bridgeLog, `Error: ${err.message || err}`);
    }
}
