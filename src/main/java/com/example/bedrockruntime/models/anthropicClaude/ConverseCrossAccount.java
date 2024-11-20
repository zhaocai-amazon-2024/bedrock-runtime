package com.example.bedrockruntime.models.anthropicClaude;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;

/**
 * 与Amazon Bedrock Claude模型进行对话的类
 */
public class ConverseCrossAccount {

/**
     * 将字符串转换为JSON格式并验证
     * @param inputString 输入的字符串
     * @return 转换结果对象
     */
    public static Object convertStringToJson(String inputString) {
        if (inputString == null || inputString.trim().isEmpty()) {
            return null;
        }

        try {
            // 创建Gson实例,设置不转义HTML,美化输出
            Gson gson = new GsonBuilder()
                    .disableHtmlEscaping()
                    .setPrettyPrinting()
                    .create();

            // 验证是否为有效的JSON格式
            JsonParser.parseString(inputString);
            
            // 格式化JSON字符串
            Object jsonObject = gson.fromJson(inputString, Object.class);
            // String formattedJson = gson.toJson(jsonObject);
            
            return jsonObject;
            
        } catch (Exception e) {
            throw new RuntimeException("string转换json失败: " + e.getMessage(), e);
        }
        
    }

    // 支持的图片格式集合
    private static final Set<String> SUPPORTED_IMAGE_FORMATS = new HashSet<String>() {{
        add("gif");
        add("jpeg");
        add("jpg"); // jpg也是jpeg格式
        add("png");
        add("webp");
    }};

    /**
     * 验证图片格式是否支持
     * @param fileType 图片格式
     * @throws IllegalArgumentException 当图片格式不支持时抛出
     */
    private static void validateImageFormat(String fileType) {
        if (!SUPPORTED_IMAGE_FORMATS.contains(fileType.toLowerCase())) {
            throw new IllegalArgumentException(
                "Unsupported image format: " + fileType + 
                ". Supported formats are: " + String.join(", ", SUPPORTED_IMAGE_FORMATS)
            );
        }
    }

    /**
     * 将图片文件转换为Base64编码字符串
     * @param imagePath 图片文件路径
     * @return Base64编码的图片字符串
     * @throws IOException 当图片读取或转换失败时抛出
     */
    private static String imageToBase64(String imagePath) throws IOException {
        if (imagePath == null || imagePath.isEmpty()) {
            throw new IllegalArgumentException("Image path cannot be null or empty");
        }

        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            throw new FileNotFoundException("Image file not found: " + imagePath);
        }

        String fileType = getImageFileType(imagePath);
        validateImageFormat(fileType);

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

    /**
     * 获取图片文件的格式类型
     * @param imagePath 图片文件路径
     * @return 图片格式(如png, jpg等)
     */
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

    public static String getSecret(String secretName, Region region) {

        // String secretName = "bedrock_ak_sk";
        // Region region = Region.of(str_region);
    
        // Create a Secrets Manager client
        SecretsManagerClient client = SecretsManagerClient.builder()
                .region(region)
                .build();
    
        GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest.builder()
                .secretId(secretName)
                .build();
    
        GetSecretValueResponse getSecretValueResponse;
    
        try {
            getSecretValueResponse = client.getSecretValue(getSecretValueRequest);
        } catch (Exception e) {
            // For a list of exceptions thrown, see
            // https://docs.aws.amazon.com/secretsmanager/latest/apireference/API_GetSecretValue.html
            throw e;
        }
    
        String secret = getSecretValueResponse.secretString();

        return secret;
    
        // Your code goes here.
    }

    /**
     * 读取文本文件内容
     * @param filePath 文件路径
     * @return 文件内容字符串
     * @throws IOException 当文件读取失败时抛出
     */
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
    
