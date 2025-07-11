package com.linsheng.FATJS.server;

import android.util.Log;

import com.caoccao.javet.exceptions.JavetException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.linsheng.FATJS.config.GlobalVariableHolder;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class FatJSHttpServer extends NanoHTTPD {

    private static final String TAG = "FatJSHttpServer";
    public static final int DEFAULT_PORT = 8080;
    private final Gson gson;

    public FatJSHttpServer(int port) throws IOException {
        super(port);
        // Ensure V8 is ready before starting the server, though Application class should do it.
        // GlobalVariableHolder.ensureV8RuntimeInitialized();
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        Log.i(TAG, "FatJSHttpServer running on port: " + port);
        this.gson = new GsonBuilder().serializeNulls().create();
    }

    public FatJSHttpServer() throws IOException {
        this(DEFAULT_PORT);
    }

    // Define a simple class for request parsing
    private static class ScriptRequest {
        String script;
        String functionName;
        List<Object> args;
    }

    // Define a simple class for JSON responses
    private static class ScriptResponse {
        String status; // "success" or "error"
        Object result;
        String message;

        public ScriptResponse(String status, Object result, String message) {
            this.status = status;
            this.result = result;
            this.message = message;
        }
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();
        Log.i(TAG, "Received request: " + method + " " + uri);

        if (Method.POST.equals(method) && "/run-script".equals(uri)) {
            return handleRunScript(session);
        }

        String msg = "<html><body><h1>FatJS HTTP Server</h1><p>Endpoint not found or method not supported.</p></body></html>";
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/html", msg);
    }

    private Response handleRunScript(IHTTPSession session) {
        Map<String, String> files = new HashMap<>();
        String requestBody;
        try {
            // NanoHTTPD requires parsing the request body this way for POST
            session.parseBody(files); // Needed to populate params for application/x-www-form-urlencoded
                                      // For JSON, we need to read the input stream directly if content type is application/json

            // Read the raw POST body
            // To handle large bodies, NanoHTTPD might put it into a temp file.
            // For simplicity, assuming it's in memory for now (session.getInputStream()).
            // This part needs to be robust for different ways NanoHTTPD handles POST body.

            long contentLength = 0;
            String contentLengthHeader = session.getHeaders().get("content-length");
            if (contentLengthHeader != null) {
                try {
                    contentLength = Long.parseLong(contentLengthHeader);
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Could not parse content-length header: " + contentLengthHeader);
                }
            }

            if (contentLength > 0) {
                InputStream inputStream = session.getInputStream();
                // NanoHTTPD might provide a custom InputStream that needs careful handling
                // For JSON, we'd typically read it like this:
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                requestBody = sb.toString();
            } else {
                 // Fallback or if NanoHTTPD puts form data into params (less likely for raw JSON POST)
                requestBody = session.getQueryParameterString(); // This might be null or form-urlencoded
                if (requestBody == null && files.containsKey("postData")) { // NanoHTTPD internal for large bodies
                    String tempFilePath = files.get("postData");
                    // Read from tempFilePath if needed, this is more complex.
                    // For now, we assume small JSON body passed directly.
                    // This part might need refinement based on how NanoHTTPD handles large JSON.
                    // For now, assuming the above direct read from input stream works for typical JSON posts.
                    Log.e(TAG, "Request body was potentially large and saved to temp file, not handled yet: " + tempFilePath);
                    return newJsonResponse(new ScriptResponse("error", null, "Request body potentially too large or not processed correctly."), Response.Status.BAD_REQUEST);
                }
                if (requestBody == null) { // If still null, means no body or not read.
                     return newJsonResponse(new ScriptResponse("error", null, "Request body is empty or could not be read."), Response.Status.BAD_REQUEST);
                }
            }

            Log.d(TAG, "Request Body: " + requestBody);

        } catch (IOException | ResponseException e) {
            Log.e(TAG, "Error reading request body", e);
            return newJsonResponse(new ScriptResponse("error", null, "Error reading request body: " + e.getMessage()), Response.Status.INTERNAL_SERVER_ERROR);
        }

        ScriptRequest scriptRequest;
        try {
            scriptRequest = gson.fromJson(requestBody, ScriptRequest.class);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "JSON parsing error", e);
            return newJsonResponse(new ScriptResponse("error", null, "Invalid JSON format: " + e.getMessage()), Response.Status.BAD_REQUEST);
        }

        if (scriptRequest == null || (scriptRequest.script == null && scriptRequest.functionName == null)) {
            return newJsonResponse(new ScriptResponse("error", null, "Invalid request: 'script' or 'functionName' is required."), Response.Status.BAD_REQUEST);
        }

        // Ensure V8 runtime is ready
        GlobalVariableHolder.ensureV8RuntimeInitialized();
        if (GlobalVariableHolder.v8Runtime == null) {
            Log.e(TAG, "V8 Runtime is not initialized. Cannot execute script.");
            return newJsonResponse(new ScriptResponse("error", null, "V8 Runtime not available."), Response.Status.INTERNAL_SERVER_ERROR);
        }

        String jsToExecute;
        if (scriptRequest.script != null && !scriptRequest.script.trim().isEmpty()) {
            jsToExecute = scriptRequest.script;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(scriptRequest.functionName).append("(");
            if (scriptRequest.args != null) {
                for (int i = 0; i < scriptRequest.args.size(); i++) {
                    Object arg = scriptRequest.args.get(i);
                    // Simple argument stringification. For complex objects, gson.toJson(arg) is safer.
                    if (arg instanceof String) {
                        sb.append("'").append(arg.toString().replace("'", "\\'")).append("'");
                    } else if (arg instanceof Map || arg instanceof List) {
                        sb.append(gson.toJson(arg)); // Convert maps/lists to JSON strings for JS
                    } else {
                        sb.append(arg); // Numbers, booleans
                    }
                    if (i < scriptRequest.args.size() - 1) {
                        sb.append(", ");
                    }
                }
            }
            sb.append(");");
            jsToExecute = sb.toString();
        }

        Log.i(TAG, "Executing JavaScript: " + jsToExecute);

        try {
            // It's generally better to run UI-interacting or potentially long tasks off the HTTP request thread.
            // For now, direct execution for simplicity. Consider an ExecutorService for real use.
            Object result = GlobalVariableHolder.v8Runtime.getExecutor(jsToExecute).executeValue();
            return newJsonResponse(new ScriptResponse("success", result, "Script executed successfully."), Response.Status.OK);
        } catch (JavetException e) {
            Log.e(TAG, "JavetException during script execution", e);
            // Get the detailed JS stack trace if available
            String jsError = e.getMessage();
            if (e.getDetails() != null) {
                jsError = e.getDetails().getMessage();
            }
            return newJsonResponse(new ScriptResponse("error", null, "JavaScript execution error: " + jsError), Response.Status.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected exception during script execution", e);
            return newJsonResponse(new ScriptResponse("error", null, "Unexpected error: " + e.getMessage()), Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private Response newJsonResponse(Object responseObject, Response.Status status) {
        String json = gson.toJson(responseObject);
        return newFixedLengthResponse(status, "application/json", json);
    }

    public void stopServer() {
        stop();
        Log.i(TAG, "FatJSHttpServer stopped.");
    }
}
