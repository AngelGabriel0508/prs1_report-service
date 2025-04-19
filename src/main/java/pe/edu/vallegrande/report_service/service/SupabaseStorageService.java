package pe.edu.vallegrande.report_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
public class SupabaseStorageService {

    private final WebClient webClient;
    private final String bucket;
    private final String projectUrl;

    public SupabaseStorageService(
            @Value("${supabase.project-url}") String projectUrl,
            @Value("${supabase.api-key}") String apiKey,
            @Value("${supabase.bucket}") String bucket
    ) {
        this.projectUrl = projectUrl;
        this.bucket = bucket;
        this.webClient = WebClient.builder()
                .baseUrl(projectUrl + "/storage/v1")
                .defaultHeader("apikey", apiKey)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }

    public Mono<String> uploadBase64Image(String folder, String base64) {
        if (!StringUtils.hasText(base64)) return Mono.empty();

        try {
            String[] parts = base64.split(",");
            String contentType = parts[0];
            String extension = contentType.contains("image/png") ? ".png" :
                    contentType.contains("image/jpeg") ? ".jpg" :
                            contentType.contains("image/jpg") ? ".jpg" :
                                    contentType.contains("image/gif") ? ".gif" :
                                            contentType.contains("image/webp") ? ".webp" : "";

            if (extension.isEmpty()) {
                log.warn("❌ Formato de imagen no soportado: {}", contentType);
                return Mono.empty();
            }

            byte[] imageBytes = Base64.getDecoder().decode(parts[1]);
            String fileName = UUID.randomUUID() + extension;
            String path = folder + "/" + fileName;

            return webClient.put()
                    .uri(uriBuilder -> uriBuilder.path("/object/{bucket}/{path}")
                            .build(bucket, path))
                    .header("x-upsert", "true")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(BodyInserters.fromValue(imageBytes))
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(response -> projectUrl + "/storage/v1/object/public/" + bucket + "/" + path);

        } catch (Exception e) {
            log.error("❌ Error al subir imagen a Supabase:", e);
            return Mono.empty();
        }
    }

    public Mono<Void> deleteImage(String publicUrl) {
        if (!StringUtils.hasText(publicUrl)) return Mono.empty();

        String[] parts = publicUrl.split("/object/public/");
        if (parts.length < 2) return Mono.empty();
        String objectPath = parts[1];

        return webClient.delete()
                .uri(uriBuilder -> uriBuilder.path("/object/{bucket}/{path}")
                        .build(bucket, objectPath))
                .retrieve()
                .bodyToMono(Void.class);
    }
}
