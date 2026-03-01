server:
port: 8081

spring:
application:
name: user-service
datasource:
url: jdbc:mysql://db:3306/user_db?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false
username: root
password: root
driver-class-name: com.mysql.cj.jdbc.Driver
jpa:
database-platform: org.hibernate.dialect.MySQL8Dialect
hibernate:
ddl-auto: update
show-sql: true
        open-in-view: false

        eureka:
        client:
        service-url:
        defaultZone: http://discovery:8761/eureka/

        app:
        jwt:
        secret: "SuaChaveSecretaMuitoLongaEComplexaParaSegurancaDoJWT123456" # Troque por env var em prod
        expiration: 3600000