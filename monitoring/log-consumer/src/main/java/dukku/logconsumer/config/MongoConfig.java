package dukku.logconsumer.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;

@Configuration
/**
 * log-consumer가 환경별 MongoDB 연결 대상을 해석하도록 하는 설정입니다.
 */
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.host:${MONGO_HOST:mongodb-service}}")
    private String host;

    @Value("${spring.data.mongodb.port:${MONGO_PORT:27017}}")
    private int port;

    @Value("${spring.data.mongodb.database:log_service}")
    private String database;

    @Override
    protected String getDatabaseName() {
        return database;
    }

    @Override
    public MongoClient mongoClient() {
        return MongoClients.create("mongodb://" + host + ":" + port);
    }
}