    /**
     * 与Claude模型进行对话
     * @param imagePath 图片文件路径
     * @param promptPath 提示文本文件路径
     * @return 模型的响应文本
     * @throws IOException 当文件操作失败时抛出
     */
    public static String converse(String imagePath, String promptPath) throws IOException {
        if (imagePath == null || promptPath == null) {
            throw new IllegalArgumentException("Image path and prompt path cannot be null");
        }
        String secretName = "bedrock_ak_sk";
        Region region = Region.of("us-west-2");
        // String secrets = getSecret(secretName, region);

        // Gson gson = new GsonBuilder()
        // .disableHtmlEscaping()
        // .setPrettyPrinting()
        // .create();
        // Object jsonObject = convertStringToJson(secrets);
        // JsonObject json = gson.toJsonTree(jsonObject).getAsJsonObject();
        // String skName = "aws_secret_access_key";
        // String akName = "aws_access_key_id";
        // JsonElement element_ak = json.get(akName);
        // JsonElement element_sk = json.get(skName);
        // System.out.println(String.format("aws_access_key_id: %s ", element_ak.toString()));
        // System.out.println(String.format("aws_secret_access_key: %s ", element_sk.toString()));
        // AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(
        //     element_ak.toString(),     // 替换为您的访问密钥ID
        //     element_sk.toString() // 替换为您的私有访问密钥
        // );
        StsClient stsClient = StsClient.builder()
        .region(Region.US_EAST_1)
        .build();

        AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder()
                .roleArn("arn:aws:iam::713978751793:role/cross-bedrock")
                .roleSessionName("bedrock-cross-account-session")
                .durationSeconds(3600)
                .build();
        AssumeRoleResponse assumeRoleResponse = stsClient.assumeRole(assumeRoleRequest);
        AwsSessionCredentials credentials = AwsSessionCredentials.create(
                assumeRoleResponse.credentials().accessKeyId(),
                assumeRoleResponse.credentials().secretAccessKey(),
                assumeRoleResponse.credentials().sessionToken()
        );
        BedrockRuntimeClient client = null;
        try {
            // 创建Bedrock运行时客户端
            client = BedrockRuntimeClient.builder()
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .region(Region.US_EAST_1)
                    .build();

            // 设置模型ID和参
            //us.anthropic.claude-3-5-sonnet-20241022-v2:0
            //US_WEST_2
            //eu.anthropic.claude-3-5-sonnet-20240620-v1:0
            // EU_CENTRAL_1
            String modelId = "anthropic.claude-3-5-sonnet-20240620-v1:0";
            String inputText = readFile(promptPath);
            String base64Image = imageToBase64(imagePath);
            String fileType = getImageFileType(imagePath);

            // 验证图片格式
            validateImageFormat(fileType);

            // 构建图片内容块
            ContentBlock imageContent = ContentBlock.builder()
                .image(ImageBlock.builder()
                    .format(fileType)
                    .source(ImageSource.builder()
                        .bytes(SdkBytes.fromByteArray(Base64.getDecoder().decode(base64Image)))
                        .build())
                    .build())
                .build();
                    
            // 构建消息对象
            Message message = Message.builder()
                .content(ContentBlock.fromText(inputText), imageContent)
                .role(ConversationRole.USER)
                .build();

            // 发送请求并获取响应
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


    /**
     * 主方法
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        try {
            // 设置文件路径
            // String promptPath = "/app/data/prompt.txt";
            // String imagePath = "/app/data/test.png";
            String promptPath = "/home/ec2-user/bedrock-runtime/src/main/java/com/example/bedrockruntime/models/anthropicClaude/prompt.txt";
            String imagePath = "/home/ec2-user/bedrock-runtime/test.png";
            // 验证文件是否存在
            if (!new File(promptPath).exists()) {
                throw new FileNotFoundException("Prompt file not found: " + promptPath);
            }
            if (!new File(imagePath).exists()) {
                throw new FileNotFoundException("Image file not found: " + imagePath);
            }

            // 调用对话方法并打印响应
            String response = converse(imagePath, promptPath);
            System.out.println("Response from Claude:");
            System.out.println(response);
            Gson gson = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();
            Object jsonObject = convertStringToJson(response);
            JsonObject json = gson.toJsonTree(jsonObject).getAsJsonObject();
            String fieldName = "number";
            try {
                // 获取指定字段的值
                
                JsonElement element = json.get(fieldName);
                
                if (element == null) {
                    System.out.println(String.format("字段 '%s' 不存在", fieldName));
                }
                // 返回格式化的结果
                System.out.println(String.format("字段名: %s\n值: %s", fieldName, element.toString()));
                int num = element.getAsInt();
                if(num > 0) {
                    System.out.println(String.format("设计图纸: %s 是螺纹图纸", imagePath));
                } else {
                   System.out.println(String.format("设计图纸: %s 不是是螺纹图纸", imagePath));

                }

            } catch (Exception e) {
                System.out.println(String.format("获取字段 '%s' 时发生错误: %s", fieldName, e.getMessage()));
            }
            // System.out.println("number:");
            // System.out.println(jsonObject);

            // String formattedJson = gson.toJson(jsonObject);
            // System.out.println("Json Formate:");
            // System.out.println(formattedJson);


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