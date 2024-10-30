package com.example.bedrockruntime.models.anthropicClaude;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Base64;

public class Converse {
    private static String imageToBase64(String imagePath) throws IOException {
        if (imagePath == null || imagePath.isEmpty()) {
            throw new IllegalArgumentException("Image path cannot be null or empty");
        }

        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            throw new FileNotFoundException("Image file not found: " + imagePath);
        }

        try {
            BufferedImage image = ImageIO.read(imageFile);
            if (image == null) {
                throw new IOException("Failed to read image: " + imagePath);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            String format = imagePath.substring(imagePath.lastIndexOf(".") + 1);
            
            if (!ImageIO.write(image, format, outputStream)) {
                throw new IOException("Failed to write image to output stream");
            }
            
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (SecurityException e) {
            throw new IOException("Security error accessing image file: " + e.getMessage(), e);
        }
    }

    private static String getImageFileType(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            throw new IllegalArgumentException("Image path cannot be null or empty");
        }
        
        String extension = imagePath.substring(imagePath.lastIndexOf(".") + 1).toLowerCase();
        if (extension.isEmpty()) {
            throw new IllegalArgumentException("Invalid image file extension");
        }
        
        return extension;
    }

    public static String readFile(String filePath) throws IOException {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }

        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + filePath);
        }

        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        } catch (SecurityException e) {
            throw new IOException("Security error accessing file: " + e.getMessage(), e);
        }
    }
    
    public static String converse(String imagePath, String promptPath) throws IOException {
        if (imagePath == null || promptPath == null) {
            throw new IllegalArgumentException("Image path and prompt path cannot be null");
        }

        BedrockRuntimeClient client = null;
        try {
            client = BedrockRuntimeClient.builder()
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .region(Region.US_WEST_2)
                    .build();

            String modelId = "us.anthropic.claude-3-5-sonnet-20241022-v2:0";
            String inputText = readFile(promptPath);
            String base64Image = imageToBase64(imagePath);
            String fileType = getImageFileType(imagePath);

            ContentBlock imageContent = ContentBlock.builder()
                .image(ImageBlock.builder()
                    .format(fileType)
                    .source(ImageSource.builder()
                        .bytes(SdkBytes.fromByteArray(Base64.getDecoder().decode(base64Image)))
                        .build())
                    .build())
                .build();
                    
            Message message = Message.builder()
                .content(ContentBlock.fromText(inputText), imageContent)
                .role(ConversationRole.USER)
                .build();

            ConverseResponse response = client.converse(request -> request
                    .modelId(modelId)
                    .messages(message)
                    .inferenceConfig(config -> config
                            .maxTokens(4096)
                            .temperature(0.5F)
                            .topP(0.9F)));

            return response.output().message().content().get(0).text();

        } catch (SdkClientException e) {
            throw new RuntimeException("AWS SDK client error: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid argument: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error: " + e.getMessage(), e);
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    public static void main(String[] args) {
        try {
            String promptPath = "/home/ec2-user/bedrock-runtime/src/main/java/com/example/bedrockruntime/models/anthropicClaude/prompt.txt";
            String imagePath = "/home/ec2-user/bedrock-runtime/test.png";
            
            // Validate file paths
            if (!new File(promptPath).exists()) {
                throw new FileNotFoundException("Prompt file not found: " + promptPath);
            }
            if (!new File(imagePath).exists()) {
                throw new FileNotFoundException("Image file not found: " + imagePath);
            }

            String response = converse(imagePath, promptPath);
            System.out.println("Response from Claude:");
            System.out.println(response);

        } catch (FileNotFoundException e) {
            System.err.println("File error: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("IO error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}