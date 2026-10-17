# Seck8s Web Frontend

Standalone browser client for the existing gateway. No backend source files are changed.

Run a static server from this directory, for example:

```powershell
npx serve . -l 5173
```

Open `http://127.0.0.1:5173`. The default API address is `http://127.0.0.1:8080`; use the settings button to change it. The gateway must allow the page origin with CORS for browser requests.
