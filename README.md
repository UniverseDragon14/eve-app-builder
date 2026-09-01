# EVE App Builder

Local Next.js project-forging dashboard with bundled Android prototypes.

## Current architecture

| Area | Implementation |
|---|---|
| Browser dashboard | Next.js App Router UI |
| EVE plan endpoint | deterministic keyword/app-type planner; no model call |
| Forge endpoint | invokes a fixed local Python forge script |
| Build endpoint | runs npm install and npm run build inside a validated project directory |
| Run endpoint | starts a selected project development server and records PID/port state |
| Stop endpoint | terminates the recorded process and removes its state entry |
| Android sources | EVE Mobile and UDOS Mobile prototype projects |
| CI | workflows that build/upload Android debug APK artifacts when project files exist |

API routes:

- POST /api/eve
- GET and POST /api/forge
- POST /api/build
- GET and POST /api/run
- POST /api/stop

## Start the dashboard

~~~bash
npm install
npm run dev
~~~

The configured development server listens on port 3050.

## Local dependency

Forge/build/run operations expect a local EVE Forge workspace and forge.py under the current user’s home directory. That external workspace is not created by this repository automatically.

## Truth boundary

The EVE planning route returns a local rules-based plan. “Build ready” means the UI can continue to the preview/build workflow; it is not proof that an AI independently designed or verified an application.

Android folders are source prototypes. A successful workflow artifact, not the presence of a folder, is the build proof.

## Security warning

Treat the process-control APIs as **local administration endpoints**:

- they currently have no user authentication or authorization
- run/build can install packages and launch project scripts
- stop sends signals to a stored process identifier
- development projects bind to network interfaces
- state and logs are stored in the local EVE Forge workspace

Do not publish this server directly. Add authenticated owner approval, CSRF/origin protection, strict project ownership checks, dependency policy, resource limits, isolated build containers, and audit logging before multi-user or internet use.
