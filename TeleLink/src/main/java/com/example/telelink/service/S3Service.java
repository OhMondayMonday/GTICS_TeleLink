package com.example.telelink.service; // Paquete actualizado

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.UUID;

@Service
public class S3Service {

    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);

    private final S3Client s3Client;

    // Inyecta el nombre del bucket desde application.properties
    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Autowired
    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    /*
     * Sube un archivo al bucket S3.
     *
     * @param file El archivo MultipartFile a subir.
     * @return Un mensaje indicando el resultado de la operación, incluyendo la URL del objeto si fue exitoso.
     */
    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            logger.warn("Intento de subir un archivo nulo o vacío.");
            return "Error: El archivo está vacío o es nulo. Por favor seleccione un archivo válido.";
        }

        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        // Generar un nombre de archivo único para evitar colisiones y añadir la extensión original
        String fileNameInS3 = UUID.randomUUID().toString() + fileExtension;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileNameInS3)
                    .contentType(file.getContentType()) // Es buena práctica establecer el ContentType
                    // Opcional: Para hacer el objeto públicamente legible al subirlo
                    // (Requiere que el bucket no tenga bloqueado el acceso público a ACLs)
                    // .acl(ObjectCannedACL.PUBLIC_READ)
                    .build();

            // Subir el archivo usando un InputStream
            PutObjectResponse response = s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            logger.info("Archivo subido exitosamente a S3. Bucket: {}, Key: {}, ETag: {}", bucketName, fileNameInS3, response.eTag());

            // Construir la URL del objeto.
            // Esta URL es un formato estándar. El acceso dependerá de los permisos del bucket y del objeto.
            // Si el objeto no es público, esta URL no será accesible directamente sin autenticación o URLs prefirmadas.
            String fileUrl = String.format("https://%s.s3.%s.amazonaws.com/%s",
                    bucketName, s3Client.serviceClientConfiguration().region().id(), fileNameInS3);

            return "Archivo '" + originalFilename + "' subido exitosamente como '" + fileNameInS3 + "'. URL: " + fileUrl;

        } catch (S3Exception e) {
            // Captura errores específicos de AWS S3
            logger.error("Error de S3 al subir el archivo '{}': Código AWS [{}], Mensaje AWS [{}], ID de Solicitud [{}]",
                    originalFilename,
                    e.awsErrorDetails().errorCode(),
                    e.awsErrorDetails().errorMessage(),
                    e.requestId(),
                    e);
            return "Error de S3 al subir el archivo: " + e.awsErrorDetails().errorMessage() + " (Código: " + e.awsErrorDetails().errorCode() + ")";
        } catch (IOException e) {
            // Captura errores al leer el archivo de entrada
            logger.error("Error de IO al procesar el archivo para subir: {}", originalFilename, e);
            return "Error de IO al subir el archivo: " + e.getMessage();
        } catch (Exception e) {
            // Captura cualquier otra excepción inesperada
            logger.error("Error inesperado al subir el archivo '{}' a S3.", originalFilename, e);
            return "Error inesperado al subir el archivo: " + e.getMessage();
        }
    }

    // Elimina una foto y retorna un mensaje de comprobación
    public String deleteFile(String fileKey) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            DeleteObjectResponse response = s3Client.deleteObject(deleteRequest);

            // Puedes verificar el ETag o la metadata, aunque para delete no siempre viene algo útil
            logger.info("DeleteObjectResponse: {}", response);

            return "Archivo '" + fileKey + "' eliminado exitosamente de S3.";

        } catch (S3Exception e) {
            logger.error("Error de S3 al eliminar el archivo '{}': Código [{}], Mensaje [{}]",
                    fileKey, e.awsErrorDetails().errorCode(), e.awsErrorDetails().errorMessage(), e);
            return "Error al eliminar el archivo: " + e.awsErrorDetails().errorMessage();
        } catch (Exception e) {
            logger.error("Error inesperado al eliminar el archivo '{}' de S3.", fileKey, e);
            return "Error inesperado al eliminar el archivo: " + e.getMessage();
        }
    }
}
