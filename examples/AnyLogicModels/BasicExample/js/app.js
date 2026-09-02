        // Listen for the response event from AnyLogic
        AnyLogic.events.on('helloResponse', (msg) => {
            document.getElementById('result').innerText = "AnyLogic says: " + msg;
        });

        document.getElementById('helloBtn').addEventListener('click', async () => {
            try {
                // Call 'hello' command in AnyLogic
                await AnyLogic.call("hello");
            } catch (error) {
                console.error(error);
                document.getElementById('result').innerText = "Error: " + error.message;
            }
        });
