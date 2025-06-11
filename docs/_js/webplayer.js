let binaryData = null, fileType = null, descriptorData = null, descriptorType = null, scratchpadData = null;

let OSDVisible = false, screenRotated = false, fastForwarding = false, midletPaused = false;

const keyValues = {
    k0: 0, k1: 0, k2: 0, k3: 0, k4: 0,
    k5: 0, k6: 0, k7: 0, k8: 0, k9: 0,
    ka: 0, kb: 0, ku: 0, kd: 0, kl: 0,
    kr: 0, kc: 0, ls: 0, rs: 0, ff: 0,
    ro: 0, pa: 0
};

const originalConsoleLog = console.log;

// console override function to check for the FreeJ2ME message indicating that the canvas was created (which is when we show the cheerpJ display)
function checkForKeyword(message) 
{
    const keyword = "Create Canvas:";
    if (typeof message === 'string' && message.includes(keyword)) 
    {
        // Run your code here
        document.getElementById('cheerpjDisplay').style.transitionDuration = '0.5s';
        document.getElementById('loadAnim').style.opacity = '0';
        setTimeout(() => {
            document.getElementById('disclaimer').style.display = 'none';
            document.getElementById('loadAnim').style.display = 'none';
            document.getElementById('cheerpjDisplay').style.visibility = 'visible';
            document.getElementById('commandBar').style.display = 'block';
            setTimeout(() => 
            { 
                document.getElementById('commandBar').style.opacity = '1'; 
                document.getElementById('cheerpjDisplay').style.opacity = '1'; 
            }, 500);
        }, 500);
    }
}

console.log = function(...args) 
{
    originalConsoleLog.apply(console, args);

    args.forEach(arg => checkForKeyword(arg));
};


// OSD commandBar functions 
function toggleOSDButtons() 
{
    OSDVisible = !OSDVisible;
    if(OSDVisible) { document.getElementById('onScreenButtons').style.display = 'block'; }
    else { document.getElementById('onScreenButtons').style.display = 'none'; }
}

function rotateScreen() // Ctrl+Alt+R key
{ 
    screenRotated = !screenRotated;
    if(screenRotated) { keyValues['ro'] = 1; }
    else { keyValues['ro'] = 0; }
    updateKeyPresses(); 
}

function pauseResume() // Ctrl+Alt+X key
{ 
    midletPaused = !midletPaused;
    if(midletPaused) { keyValues['pa'] = 1; }
    else { keyValues['pa'] = 0; }
    updateKeyPresses(); 
}

function fastForward() // Ctrl+Alt+Space key
{ 
    fastForwarding = !fastForwarding;
    if(fastForwarding) { keyValues['ff'] = 1; }
    else { keyValues['ff'] = 0; }
    updateKeyPresses(); 
}

// OSD Phone buttons
function pressKey(index)
{
    keyValues[index] = 1;
    updateKeyPresses();
}

function releaseKey(index)
{
    keyValues[index] = 0;
    updateKeyPresses();
}

function updateKeyPresses() 
{
    // Create and dispatch the key change event
    let keyStrings = Object.entries(keyValues)
        .map(([key, value]) => `${key}:${value}`)
        .join('\n');

    cheerpOSAddStringFile("/str/FreeJ2MEExternalKeyEvents.txt", keyStrings);
}

