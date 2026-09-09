/* One owner for the existing UDOS controls; no delayed button overrides. */
(function () {
  "use strict";

  function byId(id) { return document.getElementById(id); }
  function safe(text) {
    return String(text == null ? "" : text).replace(/[&<>"']/g, function (c) {
      return {"&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"}[c];
    });
  }

  window.noteRaw = function (html) {
    var out = byId("out");
    if (out) out.innerHTML = html;
  };

  window.callNative = function (method, argument) {
    var allowed = ["command", "listen", "wake", "sleep", "speak", "openCamera",
      "openSettings", "openApps", "openSite", "voiceStatus", "setVoiceLanguage"];
    if (allowed.indexOf(method) < 0) return false;
    try {
      if (window.UDOS && typeof window.UDOS[method] === "function") {
        if (typeof argument === "undefined") window.UDOS[method]();
        else window.UDOS[method](argument);
        return true;
      }
      window.noteRaw("<b>PHONE:</b> Open the installed UDOS Mobile app to use microphone and phone controls.");
    } catch (error) {
      window.noteRaw("<b>PHONE:</b> " + safe(error.message || "The Android action failed."));
    }
    return false;
  };

  window.cmd = function (input) {
    var text = String(input || "status").trim();
    var lower = text.toLowerCase();
    if (text.length > 1000) {
      window.noteRaw("<b>NOVA:</b> Please use a command of 1,000 characters or fewer.");
      return;
    }
    if (lower === "home") { window.home(); return; }
    if (lower === "tools" || lower === "tool") { window.tools(); return; }
    if (lower === "map" || lower === "project map" || lower === "brain map") { window.mapCore(); return; }
    if (lower === "set" || lower === "settings panel") { window.settings(); return; }
    if (lower === "voice" || lower === "mic" || lower === "listen") { window.callNative("listen"); return; }
    if (lower === "wake") { window.callNative("wake"); return; }
    if (lower === "sleep" || lower === "stop" || lower === "stop listening") { window.callNative("sleep"); return; }
    if (lower === "speak" || lower === "read" || lower === "read response") { window.readResponse(); return; }
    if (lower === "voice check" || lower === "voice status") { window.callNative("voiceStatus"); return; }
    window.noteRaw("<b>COMMAND:</b> " + safe(text));
    window.callNative("command", text);
  };

  window.send = function () {
    var q = byId("q");
    var text = q ? q.value : "status";
    if (q) q.value = "";
    window.cmd(text);
  };

  window.readResponse = function () {
    var out = byId("out");
    var text = out ? out.innerText || out.textContent : "UDOS Mobile is ready.";
    window.callNative("speak", text);
  };

  window.voiceSetup = function () { window.callNative("listen"); };
  window.closeSheet = function () { byId("sheet").style.display = "none"; };
  function show(html) {
    byId("sheetBody").innerHTML = html;
    byId("sheet").style.display = "flex";
  }
  function closeButton() { return '<button class="close" onclick="closeSheet()">Close</button>'; }
  function action(label, detail, code) {
    return '<div class="row"><div><strong>' + label + '</strong><span>' + detail
      + '</span></div><button class="chip" onclick="closeSheet();' + code + '">OPEN</button></div>';
  }

  window.home = function () {
    window.closeSheet();
    var q = byId("q");
    if (q) q.value = "";
    window.noteRaw("<b>NOVA:</b> Camera, settings, apps and text commands work without Pi5.<br>"
      + "Tap Voice for one command. Wake starts a two-minute session while UDOS is visible.<br>"
      + "Say <b>Hey Nova, open camera</b>. Say <b>sleep</b> to stop.<br>"
      + "Tools → Voice Check shows actual microphone, speech and TTS availability.");
  };

  window.settings = function () {
    show('<h2>UDOS Settings</h2>'
      + action('Phone settings', 'Android settings and default Home app.', "callNative('openSettings')")
      + action('Voice Check', 'Microphone permission, recognition service and speech data.', "callNative('voiceStatus')")
      + action('English recognition', 'Select en-US. Engine support is checked when listening starts.', "callNative('setVoiceLanguage','en-US')")
      + action('Tamil recognition', 'Select ta-IN. Requires Tamil recognition data in the phone speech service.', "callNative('setVoiceLanguage','ta-IN')")
      + '<p>Recognition uses the Android on-device service when available. The default speech service may send audio online. '
      + 'Wake stops when you leave UDOS. Pi5 is optional.</p>' + closeButton());
  };

  window.tools = function () {
    show('<h2>UDOS Tools</h2>'
      + action('Voice', 'Listen for one command.', "callNative('listen')")
      + action('Voice Check', 'Inspect the phone speech capabilities.', "callNative('voiceStatus')")
      + action('Camera', 'Open the Android camera.', "callNative('openCamera')")
      + action('Apps', 'Open Android apps settings.', "callNative('openApps')")
      + action('Phone settings', 'Open Android settings.', "callNative('openSettings')")
      + action('Live UDOS', 'Open the existing website.', "callNative('openSite')")
      + action('Project map', 'View the existing component roles.', 'mapCore()')
      + action('Pi status', 'Optional explicit request to the configured EVE endpoint.', "cmd('pi status')")
      + closeButton());
  };

  window.mapCore = function () {
    show('<h2>Universal Dragon · Aslam</h2>'
      + '<p>Phone voice or text → UDOS Mobile → bounded Android commands → visible result and spoken reply.</p>'
      + '<p>NOVA / NovaKutty and EVE are intelligence components. QBIT NOVA is the language/runtime layer.</p>'
      + '<p>The existing Pi status hook remains optional. Camera, settings, apps and the launcher work without Pi5. '
      + 'General AI conversation, background wake and control inside other apps are not implemented in this repair.</p>'
      + closeButton());
  };
  window.udosHome = window.home;
}());
