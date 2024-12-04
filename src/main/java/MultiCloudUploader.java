import io.minio.MinioClient;
import io.minio.PutObjectArgs;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MultiCloudUploader {
    public static void main(String[] args) throws IOException {

        Properties properties = new Properties();

        String gcsAccessKey = null;
        String gcsSecretKey = null;
        String awsAccessKey = null;
        String awsSecretKey = null;

        // Load properties file directory
        try (InputStream inputStream = MultiCloudUploader.class.getClassLoader().getResourceAsStream("aws-gcs-config.properties")) {
            if (inputStream == null) {
                System.out.println("Could not find aws-gcs-config.properties in the resources directory.");
                return;
            }
            properties.load(inputStream);
            System.out.println("Properties loaded successfully!");

            // Access properties
            gcsAccessKey = properties.getProperty("gcs.accessKey");
            gcsSecretKey = properties.getProperty("gcs.secretKey");
            awsAccessKey = properties.getProperty("aws.accessKey");
            awsSecretKey = properties.getProperty("aws.secretKey");

        } catch (Exception e) {
            System.out.println("Error occurred while loading properties file: " + e.getMessage());
            e.printStackTrace();
        }

        // File to upload
        String filePath = "block.txt";     // Relative to the resources directory

        // AWS S3 Configuration
        String awsEndpoint = "https://s3.amazonaws.com";
        String awsBucketName = "hedera-blocks-bucket";

        // GCS Configuration (using S3-compatible GCS endpoint)
        String gcsEndpoint = "https://storage.googleapis.com";
        String gcsBucketName = "hedera-blocks-bucket";


        try {
            // Get the file as a resource
            InputStream inputStream = MultiCloudUploader.class.getClassLoader().getResourceAsStream(filePath);
            if (inputStream == null) {
                System.err.println("File not found in resources: " + filePath);
                return;
            }
            System.out.println("File successfully loaded from resources: " + filePath);

            String fileName = "uploaded-file.txt";              // File name in the bucket

            // Upload to AWS S3
            uploadToCloud(awsEndpoint, awsAccessKey, awsSecretKey, awsBucketName, inputStream, fileName);
            System.out.println("File uploaded to AWS S3 successfully.");

            // Upload to GCS
            uploadToCloud(gcsEndpoint, gcsAccessKey, gcsSecretKey, gcsBucketName, inputStream, fileName);
            System.out.println("File uploaded to GCS successfully.");

        } catch (Exception e) {
            System.err.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Uploads a file to the specified cloud storage.
     *
     * @param endpoint     The endpoint of the cloud storage.
     * @param accessKey    The access key for authentication.
     * @param secretKey    The secret key for authentication.
     * @param bucketName   The name of the bucket to upload the file.
     * @param fileName     The name to save the file as in the bucket.
     * @throws Exception if an error occurs during upload.
     */
    private static void uploadToCloud(String endpoint, String accessKey, String secretKey,
                                      String bucketName, InputStream inputStream, String fileName) throws Exception {
        // Create a MinIO client
        MinioClient minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();

        // Upload file to the bucket
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .stream(inputStream, inputStream.available(), -1)
                        .contentType("application/octet-stream")
                        .build()
        );
    }
}
