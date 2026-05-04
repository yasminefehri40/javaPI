package otemps.services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;

public class AIService {

    private static final String DEFAULT_GROQ_API_KEY = "";
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "meta-llama/llama-4-scout-17b-16e-instruct";
    private static final long MAX_BASE64_IMAGE_BYTES = 4L * 1024L * 1024L;

    public AIService() {
        System.out.println("AIService initialise");
    }

    public String analyzeImage(File imageFile) {
        try {
            if (imageFile == null || !imageFile.exists()) {
                return buildErrorAnalysis("Fichier image introuvable.");
            }

            if (imageFile.length() > MAX_BASE64_IMAGE_BYTES) {
                return buildErrorAnalysis("Image trop lourde pour l'analyse vision Groq. Utilisez une image de moins de 4 MB.");
            }

            return callGroqVisionApi(imageFile);
        } catch (Exception e) {
            System.err.println("Erreur analyse image: " + e.getMessage());
            return buildErrorAnalysis("Erreur pendant l'analyse: " + e.getMessage());
        }
    }

    private String callGroqVisionApi(File imageFile) throws IOException {
        String apiKey = getGroqApiKey();
        String mimeType = detectMimeType(imageFile);
        String base64Image = Base64.getEncoder().encodeToString(Files.readAllBytes(imageFile.toPath()));
        String dataUrl = "data:" + mimeType + ";base64," + base64Image;

        URL url = new URL(GROQ_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);

        String requestBody = buildVisionRequestBody(dataUrl);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestBody.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        String responseBody = readResponse(responseCode >= 200 && responseCode < 300
                ? conn.getInputStream()
                : conn.getErrorStream());

        if (responseCode < 200 || responseCode >= 300) {
            System.err.println("Groq API error: " + responseBody);
            return buildErrorAnalysis("La reponse API a echoue (" + responseCode + ").");
        }

        return parseGroqResponse(responseBody);
    }

    private String getGroqApiKey() {
        String envKey = System.getenv("GROQ_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            return envKey.trim();
        }
        return DEFAULT_GROQ_API_KEY;
    }

    private String detectMimeType(File imageFile) throws IOException {
        String mimeType = Files.probeContentType(imageFile.toPath());
        if (mimeType != null && mimeType.startsWith("image/")) {
            return mimeType;
        }

        String fileName = imageFile.getName().toLowerCase();
        if (fileName.endsWith(".png")) {
            return "image/png";
        }
        if (fileName.endsWith(".gif")) {
            return "image/gif";
        }
        if (fileName.endsWith(".bmp")) {
            return "image/bmp";
        }
        if (fileName.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }

    private String buildVisionRequestBody(String dataUrl) {
        String prompt = """
                Analyse cette image patrimoniale ou historique.
                Reponds uniquement sous forme d'objet JSON avec exactement ces cles:
                TITRE, EPOQUE, ORIGINE, MATERIAUX, DESCRIPTION, IMPORTANCE.
                Regles:
                - Si une information n'est pas certaine, ecris "Non determine".
                - DESCRIPTION doit etre concise mais utile.
                - IMPORTANCE doit expliquer en 1 ou 2 phrases l'interet historique, culturel ou artistique.
                - N'ajoute aucun texte hors JSON.
                """;

        JSONObject body = new JSONObject();
        body.put("model", MODEL);
        body.put("temperature", 0.2);
        body.put("max_completion_tokens", 700);
        body.put("response_format", new JSONObject().put("type", "json_object"));

        JSONArray messages = new JSONArray();
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");

        JSONArray content = new JSONArray();
        content.put(new JSONObject()
                .put("type", "text")
                .put("text", prompt));
        content.put(new JSONObject()
                .put("type", "image_url")
                .put("image_url", new JSONObject().put("url", dataUrl)));

        userMessage.put("content", content);
        messages.put(userMessage);
        body.put("messages", messages);

        return body.toString();
    }

    private String parseGroqResponse(String responseBody) {
        try {
            JSONObject root = new JSONObject(responseBody);
            JSONArray choices = root.getJSONArray("choices");
            if (choices.isEmpty()) {
                return buildErrorAnalysis("Aucune reponse retournee par le modele.");
            }

            JSONObject message = choices.getJSONObject(0).getJSONObject("message");
            String content = message.optString("content", "").trim();
            if (content.isEmpty()) {
                return buildErrorAnalysis("Reponse vide du modele.");
            }

            JSONObject resultJson = new JSONObject(content);
            return formatAnalysis(resultJson);
        } catch (Exception e) {
            System.err.println("Erreur parsing Groq: " + e.getMessage());
            return buildErrorAnalysis("Impossible de lire la reponse du modele.");
        }
    }

    private String formatAnalysis(JSONObject resultJson) {
        return "TITRE: " + normalizeField(resultJson, "TITRE") + "\n" +
                "Ã‰POQUE: " + normalizeField(resultJson, "EPOQUE") + "\n" +
                "ORIGINE: " + normalizeField(resultJson, "ORIGINE") + "\n" +
                "MATÃ‰RIAUX: " + normalizeField(resultJson, "MATERIAUX") + "\n" +
                "DESCRIPTION: " + normalizeField(resultJson, "DESCRIPTION") + "\n" +
                "IMPORTANCE: " + normalizeField(resultJson, "IMPORTANCE");
    }

    private String normalizeField(JSONObject resultJson, String key) {
        String value = resultJson.optString(key, "").trim();
        return value.isEmpty() ? "Non determine" : value;
    }

    private String buildErrorAnalysis(String message) {
        return "TITRE: Analyse indisponible\n" +
                "Ã‰POQUE: Non determine\n" +
                "ORIGINE: Non determine\n" +
                "MATÃ‰RIAUX: Non determine\n" +
                "DESCRIPTION: " + message + "\n" +
                "IMPORTANCE: Non determine";
    }

    private String readResponse(InputStream is) throws IOException {
        if (is == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}
