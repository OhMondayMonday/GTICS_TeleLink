package com.example.telelink.config; // Paquete actualizado

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {

    // Inyecta la región desde application.properties
    @Value("${aws.s3.region}")
    private String awsRegionString;

    @Bean
    public S3Client s3Client() {
        // Convierte la cadena de la región a un objeto Region de AWS SDK
        Region region = Region.of(awsRegionString);

        // DefaultCredentialsProvider buscará credenciales en el siguiente orden:
        // 1. Variables de entorno Java (aws.accessKeyId, aws.secretKey, aws.sessionToken).
        // 2. Variables de entorno del sistema (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_SESSION_TOKEN).
        // 3. Web Identity Token credentials desde el sistema de archivos.
        // 4. Archivo de credenciales compartido de AWS (~/.aws/credentials y ~/.aws/config).
        // 5. Credenciales de contenedor de Amazon ECS.
        // 6. Perfil de instancia de IAM (para EC2, EKS, etc.).
        // Esto permite que funcione tanto localmente (con archivo de credenciales o variables de entorno)
        // como en EC2 (con rol IAM) sin cambiar el código.
        return S3Client.builder()
                .region(region)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
