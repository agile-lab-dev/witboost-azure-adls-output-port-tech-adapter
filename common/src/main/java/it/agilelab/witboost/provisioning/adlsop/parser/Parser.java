package it.agilelab.witboost.provisioning.adlsop.parser;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import io.vavr.control.Either;
import io.vavr.control.Try;
import it.agilelab.witboost.provisioning.adlsop.common.FailedOperation;
import it.agilelab.witboost.provisioning.adlsop.common.Problem;
import it.agilelab.witboost.provisioning.adlsop.model.Component;
import it.agilelab.witboost.provisioning.adlsop.model.Descriptor;
import it.agilelab.witboost.provisioning.adlsop.model.azure.StorageAccountInfo;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Parser {

    private static final Logger logger = LoggerFactory.getLogger(Parser.class);

    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    static {
        mapper.registerModule(new Jdk8Module());
    }

    public static Either<FailedOperation, Descriptor> parseDescriptor(String yamlDescriptor) {

        return Try.of(() -> mapper.readValue(yamlDescriptor, Descriptor.class))
                .toEither()
                .mapLeft(throwable -> {
                    String errorMessage =
                            "Failed to deserialize the Yaml Descriptor. Details: " + throwable.getMessage();
                    logger.error(errorMessage, throwable);
                    return new FailedOperation(Collections.singletonList(new Problem(errorMessage, throwable)));
                });
    }

    public static <U> Either<FailedOperation, Component<U>> parseComponent(JsonNode node, Class<U> specificClass) {
        return Try.of(() -> {
                    JavaType javaType = mapper.getTypeFactory().constructParametricType(Component.class, specificClass);
                    return mapper.<Component<U>>readValue(node.toString(), javaType);
                })
                .toEither()
                .mapLeft(throwable -> {
                    String errorMessage = "Failed to deserialize the component. Details: " + throwable.getMessage();
                    logger.error(errorMessage, throwable);
                    return new FailedOperation(Collections.singletonList(new Problem(errorMessage, throwable)));
                });
    }

    public static Either<FailedOperation, List<StorageAccountInfo>> parseStorageAccountInfoList(Object node) {
        return Try.of(() -> {
                    JavaType javaType =
                            mapper.getTypeFactory().constructParametricType(List.class, StorageAccountInfo.class);
                    return mapper.<List<StorageAccountInfo>>convertValue(node, javaType);
                })
                .toEither()
                .mapLeft(throwable -> {
                    String errorMessage = "Failed to deserialize the component. Details: " + throwable.getMessage();
                    logger.error(errorMessage, throwable);
                    return new FailedOperation(Collections.singletonList(new Problem(errorMessage, throwable)));
                });
    }

    public static <T> Either<FailedOperation, T> parseObject(String object, Class<T> clazz) {
        return Try.of(() -> mapper.readTree(object))
                .toEither()
                .mapLeft(throwable -> {
                    String errorMessage = "Failed to deserialize object. Details: " + throwable.getMessage();
                    logger.error(errorMessage, throwable);
                    return new FailedOperation(Collections.singletonList(new Problem(errorMessage, throwable)));
                })
                .flatMap(node -> parseObject(node, clazz));
    }

    public static <T> Either<FailedOperation, T> parseObject(JsonNode node, Class<T> clazz) {
        return Try.of(() -> {
                    JavaType javaType = mapper.getTypeFactory().constructType(clazz);
                    return mapper.<T>treeToValue(node, javaType);
                })
                .toEither()
                .mapLeft(throwable -> {
                    String errorMessage = "Failed to deserialize the component. Details: " + throwable.getMessage();
                    logger.error(errorMessage, throwable);
                    return new FailedOperation(Collections.singletonList(new Problem(errorMessage, throwable)));
                });
    }
}