// Page content functions
function loadPage() 
{
    document.getElementById('binaryLoad').style.opacity = '1';
    document.getElementById('commandBar').style.display = 'none';
    document.getElementById('onScreenButtons').style.display = 'none';
    document.getElementById('descriptorLoad').style.display = 'none';
    document.getElementById('startupSettings').style.display = 'none';
    document.getElementById('loadAnim').style.display = 'none';

    document.getElementById('jarFilePickerButton').addEventListener('click', () => 
    {
        document.getElementById('jarFileInput').click();
    });

    document.getElementById('descFilePickerButton').addEventListener('click', () => 
    {
        document.getElementById('descFileInput').click();
    });

    document.getElementById('spFilePickerButton').addEventListener('click', () => 
    {
        document.getElementById('spFileInput').click();
    });

    document.getElementById('jarFileInput').addEventListener('change', (event) => 
    {
        const files = event.target.files;
        if (files.length > 0) 
        {
            const file = files[0];
            const reader = new FileReader();

            reader.onload = async (e) => 
            {
                binaryData = new Uint8Array(e.target.result);
                const lastDotIndex = file.name.lastIndexOf('.');
                fileType = file.name.substring(lastDotIndex + 1);
                document.getElementById('binaryLoad').style.opacity = '0';
                setTimeout(() => {
                    document.getElementById('binaryLoad').style.display = 'none';
                    document.getElementById('descriptorLoad').style.display = 'flex';
                    setTimeout(() => { document.getElementById('descriptorLoad').style.opacity = '1'; }, 16);
                }, 500);
                
            };

            reader.readAsArrayBuffer(file);
        }
    });

    document.getElementById('skipDescFile').addEventListener('click', () => 
    {
        document.getElementById('descriptorLoad').style.opacity = '0';
        setTimeout(() => {
            document.getElementById('descriptorLoad').style.display = 'none';
            document.getElementById('startupSettings').style.display = 'flex';
            setTimeout(() => { document.getElementById('startupSettings').style.opacity = '1'; }, 16);
        }, 500);
    });

    document.getElementById('descFileInput').addEventListener('change', (event) => 
    {
        const files = event.target.files;
        if (files.length > 0) 
        {
            const file = files[0];
            const reader = new FileReader();

            reader.onload = async (e) => 
            {
                descriptorData = new Uint8Array(e.target.result);
                const lastDotIndex = file.name.lastIndexOf('.');
                descriptorType = file.name.substring(lastDotIndex + 1);
                if(descriptorType === "jam") 
                {
                    document.getElementById('dojaHelperText').style.display = 'block';
                    document.getElementById('spFilePickerButton').style.display = 'block';
                    document.getElementById('skipDescFile').style.display = 'none';
                }
                else 
                {
                    document.getElementById('descriptorLoad').style.opacity = '0';
                    setTimeout(() => {
                        document.getElementById('descriptorLoad').style.display = 'none';
                        document.getElementById('startupSettings').style.display = 'flex';
                        setTimeout(() => { document.getElementById('startupSettings').style.opacity = '1'; }, 16);
                    }, 500);
                    document.getElementById('startupSettings').style.opacity = '1';
                }
            };

            reader.readAsArrayBuffer(file);
        }
    });

    document.getElementById('spFileInput').addEventListener('change', (event) => 
    {
        const files = event.target.files;
        if (files.length > 0) 
        {
            const file = files[0];
            const reader = new FileReader();

            reader.onload = async (e) => 
            {
                scratchpadData = new Uint8Array(e.target.result);
                document.getElementById('descriptorLoad').style.opacity = '0';
                setTimeout(() => {
                    document.getElementById('descriptorLoad').style.display = 'none';
                    document.getElementById('startupSettings').style.display = 'flex';
                    setTimeout(() => { document.getElementById('startupSettings').style.opacity = '1'; }, 16);
                }, 500);
            };

            reader.readAsArrayBuffer(file);
        }
    });

    document.getElementById('startFreeJ2ME').addEventListener('click', () => 
    {
        document.getElementById('startupSettings').style.opacity = '0';
        setTimeout(() => {
            document.getElementById('startupSettings').style.display = 'none';
            document.getElementById('loadAnim').style.display = 'flex';
            setTimeout(() => { document.getElementById('loadAnim').style.opacity = '1'; }, 16);
            setTimeout(() => { document.getElementById('loadingindicator').style.opacity = '1'; }, 516);
            startFreeJ2ME();
        }, 500);        
    });
}

function getScreenRes() 
{
    const selector = document.getElementById('screenResSelector');
    const selectedValue = selector.value;
    const tokens = selectedValue.split('x');

    cmdWidth = tokens[0];
    cmdHeight = tokens[1];
}

async function startFreeJ2ME() 
{
    await cheerpjInit({ version:8, javaProperties: ["file.encoding="+document.getElementById('encodingSelector').value]});
    
    // Add the jar's binary and often optional descriptor data to CheerpJ (unless it's doja, in which case both descriptor and scratchpad must be sent)
    cheerpOSAddStringFile("/str/app."+fileType, binaryData);
    if(descriptorData !== null) { cheerpOSAddStringFile("/str/app."+descriptorType, descriptorData); }
    if(scratchpadData !== null) { cheerpOSAddStringFile("/str/app.sp", scratchpadData); }
    
    getScreenRes();

    let keyLayout = document.getElementById('keyMapSelector').value;

    cheerpjCreateDisplay(-1, -1, document.body);
    document.getElementById('cheerpjDisplay').style.visibility = 'hidden';
    document.getElementById('cheerpjDisplay').style.opacity = '0';
    await cheerpjRunJar("/app/freej2me-plus/_webplayer/freej2me.jar", "/str/app."+fileType, "1", cmdWidth, cmdHeight, "1", keyLayout);
}

document.addEventListener("DOMContentLoaded", loadPage);