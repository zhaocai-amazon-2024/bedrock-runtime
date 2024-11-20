mvn clean compile

# 打包项目
mvn package

java -cp target/BedrockRuntime-1.0-SNAPSHOT.jar com.example.bedrockruntime.models.anthropicClaude.Converse


