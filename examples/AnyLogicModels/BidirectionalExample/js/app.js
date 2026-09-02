        const logs = document.getElementById('logs');
        function log(msg) {
            const div = document.createElement('div');
            div.innerText = `> ${msg}`;
            logs.appendChild(div);
            logs.scrollTop = logs.scrollHeight;
        }

        async function setSpeed(speed) {
            try {
                log(`Sending command: setSpeed(${speed})`);
                await AnyLogic.call("setSpeed", { speed: speed });
                log(`Command successful.`);
            } catch (error) {
                log(`Error: ${error.message}`);
            }
        }

        // Listen for events from Java
        AnyLogic.events.on('timeUpdated', (data) => {
            document.getElementById('timeValue').innerText = data.time.toFixed(2);
        });

        AnyLogic.events.on('populationChanged', (data) => {
            document.getElementById('agentCount').innerText = data.count;
            log(`Population changed: ${data.count} agents`);
        });
